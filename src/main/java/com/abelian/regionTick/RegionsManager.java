package com.abelian.regionTick;

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
    private static final Map<String, Region> ID_TO_REGION = new HashMap<>();
    private record DimensionChunkKey(RegistryKey<World> dimension, long chunkPos) {}

    private static final Map<DimensionChunkKey, Region> CHUNK_TO_REGION = new HashMap<>();
    private static boolean loadedFromPersistentState = false;
    private static boolean shuttingDown = false;

    public static void createRegion(String id, Set<Long> chunkPositions, ServerWorld world) {
        Region existing = ID_TO_REGION.remove(id);
        if (existing != null) {
            releaseControl(existing);
            removeMappings(existing);
        }

        Set<Long> chunks = new HashSet<>(chunkPositions);
        Region newRegion = new Region(id, world.getRegistryKey(), chunks);
        newRegion.setRegionPriority(nextAvailablePriority());
        ID_TO_REGION.put(id, newRegion);

        for (long pos : chunks) {
            removeChunkFromCurrentRegion(pos, world);
            DimensionChunkKey key = key(world, pos);
            CHUNK_TO_REGION.put(key, newRegion);
        }

        syncRegion(id, newRegion);
        savePersistentState();
    }

    public static boolean addChunkToRegion(String id, long chunkPos, ServerWorld world) {
        Region region = ID_TO_REGION.get(id);
        if (region == null) {
            createRegion(id, Set.of(chunkPos), world);
            return true;
        }

        if (!region.isInWorld(world)) return false;

        String currentId = getRegionId(key(world, chunkPos));
        if (id.equals(currentId)) {
            syncRegion(id, region);
            return false;
        }

        removeChunkFromCurrentRegion(chunkPos, world);
        boolean added = region.addChunk(chunkPos, world);
        DimensionChunkKey key = key(world, chunkPos);
        CHUNK_TO_REGION.put(key, region);
        syncRegion(id, region);
        savePersistentState();
        return added;
    }

    public static int addChunksToRegion(String id, Set<Long> chunkPositions, ServerWorld world) {
        if (chunkPositions.isEmpty()) return 0;

        Region region = ID_TO_REGION.get(id);
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

    public static boolean removeChunkFromRegion(String id, long chunkPos, ServerWorld world) {
        Region region = ID_TO_REGION.get(id);
        if (region == null || !region.removeChunk(chunkPos, world)) {
            return false;
        }

        DimensionChunkKey key = key(world, chunkPos);
        CHUNK_TO_REGION.remove(key);
        if (region.getChunkPositions().isEmpty()) {
            ID_TO_REGION.remove(id);
        }
        syncRegion(id, region);
        savePersistentState();
        return true;
    }

    public static void removeRegion(String id) {
        Region region = ID_TO_REGION.remove(id);
        if (region != null) {
            removeMappings(region);
            savePersistentState();
        }
    }

    public static Region getRegionByChunk(ServerWorld world, long chunkPos) {
        return CHUNK_TO_REGION.get(key(world, chunkPos));
    }

    public static Region getControlledRegionByScheduler(ChunkTickScheduler<?> scheduler) {
        Region region = ControlledSchedulerRegistry.getRegion(scheduler);
        return region != null && region.isControlled() ? region : null;
    }

    public static String getRegionIdByChunk(ServerWorld world, long chunkPos) {
        return getRegionId(key(world, chunkPos));
    }

    public static Region getRegion(String id) {
        return ID_TO_REGION.get(id);
    }

    public static Set<String> getRegionIds() {
        return ID_TO_REGION.keySet();
    }

    public static List<String> getRegionIdsByPriority() {
        return ID_TO_REGION.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> entry.getValue().getRegionPriority()))
                .map(Map.Entry::getKey)
                .toList();
    }

    public static void restorePersistentStates() {
        shuttingDown = false;
        for (Region region : ID_TO_REGION.values()) {
            region.setPendingSteps(0);
            region.setAccumulator(0.0);
            if (!region.isControlled()) continue;

            ServerWorld world = getServer().getWorld(region.getDimension());
            if (world == null) continue;
            region.setFreezeStartTime(world.getTime());
            region.takeOverRegion(world.getBlockTickScheduler(), world.getTime());
            region.takeOverRegion(world.getFluidTickScheduler(), world.getTime());
        }
    }

    public static void prepareForShutdown() {
        savePersistentState();
        shuttingDown = true;
        for (Region region : ID_TO_REGION.values()) {
            region.setPendingSteps(0);
            region.setAccumulator(0.0);
            releaseControl(region);
        }
    }

    public static void onChunkLoad(ServerWorld world, long chunkPos) {
        if (shuttingDown) return;
        Region region = getRegionByChunk(world, chunkPos);
        if (region != null && region.isControlled()) {
            region.takeOverChunk(chunkPos, world);
        }
    }

    public static void onChunkUnload(ServerWorld world, long chunkPos) {
        Region region = getRegionByChunk(world, chunkPos);
        if (region != null && region.isControlled()) {
            region.releaseChunk(chunkPos, world);
        }
    }

    public static void syncAllRegions(ServerPlayerEntity player) {
        for (Map.Entry<String, Region> entry : ID_TO_REGION.entrySet()) {
            ServerPlayNetworking.send(player, createSyncPayload(entry.getKey(), entry.getValue()));
        }
    }

    public static void clear() {
        for (Region region : ID_TO_REGION.values()) {
            ControlledSchedulerRegistry.clearRegion(region);
        }
        ID_TO_REGION.clear();
        CHUNK_TO_REGION.clear();
        loadedFromPersistentState = false;
        shuttingDown = false;
    }

    public static void setRegionPriority(String id, int priority) {
        Region region = ID_TO_REGION.get(id);
        if (region != null) {
            region.setRegionPriority(priority);
            savePersistentState();
        }
    }

    public static boolean isPriorityAvailable(int priority, String exceptId) {
        if (priority < 1) return false;
        return ID_TO_REGION.entrySet().stream().noneMatch(entry -> !entry.getKey().equals(exceptId) && entry.getValue().getRegionPriority() == priority);
    }

    public static void setRegionTickDurationLimit(String id, double maxRegionCostMs) {
        Region region = ID_TO_REGION.get(id);
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
            Region region = new Region(id, data.dimension(), data.chunks());
            region.setRate(data.rate());
            region.setMaxRegionCostMs(data.tickDurationLimit());
            int priority = data.regionPriority();
            region.setRegionPriority(isPriorityAvailable(priority, id) ? priority : nextAvailablePriority());
            region.setState(data.state());
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
        Map<String, RegionPersistentState.RegionData> regions = new HashMap<>();
        for (Map.Entry<String, Region> entry : ID_TO_REGION.entrySet()) {
            Region region = entry.getValue();
            regions.put(entry.getKey(), new RegionPersistentState.RegionData(
                    region.getDimension(),
                    region.getChunkPositions(),
                    region.getRate(),
                    region.getTickDurationLimit(),
                    region.getRegionPriority(),
                    region.getState()
            ));
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

        Region currentRegion = ID_TO_REGION.get(currentId);

        if (currentRegion != null) {
            currentRegion.removeChunk(chunkPos, world);
            if (currentRegion.getChunkPositions().isEmpty()) {
                ID_TO_REGION.remove(currentId);
            }
            syncRegion(currentId, currentRegion);
        }
        CHUNK_TO_REGION.remove(key);
    }

    private static void releaseControl(Region region) {
        if (!region.isControlled()) return;

        ServerWorld world = getServer().getWorld(region.getDimension());
        if (world == null) return;
        region.releaseRegion(world.getBlockTickScheduler(), world.getTime());
        region.releaseRegion(world.getFluidTickScheduler(), world.getTime());
    }

    private static void removeMappings(Region region) {
        CHUNK_TO_REGION.entrySet().removeIf(entry -> entry.getValue() == region);
        ControlledSchedulerRegistry.clearRegion(region);
    }

    private static String getRegionId(DimensionChunkKey key) {
        Region region = CHUNK_TO_REGION.get(key);
        return region == null ? null : region.getID();
    }

    private static void syncRegion(String id, Region region) {
        ServerWorld serverWorld = getServer().getWorld(region.getDimension());
        if (serverWorld == null) return;

        RegionSyncPayload payload = createSyncPayload(id, region);
        serverWorld.getPlayers().forEach(player -> ServerPlayNetworking.send(player, payload));
    }

    private static RegionSyncPayload createSyncPayload(String id, Region region) {
        return new RegionSyncPayload(id, region.getDimensionId(), region.getChunkPositions(),
                region.getState(), region.getRate());
    }

    private static DimensionChunkKey key(ServerWorld world, long chunkPos) {
        return new DimensionChunkKey(world.getRegistryKey(), chunkPos);
    }
}
