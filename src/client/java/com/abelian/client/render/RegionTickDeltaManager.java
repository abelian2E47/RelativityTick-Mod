package com.abelian.client.render;

import com.abelian.client.clientRegionTick.ClientRegion;
import com.abelian.client.clientRegionTick.ClientRegionManager;

public class RegionTickDeltaManager {

    public static float getTickDelta(String id) {
        ClientRegion region = ClientRegionManager.getRegion(id);
        if (region == null || region.getRegionState() == null) return 0.0f;
        region.updateRenderDelta();
        return region.getInterpolationState().tickDelta;
    }

    public static boolean hasActiveInterpolation(String id) {
        ClientRegion region = ClientRegionManager.getRegion(id);
        if (region == null) return false;
        InterpolationState state = region.getInterpolationState();
        if (state == null || state.lastTickTime == 0) return false;
        region.updateRenderDelta();
        return state.tickDelta < 1.0f;
    }

}
