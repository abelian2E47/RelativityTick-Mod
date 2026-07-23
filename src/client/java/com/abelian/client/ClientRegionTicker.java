package com.abelian.client;

import com.abelian.RegionTickContext;
import com.abelian.client.mixin.ClientEntityManagerAccessor;
import com.abelian.client.mixin.ClientWorldAccessor;
import com.abelian.network.EntityStateRecord;
import com.abelian.network.RegionEntitySyncPayload;
import com.abelian.network.RegionStepPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.entity.ClientEntityManager;
import net.minecraft.world.entity.SectionedEntityCache;
import net.minecraft.util.math.Vec3d;

import java.util.*;

import static com.abelian.client.EntityInterpolationManager.ENTITY_INTERPOLATIONS;

public class ClientRegionTicker {
    private static final List<Entity> ENTITY_TICK_BUFFER = new java.util.ArrayList<>(128);
    private static long nextRegionTickTime = Long.MIN_VALUE;

    public static void clearRegion(String regionId) {
        ENTITY_INTERPOLATIONS.entrySet().removeIf(entry -> entry.getValue().regionId().equals(regionId));
    }

    public static void clear() {
        ENTITY_TICK_BUFFER.clear();
        ENTITY_INTERPOLATIONS.clear();
    }

    public static void register(){
        //接收step同步包
        ClientPlayNetworking.registerGlobalReceiver(RegionStepPayload.ID, (payload, context) -> context.client().execute(() -> {
            ClientWorld world = context.client().world;
            if (world == null) return;

            ClientRegion region = ClientRegionManager.getRegion(payload.regionID());
            if (region == null) return;

            RegionTickDeltaManager.recordTickStep(region.getId());
        }));

        //接收实体同步包
        ClientPlayNetworking.registerGlobalReceiver(RegionEntitySyncPayload.ID, (payload, context) -> context.client().execute(() -> {
            ClientWorld world = context.client().world;
            if (world == null) return;

            applyEntityStates(world, payload.entities());
        }));

        //step
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            //遍历region
            for (ClientRegion region : ClientRegionManager.getRegions()) {
                ServerWorld world = Objects.requireNonNull(client.getServer()).getWorld(region.getDimension());
                if (world == null) continue;

                if (region.isControlled() && region.isStepping() && !region.isRunning()) {

                }
            }
        });
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
            RegionTickContext.begin(world, nextRegionTickTime++);

            collectTickingEntities(world, chunkSet);
            for (Entity entity : ENTITY_TICK_BUFFER) {
                if (entity.isRemoved()) continue;

                previousPositions.putIfAbsent(entity.getId(), entity.getPos());
                tickedEntities.put(entity.getId(), entity);

                ClientTickBridge.setCustomTickInProgress(true);
                // 客户端tick实体
                ((ClientWorldAccessor) world).invokeTickEntity(entity);
                System.out.println(entity.toString() + "has been ticked");
                ClientTickBridge.setCustomTickInProgress(false);
            }
            ENTITY_TICK_BUFFER.clear();
            ClientRegionBlockEntityTicker.tickBlockEntities(world, chunkSet);

            RegionTickContext.end();
        }

        for (Map.Entry<Integer, Entity> entry : tickedEntities.entrySet()) {
            Entity entity = entry.getValue();
            Vec3d previous = previousPositions.get(entry.getKey());
            if (previous != null && !entity.isRemoved()) {
                ENTITY_INTERPOLATIONS.put(entry.getKey(), new EntityInterpolationManager.EntityRenderInterpolation(regionId, previous, entity.getPos()));
            }
        }
    }

    //收集region内的实体
    private static void collectTickingEntities(ClientWorld world, Set<Long> chunkSet) {
        ENTITY_TICK_BUFFER.clear();
        ClientEntityManager<Entity> entityManager = ((ClientWorldAccessor) world).getEntityManager();
        SectionedEntityCache<Entity> cache = ((ClientEntityManagerAccessor<Entity>) entityManager).getCache();

        for (long chunkPos : chunkSet) {
            cache.getTrackingSections(chunkPos).forEach(section -> {
                if (section.getStatus().shouldTrack()) {
                    section.stream().forEach(entity -> {
                        if (!(entity instanceof PlayerEntity) && !entity.isRemoved() && !isPassenger(entity)) {
                            ENTITY_TICK_BUFFER.add(entity);
                        }
                    });
                }
            });
        }
    }

    private static boolean isPassenger(Entity entity) {
        Entity vehicle = entity.getVehicle();
        return vehicle != null && !vehicle.isRemoved() && vehicle.hasPassenger(entity);
    }
}
