package com.abelian.client;

import com.abelian.network.RegionSyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkPos;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClientRegionManager {
    private static final Map<String, RegionSyncPayload> REGIONS = new ConcurrentHashMap<>();
    private record DimensionChunkKey(String dimension, long chunkPos) {}

    private static final Map<DimensionChunkKey, String> CHUNK_TO_REGION_ID = new ConcurrentHashMap<>();

    public static void register(){
        ClientPlayNetworking.registerGlobalReceiver(RegionSyncPayload.ID, (payload, context) -> context.client().execute(() -> {
            removeChunkIndex(payload.id());
            if (payload.chunkPositions().isEmpty()) {
                REGIONS.remove(payload.id());
                ClientRegionTicker.clearRegion(payload.id());
                RegionTickDeltaManager.clearRegion(payload.id());
            } else {
                REGIONS.put(payload.id(), payload);
                indexChunks(payload.id(), payload.dimension(), payload.chunkPositions());
            }
        }));
    }

    public static String getRegionIDFromChunk(ClientWorld world, ChunkPos chunkPos){
        return CHUNK_TO_REGION_ID.get(key(world, chunkPos.toLong()));
    }

    public static RegionSyncPayload getRegion(String id){
        RegionSyncPayload region = REGIONS.get(id);
        return isCurrentDimension(region) ? region : null;
    }

    public static RegionSyncPayload getRegion(ClientWorld world, ChunkPos chunkPos) {
        String id = getRegionIDFromChunk(world, chunkPos);
        return id == null ? null : REGIONS.get(id);
    }

    public static boolean isRegionControlled(ClientWorld world, ChunkPos chunkPos) {
        RegionSyncPayload regionData = getRegion(world, chunkPos);
        return regionData != null && regionData.isControlled();
    }

    public static boolean isRegionFrozenAndIdle(RegionSyncPayload regionData) {
        return regionData != null && regionData.isControlled() && !regionData.isRunning() && !regionData.stepping();
    }
    
    private static void removeChunkIndex(String id) {
        RegionSyncPayload oldRegion = REGIONS.get(id);
        if (oldRegion == null) return;

        for (long chunkPos : oldRegion.chunkPositions()) {
            CHUNK_TO_REGION_ID.remove(new DimensionChunkKey(oldRegion.dimension(), chunkPos), id);
        }
    }

    private static void indexChunks(String id, String dimension, Set<Long> chunkPositions) {
        for (long chunkPos : chunkPositions) {
            CHUNK_TO_REGION_ID.put(new DimensionChunkKey(dimension, chunkPos), id);
        }
    }

    public static void clear() {
        REGIONS.clear();
        CHUNK_TO_REGION_ID.clear();
    }

    private static DimensionChunkKey key(ClientWorld world, long chunkPos) {
        return new DimensionChunkKey(world.getRegistryKey().getValue().toString(), chunkPos);
    }

    private static boolean isCurrentDimension(RegionSyncPayload region) {
        ClientWorld world = MinecraftClient.getInstance().world;
        return region != null && world != null && region.dimension().equals(world.getRegistryKey().getValue().toString());
    }

    public static Collection<RegionSyncPayload> getRegions() {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) return java.util.List.of();
        String dimension = world.getRegistryKey().getValue().toString();
        return REGIONS.values().stream().filter(region -> region.dimension().equals(dimension)).toList();
    }

}
