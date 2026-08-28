package com.abelian.regionTick;

import com.abelian.RegionPersistentState;
import com.abelian.network.RegionSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraft.world.tick.ChunkTickScheduler;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.abelian.RelativityTickUtils.getServer;

public class RegionsManager {
    private static final Map<String, RegionTickManager> ID_TO_REGION = new HashMap<>();
    private record DimensionChunkKey(RegistryKey<World> dimension, long chunkPos) {}

    private static final Map<DimensionChunkKey, RegionTickManager> CHUNK_TO_REGION = new HashMap<>();
    private static List<String> REGION_IDS_BY_PRIORITY = List.of();
    private static boolean regionPriorityOrderDirty = true;
    private static boolean loadedFromPersistentState = false;
    private static boolean shuttingDown = false;

    public static void createRegion(String id, Set<Long> chunkPositions, ServerWorld world) {
        RegionTickManager existing = ID_TO_REGION.remove(id);
        if (existing != null) {
            releaseControl(existing);
            removeMappings(existing);
        }

        Set<Long> chunks = new HashSet<>(chunkPositions);
        RegionTickManager newRegion = new RegionTickManager(id, world.getRegistryKey(), chunks);
        newRegion.setRegionPriority(nextAvailablePriority());
        regionPriorityOrderDirty = true;
        ID_TO_REGION.put(id, newRegion);

        for (long pos : chunks) {
            removeChunkFromCurrentRegion(pos, world);
            DimensionChunkKey key = key(world, pos);
            CHUNK_TO_REGION.put(key, newRegion);
        }

        syncRegion(id, newRegion);
        savePersistentState();
    }

    public static void addChunkToRegion(String id, long chunkPos, ServerWorld world) {
        RegionTickManager region = ID_TO_REGION.get(id);
        if (region == null) {
            createRegion(id, Set.of(chunkPos), world);
            return;
        }

        String currentId = getRegionId(key(world, chunkPos));
        if (id.equals(currentId)) {
            syncRegion(id, region);
            return;
        }

        removeChunkFromCurrentRegion(chunkPos, world);
        region.addChunk(chunkPos, world);
        CHUNK_TO_REGION.put(key(world, chunkPos), region);
        syncRegion(id, region);
        savePersistentState();
    }

    public static int addChunksToRegion(String id, Set<Long> chunkPositions, ServerWorld world) {
        if (chunkPositions.isEmpty()) return 0;

        RegionTickManager region = ID_TO_REGION.get(id);
        if (region == null) {
            createRegion(id, chunkPositions, world);
            return chunkPositions.size();
        }

        if (!region.isInWorld(world)) return 0;

        int added = 0;
        for (long chunkPos : chunkPositions) {
            String currentId = getRegionId(key(world, chunkPos));
            if (id.equals(currentId)) continue;

            removeChunkFromCurrentRegion(chunkPos, world);
            if (region.addChunk(chunkPos, world)) {
                added++;
            }
            DimensionChunkKey key = key(world, chunkPos);
            CHUNK_TO_REGION.put(key, region);
        }

        syncRegion(id, region);
        savePersistentState();
        return added;
    }

    public static void removeChunkFromRegion(String id, long chunkPos, ServerWorld world) {
        RegionTickManager region = ID_TO_REGION.get(id);
        if (region == null || !region.isInWorld(world)) return;

        DimensionChunkKey key = key(world, chunkPos);
        if (CHUNK_TO_REGION.get(key) != region) return;
        if (!region.removeChunk(chunkPos, world)) return;

        CHUNK_TO_REGION.remove(key, region);
        if (region.getChunkPositions().isEmpty()) {
            ID_TO_REGION.remove(id, region);
            removeMappings(region);
            regionPriorityOrderDirty = true;
        }

        syncRegion(id, region);
        savePersistentState();
    }

    public static void removeRegion(String id) {
        RegionTickManager region = ID_TO_REGION.get(id);
        if (region == null) return;

        releaseControl(region);
        region.setState(RegionTickManager.RegionState.RELEASED);
        region.setPendingSteps(0);
        region.setAccumulator(0.0);

        if (!ID_TO_REGION.remove(id, region)) return;
        regionPriorityOrderDirty = true;
        removeMappings(region);
        syncRegionRemoval(id, region);
        savePersistentState();
    }

    public static RegionTickManager getRegionByChunk(ServerWorld world, long chunkPos) {
        return CHUNK_TO_REGION.get(key(world, chunkPos));
    }

    public static RegionTickManager getControlledRegionByScheduler(ChunkTickScheduler<?> scheduler) {
        RegionTickManager region = ControlledSchedulerRegistry.getRegion(scheduler);
        return region != null && region.isControlled() ? region : null;
    }

    public static String getRegionIdByChunk(ServerWorld world, long chunkPos) {
        return getRegionId(key(world, chunkPos));
    }

    public static RegionTickManager getRegion(String id) {
        return ID_TO_REGION.get(id);
    }

    public static Set<String> getRegionIds() {
        return ID_TO_REGION.keySet();
    }

    public static List<String> getRegionIdsByPriority() {
        if (regionPriorityOrderDirty) {
            REGION_IDS_BY_PRIORITY = ID_TO_REGION.entrySet().stream()
                    .sorted(Comparator.comparingInt(entry -> entry.getValue().getRegionPriority()))
                    .map(Map.Entry::getKey)
                    .toList();
            regionPriorityOrderDirty = false;
        }
        return REGION_IDS_BY_PRIORITY;
    }

    public static void restorePersistentStates() {
        shuttingDown = false;
        for (RegionTickManager region : ID_TO_REGION.values()) {
            region.setPendingSteps(0);
            region.setAccumulator(0.0);
        }
    }

    public static void prepareForShutdown() {
        shuttingDown = true;
        for (RegionTickManager region : ID_TO_REGION.values()) {
            releaseControl(region);
            region.setState(RegionTickManager.RegionState.RELEASED);
            region.setPendingSteps(0);
            region.setAccumulator(0.0);
        }
        savePersistentState();
    }

    public static void onChunkLoad(ServerWorld world, long chunkPos) {
        if (shuttingDown) return;
        RegionTickManager region = getRegionByChunk(world, chunkPos);
        if (region == null) return;
        if (region.isControlled()) {
            region.takeOverChunk(chunkPos, world);
        } else {
            //区域已释放但区块带着虚拟时间线锚点落盘,还给 vanilla 前换算回真实时间线
            region.releaseChunkToWorld(chunkPos, world);
        }
    }

    public static void syncAllRegions(ServerPlayerEntity player) {
        for (Map.Entry<String, RegionTickManager> entry : ID_TO_REGION.entrySet()) {
            ServerPlayNetworking.send(player, createSyncPayload(entry.getKey(), entry.getValue()));
        }
    }

    public static void clear() {
        for (RegionTickManager region : ID_TO_REGION.values()) {
            ControlledSchedulerRegistry.clearRegion(region);
        }
        ID_TO_REGION.clear();
        CHUNK_TO_REGION.clear();
        REGION_IDS_BY_PRIORITY = List.of();
        regionPriorityOrderDirty = true;
        loadedFromPersistentState = false;
        shuttingDown = false;
    }

    public static void setRegionPriority(String id, int priority) {
        RegionTickManager region = ID_TO_REGION.get(id);
        if (region != null) {
            region.setRegionPriority(priority);
            regionPriorityOrderDirty = true;
            savePersistentState();
        }
    }

    public static boolean isPriorityAvailable(int priority, String exceptId) {
        if (priority < 1) return false;
        return ID_TO_REGION.entrySet().stream().noneMatch(entry -> !entry.getKey().equals(exceptId) && entry.getValue().getRegionPriority() == priority);
    }

    public static void setRegionTickDurationLimit(String id, double maxRegionCostMs) {
        RegionTickManager region = ID_TO_REGION.get(id);
        if (region != null) {
            region.setMaxRegionCostMs(maxRegionCostMs);
            savePersistentState();
        }
    }

    public static void loadPersistentState() {
        if (loadedFromPersistentState || getServer() == null || getServer().getOverworld() == null) return;
        RegionPersistentState state = getServer().getOverworld().getPersistentStateManager().getOrCreate(RegionPersistentState.getType(), RegionPersistentState.ID);
        ID_TO_REGION.clear();
        CHUNK_TO_REGION.clear();

        for (Map.Entry<String, RegionPersistentState.RegionData> entry : state.getRegions().entrySet()) {
            String id = entry.getKey();
            RegionPersistentState.RegionData data = entry.getValue();
            //重进后重新添加区域,保持非 controlled 状态,不恢复退出前的配置/时间线
            RegionTickManager region = new RegionTickManager(id, data.dimension(), data.chunks());
            region.setRegionPriority(nextAvailablePriority());
            ID_TO_REGION.put(id, region);

            for (long chunkPos : data.chunks()) {
                DimensionChunkKey key = new DimensionChunkKey(data.dimension(), chunkPos);
                CHUNK_TO_REGION.put(key, region);
            }
        }

        loadedFromPersistentState = true;
    }

    public static void savePersistentState() {
        if (getServer() == null || getServer().getOverworld() == null) return;
        RegionPersistentState state = getServer().getOverworld().getPersistentStateManager().getOrCreate(RegionPersistentState.getType(), RegionPersistentState.ID);
        Map<String, RegionPersistentState.RegionData> regions = new HashMap<>(ID_TO_REGION.size());
        for (Map.Entry<String, RegionTickManager> entry : ID_TO_REGION.entrySet()) {
            regions.put(entry.getKey(), entry.getValue().toPersistentData());
        }
        state.replaceRegions(regions);
    }

    private static int nextAvailablePriority() {
        int priority = 1;
        while (!isPriorityAvailable(priority, null)) {
            priority++;
        }
        return priority;
    }

    private static void removeChunkFromCurrentRegion(long chunkPos, ServerWorld world) {
        DimensionChunkKey key = key(world, chunkPos);
        String currentId = getRegionId(key);
        if (currentId == null) return;

        RegionTickManager currentRegion = ID_TO_REGION.get(currentId);

        if (currentRegion != null) {
            currentRegion.removeChunk(chunkPos, world);
            if (currentRegion.getChunkPositions().isEmpty()) {
                ID_TO_REGION.remove(currentId);
            }
            syncRegion(currentId, currentRegion);
        }
        CHUNK_TO_REGION.remove(key);
    }

    private static void releaseControl(RegionTickManager region) {
        if (!region.isControlled()) return;

        ServerWorld world = getServer().getWorld(region.getDimension());
        if (world == null) return;
        region.releaseRegion(world.getBlockTickScheduler(), world.getTime());
        region.releaseRegion(world.getFluidTickScheduler(), world.getTime());
    }

    private static void removeMappings(RegionTickManager region) {
        CHUNK_TO_REGION.entrySet().removeIf(entry -> entry.getValue() == region);
        ControlledSchedulerRegistry.clearRegion(region);
    }
    private static String getRegionId(DimensionChunkKey key) {
        RegionTickManager region = CHUNK_TO_REGION.get(key);
        return region == null ? null : region.getID();
    }

    private static void syncRegion(String id, RegionTickManager region) {
        RegionSyncPayload payload = createSyncPayload(id, region);
        getServer().getPlayerManager().getPlayerList()
                .forEach(player -> ServerPlayNetworking.send(player, payload));
    }

    private static void syncRegionRemoval(String id, RegionTickManager region) {
        RegionSyncPayload payload = new RegionSyncPayload(
                id,
                region.getDimensionId(),
                Set.of(),
                RegionTickManager.RegionState.RELEASED,
                region.getRate(),
                region.getVirtualTime(),
                region.isDisableHopperTick(),
                region.isDisableEntityTick(),
                region.isDisableObserverTick()
        );
        getServer().getPlayerManager().getPlayerList()
                .forEach(player -> ServerPlayNetworking.send(player, payload));
    }

    private static RegionSyncPayload createSyncPayload(String id, RegionTickManager region) {
        return new RegionSyncPayload(id, region.getDimensionId(), region.getChunkPositions(),
                region.getState(), region.getRate(), region.getVirtualTime(),
                region.isDisableHopperTick(), region.isDisableEntityTick(), region.isDisableObserverTick());
    }

    private static DimensionChunkKey key(ServerWorld world, long chunkPos) {
        return new DimensionChunkKey(world.getRegistryKey(), chunkPos);
    }
}
