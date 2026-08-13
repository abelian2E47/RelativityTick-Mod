package com.abelian;

import com.abelian.config.RelativityTickConfig;
import com.abelian.network.*;
import com.abelian.regionTick.RegionTickManager;
import com.abelian.regionTick.RegionsManager;
import com.abelian.regionTick.RegionTickManager.RegionState;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
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
import net.minecraft.server.network.ServerPlayerEntity;
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
        dispatcher.register(relativityConfigCommand("relativityTick"));

        dispatcher.register(CommandManager.literal("regionManager")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("remove")
                        .then(regionId().executes(ctx -> executeRemove(RegionCommandContext.of(ctx)))))
                .then(CommandManager.literal("setting")
                        .then(CommandManager.argument("setting", StringArgumentType.word())
                                .suggests(ServerCommands::suggestSettings)
                                .then(regionId()
                                        .then(CommandManager.literal("enable")
                                                .executes(ctx -> setSetting(RegionCommandContext.of(ctx), StringArgumentType.getString(ctx, "setting"), false)))
                                        .then(CommandManager.literal("disable")
                                                .executes(ctx -> setSetting(RegionCommandContext.of(ctx), StringArgumentType.getString(ctx, "setting"), true))))))
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
                                        .executes(ctx -> setRate(RegionCommandContext.of(ctx), DoubleArgumentType.getDouble(ctx, "rate"))))))
                .then(CommandManager.literal("status")
                        .executes(ctx -> getAllRegionStatus(ctx.getSource()))
                        .then(regionId().executes(ctx -> getRegionStatus(RegionCommandContext.of(ctx)))))
        );

    }
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> relativityConfigCommand(String literal) {
        return CommandManager.literal(literal)
                .executes(ctx -> showRelativityTickConfig(ctx.getSource()))
                .then(CommandManager.literal("maxMspt")
                        .executes(ctx -> showMaxMsptConfig(ctx.getSource()))
                        .then(CommandManager.argument("value", DoubleArgumentType.doubleArg(1.0, 50.0))
                                .requires(source -> source.hasPermissionLevel(2))
                                .executes(ctx -> setMaxMspt(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "value")))))
                .then(CommandManager.literal("chunkTick")
                        .executes(ctx -> showChunkTickConfig(ctx.getSource()))
                        .then(CommandManager.literal("enabled")
                                .then(CommandManager.argument("value", BoolArgumentType.bool())
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(ctx -> setChunkTickEnabled(
                                                ctx.getSource(), BoolArgumentType.getBool(ctx, "value"))))));
    }

    private static int showChunkTickConfig(ServerCommandSource source) {
        source.sendFeedback(() -> Text.translatable("relativitytick.command.config.chunk_tick",
                RelativityTickConfig.isChunkTickEnabled()), false);
        return 1;
    }

    private static int showMaxMsptConfig(ServerCommandSource source) {
        source.sendFeedback(() -> Text.translatable("relativitytick.command.config.max_mspt",
                formatConfigValue(RelativityTickConfig.getMaxMspt())), false);
        return 1;
    }

    private static int setChunkTickEnabled(ServerCommandSource source, boolean value) {
        try {
            RelativityTickConfig.setChunkTickEnabled(value);
            return showChunkTickConfig(source);
        } catch (IOException e) {
            source.sendError(Text.translatable("relativitytick.command.error.config_save_failed").formatted(Formatting.RED));
            return 0;
        }
    }

    private static int showRelativityTickConfig(ServerCommandSource source) {
        showMaxMsptConfig(source);
        showChunkTickConfig(source);
        return 1;
    }



    private static int setMaxMspt(ServerCommandSource source, double value) {
        try {
            RelativityTickConfig.setMaxMspt(value);
            return showMaxMsptConfig(source);
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

    private static CompletableFuture<Suggestions> suggestSettings(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        builder.suggest("hopperTick");
        builder.suggest("entityTick");
        builder.suggest("observerScheduledTick");
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
            rcc.manager.setAccumulator(1.0);
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

            //发送step同步包
            RegionStepPayload regionStepPayload = new RegionStepPayload(rcc.id, totalPending);

            for (ServerPlayerEntity player : rcc.source.getWorld().getPlayers()) {
                ServerPlayNetworking.send(player, regionStepPayload);
            }

            sendFeedback(rcc.source, rcc.id, "relativitytick.command.region.stepping", steps);
            syncRegionState(rcc);
            return 1;
        }

        BiConsumer<BlockPos, Block> blockTicker = (pos, block) -> RelativityTickUtils.tickBlock(rcc.world, pos, block);
        BiConsumer<BlockPos, Fluid> fluidTicker = (pos, fluid) -> RelativityTickUtils.tickFluid(rcc.world, pos, fluid);

        Map<Integer, EntityStateRecord> entityStates = new LinkedHashMap<>();
        com.abelian.ServerTickBridge.beginRegionTickBatch();
        for (int i = 0; i < steps; i++) {
            rcc.manager.tickRegion(rcc.world, rcc.world.getBlockTickScheduler(), blockTicker, rcc.world.getFluidTickScheduler(), fluidTicker);
        }
        for (EntityStateRecord state : rcc.manager.collectEntityStates(rcc.world)) {
            entityStates.put(state.entityId(), state);
        }

        sendFeedback(rcc.source, rcc.id, "relativitytick.command.region.stepped", steps);
        syncRegionState(rcc);
        sendStepPayload(rcc, steps);
        sendEntitySyncPayload(rcc, new ArrayList<>(entityStates.values()));
        ServerPlayNetworking.send(rcc.source.getPlayer(), new RegionTimePayload(rcc.id, rcc.manager.getVirtualTime()));
        return 1;
    }

    private static int setRate(RegionCommandContext rcc, double rate) {
        if (rcc.isInvalid()) return 0;
        boolean wasControlled = rcc.manager.isControlled();
        boolean wasRunning = rcc.manager.isRunning();

        rcc.manager.setRate(rate);
        if (!wasControlled) {
            takeOver(rcc);
            rcc.manager.setAccumulator(1.0);
            rcc.manager.setState(RegionState.RUNNING);
        } else {
            rcc.manager.setState(wasRunning ? RegionState.RUNNING : RegionState.FROZEN);
        }

        RegionsManager.savePersistentState();
        //重置TPS统计缓存
        rcc.manager.resetRecentStepCount(rate);

        sendFeedback(rcc.source, rcc.id, wasControlled ? "relativitytick.command.region.rate_set" : "relativitytick.command.region.taken_over_rate_set", formatConfigValue(rate));
        syncRegionState(rcc);
        syncRegionTPS(rcc);
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

    private static int setSetting(RegionCommandContext rcc, String setting, boolean disable) {
        if (rcc.isInvalid()) return 0;

        String settingKey;
        switch (setting) {
            case "hopperTick" -> {
                rcc.manager.setDisableHopperTick(disable);
                settingKey = "relativitytick.command.setting.hopper_tick";
            }
            case "entityTick" -> {
                rcc.manager.setDisableEntityTick(disable);
                settingKey = "relativitytick.command.setting.entity_tick";
            }
            case "observerScheduledTick" -> {
                rcc.manager.setDisableObserverTick(disable);
                settingKey = "relativitytick.command.setting.observer_scheduled_tick";
            }
            default -> {
                rcc.source.sendError(Text.translatable("relativitytick.command.error.unknown_setting", setting).formatted(Formatting.RED));
                return 0;
            }
        }

        RegionsManager.savePersistentState();
        String valueKey = disable ? "relativitytick.command.setting.disabled" : "relativitytick.command.setting.enabled";
        sendFeedback(rcc.source, rcc.id, "relativitytick.command.region.setting_set",
                Text.translatable(settingKey), Text.translatable(valueKey));
        syncRegionState(rcc);
        return 1;
    }

    private static int executeRemove(RegionCommandContext rcc) {
        if (rcc.isInvalid()) return 0;
        RegionsManager.removeRegion(rcc.id);

        rcc.source.sendFeedback(() -> Text.translatable("relativitytick.command.region.removed",
                Text.literal(rcc.id).formatted(Formatting.GOLD)), false);
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
        source.sendFeedback(() -> Text.translatable("relativitytick.command.status.region_time",
                Text.literal(String.valueOf(mgr.getRegionTime())).formatted(Formatting.AQUA)), false);
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
                    Text.literal(String.format("%.2f", mgr.getTPS())).formatted(Formatting.GREEN),
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

        //提示
        if (mgr.hasReachedMsptLimit()) {
            source.sendFeedback(() -> Text.translatable("relativitytick.command.warning.mspt_slowdown").formatted(Formatting.ITALIC, Formatting.RED), false);
        }else if (mgr.hasReachedTickDurationLimit()){
            source.sendFeedback(() -> Text.translatable("relativitytick.command.warning.tick_duration_slowdown").formatted(Formatting.ITALIC, Formatting.RED), false);
        }

        //禁用设置
        source.sendFeedback(() -> Text.translatable("relativitytick.command.status.hopper_tick",
                settingValue(mgr.isDisableHopperTick())), false);
        source.sendFeedback(() -> Text.translatable("relativitytick.command.status.entity_tick",
                settingValue(mgr.isDisableEntityTick())), false);
        source.sendFeedback(() -> Text.translatable("relativitytick.command.status.observer_tick",
                settingValue(mgr.isDisableObserverTick())), false);
    }

    private static Text settingValue(boolean disabled) {
        return disabled
                ? Text.translatable("relativitytick.command.setting.disabled").formatted(Formatting.RED)
                : Text.translatable("relativitytick.command.setting.enabled").formatted(Formatting.GREEN);
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
                rcc.manager.getState(), rcc.manager.getRate(), rcc.manager.getVirtualTime(),
                rcc.manager.isDisableHopperTick(), rcc.manager.isDisableEntityTick(), rcc.manager.isDisableObserverTick());
        sendToWorldPlayers(rcc.world, payload);
    }

    private static void syncRegionTPS(RegionCommandContext rcc) {
        RegionTPSPayload payload = new RegionTPSPayload(rcc.id, rcc.manager.getRegionTickDuration(), rcc.manager.getTPS(), rcc.manager.getVirtualTime());
        sendToWorldPlayers(rcc.world, payload);
    }


    private static void sendStepPayload(RegionCommandContext rcc, int steps) {
        RegionStepPayload payload = new RegionStepPayload(rcc.id, steps);
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