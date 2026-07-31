package com.abelian;

import com.abelian.mixin.ServerChunkManagerAccessor;
import com.abelian.mixin.ServerWorldAccessor;
import net.minecraft.world.EntityList;
import java.util.ArrayList;
import java.util.List;
import com.abelian.regionTick.RegionTickManager;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.SpawnDensityCapper;
import net.minecraft.world.SpawnHelper;

import java.util.IdentityHashMap;
import java.util.Map;

public class ServerTickBridge {
    private static final ThreadLocal<Boolean> CUSTOM_TICK_IN_PROGRESS = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Map<Entity, RegionTickManager>> REGION_TICK_OWNERS =
            ThreadLocal.withInitial(IdentityHashMap::new);
    private static final ThreadLocal<Map<ServerWorld, SpawnHelper.Info>> SPAWN_INFOS =
            ThreadLocal.withInitial(IdentityHashMap::new);
    private static final ThreadLocal<IdentityHashMap<EntityList, EntityListState>> ENTITY_LIST_STATES =
            ThreadLocal.withInitial(IdentityHashMap::new);

    public static boolean isCustomTickInProgress() {
        return CUSTOM_TICK_IN_PROGRESS.get();
    }

    public static void setCustomTickInProgress(boolean value) {
        CUSTOM_TICK_IN_PROGRESS.set(value);
    }

    public static void beginRegionTickBatch() {
        REGION_TICK_OWNERS.get().clear();
        SPAWN_INFOS.get().clear();
        ENTITY_LIST_STATES.get().clear();
    }

    public static void markEntityListMutated(EntityList entityList) {
        EntityListState state = ENTITY_LIST_STATES.get().get(entityList);
        if (state != null) {
            state.revision++;
        }
    }

    public static List<Entity> getOrderedEntitySnapshot(ServerWorld world) {
        EntityList entityList = ((ServerWorldAccessor) world).getEntityList();
        IdentityHashMap<EntityList, EntityListState> states = ENTITY_LIST_STATES.get();
        EntityListState state = states.get(entityList);
        if (state == null) {
            state = new EntityListState();
            states.put(entityList, state);
        }

        if (state.snapshot != null && state.snapshotRevision == state.revision) {
            return state.snapshot;
        }

        List<Entity> snapshot = new ArrayList<>();
        entityList.forEach(snapshot::add);
        state.snapshot = snapshot;
        state.snapshotRevision = state.revision;
        return snapshot;
    }

    private static final class EntityListState {
        private long revision;
        private long snapshotRevision = Long.MIN_VALUE;
        private List<Entity> snapshot;
    }

    public static boolean claimEntity(Entity entity, RegionTickManager region) {
        RegionTickManager owner = REGION_TICK_OWNERS.get().putIfAbsent(entity, region);
        return owner == null || owner == region;
    }

    public static SpawnHelper.Info getSpawnInfo(ServerWorld world) {
        return SPAWN_INFOS.get().computeIfAbsent(world, ServerTickBridge::createSpawnInfo);
    }

    private static SpawnHelper.Info createSpawnInfo(ServerWorld world) {
        ServerChunkManagerAccessor managerAccessor = (ServerChunkManagerAccessor) world.getChunkManager();
        return SpawnHelper.setupSpawn(
                managerAccessor.getTicketManager().getTickedChunkCount(),
                world.iterateEntities(),
                managerAccessor::invokeIfChunkLoaded,
                new SpawnDensityCapper(managerAccessor.getChunkLoadingManager())
        );
    }
}
