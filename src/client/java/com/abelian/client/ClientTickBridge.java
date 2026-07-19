package com.abelian.client;

public class ClientTickBridge {
    private static final ThreadLocal<Boolean> CUSTOM_TICK_IN_PROGRESS =
        ThreadLocal.withInitial(() -> false);

    public static boolean isCustomTickInProgress() {
        return CUSTOM_TICK_IN_PROGRESS.get();
    }

    public static void setCustomTickInProgress(boolean value) {
        CUSTOM_TICK_IN_PROGRESS.set(value);
    }
}
