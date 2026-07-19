package com.abelian.regionFreeze;

import com.abelian.RelativityTickUtils;
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

public class RegionTickManager {
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

    private long totalNanoTimeThisSecond = 0;
    private int stepsThisSecond = 0;
    private int ticksProcessedThisSecond = 0;
    private double regionTPS = 0;
    private float RegionTickDuration = 0;

    private boolean reachMsptLimit = false;
    private boolean reachTickDurationLimit = false;

    private long lastStatTime = System.currentTimeMillis();

    public RegionTickManager(String id, RegistryKey<World> dimension, Set<Long> chunkPositions){
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

    public <T> void releaseRegion(WorldTickScheduler<T> worldScheduler, long currentWorldTime) {
        for (ChunkTickManager chunk : region){
            chunk.releaseChunk(worldScheduler, this, currentWorldTime,freezeStartTime,stepped);
        }

    }


    public List<EntityStateRecord> stepRegion(ServerWorld world, WorldTickScheduler<Block> blockScheduler, BiConsumer<BlockPos, Block> blockTicker, WorldTickScheduler<Fluid> fluidScheduler, BiConsumer<BlockPos, Fluid> fluidTicker) {
        List<EntityStateRecord> entityStates = new ArrayList<>();
        setCurrentWorldTime(world.getTime());
        if (isControlled()){
            stepped++;
            long virtualTrigger = freezeStartTime + stepped;
            for (ChunkTickManager chunk : region){
                chunk.stepScheduledTicks(blockScheduler,blockTicker,virtualTrigger);
                chunk.stepScheduledTicks(fluidScheduler,fluidTicker,virtualTrigger);
                chunk.stepBlockEntities(world);
                entityStates.addAll(chunk.stepEntities(world));
            }
            markStep();
        }
        return entityStates;
    }

    public void stepRegionWithoutState(ServerWorld world, WorldTickScheduler<Block> blockScheduler, BiConsumer<BlockPos, Block> blockTicker, WorldTickScheduler<Fluid> fluidScheduler, BiConsumer<BlockPos, Fluid> fluidTicker) {
        setCurrentWorldTime(world.getTime());
        if (isControlled()){
            stepped++;
            long virtualTrigger = freezeStartTime + stepped;
            for (ChunkTickManager chunk : region){
                chunk.stepScheduledTicks(blockScheduler,blockTicker,virtualTrigger);
                chunk.stepScheduledTicks(fluidScheduler,fluidTicker,virtualTrigger);
                chunk.stepBlockEntities(world);
                chunk.stepEntitiesWithoutState(world);
            }
            markStep();
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
        this.totalNanoTimeThisSecond += totalNanoInTick;
        this.ticksProcessedThisSecond++; // 每次调用代表经历了一个 GT
    }

    public void markStep() {
        this.stepsThisSecond++;
    }

    public void updateTickStats() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastStatTime;

        if (elapsed > 5000) {
            this.stepsThisSecond = 0;
            this.totalNanoTimeThisSecond = 0;
            this.ticksProcessedThisSecond = 0;
            this.lastStatTime = now;
            return;
        }

        if (elapsed >= 1000) {
            this.regionTPS = RelativityTickUtils.computeTPS(stepsThisSecond, elapsed);
            this.RegionTickDuration = (float) RelativityTickUtils.computeMsPerGameTick(totalNanoTimeThisSecond, ticksProcessedThisSecond);

            this.stepsThisSecond = 0;
            this.totalNanoTimeThisSecond = 0;
            this.ticksProcessedThisSecond = 0;
            this.lastStatTime = now;
        }
    }

    public double getLastMeasuredTPS() { return regionTPS; }
    public float getRegionTickDuration() { return RegionTickDuration; }

}
