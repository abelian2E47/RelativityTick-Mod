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
    private static final Map<String, ClientRegion> REGIONS = new ConcurrentHashMap<>();
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
                REGIONS.put(payload.id(), new ClientRegion(payload));
                indexChunks(payload.id(), payload.dimension(), payload.chunkPositions());
            }
        }));
    }

    public static String getRegionIDFromChunk(ClientWorld world, ChunkPos chunkPos){
        return CHUNK_TO_REGION_ID.get(key(world, chunkPos.toLong()));
    }

    public static ClientRegion getRegion(String id){
        ClientRegion region = REGIONS.get(id);
        return isCurrentDimension(region) ? region : null;
    }

    public static ClientRegion getRegion(ClientWorld world, ChunkPos chunkPos) {
        String id = getRegionIDFromChunk(world, chunkPos);
        return id == null ? null : REGIONS.get(id);
    }
    
    private static void removeChunkIndex(String id) {
        ClientRegion oldRegion = REGIONS.get(id);
        if (oldRegion == null) return;

        for (long chunkPos : oldRegion.getChunkPositions()) {
            CHUNK_TO_REGION_ID.remove(new DimensionChunkKey(oldRegion.getDimension(), chunkPos), id);
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

    private static boolean isCurrentDimension(ClientRegion region) {
        ClientWorld world = MinecraftClient.getInstance().world;
        return region != null && world != null && region.getDimension().equals(world.getRegistryKey().getValue().toString());
    }

    public static Collection<ClientRegion> getRegions() {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) return java.util.List.of();
        String dimension = world.getRegistryKey().getValue().toString();
        return REGIONS.values().stream().filter(region -> region.getDimension().equals(dimension)).toList();
    }

}
