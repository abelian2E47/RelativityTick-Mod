package com.abelian.client;

import com.abelian.network.RegionTPSPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class RegionTickDeltaManager {
    private static final Map<String, TickDeltaState> RegionTickDeltaState = new ConcurrentHashMap<>();

    public static class TickDeltaState {
        public long lastPacketTime;
        public long lastTickTime;
        public float packetInterval;
        public double tps;
        public double rate;
        public float phase;
        public float tickDelta = 0.0f;

        public TickDeltaState() {
            this.lastPacketTime = 0;
            this.lastTickTime = 0;
        }
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(RegionTPSPayload.ID, (payload, context) -> context.client().execute(() -> {
            RegionTickDeltaState.computeIfAbsent(payload.regionID(), k -> new TickDeltaState());
            TickDeltaState state = RegionTickDeltaState.get(payload.regionID());
            if (state != null) {
                state.tps = payload.TPS();
            }
        }));
    }

    public static void computeTickDelta(TickDeltaState state) {
        if (state == null || state.lastTickTime == 0) return;
        long currentTime = System.nanoTime();
        float elapsedMs = (currentTime - state.lastTickTime) / 1_000_000.0f;
        if (state.rate > 0) {
            state.tickDelta = Math.min(1.0f, state.phase + elapsedMs * (float)state.rate / 1000.0f);
        } else if (state.packetInterval > 0) {
            state.tickDelta = Math.min(1.0f, elapsedMs / state.packetInterval);
        }
    }

    public static float getTickDelta(String id) {
        TickDeltaState state = RegionTickDeltaState.get(id);
        if (state == null) return 0.0f;
        computeTickDelta(state);
        return state.tickDelta;
    }

    public static boolean hasActiveInterpolation(String id) {
        TickDeltaState state = RegionTickDeltaState.get(id);
        if (state == null || state.lastTickTime == 0) return false;
        computeTickDelta(state);
        return state.tickDelta < 1.0f;
    }

    public static void recordTickStep(String id, double rate, double accumulatorPhase) {
        TickDeltaState state = RegionTickDeltaState.computeIfAbsent(id, k -> new TickDeltaState());
        long now = System.nanoTime();
        state.lastPacketTime = now;
        state.rate = rate;
        state.phase = (float)accumulatorPhase;
        state.packetInterval = (float)(1000.0 / rate);
        state.lastTickTime = now;
        state.tickDelta = (float)accumulatorPhase;
    }

    public static void clearRegion(String id) {
        RegionTickDeltaState.remove(id);
    }

    public static void clear() {
        RegionTickDeltaState.clear();
    }

}
