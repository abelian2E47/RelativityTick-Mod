package com.abelian.client.clientRegionTick;

import com.abelian.network.ScheduledTickDataPayload;
import com.abelian.network.ScheduledTickRecord;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClientScheduledTickManager {
    private static volatile List<ScheduledTickRecord> scheduledTicks = List.of();
    private static volatile long receiveTime = 0L;

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ScheduledTickDataPayload.ID, (payload, context) -> context.client().execute(() -> {
            scheduledTicks = payload.scheduledTicks();
            receiveTime = System.currentTimeMillis();
        }));
    }

    public record ScheduledTickDisplay(Vec3d pos, long remainingTick, int subOrderRank, int priority) { }

    public static List<ScheduledTickDisplay> getDisplayData() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world == null) return List.of();

        List<ScheduledTickRecord> records = getScheduledTicks();
        if (records.isEmpty()) return List.of();

        // 按区域分组：subTickOrder 是调度器内序号，只在同一区域内可比
        Map<ClientRegion, List<ScheduledTickRecord>> recordsByRegion = new LinkedHashMap<>();
        for (ScheduledTickRecord record : records) {
            BlockPos pos = record.pos();
            if (!world.getChunkManager().isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) continue;

            ClientRegion region = ClientRegionManager.getRegion(world, new ChunkPos(pos));
            if (region == null || !region.isControlled()) continue;
            recordsByRegion.computeIfAbsent(region, r -> new ArrayList<>()).add(record);
        }

        // 区域内的计划刻按 subTickOrder 升序从 1 开始重排，渲染时展示重排后的序号
        List<ScheduledTickDisplay> displays = new ArrayList<>();
        for (Map.Entry<ClientRegion, List<ScheduledTickRecord>> entry : recordsByRegion.entrySet()) {
            ClientRegion region = entry.getKey();
            List<ScheduledTickRecord> regionRecords = entry.getValue();
            regionRecords.sort(Comparator.comparingLong(ScheduledTickRecord::subTickOrder));
            int rank = 1;
            for (ScheduledTickRecord record : regionRecords) {
                long remaining = record.trigger() - region.getVirtualTime();
                displays.add(new ScheduledTickDisplay(
                        new Vec3d(record.pos().getX() + 0.5, record.pos().getY(), record.pos().getZ() + 0.5),
                        remaining, rank++, record.priority()));
            }
        }
        return displays;
    }

    public static List<ScheduledTickRecord> getScheduledTicks() {
        if (receiveTime <= 0) {
            return List.of();
        }
        return scheduledTicks;
    }

    public static void clear() {
        scheduledTicks = List.of();
        receiveTime = 0L;
    }
}
