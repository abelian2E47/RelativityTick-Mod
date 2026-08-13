package com.abelian.client.clientRegionTick;

import com.abelian.RegionTickContext;
import com.abelian.client.render.EntityInterpolationManager;
import com.abelian.client.mixin.ClientEntityManagerAccessor;
import com.abelian.client.mixin.ClientWorldAccessor;
import com.abelian.network.EntityStateRecord;
import com.abelian.network.PassengerSyncPayload;
import com.abelian.network.RegionEntitySyncPayload;
import com.abelian.network.RegionStepPayload;
import com.abelian.network.RegionTimePayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.entity.ClientEntityManager;
import net.minecraft.world.entity.SectionedEntityCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.abelian.client.render.EntityInterpolationManager.ENTITY_INTERPOLATIONS;

public class ClientRegionTicker {
    private static final List<Entity> ENTITY_TICK_BUFFER = new ArrayList<>(128);

    public static void clearRegion(String regionId) {
        ENTITY_INTERPOLATIONS.entrySet().removeIf(entry -> entry.getValue().regionId().equals(regionId));
    }

    public static void clear() {
        ENTITY_TICK_BUFFER.clear();
        ENTITY_INTERPOLATIONS.clear();
    }

    public static void register(){
        ClientPlayNetworking.registerGlobalReceiver(RegionStepPayload.ID, (payload, context) -> context.client().execute(() -> {
            ClientWorld world = context.client().world;
            if (world == null) return;

            ClientRegion region = ClientRegionManager.getRegion(payload.regionID());
            if (region == null) return;

            region.setPendingSteps(payload.steps());
        }));

        ClientPlayNetworking.registerGlobalReceiver(RegionEntitySyncPayload.ID, (payload, context) -> context.client().execute(() -> {
            ClientWorld world = context.client().world;
            if (world == null) return;

            applyEntityStates(world, payload.entities());
        }));

        ClientPlayNetworking.registerGlobalReceiver(RegionTimePayload.ID, (payload, context) -> context.client().execute(() -> {
            ClientRegion region = ClientRegionManager.getRegion(payload.regionId());
            if (region != null) {
                region.updateVirtualTime(payload.virtualTime());
            }
        }));

        //乘客状态同步
        ClientPlayNetworking.registerGlobalReceiver(PassengerSyncPayload.ID, (payload, context) -> context.client().execute(() -> {
            ClientWorld world = context.client().world;
            if (world == null) return;

            applyPassengerStates(world, payload);
        }));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientWorld world = client.world;
            if (world == null) return;

            for (ClientRegion region : ClientRegionManager.getRegions()) {
                if (!region.isControlled()) continue;

                if (region.isStepping() && !region.isRunning()) {
                    int stepsToTake = region.accumulateSteps();
                    stepsToTake = region.consumePendingSteps(stepsToTake);
                    if (stepsToTake > 0) {
                        tickRegion(world, region, region.getChunkPositions(), stepsToTake);
                        region.recordStep();
                    }
                    continue;
                }

                if (region.isRunning()) {
                    int stepsToTake = region.accumulateSteps();
                    if (stepsToTake > 0) {
                        tickRegion(world, region, region.getChunkPositions(), stepsToTake);
                        region.recordStep();
                    }
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

    private static void applyPassengerStates(ClientWorld world, PassengerSyncPayload payload) {
        Entity passenger = world.getEntityById(payload.passengerId());
        if (payload.vehicleID() != -1 && passenger != null){
            Entity vehicle = world.getEntityById(payload.vehicleID());
            if (vehicle != null) {
                passenger.startRiding(vehicle, true);
            }
        }else if (payload.vehicleID() == -1 && passenger != null){
            passenger.stopRiding();
        }



    }

    private static void tickRegion(ClientWorld world, ClientRegion region, Set<Long> chunkSet, int stepsTaken) {
        Map<Integer, Vec3d> previousPositions = new HashMap<>();
        Map<Integer, Entity> tickedEntities = new HashMap<>();

        for (int i = 0; i < stepsTaken; i++) {
            RegionTickContext.begin(world, region.nextVirtualTime());

            ClientTickBridge.setCustomTickInProgress(true);
            try {
                if (!region.isDisableEntityTick()) {
                    collectTickingEntities(world, chunkSet);
                    for (Entity entity : ENTITY_TICK_BUFFER) {
                        if (entity.isRemoved()) continue;

                        previousPositions.putIfAbsent(entity.getId(), entity.getPos());
                        tickedEntities.put(entity.getId(), entity);
                        ((ClientWorldAccessor) world).invokeTickEntity(entity);
                    }
                }

                ENTITY_TICK_BUFFER.clear();
                if (!region.isDisableHopperTick()) {
                    ClientRegionBlockEntityTicker.tickBlockEntities(world, chunkSet);
                }
            } finally {
                ClientTickBridge.setCustomTickInProgress(false);
                RegionTickContext.end();
            }
        }

        for (Map.Entry<Integer, Entity> entry : tickedEntities.entrySet()) {
            Entity entity = entry.getValue();
            Vec3d previous = previousPositions.get(entry.getKey());
            if (previous != null && !entity.isRemoved()) {
                ENTITY_INTERPOLATIONS.put(entry.getKey(), new EntityInterpolationManager.EntityRenderInterpolation(region.getId(), previous, entity.getPos()));
            }
        }
    }


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
