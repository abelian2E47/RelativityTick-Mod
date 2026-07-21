package com.abelian;

import com.abelian.config.RelativityTickConfig;
import com.abelian.network.EntityStateRecord;
import com.abelian.network.RegionEntitySyncPayload;
import com.abelian.network.RegionStepPayload;
import com.abelian.network.RegionSyncPayload;
import com.abelian.regionTick.RegionTickManager;
import com.abelian.regionTick.RegionsManager;
import com.abelian.regionTick.RegionTickManager.RegionState;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.fluid.Fluid;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.tick.WorldTickScheduler;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class ServerCommands {

    private record RegionCommandContext(ServerCommandSource source, RegionTickManager manager, String id, ServerWorld world) {
        public static RegionCommandContext of(CommandContext<ServerCommandSource> ctx) {
            ServerCommandSource source = ctx.getSource();
            String id = StringArgumentType.getString(ctx, "id");
            RegionTickManager manager = RegionsManager.getRegion(id);
            ServerWorld world = manager == null ? source.getWorld() : source.getServer().getWorld(manager.getDimension());
            return new RegionCommandContext(source, manager, id, world == null ? source.getWorld() : world);
        }

        public static RegionCommandContext current(CommandContext<ServerCommandSource> ctx) {
            ServerCommandSource source = ctx.getSource();
            BlockPos pos = BlockPos.ofFloored(source.getPosition());
            String id = RegionsManager.getRegionIdByChunk(source.getWorld(), net.minecraft.util.math.ChunkPos.toLong(pos));
            return new RegionCommandContext(source, id == null ? null : RegionsManager.getRegion(id), id, source.getWorld());
        }

        public boolean isInvalid() {
            if (manager == null) {
                if (id == null) {
                    source.sendError(Text.translatable("relativitytick.command.error.not_in_region").formatted(Formatting.RED));
                } else {
                    source.sendError(Text.translatable("relativitytick.command.error.region_not_found", id).formatted(Formatting.RED));
                }
                return true;
            }
            return false;
        }
    }

    private static RequiredArgumentBuilder<ServerCommandSource, String> regionId() {
        return CommandManager.argument("id", StringArgumentType.string()).suggests(ServerCommands::suggestRegions);
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("relativityTick")
                .executes(ctx -> showRelativityTickConfig(ctx.getSource()))
                .then(CommandManager.literal("maxMspt")
                        .executes(ctx -> showRelativityTickConfig(ctx.getSource()))
                        .then(CommandManager.argument("value", DoubleArgumentType.doubleArg(1.0, 50.0))
                                .requires(source -> source.hasPermissionLevel(2))
                                .executes(ctx -> setMaxMspt(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "value"))))));

        dispatcher.register(CommandManager.literal("regionManager")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("remove")
                        .then(regionId().executes(ctx -> executeRemove(RegionCommandContext.of(ctx)))))
                .then(CommandManager.literal("status")
                        .executes(ctx -> getAllRegionStatus(ctx.getSource()))
                        .then(regionId().executes(ctx -> getRegionStatus(RegionCommandContext.of(ctx)))))
                .then(CommandManager.literal("parameter")
                        .then(CommandManager.literal("priority")
                                .then(regionId()
                                        .then(CommandManager.argument("value", IntegerArgumentType.integer(1))
                                                .executes(ctx -> setPriority(RegionCommandContext.of(ctx), IntegerArgumentType.getInteger(ctx, "value"))))))
                        .then(CommandManager.literal("tickDurationLimit")
                                .then(regionId()
                                        .then(CommandManager.argument("value", DoubleArgumentType.doubleArg(1.0))
                                                .executes(ctx -> setTickDurationLimit(RegionCommandContext.of(ctx), DoubleArgumentType.getDouble(ctx, "value"))))))));

        dispatcher.register(CommandManager.literal("regionTick")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("takeover")
                        .executes(ctx -> toggleTakeover(RegionCommandContext.current(ctx)))
                        .then(regionId().executes(ctx -> toggleTakeover(RegionCommandContext.of(ctx)))))
                .then(CommandManager.literal("freeze")
                        .executes(ctx -> toggleFreezeRegion(RegionCommandContext.current(ctx)))
                        .then(regionId().executes(ctx -> toggleFreezeRegion(RegionCommandContext.of(ctx)))))
                .then(CommandManager.literal("step")
                        .executes(ctx -> stepRegion(RegionCommandContext.current(ctx), 1, false))
                        .then(CommandManager.argument("ticks", IntegerArgumentType.integer(1))
                                .executes(ctx -> stepRegion(RegionCommandContext.current(ctx), IntegerArgumentType.getInteger(ctx, "ticks"), false)))
                        .then(regionId()
                                .executes(ctx -> stepRegion(RegionCommandContext.of(ctx), 1, false))
                                .then(CommandManager.argument("ticks", IntegerArgumentType.integer(1))
                                        .executes(ctx -> stepRegion(RegionCommandContext.of(ctx), IntegerArgumentType.getInteger(ctx, "ticks"), false)))))
                .then(CommandManager.literal("dash")
                        .executes(ctx -> stepRegion(RegionCommandContext.current(ctx), 1, true))
                        .then(CommandManager.argument("ticks", IntegerArgumentType.integer(1, 1000))
                                .executes(ctx -> stepRegion(RegionCommandContext.current(ctx), IntegerArgumentType.getInteger(ctx, "ticks"), true)))
                        .then(regionId()
                                .executes(ctx -> stepRegion(RegionCommandContext.of(ctx), 1, true))
                                .then(CommandManager.argument("ticks", IntegerArgumentType.integer(1, 1000))
                                        .executes(ctx -> stepRegion(RegionCommandContext.of(ctx), IntegerArgumentType.getInteger(ctx, "ticks"), true)))))
                .then(CommandManager.literal("rate")
                        .executes(ctx -> setRate(RegionCommandContext.current(ctx), 20))
                        .then(CommandManager.argument("rate", DoubleArgumentType.doubleArg(0.1, 10000))
                                .executes(ctx -> setRate(RegionCommandContext.current(ctx), DoubleArgumentType.getDouble(ctx, "rate"))))
                        .then(regionId()
                                .executes(ctx -> setRate(RegionCommandContext.of(ctx), 20))
                                .then(CommandManager.argument("rate", DoubleArgumentType.doubleArg(0.1, 10000))
                                        .executes(ctx -> setRate(RegionCommandContext.of(ctx), DoubleArgumentType.getDouble(ctx, "rate")))))));

    }

    private static int showRelativityTickConfig(ServerCommandSource source) {
        source.sendFeedback(() -> Text.translatable("relativitytick.command.config.max_mspt", formatConfigValue(RelativityTickConfig.getMaxMspt())), false);
        return 1;
    }

    private static int setMaxMspt(ServerCommandSource source, double value) {
        try {
            RelativityTickConfig.setMaxMspt(value);
            return showRelativityTickConfig(source);
        } catch (IOException e) {
            source.sendError(Text.translatable("relativitytick.command.error.config_save_failed").formatted(Formatting.RED));
            return 0;
        }
    }

    private static String formatConfigValue(double value) {
        return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static CompletableFuture<Suggestions> suggestRegions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        RegionsManager.getRegionIds().forEach(builder::suggest);
        return builder.buildFuture();
    }

    private static int toggleFreezeRegion(RegionCommandContext rcc) {
        if (rcc.isInvalid()) return 0;
        boolean takenOver = ensureTakenOver(rcc);

        if (takenOver || rcc.manager.isRunning()) {
            rcc.manager.setState(RegionState.FROZEN);
            sendFeedback(rcc.source, rcc.id, takenOver ? "relativitytick.command.region.taken_over_frozen" : "relativitytick.command.region.frozen");
            sendEntitySyncPayload(rcc, rcc.manager.collectEntityStates(rcc.world));
        } else {
            int canceledSteps = rcc.manager.getPendingSteps();
            if (canceledSteps > 0) {
                rcc.manager.setPendingSteps(0);
            }
            rcc.manager.setState(RegionState.RUNNING);
            if (canceledSteps > 0) {
                sendFeedback(rcc.source, rcc.id, "relativitytick.command.region.unfrozen_canceled", formatConfigValue(rcc.manager.getRate()), canceledSteps);
            } else {
                sendFeedback(rcc.source, rcc.id, "relativitytick.command.region.unfrozen", formatConfigValue(rcc.manager.getRate()));
            }
        }

        RegionsManager.savePersistentState();
        syncRegionState(rcc);
        return 1;
    }

    private static int toggleTakeover(RegionCommandContext rcc) {
        if (rcc.isInvalid()) return 0;

        if (!rcc.manager.isControlled()) {
            takeOver(rcc);
            sendFeedback(rcc.source, rcc.id, "relativitytick.command.region.taken_over");
        } else {
            release(rcc);
            sendFeedback(rcc.source, rcc.id, "relativitytick.command.region.released");
        }

        RegionsManager.savePersistentState();
        syncRegionState(rcc);
        return 1;
    }

    private static boolean ensureTakenOver(RegionCommandContext rcc) {
        if (rcc.manager.isControlled()) return false;
        takeOver(rcc);
        return true;
    }

    private static void takeOver(RegionCommandContext rcc) {
        WorldTickScheduler<Block> blockScheduler = rcc.world.getBlockTickScheduler();
        WorldTickScheduler<Fluid> fluidScheduler = rcc.world.getFluidTickScheduler();

        rcc.manager.setFreezeStartTime(rcc.world.getTime());
        rcc.manager.takeOverRegion(blockScheduler, rcc.world.getTime());
        rcc.manager.takeOverRegion(fluidScheduler, rcc.world.getTime());
        rcc.manager.setState(RegionState.FROZEN);
    }

    private static void release(RegionCommandContext rcc) {
        WorldTickScheduler<Block> blockScheduler = rcc.world.getBlockTickScheduler();
        WorldTickScheduler<Fluid> fluidScheduler = rcc.world.getFluidTickScheduler();

        rcc.manager.releaseRegion(blockScheduler, rcc.world.getTime());
        rcc.manager.releaseRegion(fluidScheduler, rcc.world.getTime());
        rcc.manager.setState(RegionState.RELEASED);
        rcc.manager.setPendingSteps(0);
        rcc.manager.setAccumulator(0.0);
    }

    private static int stepRegion(RegionCommandContext rcc, int steps, boolean dash) {
        if (rcc.isInvalid()) return 0;
        if (!rcc.manager.isControlled()) {
            rcc.source.sendError(Text.translatable("relativitytick.command.error.region_must_be_taken_over").formatted(Formatting.RED));
            return 0;
        }
        if (rcc.manager.isRunning()) {
            rcc.source.sendError(Text.translatable("relativitytick.command.error.region_must_be_frozen").formatted(Formatting.RED));
            return 0;
        }

        if (!dash) {
            int totalPending = rcc.manager.getPendingSteps() + steps;
            rcc.manager.setPendingSteps(totalPending);
            sendFeedback(rcc.source, rcc.id, "relativitytick.command.region.stepping", steps);
            syncRegionState(rcc);
            return 1;
        }

        BiConsumer<BlockPos, Block> blockTicker = (pos, block) -> rcc.world.getBlockState(pos).scheduledTick(rcc.world, pos, rcc.world.getRandom());
        BiConsumer<BlockPos, Fluid> fluidTicker = (pos, fluid) -> rcc.world.getFluidState(pos).onScheduledTick(rcc.world, pos, fluid.getDefaultState().getBlockState());

        Map<Integer, EntityStateRecord> entityStates = new LinkedHashMap<>();
        for (int i = 0; i < steps; i++) {
            for (EntityStateRecord state : rcc.manager.stepRegion(rcc.world, rcc.world.getBlockTickScheduler(), blockTicker, rcc.world.getFluidTickScheduler(), fluidTicker)) {
                entityStates.put(state.entityId(), state);
            }
        }

        sendFeedback(rcc.source, rcc.id, "relativitytick.command.region.stepped", steps);
        syncRegionState(rcc);
        sendStepPayload(rcc, steps);
        sendEntitySyncPayload(rcc, new ArrayList<>(entityStates.values()));
        return 1;
    }

    private static int setRate(RegionCommandContext rcc, double rate) {
        if (rcc.isInvalid()) return 0;
        boolean wasControlled = rcc.manager.isControlled();
        boolean wasRunning = rcc.manager.isRunning();
        if (!wasControlled) {
            takeOver(rcc);
            toggleFreezeRegion(rcc);
        }
        rcc.manager.setRate(rate);
        rcc.manager.setState(wasRunning ? RegionState.RUNNING : RegionState.FROZEN);
        RegionsManager.savePersistentState();
        sendFeedback(rcc.source, rcc.id, wasControlled ? "relativitytick.command.region.rate_set" : "relativitytick.command.region.taken_over_rate_set", formatConfigValue(rate));
        syncRegionState(rcc);
        return 1;
    }

    private static int setPriority(RegionCommandContext rcc, int priority) {
        if (rcc.isInvalid()) return 0;
        if (!RegionsManager.isPriorityAvailable(priority, rcc.id)) {
            rcc.source.sendError(Text.translatable("relativitytick.command.error.priority_used", priority).formatted(Formatting.RED));
            return 0;
        }

        RegionsManager.setRegionPriority(rcc.id, priority);
        sendFeedback(rcc.source, rcc.id, "relativitytick.command.region.priority_set", priority);
        return 1;
    }

    private static int setTickDurationLimit(RegionCommandContext rcc, double tickDurationLimit) {
        if (rcc.isInvalid()) return 0;
        RegionsManager.setRegionTickDurationLimit(rcc.id, tickDurationLimit);
        sendFeedback(rcc.source, rcc.id, "relativitytick.command.region.tick_duration_limit_set", String.format("%.3f ms", tickDurationLimit));
        return 1;
    }

    private static int executeRemove(RegionCommandContext rcc) {
        if (rcc.isInvalid()) return 0;
        if (rcc.manager.isControlled()) release(rcc);
        RegionsManager.removeRegion(rcc.id);

        RegionSyncPayload payload = new RegionSyncPayload(rcc.id, rcc.manager.getDimensionId(), java.util.Collections.emptySet(), false, false, false, 20, rcc.manager.getVirtualTime());
        sendToWorldPlayers(rcc.world, payload);

        rcc.source.sendFeedback(() -> Text.translatable("relativitytick.command.region.removed", Text.literal(rcc.id).formatted(Formatting.GOLD)), false);
        return 1;
    }


    private static int getAllRegionStatus(ServerCommandSource source) {
        if (RegionsManager.getRegionIds().isEmpty()) {
            source.sendFeedback(() -> Text.translatable("relativitytick.command.status.no_regions"), false);
            return 0;
        }

        RegionsManager.getRegionIdsByPriority().forEach(id -> sendRegionStatus(source, id, RegionsManager.getRegion(id)));
        return 1;
    }

    private static int getRegionStatus(RegionCommandContext rcc) {
        if (rcc.isInvalid()) return 0;

        sendRegionStatus(rcc.source, rcc.id, rcc.manager);
        return 1;
    }

    private static void sendRegionStatus(ServerCommandSource source, String id, RegionTickManager mgr) {
        boolean controlled = mgr.isControlled();
        boolean running = controlled && mgr.isRunning();
        int pending = mgr.getPendingSteps();
        String stateKey = !controlled ? "relativitytick.command.state.released" : running ? "relativitytick.command.state.running" : pending > 0 ? "relativitytick.command.state.stepping" : "relativitytick.command.state.frozen";

        source.sendFeedback(() -> Text.translatable("relativitytick.command.status.header",
                Text.literal(id).formatted(Formatting.AQUA)), false);
        source.sendFeedback(() -> Text.translatable("relativitytick.command.status.state",
                Text.translatable(stateKey).formatted(stateFormatting(stateKey))), false);
        source.sendFeedback(() -> Text.translatable("relativitytick.command.status.chunks",
                Text.literal(String.valueOf(mgr.getChunkPositions().size())).formatted(Formatting.AQUA)), false);

        source.sendFeedback(() -> Text.translatable("relativitytick.command.status.priority",
                Text.literal(String.valueOf(mgr.getRegionPriority())).formatted(Formatting.GOLD)), false);
        double regionTickDuration = mgr.getRegionTickDuration();
        double regionTickDurationLimit = mgr.getTickDurationLimit();
        double costRatio = regionTickDuration / regionTickDurationLimit;
        int costColor = costRatio >= 0.9 ? 0xFF5555 : costRatio >= 0.6 ? 0xFFA500 : 0x55FF55;

        if (running) {
            source.sendFeedback(() -> Text.translatable("relativitytick.command.status.tps_rate",
                    Text.literal(String.format("%.2f", mgr.getLastMeasuredTPS())).formatted(Formatting.GREEN),
                    Text.literal(String.format("%.2f", mgr.getRate())).formatted(Formatting.GREEN)), false);

        } else {
            source.sendFeedback(() -> Text.translatable("relativitytick.command.status.region_tps_rate",
                    Text.literal("-").formatted(Formatting.GRAY),
                    Text.literal(String.format("%.2f", mgr.getRate())).formatted(Formatting.GREEN)), false);
        }

        source.sendFeedback(() -> Text.translatable("relativitytick.command.status.tick_duration_limit",
                Text.literal(String.format("%.3f", regionTickDuration) + "ms").styled(style -> style.withColor(costColor)),
                Text.literal(String.format("%.3f", regionTickDurationLimit) + "ms").formatted(Formatting.GREEN)), false);

        if (controlled && !running) {
            source.sendFeedback(() -> Text.translatable("relativitytick.command.status.pending_steps",
                    Text.literal(String.valueOf(pending)).formatted(pending > 0 ? Formatting.GOLD : Formatting.GRAY)), false);
        }

        //达到限制提醒
        if (mgr.hasReachedMsptLimit()) {
            source.sendFeedback(() -> Text.translatable("relativitytick.command.warning.mspt_slowdown").formatted(Formatting.ITALIC, Formatting.RED), false);
        }else if (mgr.hasReachedTickDurationLimit()){
            source.sendFeedback(() -> Text.translatable("relativitytick.command.warning.tick_duration_slowdown").formatted(Formatting.ITALIC, Formatting.RED), false);
        }
    }

    private static Formatting stateFormatting(String stateKey) {
        return switch (stateKey) {
            case "relativitytick.command.state.running" -> Formatting.GREEN;
            case "relativitytick.command.state.stepping" -> Formatting.GOLD;
            case "relativitytick.command.state.frozen" -> Formatting.AQUA;
            default -> Formatting.GRAY;
        };
    }

    private static void syncRegionState(RegionCommandContext rcc) {
        RegionSyncPayload payload = new RegionSyncPayload(rcc.id, rcc.manager.getDimensionId(), rcc.manager.getChunkPositions(),
                rcc.manager.isControlled(), rcc.manager.isRunning(), rcc.manager.getPendingSteps() > 0,
                rcc.manager.getRate(), rcc.manager.getVirtualTime());
        sendToWorldPlayers(rcc.world, payload);
    }

    private static void sendStepPayload(RegionCommandContext rcc, int steps) {
        RegionStepPayload payload = new RegionStepPayload(rcc.id,  steps, rcc.manager.getAccumulator(), rcc.manager.getVirtualTime());
        sendToWorldPlayers(rcc.world, payload);
    }

    private static void sendEntitySyncPayload(RegionCommandContext rcc, java.util.List<EntityStateRecord> entityStates) {
        RegionEntitySyncPayload payload = new RegionEntitySyncPayload(rcc.id, entityStates);
        sendToWorldPlayers(rcc.world, payload);
    }

    private static void sendToWorldPlayers(ServerWorld world, net.minecraft.network.packet.CustomPayload payload) {
        world.getPlayers().forEach(player -> ServerPlayNetworking.send(player, payload));
    }

    private static void sendFeedback(ServerCommandSource source, String id, String translationKey, Object... args) {
        Object[] translationArgs = new Object[args.length + 1];
        translationArgs[0] = Text.literal(id).formatted(Formatting.AQUA);
        System.arraycopy(args, 0, translationArgs, 1, args.length);
        source.sendFeedback(() -> Text.translatable(translationKey, translationArgs), false);
    }
}