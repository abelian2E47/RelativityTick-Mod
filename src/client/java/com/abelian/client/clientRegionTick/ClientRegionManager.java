package com.abelian.client.clientRegionTick;

import com.abelian.network.RegionStatusPayload;
import com.abelian.network.RegionStatusRequestPayload;
import com.abelian.network.RegionSyncPayload;
import com.abelian.network.RegionTPSPayload;
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
    private static final Map<String, RegionStatusPayload> LIVE_STATUS = new ConcurrentHashMap<>();
    private static final Map<String, Long> LIVE_STATUS_NANOS = new ConcurrentHashMap<>();

    public static void register(){
        ClientPlayNetworking.registerGlobalReceiver(RegionSyncPayload.ID, (payload, context) -> context.client().execute(() -> {
            ClientRegion region = REGIONS.get(payload.id());
            if (payload.chunkPositions().isEmpty()) {
                removeChunkIndex(payload.id());
                REGIONS.remove(payload.id());
                ClientRegionTicker.clearRegion(payload.id());
                ClientScheduledTickManager.clearRegion(payload.id());
                return;
            }

            if (region == null) {
                region = new ClientRegion(payload);
                REGIONS.put(payload.id(), region);
            } else {
                removeChunkIndex(payload.id());
                region.updateRegionState(payload);
            }

            indexChunks(payload.id(), payload.dimension(), payload.chunkPositions());
        }));

        ClientPlayNetworking.registerGlobalReceiver(RegionTPSPayload.ID, (payload, context) -> context.client().execute(() -> {
            ClientRegion region = REGIONS.get(payload.regionID());
            if (region != null) {
                region.updateRegionTPS(payload);
            }
        }));

        //面板按需拉取的实时快照：只在玩家打开监测/配置面板时才回包，存起来给界面读
        ClientPlayNetworking.registerGlobalReceiver(RegionStatusPayload.ID, (payload, context) -> context.client().execute(() -> {
            LIVE_STATUS.put(payload.regionId(), payload);
            LIVE_STATUS_NANOS.put(payload.regionId(), System.nanoTime());
        }));
    }

    /** 向服务端要一份该区域"此刻"的快照；面板每 4 tick 调一次。 */
    public static void requestLiveStatus(String regionId) {
        if (regionId == null) return;
        if (!ClientPlayNetworking.canSend(RegionStatusRequestPayload.ID)) return;
        ClientPlayNetworking.send(new RegionStatusRequestPayload(regionId));
    }

    /** 最近一次拉取到的快照；没拉过（或已过期）时为 null，界面据此显示"正在获取"。 */
    public static RegionStatusPayload getLiveStatus(String regionId) {
        return regionId == null ? null : LIVE_STATUS.get(regionId);
    }

    public static long getLiveStatusAgeMillis(String regionId) {
        Long nanos = regionId == null ? null : LIVE_STATUS_NANOS.get(regionId);
        return nanos == null ? -1L : (System.nanoTime() - nanos) / 1_000_000L;
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

    public static boolean isRegionControlled(ClientWorld world, ChunkPos chunkPos) {
        ClientRegion region = getRegion(world, chunkPos);
        return region != null && region.isControlled();
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
        LIVE_STATUS.clear();
        LIVE_STATUS_NANOS.clear();
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
