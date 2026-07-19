package com.abelian.client;

import com.abelian.RelativityTickUtils;
import com.abelian.client.mixin.ClientEntityManagerAccessor;
import com.abelian.client.mixin.ClientWorldAccessor;
import com.abelian.network.EntityStateRecord;
import com.abelian.network.RegionEntitySyncPayload;
import com.abelian.network.RegionStepPayload;
import com.abelian.network.RegionSyncPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.entity.ClientEntityManager;
import net.minecraft.world.entity.SectionedEntityCache;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ClientRegionTicker {
    // Uses the section-cache path so region ticking scans only entities in region chunks.
    private static final boolean USE_SECTION_ENTITY_SCAN = true;
    private static final Map<String, double[]> RUNNING_ACCUMULATORS = new ConcurrentHashMap<>();
    private static final List<Entity> ENTITY_TICK_BUFFER = new java.util.ArrayList<>(128);
    private static final Map<Integer, EntityRenderInterpolation> ENTITY_INTERPOLATIONS = new ConcurrentHashMap<>();

    private record EntityRenderInterpolation(String regionId, Vec3d previousPos, Vec3d currentPos) {}

    public static Vec3d getInterpolatedEntityPos(Entity entity, String regionId, float tickDelta) {
        EntityRenderInterpolation interpolation = ENTITY_INTERPOLATIONS.get(entity.getId());
        if (interpolation == null || !interpolation.regionId().equals(regionId)) {
            return entity.getLerpedPos(tickDelta);
        }

        Vec3d previous = interpolation.previousPos();
        Vec3d current = interpolation.currentPos();
        return new Vec3d(
                previous.x + (current.x - previous.x) * tickDelta,
                previous.y + (current.y - previous.y) * tickDelta,
                previous.z + (current.z - previous.z) * tickDelta
        );
    }

    public static void clearRegion(String regionId) {
        RUNNING_ACCUMULATORS.remove(regionId);
        ENTITY_INTERPOLATIONS.entrySet().removeIf(entry -> entry.getValue().regionId().equals(regionId));
    }

    public static void clear() {
        RUNNING_ACCUMULATORS.clear();
        ENTITY_TICK_BUFFER.clear();
        ENTITY_INTERPOLATIONS.clear();
    }

    public static void register(){
        ClientPlayNetworking.registerGlobalReceiver(RegionStepPayload.ID, (payload, context) -> context.client().execute(() -> {
            ClientWorld world = context.client().world;
            if (world == null) return;

            RegionSyncPayload region = ClientRegionManager.getRegion(payload.regionID());
            if (region == null || region.isRunning()) return;

            tickRegion(world, region.id(), region.chunkPositions(), payload.steps());
            RegionTickDeltaManager.recordTickStep(region.id(), region.rate(), payload.accumulatorPhase());
        }));

        ClientPlayNetworking.registerGlobalReceiver(RegionEntitySyncPayload.ID, (payload, context) -> context.client().execute(() -> {
            ClientWorld world = context.client().world;
            if (world == null) return;

            applyEntityStates(world, payload.entities());
        }));

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            ClientWorld world = client.world;
            if (world == null) return;

            tickRunningRegions(world);
        });
    }

    private static void tickRunningRegions(ClientWorld world) {
        for (RegionSyncPayload region : ClientRegionManager.getRegions()) {
            if (!region.isControlled() || !region.isRunning() || region.stepping()) continue;

            double[] accRef = RUNNING_ACCUMULATORS.computeIfAbsent(region.id(), k -> new double[]{0.0});
            int stepsToTake = RelativityTickUtils.accumulateSteps(region.rate(), accRef);
            int stepsTaken = 0;
            long regionStartNano = System.nanoTime();
            long regionBudgetNano = 40_000_000L;
            while (stepsTaken < stepsToTake && regionBudgetNano > 0) {
                tickRegion(world, region.id(), region.chunkPositions(), 1);
                stepsTaken++;
                if (System.nanoTime() - regionStartNano >= regionBudgetNano) {
                    break;
                }
            }
            if (stepsTaken <= 0) continue;

            RegionTickDeltaManager.recordTickStep(region.id(), region.rate(), accRef[0]);
        }
    }


    private static void applyEntityStates(ClientWorld world, List<EntityStateRecord> states) {
        for (EntityStateRecord state : states) {
            Entity entity = world.getEntityById(state.entityId());
            if (entity == null || entity.isRemoved() || entity instanceof PlayerEntity) continue;

            entity.refreshPositionAndAngles(state.x(), state.y(), state.z(), state.yaw(), state.pitch());
            entity.setVelocity(new Vec3d(state.velocityX(), state.velocityY(), state.velocityZ()));
        }
    }

    private static void tickRegion(ClientWorld world, String regionId, Set<Long> chunkSet, int stepsTaken) {
        Map<Integer, Vec3d> previousPositions = new HashMap<>();
        Map<Integer, Entity> tickedEntities = new HashMap<>();

        for (int i = 0; i < stepsTaken; i++) {
            collectTickableEntities(world, chunkSet, USE_SECTION_ENTITY_SCAN);
            for (Entity entity : ENTITY_TICK_BUFFER) {
                if (entity.isRemoved()) continue;

                previousPositions.putIfAbsent(entity.getId(), entity.getPos());
                tickedEntities.put(entity.getId(), entity);
                ClientTickBridge.setCustomTickInProgress(true);
                try {
                    ((ClientWorldAccessor) world).invokeTickEntity(entity);
                } finally {
                    ClientTickBridge.setCustomTickInProgress(false);
                }
            }
            ENTITY_TICK_BUFFER.clear();
            ClientRegionBlockEntityTicker.tickBlockEntities(world, chunkSet);
        }

        for (Map.Entry<Integer, Entity> entry : tickedEntities.entrySet()) {
            Entity entity = entry.getValue();
            Vec3d previous = previousPositions.get(entry.getKey());
            if (previous != null && !entity.isRemoved()) {
                ENTITY_INTERPOLATIONS.put(entry.getKey(), new EntityRenderInterpolation(regionId, previous, entity.getPos()));
            }
        }
    }

    private static void collectTickableEntities(ClientWorld world, Set<Long> chunkSet, boolean useSectionEntityScan) {
        ENTITY_TICK_BUFFER.clear();
        if (!useSectionEntityScan) {
            collectTickableEntitiesByWorldScan(world, chunkSet);
            return;
        }

        collectTickableEntitiesBySectionCache(world, chunkSet);
    }

    private static void collectTickableEntitiesByWorldScan(ClientWorld world, Set<Long> chunkSet) {
        for (Entity entity : world.getEntities()) {
            if (entity.isRemoved() || entity instanceof PlayerEntity) continue;
            if (chunkSet.contains(ChunkPos.toLong(entity.getBlockPos()))) {
                ENTITY_TICK_BUFFER.add(entity);
            }
        }
    }

    private static void collectTickableEntitiesBySectionCache(ClientWorld world, Set<Long> chunkSet) {
        ClientEntityManager<Entity> entityManager = ((ClientWorldAccessor) world).getEntityManager();
        SectionedEntityCache<Entity> cache = ((ClientEntityManagerAccessor<Entity>) entityManager).getCache();

        for (long chunkPos : chunkSet) {
            cache.getTrackingSections(chunkPos).forEach(section -> {
                if (section.getStatus().shouldTrack()) {
                    section.stream().forEach(entity -> {
                        if (!(entity instanceof PlayerEntity) && !entity.isRemoved()) {
                            ENTITY_TICK_BUFFER.add(entity);
                        }
                    });
                }
            });
        }
    }

}
