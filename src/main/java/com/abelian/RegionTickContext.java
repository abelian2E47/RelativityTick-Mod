package com.abelian;

import net.minecraft.world.World;

public final class RegionTickContext {
    private static final ThreadLocal<State> CURRENT = new ThreadLocal<>();

    private RegionTickContext() {
    }

    public static void begin(World world, long virtualTime) {
        CURRENT.set(new State(world, virtualTime));
    }

    public static void end() {
        CURRENT.remove();
    }

    public static Long getTime(World world) {
        State state = CURRENT.get();
        return state != null && state.world == world ? state.virtualTime : null;
    }

    private record State(World world, long virtualTime) {
    }
}
