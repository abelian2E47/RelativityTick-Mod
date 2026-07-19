package com.abelian.mixin;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.world.tick.ChunkTickScheduler;
import net.minecraft.world.tick.OrderedTick;
import net.minecraft.world.tick.WorldTickScheduler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.BiConsumer;

@Mixin(WorldTickScheduler.class)
public interface WorldTickSchedulerAccessor<T> {
    @Accessor("chunkTickSchedulers")
    Long2ObjectMap<ChunkTickScheduler<T>> getChunkTickSchedulers();

    @Accessor("nextTriggerTickByChunkPos")
    Long2LongMap getNextTriggerTickByChunkPos();

    @Accessor("queuedTickConsumer")
    BiConsumer<ChunkTickScheduler<T>, OrderedTick<T>> getQueuedTickConsumer();
}