package com.abelian.regionTick;

import com.abelian.mixin.WorldTickSchedulerAccessor;
import net.minecraft.world.tick.ChunkTickScheduler;
import net.minecraft.world.tick.OrderedTick;
import net.minecraft.world.tick.WorldTickScheduler;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.BiConsumer;

public class ChunkTickManager {
    private final long chunkPosLong;

    ChunkTickManager(long chunkPosLong) {
        this.chunkPosLong = chunkPosLong;
    }

    long getChunkPosLong() {
        return chunkPosLong;
    }

    @SuppressWarnings("unchecked")
    public <T> void takeOverChunk(WorldTickScheduler<T> worldScheduler, Region region, long currentWorldTime) {
        WorldTickSchedulerAccessor<T> worldAccess = (WorldTickSchedulerAccessor<T>) worldScheduler;
        ChunkTickScheduler<T> chunkScheduler = worldAccess.getChunkTickSchedulers().get(chunkPosLong);
        if (chunkScheduler == null || ControlledSchedulerRegistry.getRegion(chunkScheduler) == region) return;

        long virtualTime = region.getFreezeStartTime() + region.getStepped();
        shiftScheduledTicks(chunkScheduler, virtualTime - currentWorldTime);
        chunkScheduler.setTickConsumer((scheduler, tick) -> {});
        ControlledSchedulerRegistry.register(chunkScheduler, region);
        worldAccess.getNextTriggerTickByChunkPos().remove(chunkPosLong);
    }

    @SuppressWarnings("unchecked")
    public <T> void releaseChunk(WorldTickScheduler<T> worldScheduler, Region region, long currentWorldTime, long freezeStartTime, int stepped) {
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


    private static final int MAX_TICKS_PER_SCHEDULER = 65536;

    public static <T> void tickScheduledTicks(
            List<ChunkTickManager> chunks,
            WorldTickScheduler<T> worldScheduler,
            BiConsumer<BlockPos, T> ticker,
            long virtualTrigger
    ) {
        WorldTickSchedulerAccessor<T> worldAccess = (WorldTickSchedulerAccessor<T>) worldScheduler;
        Queue<ChunkTickScheduler<T>> tickableSchedulers = new PriorityQueue<>(
                (first, second) -> OrderedTick.BASIC_COMPARATOR.compare(
                        first.peekNextTick(), second.peekNextTick()));

        for (ChunkTickManager chunk : chunks) {
            ChunkTickScheduler<T> scheduler = worldAccess.getChunkTickSchedulers().get(chunk.chunkPosLong);
            if (scheduler == null) continue;

            OrderedTick<T> nextTick = scheduler.peekNextTick();
            if (nextTick != null && nextTick.triggerTick() <= virtualTrigger) {
                tickableSchedulers.add(scheduler);
            }
        }

        List<OrderedTick<T>> ticksToRun = new ArrayList<>();
        while (ticksToRun.size() < MAX_TICKS_PER_SCHEDULER && !tickableSchedulers.isEmpty()) {
            ChunkTickScheduler<T> scheduler = tickableSchedulers.poll();
            OrderedTick<T> tick = scheduler.pollNextTick();
            if (tick == null) continue;
            ticksToRun.add(tick);

            OrderedTick<T> competingTick = peekNextTick(tickableSchedulers);
            while (ticksToRun.size() < MAX_TICKS_PER_SCHEDULER) {
                OrderedTick<T> nextTick = scheduler.peekNextTick();
                if (nextTick == null || nextTick.triggerTick() > virtualTrigger
                        || competingTick != null
                        && OrderedTick.BASIC_COMPARATOR.compare(nextTick, competingTick) > 0) {
                    break;
                }

                ticksToRun.add(scheduler.pollNextTick());
            }

            OrderedTick<T> nextTick = scheduler.peekNextTick();
            if (ticksToRun.size() < MAX_TICKS_PER_SCHEDULER
                    && nextTick != null && nextTick.triggerTick() <= virtualTrigger) {
                tickableSchedulers.add(scheduler);
            }
        }

        for (OrderedTick<T> tick : ticksToRun) {
            ticker.accept(tick.pos(), tick.type());
        }
    }

    private static <T> OrderedTick<T> peekNextTick(Queue<ChunkTickScheduler<T>> schedulers) {
        ChunkTickScheduler<T> scheduler = schedulers.peek();
        return scheduler == null ? null : scheduler.peekNextTick();
    }

}



