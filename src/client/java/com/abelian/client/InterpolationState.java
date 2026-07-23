package com.abelian.client;

public class InterpolationState {
    public long lastPacketTime;
    public long lastTickTime;
    public float packetInterval;
    public double tps;
    public float phase;
    public float tickDelta = 0.0f;

    public InterpolationState() {
        this.lastPacketTime = 0;
        this.lastTickTime = 0;
    }
}
