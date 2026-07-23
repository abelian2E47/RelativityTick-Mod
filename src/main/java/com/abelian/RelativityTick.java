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

    public static long tickStartTime = 0;
    //记录上次发包时的TPS，用以TPS的同步
    private static final Map<String, Double> LAST_SENT_REGION_TPS = new HashMap<>();
    //TPS相差1时重新同步
    private static final double REGION_TPS_SEND_THRESHOLD = 1;

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

        //记录tick开始的时间用作mspt计算
        ServerTickEvents.END_SERVER_TICK.register(server -> tickStartTime = System.nanoTime());

        //step
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (String id : RegionsManager.getRegionIdsByPriority()) {
                Region region = RegionsManager.getRegion(id);
                ServerWorld world = server.getWorld(region.getDimension());
                if (world == null) continue;

                WorldTickScheduler<Block> blockTickScheduler = world.getBlockTickScheduler();
                WorldTickScheduler<Fluid> fluidTickScheduler = world.getFluidTickScheduler();
                BiConsumer<BlockPos, Block> blockTicker = (pos, block) -> world.getBlockState(pos).scheduledTick(world, pos, world.getRandom());
                BiConsumer<BlockPos, Fluid> fluidTicker = (pos, fluid) -> world.getFluidState(pos).onScheduledTick(world, pos, fluid.getDefaultState().getBlockState());

                if (region.isControlled() && region.isStepping() && !region.isRunning()) {
                    long regionStartNano = System.nanoTime();
                    long regionBudgetNano = (long)(region.getTickDurationLimit() * 1_000_000L);
                    double rate = region.getRate();
                    double[] accRef = {region.getAccumulator()};
                    int stepsToTake = RelativityTickUtils.accumulateSteps(rate, accRef);
                    int remaining = region.getPendingSteps();
                    int stepsTaken = 0;

                    region.setReachedMsptLimit(false);
                    region.setReachTickDurationLimit(false);

                    Map<Integer, EntityStateRecord> entityStates = new LinkedHashMap<>();
                    while (regionBudgetNano > 0 && stepsTaken < stepsToTake && remaining > 0) {
                        region.tickRegion(world, blockTickScheduler, blockTicker, fluidTickScheduler, fluidTicker);
                        stepsTaken++;
                        remaining--;

                        if (System.nanoTime() - tickStartTime >= RelativityTickConfig.getMaxMspt() * 1_000_000L) {
                            region.setReachedMsptLimit(true);
                            break;
                        }

                        if (System.nanoTime() - regionStartNano >= regionBudgetNano) {
                            region.setReachTickDurationLimit(true);
                            break;
                        }
                    }
                    if (stepsTaken > 0) {
                        for (EntityStateRecord state : region.collectEntityStates(world)) {
                            entityStates.put(state.entityId(), state);
                        }
                    }

                    region.setAccumulator(accRef[0]);
                    region.setPendingSteps(remaining);
                    if (stepsTaken > 0 && remaining == 0) {
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
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (String id : RegionsManager.getRegionIdsByPriority()) {
                Region region = RegionsManager.getRegion(id);
                if (!region.isRunning() || !region.isControlled()) continue;

                ServerWorld world = server.getWorld(region.getDimension());
                if (world == null) continue;

                long regionStartNano = System.nanoTime();
                WorldTickScheduler<Block> blockScheduler = world.getBlockTickScheduler();
                WorldTickScheduler<Fluid> fluidScheduler = world.getFluidTickScheduler();
                BiConsumer<BlockPos, Block> bTicker = (pos, block) -> world.getBlockState(pos).scheduledTick(world, pos, world.getRandom());
                BiConsumer<BlockPos, Fluid> fTicker = (pos, fluid) -> world.getFluidState(pos).onScheduledTick(world, pos, fluid.getDefaultState().getBlockState());

                double rate = region.getRate();
                double[] accRef = {region.getAccumulator()};
                int stepsToTake = RelativityTickUtils.accumulateSteps(rate, accRef);
                int stepsTaken = 0;
                long regionBudgetNano = (long)(region.getTickDurationLimit() * 1_000_000L);

                region.setReachedMsptLimit(false);
                region.setReachTickDurationLimit(false);

                while (regionBudgetNano > 0 && stepsTaken < stepsToTake) {
                    region.tickRegion(world, blockScheduler, bTicker, fluidScheduler, fTicker);
                    stepsTaken++;

                    if (region.getPendingSteps() > 0) {
                        region.setPendingSteps(region.getPendingSteps() - 1);
                    }

                    if (System.nanoTime() - tickStartTime >= RelativityTickConfig.getMaxMspt() * 1_000_000L) {
                        region.setReachedMsptLimit(true);
                        break;
                    }

                    if (System.nanoTime() - regionStartNano >= regionBudgetNano) {
                        region.setReachTickDurationLimit(true);
                        break;
                    }
                }

                long regionDurationNano = System.nanoTime() - regionStartNano;
                region.recordTickDuration(regionDurationNano);
                region.updateTickStats();
            }
        });

        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (String id : RegionsManager.getRegionIdsByPriority()) {
                Region region = RegionsManager.getRegion(id);
                ServerWorld world = server.getWorld(region.getDimension());
                if (world == null || !region.isControlled()) continue;

                double currentTPS = region.getLastMeasuredTPS();
                Double lastTPS = LAST_SENT_REGION_TPS.get(id);
                boolean shouldSend = lastTPS == null || Math.abs(currentTPS - lastTPS) >= REGION_TPS_SEND_THRESHOLD;

                if (!shouldSend) continue;

                RegionTPSPayload payload = new RegionTPSPayload(id, region.getRegionTickDuration(), currentTPS);
                for (ServerPlayerEntity player : world.getPlayers()) {
                    ServerPlayNetworking.send(player, payload);
                }

                LAST_SENT_REGION_TPS.put(id, currentTPS);
            }
        });
	}

}
