package com.abelian.regionTick;

import com.abelian.RegionTickContext;
import com.abelian.network.EntityStateRecord;
import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.tick.WorldTickScheduler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

public class Region {
    public enum RegionState {
        RELEASED,
        FROZEN,
        RUNNING
    }

    private final RegistryKey<World> dimension;
    private final String id;
    private final Set<Long> chunkPositions;
    private final  ArrayList<ChunkTickManager> region = new ArrayList<>();
    private long freezeStartTime = 0;
    private long currentWorldTime = 0;
    private int stepped = 0;
    private int pendingSteps = 0;
    private double rate = 20;
    private double accumulator = 0.0;
    private double tickDurationLimit = 10.0;
    private int regionPriority = 1;
    private RegionState state = RegionState.RELEASED;

    private static final int TPS_AVERAGE_WINDOW_GT = 100;
    private final int[] recentStepCounts = new int[TPS_AVERAGE_WINDOW_GT];
    private int recentStepCursor = 0;
    private int recentStepSamples = 0;
    private int recentStepTotal = 0;
    private double regionTPS = 0;
    private float regionTickDuration = 0;

    private boolean reachMsptLimit = false;
    private boolean reachTickDurationLimit = false;

    

    public Region(String id, RegistryKey<World> dimension, Set<Long> chunkPositions){
        this.id = id;
        this.dimension = dimension;
        for (long chunkPos : chunkPositions){
            region.add(new ChunkTickManager(chunkPos));
        }
        this.chunkPositions = new HashSet<>(chunkPositions);
    }

    public boolean addChunk(long chunkPos, ServerWorld world) {
        if (!this.chunkPositions.add(chunkPos)) return false;

        ChunkTickManager chunk = new ChunkTickManager(chunkPos);
        if (isControlled()) {
            chunk.takeOverChunk(world.getBlockTickScheduler(), this, world.getTime());
            chunk.takeOverChunk(world.getFluidTickScheduler(), this, world.getTime());
        }
        region.add(chunk);
        return true;
    }

    public boolean removeChunk(long chunkPos, ServerWorld world) {
        if (!this.chunkPositions.remove(chunkPos)) return false;

        ChunkTickManager target = null;
        for (ChunkTickManager chunk : region) {
            if (chunk.getChunkPosLong() == chunkPos) {
                target = chunk;
                break;
            }
        }

        if (target != null) {
            if (isControlled()) {
                long currentWorldTime = world.getTime();
                target.releaseChunk(world.getBlockTickScheduler(), this, currentWorldTime, freezeStartTime, stepped);
                target.releaseChunk(world.getFluidTickScheduler(), this, currentWorldTime, freezeStartTime, stepped);
            }
            region.remove(target);
        }

        return true;
    }

    public <T> void takeOverRegion(WorldTickScheduler<T> worldScheduler, long currentWorldTime) {
        for (ChunkTickManager chunk : region){
            chunk.takeOverChunk(worldScheduler, this, currentWorldTime);
        }
    }

    public <T> void releaseRegion(WorldTickScheduler<T> worldScheduler, long currentWorldTime) {
        for (ChunkTickManager chunk : region){
            chunk.releaseChunk(worldScheduler, this, currentWorldTime,freezeStartTime,stepped);
        }

    }

    public void takeOverChunk(long chunkPos, ServerWorld world) {
        for (ChunkTickManager chunk : region) {
            if (chunk.getChunkPosLong() != chunkPos) continue;
            chunk.takeOverChunk(world.getBlockTickScheduler(), this, world.getTime());
            chunk.takeOverChunk(world.getFluidTickScheduler(), this, world.getTime());
            return;
        }
    }

    public void releaseChunk(long chunkPos, ServerWorld world) {
        for (ChunkTickManager chunk : region) {
            if (chunk.getChunkPosLong() != chunkPos) continue;
            chunk.releaseChunk(world.getBlockTickScheduler(), this, world.getTime(), freezeStartTime, stepped);
            chunk.releaseChunk(world.getFluidTickScheduler(), this, world.getTime(), freezeStartTime, stepped);
            return;
        }
    }

    public void setFreezeStartTime(long time) {
        this.freezeStartTime = time;
        this.stepped = 0;
        this.currentWorldTime = time;
    }

    public void setCurrentWorldTime(long time) {
        this.currentWorldTime = time;
    }

    public long getCurrentWorldTime() {
        return currentWorldTime;
    }

    public void tickRegion(ServerWorld world, WorldTickScheduler<Block> blockScheduler, BiConsumer<BlockPos, Block> blockTicker, WorldTickScheduler<Fluid> fluidScheduler, BiConsumer<BlockPos, Fluid> fluidTicker) {
        if (!isControlled()) return;

        setCurrentWorldTime(world.getTime());
        stepped++;
        long virtualTime = freezeStartTime + stepped;
        RegionTickContext.begin(world, virtualTime);
        try {
            tickScheduledTicks(blockScheduler, blockTicker, fluidScheduler, fluidTicker, virtualTime);
            processSyncedBlockEvents(world);
            for (ChunkTickManager chunk : region) {
                chunk.tickEntities(world);
            }
            for (ChunkTickManager chunk : region) {
                chunk.tickBlockEntities(world);
            }
        } finally {
            RegionTickContext.end();
        }
    }

    public List<EntityStateRecord> collectEntityStates(ServerWorld world) {
        List<EntityStateRecord> entityStates = new ArrayList<>();
        if (isControlled()) {
            for (ChunkTickManager chunk : region) {
                entityStates.addAll(chunk.collectEntityStates(world));
            }
        }
        return entityStates;
    }


    private void tickScheduledTicks(WorldTickScheduler<Block> blockScheduler, BiConsumer<BlockPos, Block> blockTicker, WorldTickScheduler<Fluid> fluidScheduler, BiConsumer<BlockPos, Fluid> fluidTicker, long virtualTime) {
        for (ChunkTickManager chunk : region) {
            chunk.tickScheduledTicks(blockScheduler, blockTicker, virtualTime);
            chunk.tickScheduledTicks(fluidScheduler, fluidTicker, virtualTime);
        }
    }

    private void processSyncedBlockEvents(ServerWorld world) {
        RegionBlockEventProcessor.process(world, owner -> owner == this);
    }


    public void setPendingSteps(int steps){
        this.pendingSteps = steps;
    }

    public int getPendingSteps(){return this.pendingSteps;}

    public void setRate(double rate){this.rate = rate;}

    public double getRate(){return rate;}

    public void setAccumulator(double accumulator) {this.accumulator = accumulator;}

    public double getAccumulator() {return accumulator;}

    public double getTickDurationLimit() {return tickDurationLimit;}

    public void setMaxRegionCostMs(double maxRegionCostMs) {this.tickDurationLimit = Math.max(1.0, maxRegionCostMs);}

    public int getRegionPriority() {return regionPriority;}

    public void setRegionPriority(int regionPriority) {this.regionPriority = Math.max(1, regionPriority);}

    public RegionState getState() { return state; }

    public void setState(RegionState state) { this.state = state; }

    public boolean isControlled(){ return state != RegionState.RELEASED; }

    public boolean isRunning(){ return state == RegionState.RUNNING; }

    public boolean isStepping(){ return pendingSteps > 0;}

    public long getFreezeStartTime(){
        return freezeStartTime;
    }

    public int getStepped(){
        return stepped;
    }

    public String getID() {
        return id;
    }

    public RegistryKey<World> getDimension() {
        return dimension;
    }

    public String getDimensionId() {
        return dimension.getValue().toString();
    }

    public boolean isInWorld(ServerWorld world) {
        return world.getRegistryKey().equals(dimension);
    }

    public Set<Long> getChunkPositions() {
        return Set.copyOf(this.chunkPositions);
    }

    public boolean hasReachedMsptLimit() { return reachMsptLimit; }

    public void setReachedMsptLimit(boolean reachMsptLimit) { this.reachMsptLimit = reachMsptLimit; }

    public boolean hasReachedTickDurationLimit() { return reachTickDurationLimit; }

    public void  setReachTickDurationLimit(boolean reachTickDurationLimit) { this.reachTickDurationLimit = reachTickDurationLimit; }

    public void recordTickDuration(long totalNanoInTick) {
        this.regionTickDuration = (float)(totalNanoInTick / 1_000_000.0);
    }

    public void recordGlobalTickSteps(int stepsTaken) {
        this.recentStepTotal -= this.recentStepCounts[this.recentStepCursor];
        this.recentStepCounts[this.recentStepCursor] = stepsTaken;
        this.recentStepTotal += stepsTaken;
        this.recentStepCursor = (this.recentStepCursor + 1) % TPS_AVERAGE_WINDOW_GT;
        if (this.recentStepSamples < TPS_AVERAGE_WINDOW_GT) {
            this.recentStepSamples++;
        }

        this.regionTPS = this.recentStepTotal * 20.0 / this.recentStepSamples;
    }

    public boolean hasFullTpsSampleWindow() {
        return this.recentStepSamples >= TPS_AVERAGE_WINDOW_GT;
    }

    public double getTPS() { return regionTPS; }
    public float getRegionTickDuration() { return regionTickDuration; }

}
