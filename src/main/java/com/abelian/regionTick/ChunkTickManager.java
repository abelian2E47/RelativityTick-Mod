package com.abelian.regionTick;

import com.abelian.mixin.WorldTickSchedulerAccessor;
import net.minecraft.world.tick.ChunkTickScheduler;
import net.minecraft.world.tick.OrderedTick;
import net.minecraft.world.tick.WorldTickScheduler;
import java.util.List;


public class ChunkTickManager {
    private final long chunkPosLong;

    ChunkTickManager(long chunkPosLong) {
        this.chunkPosLong = chunkPosLong;
    }

    long getChunkPosLong() {
        return chunkPosLong;
    }

    @SuppressWarnings("unchecked")
    public <T> void takeOverChunk(WorldTickScheduler<T> worldScheduler, RegionTickManager region) {
        WorldTickSchedulerAccessor<T> worldAccess = (WorldTickSchedulerAccessor<T>) worldScheduler;
        ChunkTickScheduler<T> chunkScheduler = worldAccess.getChunkTickSchedulers().get(chunkPosLong);
        if (chunkScheduler == null || ControlledSchedulerRegistry.getRegion(chunkScheduler) == region) return;

        shiftScheduledTicks(chunkScheduler, 0);
        chunkScheduler.setTickConsumer((scheduler, tick) -> {});
        ControlledSchedulerRegistry.register(chunkScheduler, region);
        worldAccess.getNextTriggerTickByChunkPos().remove(chunkPosLong);
    }

    //重新加载游戏时恢复区块状态
    @SuppressWarnings("unchecked")
    public <T> void retakeOverChunk(WorldTickScheduler<T> worldScheduler, RegionTickManager region, long currentWorldTime) {
        WorldTickSchedulerAccessor<T> worldAccess = (WorldTickSchedulerAccessor<T>) worldScheduler;
        ChunkTickScheduler<T> chunkScheduler = worldAccess.getChunkTickSchedulers().get(chunkPosLong);
        if (chunkScheduler == null || ControlledSchedulerRegistry.getRegion(chunkScheduler) == region) return;

        long virtualTime = region.getStartTime() + region.getStepped();
        System.out.println("offset" + (virtualTime - currentWorldTime));
        shiftScheduledTicks(chunkScheduler, 0);
        chunkScheduler.setTickConsumer((scheduler, tick) -> {});
        ControlledSchedulerRegistry.register(chunkScheduler, region);
    }

    @SuppressWarnings("unchecked")
    public <T> void releaseChunk(WorldTickScheduler<T> worldScheduler, RegionTickManager region, long currentWorldTime, long freezeStartTime, int stepped) {
        WorldTickSchedulerAccessor<T> worldAccess = (WorldTickSchedulerAccessor<T>) worldScheduler;
        ChunkTickScheduler<T> chunkScheduler = worldAccess.getChunkTickSchedulers().get(chunkPosLong);
        if (chunkScheduler == null || ControlledSchedulerRegistry.getRegion(chunkScheduler) != region) return;

        ControlledSchedulerRegistry.unregister(chunkScheduler, region);
        chunkScheduler.setTickConsumer(worldAccess.getQueuedTickConsumer());
        shiftScheduledTicks(chunkScheduler, currentWorldTime - (freezeStartTime + stepped));

        OrderedTick<T> nextTick = chunkScheduler.peekNextTick();
        if (nextTick != null) {
            worldAccess.getNextTriggerTickByChunkPos().put(chunkPosLong, nextTick.triggerTick());
        } else {
            worldAccess.getNextTriggerTickByChunkPos().remove(chunkPosLong);
        }
    }

    //区域已释放:把残留的虚拟时间线锚点换算回真实时间线(无注册守卫,供 vanilla 正常执行)
    @SuppressWarnings("unchecked")
    public <T> void releaseChunkToWorld(WorldTickScheduler<T> worldScheduler, RegionTickManager region, long currentWorldTime) {
        WorldTickSchedulerAccessor<T> worldAccess = (WorldTickSchedulerAccessor<T>) worldScheduler;
        ChunkTickScheduler<T> chunkScheduler = worldAccess.getChunkTickSchedulers().get(chunkPosLong);
        if (chunkScheduler == null) return;
        shiftScheduledTicks(chunkScheduler, currentWorldTime - (region.getStartTime() + region.getStepped()));
    }

    //计划刻触发时间偏移
    private static <T> void shiftScheduledTicks(ChunkTickScheduler<T> chunkScheduler, long offset) {
        if (offset == 0) return;

        List<OrderedTick<T>> shiftedTicks = chunkScheduler.getQueueAsStream()
                .map(tick -> new OrderedTick<>(
                        tick.type(), tick.pos(), tick.triggerTick() + offset,
                        tick.priority(), tick.subTickOrder()))
                .toList();
        chunkScheduler.removeTicksIf(tick -> true);
        for (OrderedTick<T> tick : shiftedTicks) {
            chunkScheduler.scheduleTick(tick);
        }
    }

}



