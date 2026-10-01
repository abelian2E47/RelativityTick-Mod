package com.abelian;

import net.minecraft.world.World;

//区分返回区域时间与全局时间
public final class RegionTimeContext {
    private static final ThreadLocal<State> CURRENT = new ThreadLocal<>();

    public static void begin(World world, long tickTime) {
        State state = CURRENT.get();
        if (state == null) {
            state = new State();
            CURRENT.set(state);
        }
        state.world = world;
        state.tickTime = tickTime;
    }

    public static void end() {
        State state = CURRENT.get();
        if (state != null) {
            state.world = null;
        }
    }

    public static Long getTime(World world) {
        State state = CURRENT.get();
        return state != null && state.world == world ? state.tickTime : null;
    }

    private static final class State {
        private World world;
        private long tickTime;
    }
}
