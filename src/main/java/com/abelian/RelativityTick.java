package com.abelian;

import com.abelian.config.RelativityTickConfig;
import com.abelian.network.*;
import com.abelian.regionTick.Region;
import com.abelian.regionTick.RegionPersistentState;
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

    private static final double REGION_TPS_RELATIVE_SEND_THRESHOLD = 0.01;
    private static final int REGION_TPS_STABLE_SEND_GT = 20;
    private static final Map<String, Double> LAST_SENT_REGION_TPS = new HashMap<>();
    private static final Map<String, Integer> REGION_TPS_SEND_CANDIDATE_TICKS = new HashMap<>();

	@Override
	public void onInitialize() {
		RelativityTickConfig.initialize();
		CommandRegistrationCallback.EVENT.register(ServerCommands::register);
		PayloadTypeRegistry.playS2C().register(RegionSyncPayload.ID, RegionSyncPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SelectionOperationPayload.ID, SelectionOperationPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(RegionTPSPayload.ID, RegionTPSPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(RegionStepPayload.ID, RegionStepPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(RegionEntitySyncPayload.ID, RegionEntitySyncPayload.CODEC);

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
            REGION_TPS_SEND_CANDIDATE_TICKS.clear();
        });

        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) ->
                RegionsManager.onChunkLoad(world, chunk.getPos().toLong()));
        ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) ->
                RegionsManager.onChunkUnload(world, chunk.getPos().toLong()));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                RegionsManager.syncAllRegions(handler.player));

		ServerPlayNetworking.registerGlobalReceiver(SelectionOperationPayload.ID,
				(payload, context) -> context.server().execute(() -> {
                    Set<Long> chunkPositions = payload.chunkPositions();
                    String id = payload.id();
                    RegionsManager.createRegion(id, chunkPositions, context.player().getServerWorld());
				}));

        //step
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (String id : RegionsManager.getRegionIdsByPriority()) {
                Region region = RegionsManager.getRegion(id);
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
                    if (result.stepsTaken() > 0 && remaining == 0) {
                        RegionSyncPayload syncPayload = new RegionSyncPayload(id, region.getDimensionId(), region.getChunkPositions(),
                                region.getState(), region.getRate());
                        RegionEntitySyncPayload entityPayload = new RegionEntitySyncPayload(id, new ArrayList<>(entityStates.values()));
                        for (ServerPlayerEntity player : world.getPlayers()) {
                            ServerPlayNetworking.send(player, syncPayload);
                            ServerPlayNetworking.send(player, entityPayload);
                        }
                    }
                }
            }
        });

        //按rate运行
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (String id : RegionsManager.getRegionIdsByPriority()) {
                Region region = RegionsManager.getRegion(id);
                if (!region.isRunning() || !region.isControlled()) continue;

                ServerWorld world = server.getWorld(region.getDimension());
                if (world == null) continue;

                RegionRunResult result = runRegionTicks(server, region, world, Integer.MAX_VALUE, true);
                if (result.stepsTaken() > 0) {
                    region.recordTickDuration(result.durationNano());
                } else {
                    region.recordTickDuration(0L);
                }
                region.recordGlobalTickSteps(result.stepsTaken());
            }
        });

        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (String id : RegionsManager.getRegionIdsByPriority()) {
                Region region = RegionsManager.getRegion(id);
                ServerWorld world = server.getWorld(region.getDimension());
                if (world == null || !region.isControlled()) continue;

                double currentTPS = region.getTPS();
                if (!shouldSendRegionTps(id, region, currentTPS)) continue;

                sendRegionTpsAndEntities(id, region, world, currentTPS);
                LAST_SENT_REGION_TPS.put(id, currentTPS);
                REGION_TPS_SEND_CANDIDATE_TICKS.remove(id);
            }
        });
	}

    private static boolean shouldSendRegionTps(String id, Region region, double currentTPS) {
        Double lastTPS = LAST_SENT_REGION_TPS.get(id);
        if (lastTPS == null) return true;
        if (!region.hasFullTpsSampleWindow()) return false;

        double denominator = Math.max(Math.abs(lastTPS), 1.0);
        double relativeDiff = Math.abs(currentTPS - lastTPS) / denominator;
        if (relativeDiff < REGION_TPS_RELATIVE_SEND_THRESHOLD) {
            REGION_TPS_SEND_CANDIDATE_TICKS.remove(id);
            return false;
        }

        int candidateTicks = REGION_TPS_SEND_CANDIDATE_TICKS.getOrDefault(id, 0) + 1;
        REGION_TPS_SEND_CANDIDATE_TICKS.put(id, candidateTicks);
        return candidateTicks >= REGION_TPS_STABLE_SEND_GT;
    }

    private static void sendRegionTpsAndEntities(String id, Region region, ServerWorld world, double currentTPS) {
        RegionTPSPayload tpsPayload = new RegionTPSPayload(id, region.getRegionTickDuration(), currentTPS);
        RegionEntitySyncPayload entityPayload = new RegionEntitySyncPayload(id, new ArrayList<>(region.collectEntityStates(world)));
        for (ServerPlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send(player, tpsPayload);
            ServerPlayNetworking.send(player, entityPayload);
        }
    }

    private static RegionRunResult runRegionTicks(MinecraftServer server, Region region, ServerWorld world, int maxSteps, boolean consumePendingSteps) {
        long regionStartNano = System.nanoTime();
        long regionBudgetNano = (long)(region.getTickDurationLimit() * 1_000_000L);
        WorldTickScheduler<Block> blockScheduler = world.getBlockTickScheduler();
        WorldTickScheduler<Fluid> fluidScheduler = world.getFluidTickScheduler();
        BiConsumer<BlockPos, Block> blockTicker = (pos, block) -> world.getBlockState(pos).scheduledTick(world, pos, world.getRandom());
        BiConsumer<BlockPos, Fluid> fluidTicker = (pos, fluid) -> world.getFluidState(pos).onScheduledTick(world, pos, fluid.getDefaultState().getBlockState());

        double[] accumulator = {region.getAccumulator()};
        int stepsToTake = RelativityTickUtils.accumulateSteps(region.getRate(), accumulator);
        int stepsTaken = 0;
        int remainingSteps = maxSteps;
        long regionTickDurationNano = 0L;

        region.setReachedMsptLimit(false);
        region.setReachTickDurationLimit(false);

        while (regionBudgetNano > 0 && stepsTaken < stepsToTake && remainingSteps > 0) {
            long tickStartNano = System.nanoTime();
            region.tickRegion(world, blockScheduler, blockTicker, fluidScheduler, fluidTicker);
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

        region.setAccumulator(accumulator[0]);
        return new RegionRunResult(stepsTaken, remainingSteps, regionTickDurationNano);
    }

    private record RegionRunResult(int stepsTaken, int remainingSteps, long durationNano) {
    }

}
