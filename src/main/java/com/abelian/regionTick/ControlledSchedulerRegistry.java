package com.abelian.regionTick;

import net.minecraft.world.tick.ChunkTickScheduler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ControlledSchedulerRegistry {
    private static final Map<ChunkTickScheduler<?>, Region> SCHEDULER_TO_REGION = new ConcurrentHashMap<>();

    private ControlledSchedulerRegistry() {
    }

    public static void register(ChunkTickScheduler<?> scheduler, Region region) {
        SCHEDULER_TO_REGION.put(scheduler, region);
    }

    public static void unregister(ChunkTickScheduler<?> scheduler, Region region) {
        SCHEDULER_TO_REGION.remove(scheduler, region);
    }

    public static Region getRegion(ChunkTickScheduler<?> scheduler) {
        return SCHEDULER_TO_REGION.get(scheduler);
    }

    public static void clearRegion(Region region) {
        SCHEDULER_TO_REGION.values().removeIf(value -> value == region);
    }
}
