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

    public <T> void takeOverChunk(WorldTickScheduler<T> worldScheduler, RegionTickManager region) {
        //首次接管:setStartTime 刚把 startTime 设为当前真实时间,虚拟时间==真实时间,偏移为 0
        attach(worldScheduler, region, 0L);
    }

    //区块重载/新增时恢复接管:reanchorOffset = 当前虚拟时间 - 当前真实时间
    public <T> void retakeOverChunk(WorldTickScheduler<T> worldScheduler, RegionTickManager region, long reanchorOffset) {
        attach(worldScheduler, region, reanchorOffset);
    }

    @SuppressWarnings("unchecked")
    private <T> void attach(WorldTickScheduler<T> worldScheduler, RegionTickManager region, long reanchorOffset) {
        WorldTickSchedulerAccessor<T> worldAccess = (WorldTickSchedulerAccessor<T>) worldScheduler;
        ChunkTickScheduler<T> chunkScheduler = worldAccess.getChunkTickSchedulers().get(chunkPosLong);
        if (chunkScheduler == null || ControlledSchedulerRegistry.getRegion(chunkScheduler) == region) return;

        //顺序不变量:必须先平移再注册。ChunkTickSchedulerMixin.adjustScheduledTick 按注册状态
        //改写 trigger,若已注册会对刚平移的 tick 再做一次"真实→虚拟"修正,造成二次污染
        shiftScheduledTicks(chunkScheduler, reanchorOffset);
        chunkScheduler.setTickConsumer((scheduler, tick) -> {});
        ControlledSchedulerRegistry.register(chunkScheduler, region);
        //清除原版调度器的触发索引:防止 WorldTickScheduler 仍按真实时间收集执行这批计划刻
        //(双时钟竞争)。区块刚重载时该条目由 disable 物化路径写入,这里兜底清理
        worldAccess.getNextTriggerTickByChunkPos().remove(chunkPosLong);
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

    @SuppressWarnings("unchecked")
    public <T> void releaseChunkToWorld(WorldTickScheduler<T> worldScheduler, RegionTickManager region, long currentWorldTime) {
        WorldTickSchedulerAccessor<T> worldAccess = (WorldTickSchedulerAccessor<T>) worldScheduler;
        ChunkTickScheduler<T> chunkScheduler = worldAccess.getChunkTickSchedulers().get(chunkPosLong);
        if (chunkScheduler == null) return;
        shiftScheduledTicks(chunkScheduler, currentWorldTime - (region.getStartTime() + region.getStepped()));
    }

    //计划刻触发时间偏移(卸载/接管时在真实与虚拟时间线之间换算)
    public static <T> void shiftScheduledTicks(ChunkTickScheduler<T> chunkScheduler, long offset) {
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



