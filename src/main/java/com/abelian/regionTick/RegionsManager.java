package com.abelian.regionTick;

import com.abelian.RegionPersistentState;
import com.abelian.network.RegionSyncPayload;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraft.world.tick.ChunkTickScheduler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.abelian.RelativityTickUtils.getServer;

public class RegionsManager {
    private static final Map<String, RegionTickManager> ID_TO_REGION = new LinkedHashMap<>();

    //维度 → 区块 → 区域 的分层表。原来用 record 复合键，每次查询都要 new 一个 record（24B + 哈希），
    //而 getRegionByChunk 在原版实体刻（每实体每刻）、方块实体 tick、区块刻上都会走；
    //现在外层只按维度取值，内层直接 long 主键寻址，查询零分配。
    private static final Map<RegistryKey<World>, Long2ObjectMap<RegionTickManager>> CHUNK_TO_REGION = new HashMap<>();
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
        ID_TO_REGION.put(id, newRegion);

        for (long pos : chunks) {
            removeChunkFromCurrentRegion(pos, world);
            putMapping(world.getRegistryKey(), pos, newRegion);
        }

        syncRegion(id, newRegion);
        savePersistentState();
    }

    public static boolean addChunkToRegion(String id, long chunkPos, ServerWorld world) {
        String currentId = getRegionId(world.getRegistryKey(), chunkPos);
        if (currentId != null) {
            if (!currentId.equals(id)) return false;
            RegionTickManager self = ID_TO_REGION.get(id);
            if (self != null) syncRegion(id, self);
            return true;
        }

        RegionTickManager region = ID_TO_REGION.get(id);
        if (region == null) {
            createRegion(id, Set.of(chunkPos), world);
            return true;
        }

        region.addChunk(chunkPos, world);
        putMapping(world.getRegistryKey(), chunkPos, region);
        syncRegion(id, region);
        savePersistentState();
        return true;
    }

    public static int addChunksToRegion(String id, Set<Long> chunkPositions, ServerWorld world) {
        if (chunkPositions.isEmpty()) return 0;

        Set<Long> freeChunks = new HashSet<>();
        for (long chunkPos : chunkPositions) {
            if (getRegionId(world.getRegistryKey(), chunkPos) == null) freeChunks.add(chunkPos);
        }

        RegionTickManager region = ID_TO_REGION.get(id);
        if (region == null) {
            if (freeChunks.isEmpty()) return 0;
            createRegion(id, freeChunks, world);
            return freeChunks.size();
        }

        if (!region.isInWorld(world)) return 0;

        int added = 0;
        for (long chunkPos : freeChunks) {
            if (region.addChunk(chunkPos, world)) {
                added++;
                putMapping(world.getRegistryKey(), chunkPos, region);
            }
        }

        if (added > 0) {
            syncRegion(id, region);
            savePersistentState();
        }
        return added;
    }

    public static void removeChunkFromRegion(String id, long chunkPos, ServerWorld world) {
        RegionTickManager region = ID_TO_REGION.get(id);
        if (region == null || !region.isInWorld(world)) return;

        RegistryKey<World> dimension = world.getRegistryKey();
        if (getByChunk(dimension, chunkPos) != region) return;
        if (!region.removeChunk(chunkPos, world)) return;

        removeMapping(dimension, chunkPos, region);
        if (region.getChunkPositions().isEmpty()) {
            ID_TO_REGION.remove(id, region);
            removeMappings(region);
        }

        syncRegion(id, region);
        savePersistentState();
    }

    public static void removeRegion(String id) {
        RegionTickManager region = ID_TO_REGION.get(id);
        if (region == null) return;

        releaseControl(region);
        region.setState(RegionTickManager.RegionState.RELEASED);
        region.cancelAllPendingSteps();
        region.setAccumulator(0.0);

        if (!ID_TO_REGION.remove(id, region)) return;
        removeMappings(region);
        syncRegionRemoval(id, region);
        savePersistentState();
    }

    public static RegionTickManager getRegionByChunk(ServerWorld world, long chunkPos) {
        return getByChunk(world.getRegistryKey(), chunkPos);
    }

    public static RegionTickManager getControlledRegionByScheduler(ChunkTickScheduler<?> scheduler) {
        RegionTickManager region = ControlledSchedulerRegistry.getRegion(scheduler);
        return region != null && region.isControlled() ? region : null;
    }

    public static String getRegionIdByChunk(ServerWorld world, long chunkPos) {
        return getRegionId(world.getRegistryKey(), chunkPos);
    }

    public static RegionTickManager getRegion(String id) {
        return ID_TO_REGION.get(id);
    }

    public static Set<String> getRegionIds() {
        return ID_TO_REGION.keySet();
    }

    public static List<String> getRegionIdsInOrder() {
        return List.copyOf(ID_TO_REGION.keySet());
    }

    public static void restorePersistentStates() {
        shuttingDown = false;
        for (RegionTickManager region : ID_TO_REGION.values()) {
            region.cancelAllPendingSteps();
            region.setAccumulator(0.0);
        }
    }

    public static void prepareForShutdown() {
        shuttingDown = true;
        for (RegionTickManager region : ID_TO_REGION.values()) {
            releaseControl(region);
            region.setState(RegionTickManager.RegionState.RELEASED);
            region.cancelAllPendingSteps();
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
            region.releaseChunkToWorld(chunkPos, world);
        }
    }

    public static void onChunkUnload(ServerWorld world, long chunkPos) {
        if (shuttingDown) return;
        RegionTickManager region = getRegionByChunk(world, chunkPos);
        if (region == null || !region.isControlled()) return;
        region.detachChunk(chunkPos, world);
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
        loadedFromPersistentState = false;
        shuttingDown = false;
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
            ID_TO_REGION.put(id, region);

            for (long chunkPos : data.chunks()) {
                putMapping(data.dimension(), chunkPos, region);
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

    private static void removeChunkFromCurrentRegion(long chunkPos, ServerWorld world) {
        RegistryKey<World> dimension = world.getRegistryKey();
        String currentId = getRegionId(dimension, chunkPos);
        if (currentId == null) return;

        RegionTickManager currentRegion = ID_TO_REGION.get(currentId);

        if (currentRegion != null) {
            currentRegion.removeChunk(chunkPos, world);
            if (currentRegion.getChunkPositions().isEmpty()) {
                ID_TO_REGION.remove(currentId);
                removeMappings(currentRegion);
            }
            syncRegion(currentId, currentRegion);
        }
        removeMapping(dimension, chunkPos);
    }

    private static void releaseControl(RegionTickManager region) {
        if (!region.isControlled()) return;

        ServerWorld world = getServer().getWorld(region.getDimension());
        if (world == null) return;
        region.releaseRegion(world.getBlockTickScheduler(), world.getTime());
        region.releaseRegion(world.getFluidTickScheduler(), world.getTime());
    }

    private static void removeMappings(RegionTickManager region) {
        for (Long2ObjectMap<RegionTickManager> mappings : CHUNK_TO_REGION.values()) {
            mappings.values().removeIf(mapped -> mapped == region);
        }
        ControlledSchedulerRegistry.clearRegion(region);
    }

    private static RegionTickManager getByChunk(RegistryKey<World> dimension, long chunkPos) {
        Long2ObjectMap<RegionTickManager> mappings = CHUNK_TO_REGION.get(dimension);
        return mappings == null ? null : mappings.get(chunkPos);
    }

    private static void putMapping(RegistryKey<World> dimension, long chunkPos, RegionTickManager region) {
        CHUNK_TO_REGION.computeIfAbsent(dimension, ignored -> new Long2ObjectOpenHashMap<>()).put(chunkPos, region);
    }

    private static void removeMapping(RegistryKey<World> dimension, long chunkPos) {
        Long2ObjectMap<RegionTickManager> mappings = CHUNK_TO_REGION.get(dimension);
        if (mappings != null) mappings.remove(chunkPos);
    }

    //只在确认当前映射就是该区域时移除，等价于原来的 Map.remove(key, value)
    private static void removeMapping(RegistryKey<World> dimension, long chunkPos, RegionTickManager region) {
        Long2ObjectMap<RegionTickManager> mappings = CHUNK_TO_REGION.get(dimension);
        if (mappings != null && mappings.get(chunkPos) == region) mappings.remove(chunkPos);
    }

    private static String getRegionId(RegistryKey<World> dimension, long chunkPos) {
        RegionTickManager region = getByChunk(dimension, chunkPos);
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
}
