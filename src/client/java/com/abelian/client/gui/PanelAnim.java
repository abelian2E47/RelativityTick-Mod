package com.abelian.client.gui;

// 面板动画用到的数学：缓动、插值、与帧率无关的一阶滤波逼近
public final class PanelAnim {
    private PanelAnim() {
    }

    public static float clamp01(float value) {
        return value < 0f ? 0f : value > 1f ? 1f : value;
    }

    public static double clamp01(double value) {
        return value < 0d ? 0d : value > 1d ? 1d : value;
    }

    public static float easeOutCubic(float t) {
        float u = 1f - clamp01(t);
        return 1f - u * u * u;
    }

    public static float easeOutQuint(float t) {
        float u = 1f - clamp01(t);
        return 1f - u * u * u * u * u;
    }

    public static float easeInOut(float t) {
        float x = clamp01(t);
        return x < 0.5f ? 4f * x * x * x : 1f - (float) Math.pow(-2f * x + 2f, 3f) / 2f;
    }

    public static float lerp(float from, float to, float t) {
        return from + (to - from) * clamp01(t);
    }

    public static double lerp(double from, double to, double t) {
        return from + (to - from) * clamp01(t);
    }

    public static long nowMillis() {
        return System.nanoTime() / 1000000L;
    }

    public static float approach(float current, float target, float deltaSeconds, float rate) {
        float k = 1f - (float) Math.exp(-rate * Math.max(0f, deltaSeconds));
        return current + (target - current) * k;
    }

    public static float pulse(long nowMillis, float periodMillis) {
        if (periodMillis <= 0f) return 0f;
        float phase = (nowMillis % (long) periodMillis) / periodMillis;
        return (float) Math.sin(phase * Math.PI * 2.0);
    }

    public static float reveal(long startedMillis, long nowMillis, float durationMillis) {
        if (durationMillis <= 0f) return 1f;
        return easeOutCubic(clamp01((nowMillis - startedMillis) / durationMillis));
    }
}
