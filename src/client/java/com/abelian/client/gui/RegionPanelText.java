package com.abelian.client.gui;

import java.util.Locale;

/** 面板上的数字格式化。 */
public final class RegionPanelText {
    private RegionPanelText() {
    }

    // 显示用的紧凑数字：按数量级保留有效数字并去掉多余尾零，如 20.0 -> 20
    public static String number(double value) {
        if (!Double.isFinite(value)) return "-";
        double abs = Math.abs(value);
        String text;
        if (abs >= 1000) {
            text = String.format(Locale.ROOT, "%.0f", value);
        } else if (abs >= 100) {
            text = String.format(Locale.ROOT, "%.1f", value);
        } else if (abs >= 10) {
            text = String.format(Locale.ROOT, "%.2f", value);
        } else if (abs >= 1) {
            text = String.format(Locale.ROOT, "%.3f", value);
        } else {
            text = String.format(Locale.ROOT, "%.4f", value);
        }
        return trimTrailingZeros(text);
    }

    // 发给服务端命令用的数字：不裁剪精度、不带千分位，区别于显示用的 number
    public static String command(double value) {
        if (!Double.isFinite(value)) return "0";
        return trimTrailingZeros(Double.toString(value));
    }

    /** 千分位整型，用来显示很大的虚拟时间（gt）不会数不清位数。 */
    public static String integer(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    private static String trimTrailingZeros(String text) {
        int dot = text.indexOf('.');
        if (dot < 0) return text;
        int end = text.length();
        while (end > dot + 1 && text.charAt(end - 1) == '0') {
            end--;
        }
        if (end == dot + 1) {
            end = dot;
        }
        return text.substring(0, end);
    }
}
