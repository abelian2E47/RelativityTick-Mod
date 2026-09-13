package com.abelian.mixin;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.world.tick.ChunkTickScheduler;
import net.minecraft.world.tick.OrderedTick;
import net.minecraft.world.tick.WorldTickScheduler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;

@Mixin(WorldTickScheduler.class)
public interface WorldTickSchedulerAccessor<T> {
    @Accessor("chunkTickSchedulers")
    Long2ObjectMap<ChunkTickScheduler<T>> getChunkTickSchedulers();

    @Accessor("nextTriggerTickByChunkPos")
    Long2LongMap getNextTriggerTickByChunkPos();

    @Accessor("queuedTickConsumer")
    BiConsumer<ChunkTickScheduler<T>, OrderedTick<T>> getQueuedTickConsumer();

    //本刻"已出队、待执行/正在执行"的整批计划刻。isTicking() 只看这三个容器,
    //区域自管计划刻时必须往这里写,否则 isTicking() 恒为 false。
    @Accessor("tickableTicks")
    Queue<OrderedTick<T>> getTickableTicks();

    @Accessor("tickedTicks")
    List<OrderedTick<T>> getTickedTicks();

    @Accessor("copiedTickableTicksList")
    Set<OrderedTick<?>> getCopiedTickableTicksList();
}