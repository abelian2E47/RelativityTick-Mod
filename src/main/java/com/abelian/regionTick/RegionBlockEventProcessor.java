package com.abelian.regionTick;

import com.abelian.mixin.ServerWorldAccessor;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.network.packet.s2c.play.BlockEventS2CPacket;
import net.minecraft.server.world.BlockEvent;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class RegionBlockEventProcessor {
    private RegionBlockEventProcessor() {
    }

    private static final java.util.Map<ServerWorld, java.util.Map<RegionTickManager, List<BlockEvent>>> CONTROLLED_EVENTS = new java.util.IdentityHashMap<>();
    private static final java.util.Map<ServerWorld, List<BlockEvent>> DEFERRED_EVENTS = new java.util.IdentityHashMap<>();

    public static void process(ServerWorld world, Predicate<RegionTickManager> shouldProcess) {
        restoreDeferredEvents(world);
        ServerWorldAccessor accessor = (ServerWorldAccessor) world;
        ObjectLinkedOpenHashSet<BlockEvent> queue = accessor.getSyncedBlockEventQueue();
        if (queue.isEmpty()) return;

        List<BlockEvent> deferred = new ArrayList<>(queue.size());
        while (!queue.isEmpty()) {
            BlockEvent event = queue.removeFirst();
            RegionTickManager owner = RegionsManager.getRegionByChunk(world, ChunkPos.toLong(event.pos()));
            if (!shouldProcess.test(owner) || !world.shouldTickBlockPos(event.pos())) {
                deferred.add(event);
                continue;
            }
            processEvent(world, accessor, event);
        }
        queue.addAll(deferred);
    }

    // [修改点2] 与 ServerWorld.processSyncedBlockEvents 的抽干语义对齐时的“代”数上限，仅作自反馈事件的安全阀
    private static final int MAX_BLOCK_EVENT_GENERATIONS = 1024;

    public static void process(ServerWorld world, RegionTickManager region) {
        ServerWorldAccessor accessor = (ServerWorldAccessor) world;
        List<BlockEvent> deferred = DEFERRED_EVENTS.computeIfAbsent(world, ignored -> new ArrayList<>());
        // [修改点2] 原版 processSyncedBlockEvents 是 while(!queue.isEmpty()) 抽干队列，所以“处理方块事件时
        // 新加入的方块事件”在同一个刻内继续处理（活塞连锁、活塞 setBlockState(...,NOTIFY_ALL) 引发的连锁都在
        // 这里）。这里原来只处理一次 stage 出来的快照，链式事件会被推迟到下一个区域刻，而由这些事件引发的
        // 形状更新（含侦测器新增的 +2 计划刻）也就整体晚一个区域刻。改为对本区域反复 stage+处理，直到本区域
        // 没有待处理事件为止。
        for (int generation = 0; generation < MAX_BLOCK_EVENT_GENERATIONS; generation++) {
            stageControlledEvents(world);
            java.util.Map<RegionTickManager, List<BlockEvent>> byRegion = CONTROLLED_EVENTS.get(world);
            if (byRegion == null) return;
            List<BlockEvent> events = byRegion.remove(region);
            if (events == null || events.isEmpty()) return;

            for (BlockEvent event : events) {
                RegionTickManager owner = RegionsManager.getRegionByChunk(world, ChunkPos.toLong(event.pos()));
                if (owner == region && region.isControlled() && world.shouldTickBlockPos(event.pos())) {
                    processEvent(world, accessor, event);
                } else {
                    deferred.add(event);
                }
            }
        }
    }

    private static void stageControlledEvents(ServerWorld world) {
        ServerWorldAccessor accessor = (ServerWorldAccessor) world;
        ObjectLinkedOpenHashSet<BlockEvent> queue = accessor.getSyncedBlockEventQueue();
        java.util.Map<RegionTickManager, List<BlockEvent>> byRegion = CONTROLLED_EVENTS.computeIfAbsent(world, ignored -> new java.util.IdentityHashMap<>());
        List<BlockEvent> deferred = DEFERRED_EVENTS.computeIfAbsent(world, ignored -> new ArrayList<>());

        var regionIterator = byRegion.entrySet().iterator();
        while (regionIterator.hasNext()) {
            var entry = regionIterator.next();
            if (!entry.getKey().isControlled()) {
                deferred.addAll(entry.getValue());
                regionIterator.remove();
            }
        }

        while (!queue.isEmpty()) {
            BlockEvent event = queue.removeFirst();
            RegionTickManager owner = RegionsManager.getRegionByChunk(world, ChunkPos.toLong(event.pos()));
            if (owner != null && owner.isControlled()) {
                byRegion.computeIfAbsent(owner, ignored -> new ArrayList<>()).add(event);
            } else {
                deferred.add(event);
            }
        }
    }

    private static void restoreDeferredEvents(ServerWorld world) {
        List<BlockEvent> deferred = DEFERRED_EVENTS.remove(world);
        if (deferred == null || deferred.isEmpty()) return;
        ((ServerWorldAccessor) world).getSyncedBlockEventQueue().addAll(deferred);
    }

    private static void processEvent(ServerWorld world, ServerWorldAccessor accessor, BlockEvent event) {
        if (accessor.invokeProcessBlockEvent(event)) {
            BlockPos pos = event.pos();
            world.getServer().getPlayerManager().sendToAround(null, pos.getX(), pos.getY(), pos.getZ(), 64.0,
                    world.getRegistryKey(), new BlockEventS2CPacket(pos, event.block(), event.type(), event.data()));
        }
    }

    public static void clear() {
        CONTROLLED_EVENTS.clear();
        DEFERRED_EVENTS.clear();
    }
}
