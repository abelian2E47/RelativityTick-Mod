package com.abelian;

import com.abelian.regionTick.Region;
import net.minecraft.entity.Entity;

import java.util.IdentityHashMap;
import java.util.Map;

public class ServerTickBridge {
    private static final ThreadLocal<Boolean> CUSTOM_TICK_IN_PROGRESS = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Map<Entity, Region>> REGION_TICK_OWNERS =
            ThreadLocal.withInitial(IdentityHashMap::new);

    public static boolean isCustomTickInProgress() {
        return CUSTOM_TICK_IN_PROGRESS.get();
    }

    public static void setCustomTickInProgress(boolean value) {
        CUSTOM_TICK_IN_PROGRESS.set(value);
    }

    public static void beginRegionTickBatch() {
        REGION_TICK_OWNERS.get().clear();
    }

    public static boolean claimEntity(Entity entity, Region region) {
        Region owner = REGION_TICK_OWNERS.get().putIfAbsent(entity, region);
        return owner == null || owner == region;
    }
}
