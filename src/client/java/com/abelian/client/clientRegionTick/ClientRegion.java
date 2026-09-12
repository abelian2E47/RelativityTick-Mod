package com.abelian.client.clientRegionTick;

import com.abelian.client.render.InterpolationState;
import com.abelian.network.RegionSyncPayload;
import com.abelian.network.RegionTPSPayload;
import com.abelian.regionTick.RegionTickManager;

import java.util.HashSet;
import java.util.Set;

public class ClientRegion {
    private static final float MIN_RENDER_PERIOD_MS = 4.0f;
    private static final float MAX_RENDER_PERIOD_MS = 1000.0f;
    private static final float MAX_PHASE = 0.25f;
    private static final float MAX_ACCUMULATE_MS = 250.0f;

    private final String id;
    private String dimension;
    private final Set<Long> chunkPositions;

    private RegionTickManager.RegionState regionState;
    private final InterpolationState interpolationState = new InterpolationState();
    private double rate;
    private double regionTPS;
    private double accumulator;
    private long lastAccumulateNanos;
    private float renderPeriodMs;
    private long virtualTime;
    private int pendingSteps;
    private int announcedSteps;
    private int replayedSteps;
    private boolean disableHopperTick;
    private boolean disableEntityTick;

    public ClientRegion(RegionSyncPayload payload) {
        this.id = payload.id();
        this.dimension = payload.dimension();
        this.chunkPositions = new HashSet<>(payload.chunkPositions());
        updateRegionState(payload);
    }

    public void updateRegionState(RegionSyncPayload payload) {
        boolean wasRunning = isRunning();
        this.dimension = payload.dimension();
        this.regionState = payload.state();
        this.rate = payload.rate();
        this.virtualTime = payload.virtualTime();
        this.disableHopperTick = payload.disableHopperTick();
        this.disableEntityTick = payload.disableEntityTick();
        replaceChunks(payload.chunkPositions());
        if (!isRunning()) {
            this.accumulator = this.accumulator >= 1.0 ? 1.0 : 0.0;
            this.lastAccumulateNanos = System.nanoTime();
        } else if (!wasRunning) {
            this.lastAccumulateNanos = System.nanoTime();
        }
        if (isRunning() || regionState == RegionTickManager.RegionState.RELEASED) {
            resetReplayBudget();
        }
    }

    public void updateVirtualTime(long virtualTime) {
        this.virtualTime = virtualTime;
    }

    public long nextVirtualTime() {
        return ++virtualTime;
    }

    public long getVirtualTime() {
        return virtualTime;
    }

    public void updateRegionTPS(RegionTPSPayload payload){
        this.regionTPS = payload.TPS();
    }

    //为实体渲染插值承上启下
    public void beginInterpolationSegment() {
        long now = System.nanoTime();
        float nominalPeriod = nominalBatchPeriodMs();

        float actualGapMs = 0.0f;
        if (interpolationState.lastTickTime != 0) {
            actualGapMs = (now - interpolationState.lastTickTime) / 1_000_000.0f;
        }

        float newPeriod = (actualGapMs > 0.0f && isRunning())
                ? clampPeriod(actualGapMs, nominalPeriod)
                : nominalPeriod;

        float previousPeriod = getRenderPeriodMs();
        float overdueMs = actualGapMs > 0.0f
                ? Math.max(0.0f, (1.0f - interpolationState.phase) * previousPeriod - actualGapMs)
                : 0.0f;

        this.renderPeriodMs = newPeriod;
        interpolationState.phase = newPeriod > 0.0f ? Math.min(MAX_PHASE, overdueMs / newPeriod) : 0.0f;
        interpolationState.lastPacketTime = now;
        interpolationState.lastTickTime = now;
        interpolationState.tickDelta = interpolationState.phase;
    }

    public void recordAuthoritySync() {
        long now = System.nanoTime();
        interpolationState.lastPacketTime = now;
        interpolationState.lastTickTime = now;
        interpolationState.phase = 0.0f;
        interpolationState.tickDelta = 0.0f;
    }

    public void updateRenderDelta() {
        if (interpolationState.lastTickTime == 0) {
            interpolationState.tickDelta = 1.0f;
            return;
        }

        long now = System.nanoTime();
        float elapsedMs = (now - interpolationState.lastTickTime) / 1_000_000.0f;
        interpolationState.tickDelta = Math.min(1.0f, interpolationState.phase + elapsedMs / getRenderPeriodMs());
    }

    public int accumulateSteps() {
        long now = System.nanoTime();
        if (lastAccumulateNanos == 0) {
            lastAccumulateNanos = now;
            return 0;
        }

        float elapsedMs = Math.min(MAX_ACCUMULATE_MS, (now - lastAccumulateNanos) / 1_000_000.0f);
        lastAccumulateNanos = now;

        accumulator += rate * elapsedMs / 1000.0;
        int steps = 0;
        while (accumulator >= 1.0) {
            accumulator -= 1.0;
            steps++;
        }
        return steps;
    }

    private float getRenderPeriodMs() {
        if (renderPeriodMs > 0.0f) {
            return renderPeriodMs;
        }
        return nominalBatchPeriodMs();
    }

    private float clampPeriod(float periodMs, float nominal) {
        float lower = Math.max(MIN_RENDER_PERIOD_MS, nominal * 0.25f);
        float upper = Math.min(MAX_RENDER_PERIOD_MS, nominal * 4.0f);
        return Math.max(lower, Math.min(upper, periodMs));
    }

    //按rate推算一批区域刻的名义渲染时长
    private float nominalBatchPeriodMs() {
        double effectiveRate = rate > 0 ? rate : regionTPS;
        if (effectiveRate <= 0) {
            return 50.0f;
        }
        double ticksPerBatch = Math.max(1.0, effectiveRate / 20.0);
        return (float) (1000.0 * ticksPerBatch / effectiveRate);
    }

    //步进通告：累计本 episode 服务端请求的步数，并让首步立刻到期
    public void announceSteps(int serverPending) {
        if (serverPending > 0) {
            int added = serverPending - this.pendingSteps;
            if (added > 0) {
                this.announcedSteps += added;
                if (!isRunning()) {
                    this.accumulator = Math.max(this.accumulator, 1.0);
                    this.lastAccumulateNanos = System.nanoTime();
                }
            }
        }
        this.pendingSteps = Math.max(0, serverPending);
    }

    //本 episode 已通告但还没本地回放的步数
    public int pendingReplayCount() {
        return Math.max(0, this.announcedSteps - this.replayedSteps);
    }

    //记录已本地回放的步数
    public void markStepsReplayed(int steps) {
        this.replayedSteps += Math.max(0, steps);
    }

    //回到运行/释放状态时清空回放预算
    public void resetReplayBudget() {
        this.announcedSteps = 0;
        this.replayedSteps = 0;
    }

    public boolean isControlled(){ return regionState != RegionTickManager.RegionState.RELEASED; }

    public boolean isRunning(){ return regionState == RegionTickManager.RegionState.RUNNING; }

    public boolean isStepping(){ return pendingSteps > 0 || pendingReplayCount() > 0; }

    public boolean isDisableHopperTick(){ return disableHopperTick; }

    public boolean isDisableEntityTick(){ return disableEntityTick; }

    public Set<Long> getChunkPositions(){ return chunkPositions; }

    public String getDimension(){ return dimension; }

    public String getId(){ return id;}

    public RegionTickManager.RegionState getRegionState() {
        return regionState;
    }

    public InterpolationState getInterpolationState() {
        return interpolationState;
    }

    private void replaceChunks(Set<Long> chunks) {
        chunkPositions.clear();
        chunkPositions.addAll(chunks);
    }
}
