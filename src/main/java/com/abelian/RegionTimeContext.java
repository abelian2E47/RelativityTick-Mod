package com.abelian;

import net.minecraft.world.World;

//区分返回区域时间与全局时间
public final class RegionTimeContext {
    private static final ThreadLocal<State> CURRENT = new ThreadLocal<>();

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

    private record State(World world, long tickTime) { }
}
