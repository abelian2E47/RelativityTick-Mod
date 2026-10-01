package com.abelian;

import com.abelian.config.RelativityTickConfig;
import com.abelian.network.*;
import com.abelian.regionTick.RegionTickManager;
import com.abelian.regionTick.RegionBlockEventProcessor;
import com.abelian.regionTick.RegionDashRunner;
import com.abelian.regionTick.RegionsManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.tick.WorldTickScheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class RelativityTick implements ModInitializer {
	public static final String MOD_ID = "relativitytick";
	public static final Identifier SELECTION_OPERATION_PACKET_ID = Identifier.of(MOD_ID, "selection_operation_packet");
	public static final Identifier REGION_SYNC_PACKET_ID = Identifier.of(MOD_ID, "region_sync_packet");
	public static final Identifier REGION_TPS_SYNC_PACKET_ID = Identifier.of(MOD_ID, "region_tps_sync_packet");
	public static final Identifier REGION_STEP_PACKET_ID = Identifier.of(MOD_ID, "region_step_packet");
	public static final Identifier REGION_ENTITY_SYNC_PACKET_ID = Identifier.of(MOD_ID, "region_entity_sync_packet");
    public static final Identifier PASSENGER_SYNC_PACKET_ID = Identifier.of(MOD_ID, "passenger_sync_packet");
    public static final Identifier REGION_TIME_PACKET_ID = Identifier.of(MOD_ID, "region_time_packet");
    public static final Identifier SCHEDULED_TICK_DATA_PAYLOAD = Identifier.of(MOD_ID, "scheduled_tick_data_payload");

    private static final double REGION_TPS_RELATIVE_SEND_THRESHOLD = 0.01;
    private static final long REGION_TPS_STABLE_SEND_NANOS = 1_000_000_000L;
    private static final Map<String, Double> LAST_SENT_REGION_TPS = new HashMap<>();
    private static final Map<String, Long> REGION_TPS_SEND_CANDIDATE_SINCE = new HashMap<>();

    private static final int REGION_ENTITY_SYNC_INTERVAL_REGION_TICKS = 20;
    private static final Map<String, Integer> REGION_ENTITY_SYNC_LAST_STEPPED = new HashMap<>();

	@Override
	public void onInitialize() {
		RelativityTickConfig.initialize();
		CommandRegistrationCallback.EVENT.register(ServerCommands::register);
        PayloadTypeRegistry.playS2C().register(RegionSyncPayload.ID, RegionSyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SelectionOperationPayload.ID, SelectionOperationPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RegionTPSPayload.ID, RegionTPSPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RegionStepPayload.ID, RegionStepPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RegionEntitySyncPayload.ID, RegionEntitySyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PassengerSyncPayload.ID, PassengerSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RegionTimePayload.ID, RegionTimePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ScheduledTickDataPayload.ID, ScheduledTickDataPayload.CODEC);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            RelativityTickUtils.set(server);
            RegionPersistentState.migrateLegacyFile(server);
            RegionsManager.loadPersistentState();
            RegionsManager.restorePersistentStates();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> RegionsManager.prepareForShutdown());

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            RegionsManager.clear();
            RelativityTickUtils.clear();
            LAST_SENT_REGION_TPS.clear();
            REGION_TPS_SEND_CANDIDATE_SINCE.clear();
            REGION_ENTITY_SYNC_LAST_STEPPED.clear();
            RegionBlockEventProcessor.clear();
        });

        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) ->
                RegionsManager.onChunkLoad(world, chunk.getPos().toLong()));
        ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) ->
                RegionsManager.onChunkUnload(world, chunk.getPos().toLong()));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                RegionsManager.syncAllRegions(handler.player));

        ServerPlayNetworking.registerGlobalReceiver(SelectionOperationPayload.ID,
                (payload, context) -> {
                    if (!context.player().hasPermissionLevel(2)) return;
                    context.server().execute(() -> {
                        Set<Long> chunkPositions = payload.chunkPositions();
                        String id = payload.id();
                        RegionsManager.createRegion(id, chunkPositions, context.player().getServerWorld());
                    });
                });

        ServerTickEvents.END_SERVER_TICK.register(server -> ServerTickBridge.beginRegionTickBatch());
        //step
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (String id : RegionsManager.getRegionIdsInOrder()) {
                RegionTickManager region = RegionsManager.getRegion(id);
                ServerWorld world = server.getWorld(region.getDimension());
                if (world == null) continue;
                if (region.isControlled() && region.isStepping() && !region.isRunning()) {
                    RegionRunResult result = runRegionTicks(server, region, world, region.getPendingSteps(), false);
                    int remaining = result.remainingSteps();
                    if (result.stepsTaken() > 0) {
                        region.recordTickDuration(result.durationNano());
                    }

                    Map<Integer, EntityStateRecord> entityStates = new LinkedHashMap<>();
                    if (result.stepsTaken() > 0) {
                        for (EntityStateRecord state : region.collectEntityStates(world)) {
                            entityStates.put(state.entityId(), state);
                        }
                    }

                    region.setPendingSteps(remaining);
                    if (result.stepsTaken() > 0) {
                        sendRegionTime(id, region, world);
                    }
                    if (result.stepsTaken() > 0 && remaining == 0) {
                        RegionSyncPayload syncPayload = new RegionSyncPayload(id, region.getDimensionId(), region.getChunkPositions(),
                                region.getState(), region.getRate(), region.getVirtualTime(),
                                region.isDisableHopperTick(), region.isDisableEntityTick(), region.isDisableObserverTick());
                        RegionEntitySyncPayload entityPayload = new RegionEntitySyncPayload(id, new ArrayList<>(entityStates.values()));
                        //步进完成:广播 0 让客户端清零本地 pending,停止继续推虚拟时间(否则客户端慢于服务端时会把
                        //剩余时间显示推成负数/提前消失)。与既有 sync+entity 同批发,仅多一个极小包。
                        RegionStepPayload clearPendingPayload = new RegionStepPayload(id, 0);
                        for (ServerPlayerEntity player : world.getPlayers()) {
                            ServerPlayNetworking.send(player, syncPayload);
                            ServerPlayNetworking.send(player, entityPayload);
                            ServerPlayNetworking.send(player, clearPendingPayload);
                        }
                    }
                }
            }
        });

        //running
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (String id : RegionsManager.getRegionIdsInOrder()) {
                RegionTickManager region = RegionsManager.getRegion(id);
                if (!region.isRunning() || !region.isControlled()) continue;

                ServerWorld world = server.getWorld(region.getDimension());
                if (world == null) continue;

                RegionRunResult result = runRegionTicks(server, region, world, Integer.MAX_VALUE, true);
                if (result.stepsTaken() > 0) {
                    region.recordTickDuration(result.durationNano());
                }
                if (result.stepsTaken() > 0) {
                    sendRegionTime(id, region, world);
                }
            }
        });

        //dash 剩余步数与 sprint：同一套 dash 原语（每 gt 按预算跑满）。
        //dash 命令被预算截断后剩下的步数放在 pendingDashSteps，这里每 gt 继续跑满直到跑完；
        //sprint 则每 gt 都跑满，有界冲刺的 gt 数用完就恢复冲刺前的状态。
        //这一批里可能还有别的区域在 tick，所以不能声明"单区域批"（见 ServerTickBridge.claimEntity）。
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (String id : RegionsManager.getRegionIdsInOrder()) {
                RegionTickManager region = RegionsManager.getRegion(id);
                if (!region.isControlled()) continue;

                boolean sprinting = region.isSprinting();
                int carrySteps = region.getPendingDashSteps();
                if (!sprinting && carrySteps <= 0) continue;

                ServerWorld world = server.getWorld(region.getDimension());
                if (world == null) continue;

                RegionDashRunner.DashResult result = RegionDashRunner.runDash(server, region, world,
                        sprinting ? Integer.MAX_VALUE : carrySteps, false);

                if (result.stepsTaken() > 0) {
                    region.recordTickDuration(result.durationNano());
                }
                if (!sprinting) {
                    region.setPendingDashSteps(result.remainingSteps());
                }

                if (result.stepsTaken() > 0) {
                    sendRegionTime(id, region, world);
                    sendRegionEntityStates(id, region, world);
                }

                if (sprinting && region.consumeSprintGt()) {
                    region.stopSprint();
                    sendRegionStateSync(id, region, world);
                    server.getCommandSource().sendFeedback(() -> Text.translatable(
                            "relativitytick.command.region.sprint_finished",
                            Text.literal(id).formatted(Formatting.AQUA)), true);
                }
            }
        });

        //TPS 统计：所有推进路径都只是让 stepped 增长，所以统一在这里按 gt 采一次 stepped 增量，
        //以后新增推进路径也不会漏（没推进的 gt 记 0，读数自然衰减）。必须排在所有推进 handler 之后。
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long nowNano = System.nanoTime();
            for (String id : RegionsManager.getRegionIdsInOrder()) {
                RegionTickManager region = RegionsManager.getRegion(id);
                if (!region.isControlled()) continue;

                region.sampleStepRate(region.getStepped(), nowNano);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long nowNano = System.nanoTime();
            for (String id : RegionsManager.getRegionIdsInOrder()) {
                RegionTickManager region = RegionsManager.getRegion(id);
                ServerWorld world = server.getWorld(region.getDimension());
                if (world == null || !region.isControlled()) continue;

                double currentTPS = region.getTPS();
                if (!shouldSendRegionTps(id, region, currentTPS, nowNano)) continue;

                sendRegionTpsAndEntities(id, region, world, currentTPS);
                LAST_SENT_REGION_TPS.put(id, currentTPS);
                REGION_TPS_SEND_CANDIDATE_SINCE.remove(id);
            }
        });

        //实体权威矫正
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (String id : RegionsManager.getRegionIdsInOrder()) {
                RegionTickManager region = RegionsManager.getRegion(id);
                if (!region.isControlled()) continue;
                if (!region.isRunning() && region.getPendingSteps() <= 0) continue;

                ServerWorld world = server.getWorld(region.getDimension());
                if (world == null) continue;

                int stepped = region.getStepped();
                int lastSent = REGION_ENTITY_SYNC_LAST_STEPPED.getOrDefault(id, stepped);
                if (stepped - lastSent < REGION_ENTITY_SYNC_INTERVAL_REGION_TICKS) continue;
                REGION_ENTITY_SYNC_LAST_STEPPED.put(id, stepped);

                ArrayList<EntityStateRecord> entityStates = new ArrayList<>(region.collectEntityStates(world));
                if (entityStates.isEmpty()) continue;

                RegionEntitySyncPayload entityPayload = new RegionEntitySyncPayload(id, entityStates);
                for (ServerPlayerEntity player : world.getPlayers()) {
                    ServerPlayNetworking.send(player, entityPayload);
                }
            }
        });
	}

    private static boolean shouldSendRegionTps(String id, RegionTickManager region, double currentTPS, long nowNano) {
        Double lastTPS = LAST_SENT_REGION_TPS.get(id);
        //首次同步或速率/时间线刚重置时，必须等一个完整真实窗口，且丢弃旧发送基线。
        if (!region.isStepRateSampleReady()) {
            LAST_SENT_REGION_TPS.remove(id);
            REGION_TPS_SEND_CANDIDATE_SINCE.remove(id);
            return false;
        }
        if (lastTPS == null) return true;

        double denominator = Math.max(Math.abs(lastTPS), 1.0);
        double relativeDiff = Math.abs(currentTPS - lastTPS) / denominator;
        if (relativeDiff < REGION_TPS_RELATIVE_SEND_THRESHOLD) {
            REGION_TPS_SEND_CANDIDATE_SINCE.remove(id);
            return false;
        }

        long candidateSince = REGION_TPS_SEND_CANDIDATE_SINCE.computeIfAbsent(id, ignored -> nowNano);
        return nowNano - candidateSince >= REGION_TPS_STABLE_SEND_NANOS;
    }

    private static void sendRegionTpsAndEntities(String id, RegionTickManager region, ServerWorld world, double currentTPS) {
        RegionTPSPayload tpsPayload = new RegionTPSPayload(id, region.getRegionTickDuration(), currentTPS, region.getVirtualTime());
        RegionEntitySyncPayload entityPayload = new RegionEntitySyncPayload(id, new ArrayList<>(region.collectEntityStates(world)));
        for (ServerPlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send(player, tpsPayload);
            ServerPlayNetworking.send(player, entityPayload);
        }
    }

    private static void sendRegionTime(String id, RegionTickManager region, ServerWorld world) {
        RegionTimePayload payload = new RegionTimePayload(id, region.getVirtualTime());
        for (ServerPlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private static void sendRegionEntityStates(String id, RegionTickManager region, ServerWorld world) {
        ArrayList<EntityStateRecord> entityStates = new ArrayList<>(region.collectEntityStates(world));
        if (entityStates.isEmpty()) return;

        RegionEntitySyncPayload payload = new RegionEntitySyncPayload(id, entityStates);
        for (ServerPlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private static void sendRegionStateSync(String id, RegionTickManager region, ServerWorld world) {
        RegionSyncPayload payload = new RegionSyncPayload(id, region.getDimensionId(), region.getChunkPositions(),
                region.getState(), region.getRate(), region.getVirtualTime(),
                region.isDisableHopperTick(), region.isDisableEntityTick(), region.isDisableObserverTick());
        for (ServerPlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private static RegionRunResult runRegionTicks(MinecraftServer server, RegionTickManager region, ServerWorld world, int maxSteps, boolean consumePendingSteps) {
        long regionStartNano = System.nanoTime();
        long regionBudgetNano = (long)(region.getTickDurationLimit() * 1_000_000L);
        WorldTickScheduler<Block> blockScheduler = world.getBlockTickScheduler();
        WorldTickScheduler<Fluid> fluidScheduler = world.getFluidTickScheduler();
        BiConsumer<BlockPos, Block> blockTicker = (pos, block) -> RelativityTickUtils.tickBlock(world, pos, block);
        BiConsumer<BlockPos, Fluid> fluidTicker = (pos, fluid) -> RelativityTickUtils.tickFluid(world, pos, fluid);

        double[] accumulator = {region.getAccumulator()};
        int stepsToTake = RelativityTickUtils.accumulateSteps(region.getRate(), accumulator);
        int stepsTaken = 0;
        int remainingSteps = maxSteps;
        long regionTickDurationNano = 0L;

        region.setReachedMsptLimit(false);
        region.setReachTickDurationLimit(false);

        ServerTickBridge.LocalTickState tickState = new ServerTickBridge.LocalTickState();
        while (regionBudgetNano > 0 && stepsTaken < stepsToTake && remainingSteps > 0) {
            long tickStartNano = System.nanoTime();
            tickState.clear();
            region.tickRegion(world, blockScheduler, blockTicker, fluidScheduler, fluidTicker, tickState);
            regionTickDurationNano += System.nanoTime() - tickStartNano;
            stepsTaken++;
            remainingSteps--;

            if (consumePendingSteps && region.getPendingSteps() > 0) {
                region.setPendingSteps(region.getPendingSteps() - 1);
            }

            if (RelativityTickUtils.getServerMspt(server) >= RelativityTickConfig.getMaxMspt()) {
                region.setReachedMsptLimit(true);
                break;
            }

            if (System.nanoTime() - regionStartNano >= regionBudgetNano) {
                region.setReachTickDurationLimit(true);
                break;
            }
        }

        region.setAccumulator(accumulator[0] + Math.max(0, stepsToTake - stepsTaken));
        return new RegionRunResult(stepsTaken, remainingSteps, regionTickDurationNano);
    }

    private record RegionRunResult(int stepsTaken, int remainingSteps, long durationNano) {
    }

}
