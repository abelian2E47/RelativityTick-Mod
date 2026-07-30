package com.abelian.regionTick;

import com.abelian.mixin.WorldTickSchedulerAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.tick.ChunkTickScheduler;
import net.minecraft.world.tick.OrderedTick;
import net.minecraft.world.tick.WorldTickScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class ChunkTickManager {
    private final long chunkPosLong;
    private final List<Object> blockEntityTickBuffer = new ArrayList<>(64);

    ChunkTickManager(long chunkPosLong){
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


    public <T> void tickScheduledTicks(WorldTickScheduler<T> worldScheduler, BiConsumer<BlockPos, T> ticker, long virtualTrigger) {
        WorldTickSchedulerAccessor<T> worldAccess = (WorldTickSchedulerAccessor<T>) worldScheduler;
        ChunkTickScheduler<T> chunkScheduler = worldAccess.getChunkTickSchedulers().get(this.chunkPosLong);

        if (chunkScheduler == null) return;

        while (true) {
            OrderedTick<T> nextTick = chunkScheduler.peekNextTick();
            if (nextTick == null || nextTick.triggerTick() > virtualTrigger) break;

            OrderedTick<T> tickToRun = chunkScheduler.pollNextTick();
            if (tickToRun != null) {
                ticker.accept(tickToRun.pos(), tickToRun.type());
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void tickBlockEntities(ServerWorld world) {
        int chunkX = (int) (this.chunkPosLong & 0xFFFFFFFFL);
        int chunkZ = (int) (this.chunkPosLong >>> 32);
        WorldChunk chunk = world.getChunk(chunkX, chunkZ);

        if (chunk == null) return;


        this.blockEntityTickBuffer.clear();
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            if (!be.isRemoved() && be.hasWorld()) {
                this.blockEntityTickBuffer.add(be);
            }
        }

        for (Object obj : this.blockEntityTickBuffer) {
            BlockEntity be = (BlockEntity) obj;
            BlockState state = be.getCachedState();
            BlockEntityTicker<BlockEntity> ticker = (BlockEntityTicker<BlockEntity>) state.getBlockEntityTicker(world, be.getType());
            if (ticker != null) {
                ticker.tick(world, be.getPos(), state, be);
            }
        }

        this.blockEntityTickBuffer.clear();
    }
}



