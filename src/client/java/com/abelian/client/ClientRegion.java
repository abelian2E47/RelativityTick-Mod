package com.abelian.client;

import com.abelian.network.RegionSyncPayload;
import com.abelian.network.RegionTPSPayload;
import com.abelian.regionTick.Region;

import java.util.HashSet;
import java.util.Set;

public class ClientRegion {
    //优化网络包
    //1.全局常量包
    //2.状态包
    //3.TPS包

    private final String id;
    private final String dimension;
    private final Set<Long> chunkPositions;

    private Region.RegionState regionState;
    private InterpolationState interpolationState;
    private double regionTPS;

    private int pendingSteps;

    public ClientRegion(RegionSyncPayload payload) {
        this.id = payload.id();
        this.dimension = payload.dimension();
        this.chunkPositions = new HashSet<>(payload.chunkPositions());
        updateRegionState(payload);
    }

    public void updateRegionState(RegionSyncPayload payload) {
        this.regionState = payload.state();
    }

    public void updateRegionTPS(RegionTPSPayload payload){
        regionTPS = payload.TPS();
    }

    public void setPendingSteps(int pendingSteps) {
        this.pendingSteps = pendingSteps;
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
}
