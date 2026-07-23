package com.abelian.client;

import com.abelian.network.RegionTPSPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class RegionTickDeltaManager {
    private static final Map<String, InterpolationState> RegionTickDeltaState = new ConcurrentHashMap<>();

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(RegionTPSPayload.ID, (payload, context) -> context.client().execute(() -> {
            RegionTickDeltaState.computeIfAbsent(payload.regionID(), k -> new InterpolationState());
            InterpolationState state = RegionTickDeltaState.get(payload.regionID());
            if (state != null) {
                state.tps = payload.TPS();
            }
        }));
    }

    public static void computeTickDelta(ClientRegion region) {
        InterpolationState state = region.getInterpolationState();
        if (state == null || state.lastTickTime == 0) return;
        long currentTime = System.nanoTime();
        float elapsedMs = (currentTime - state.lastTickTime) / 1_000_000.0f;
        if (region.getRegionTPS() > 0) {
            state.tickDelta = Math.min(1.0f, state.phase + elapsedMs * (float)region.getRegionTPS()/ 1000.0f);
        } else if (state.packetInterval > 0) {
            state.tickDelta = Math.min(1.0f, elapsedMs / state.packetInterval);
        }
    }

    public static float getTickDelta(String id) {
        ClientRegion region = ClientRegionManager.getRegion(id);
        if (region == null ||region.getRegionState() == null) return 0.0f;
        computeTickDelta(region);
        return region.getInterpolationState().tickDelta;
    }

    public static boolean hasActiveInterpolation(String id) {
        ClientRegion region = ClientRegionManager.getRegion(id);
        if (region == null) return false;
        InterpolationState state = region.getInterpolationState();
        if (state == null || state.lastTickTime == 0) return false;
        computeTickDelta(region);
        return state.tickDelta < 1.0f;
    }

    public static void recordTickStep(String id) {
        InterpolationState state = RegionTickDeltaState.computeIfAbsent(id, k -> new InterpolationState());
        long now = System.nanoTime();
        state.lastPacketTime = now;
        state.phase = 0;
        state.lastTickTime = now;
        state.tickDelta = 0;
    }

    public static void clearRegion(String id) {
        RegionTickDeltaState.remove(id);
    }

    public static void clear() {
        RegionTickDeltaState.clear();
    }

}
