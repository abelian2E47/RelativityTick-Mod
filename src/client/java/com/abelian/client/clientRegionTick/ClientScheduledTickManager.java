package com.abelian.client.clientRegionTick;

import com.abelian.network.ScheduledTickDataPayload;
import com.abelian.network.ScheduledTickRecord;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClientScheduledTickManager {
    //按区域存放计划刻快照：payload 携带 regionId，区域之间互不覆盖；
    //剩余刻数 = trigger - 区域本地虚拟时间，客户端每步自然递减，无需服务端持续发包
    private static final Map<String, List<ScheduledTickRecord>> REGION_SCHEDULED_TICKS = new HashMap<>();

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ScheduledTickDataPayload.ID, (payload, context) -> context.client().execute(() -> {
            if (payload.scheduledTicks().isEmpty()) {
                REGION_SCHEDULED_TICKS.remove(payload.regionId());
            } else {
                REGION_SCHEDULED_TICKS.put(payload.regionId(), payload.scheduledTicks());
            }
        }));
    }

    public record ScheduledTickDisplay(Vec3d pos, long remainingTick, int subOrderRank, int priority) { }

    public static List<ScheduledTickDisplay> getDisplayData() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world == null || REGION_SCHEDULED_TICKS.isEmpty()) return List.of();

        String dimensionId = world.getRegistryKey().getValue().toString();
        //清理已删除/已释放区域的残留快照
        REGION_SCHEDULED_TICKS.entrySet().removeIf(entry -> {
            ClientRegion region = ClientRegionManager.getRegion(entry.getKey());
            return region == null || !region.isControlled();
        });

        // 区域内的计划刻按 subTickOrder 升序从 1 开始重排，渲染时展示重排后的序号
        List<ScheduledTickDisplay> displays = new ArrayList<>();
        for (Map.Entry<String, List<ScheduledTickRecord>> entry : REGION_SCHEDULED_TICKS.entrySet()) {
            ClientRegion region = ClientRegionManager.getRegion(entry.getKey());
            //跨维度区域保留条目，仅跳过渲染
            if (region == null || !region.isControlled() || !region.getDimension().equals(dimensionId)) continue;

            List<ScheduledTickRecord> regionRecords = new ArrayList<>(entry.getValue());
            regionRecords.sort(Comparator.comparingLong(ScheduledTickRecord::subTickOrder));
            int rank = 1;
            for (ScheduledTickRecord record : regionRecords) {
                BlockPos pos = record.pos();
                if (!world.getChunkManager().isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) continue;

                long remaining = record.trigger() - region.getVirtualTime();
                if (remaining < 0) continue;
                displays.add(new ScheduledTickDisplay(
                        new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5),
                        remaining, rank++, record.priority()));
            }
        }
        return displays;
    }

    public static void clearRegion(String regionId) {
        REGION_SCHEDULED_TICKS.remove(regionId);
    }

    public static void clear() {
        REGION_SCHEDULED_TICKS.clear();
    }
}
