package com.abelian.client.clientRegionTick;

import com.abelian.client.render.InterpolationState;
import com.abelian.network.RegionSyncPayload;
import com.abelian.network.RegionTPSPayload;
import com.abelian.regionTick.Region;

import java.util.HashSet;
import java.util.Set;

public class ClientRegion {
    private final String id;
    private final String dimension;
    private final Set<Long> chunkPositions;

    private Region.RegionState regionState;
    private final InterpolationState interpolationState = new InterpolationState();
    private double rate;
    private double regionTPS;
    private double accumulator;
    private int pendingSteps;

    public ClientRegion(RegionSyncPayload payload) {
        this.id = payload.id();
        this.dimension = payload.dimension();
        this.chunkPositions = new HashSet<>(payload.chunkPositions());
        updateRegionState(payload);
    }

    public void updateRegionState(RegionSyncPayload payload) {
        this.regionState = payload.state();
        this.rate = payload.rate();
        replaceChunks(payload.chunkPositions());
        if (!isRunning()) {
            this.accumulator = 0.0;
        }
    }

    public void updateRegionTPS(RegionTPSPayload payload){
        this.regionTPS = payload.TPS();
        System.out.println("regionTPS is update to" + payload.TPS());
    }

    public void recordStep() {
        long now = System.nanoTime();
        interpolationState.lastPacketTime = now;
        interpolationState.lastTickTime = now;
        interpolationState.phase = 0.0f;
        interpolationState.tickDelta = 0.0f;
    }

    public void updateRenderDelta() {
        if (regionTPS <= 0 || interpolationState.lastTickTime == 0) {
            interpolationState.tickDelta = 1.0f;
            return;
        }

        long now = System.nanoTime();
        float elapsedMs = (now - interpolationState.lastTickTime) / 1_000_000.0f;
        interpolationState.tickDelta = Math.min(1.0f, interpolationState.phase + elapsedMs * (float) regionTPS / 1000.0f);
    }

    public int accumulateSteps() {
        accumulator += rate / 20.0;
        int steps = 0;
        while (accumulator >= 1.0) {
            accumulator -= 1.0;
            steps++;
        }
        return steps;
    }

    public int consumePendingSteps(int maxSteps) {
        int steps = Math.min(pendingSteps, maxSteps);
        pendingSteps -= steps;
        return steps;
    }

    public void setPendingSteps(int pendingSteps) {
        this.pendingSteps = Math.max(0, pendingSteps);
    }

    public int getPendingSteps() {
        return pendingSteps;
    }

    public boolean isControlled(){ return regionState != Region.RegionState.RELEASED; }

    public boolean isRunning(){ return regionState == Region.RegionState.RUNNING; }

    public boolean isStepping(){ return pendingSteps > 0; }

    public Set<Long> getChunkPositions(){ return chunkPositions; }

    public String getDimension(){ return dimension; }

    public String getId(){ return id;}

    public double getRegionTPS() {
        return regionTPS;
    }

    public Region.RegionState getRegionState() {
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
