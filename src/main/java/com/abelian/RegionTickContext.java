package com.abelian;

import net.minecraft.world.World;

public final class RegionTickContext {
    private static final ThreadLocal<State> CURRENT = new ThreadLocal<>();

    private RegionTickContext() {
    }

    public static void begin(World world, long tickTime) {
        CURRENT.set(new State(world, tickTime));
    }

    public static void end() {
        CURRENT.remove();
    }

    public static Long getTime(World world) {
        State state = CURRENT.get();
        return state != null && state.world == world ? state.tickTime : null;
    }

    private record State(World world, long tickTime) {
    }
}
