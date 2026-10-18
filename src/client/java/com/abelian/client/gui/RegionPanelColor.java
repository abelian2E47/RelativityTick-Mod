package com.abelian.client.gui;

import java.util.Locale;

// 面板配色的色彩运算：hex 解析与格式化、混合、相对亮度、对比度与可读性兜底
public final class RegionPanelColor {
    public static final int WHITE = 0xFFFFFF;
    public static final int BLACK = 0x000000;

    /** 0..255 的 sRGB 分量 → 线性亮度分量，查表省掉每个色块每帧的 pow。 */
    private static final double[] LINEAR = new double[256];

    static {
        for (int i = 0; i < 256; i++) {
            double value = i / 255.0;
            LINEAR[i] = value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
        }
    }

    private RegionPanelColor() {
    }

    /** 解析 {@code #RRGGBB} / {@code RRGGBB}；不是 6 位十六进制就返回 {@code null}。 */
    public static Integer parse(String text) {
        if (text == null) return null;
        String value = text.trim();
        if (value.startsWith("#")) value = value.substring(1);
        if (value.length() != 6) return null;
        try {
            return Integer.parseInt(value, 16) & 0xFFFFFF;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /** 面板与配置文件里统一的写法：{@code #RRGGBB}。 */
    public static String format(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    // 补上不透明通道；配色里定义的色都是实色，只有叠层才带透明度
    public static int opaque(int rgb) {
        return 0xFF000000 | rgb & 0xFFFFFF;
    }

    // 线性混色，t = 0 取 from、t = 1 取 to；只混 RGB，返回值不带 alpha
    public static int mix(int from, int to, double t) {
        double amount = Math.clamp(t, 0.0, 1.0);
        int r = channel(from, 16) + (int) Math.round((channel(to, 16) - channel(from, 16)) * amount);
        int g = channel(from, 8) + (int) Math.round((channel(to, 8) - channel(from, 8)) * amount);
        int b = channel(from, 0) + (int) Math.round((channel(to, 0) - channel(from, 0)) * amount);
        return r << 16 | g << 8 | b;
    }

    public static double luminance(int rgb) {
        return 0.2126 * LINEAR[channel(rgb, 16)] + 0.7152 * LINEAR[channel(rgb, 8)] + 0.0722 * LINEAR[channel(rgb, 0)];
    }

    /** 两色对比度，1.0 ~ 21.0。 */
    public static double contrast(int first, int second) {
        double a = luminance(first);
        double b = luminance(second);
        return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
    }

    /** 底色是深色的吗——决定"提亮/压暗"哪一边才是可见的一层。 */
    public static boolean isDark(int rgb) {
        return luminance(rgb) < 0.3;
    }

    // 把前景往黑或白推，直到与底色的对比度达标，保证文字始终可读
    public static int ensureContrast(int foreground, int background, double target) {
        if (contrast(foreground, background) >= target) return foreground;
        boolean towardBlack = contrast(BLACK, background) >= contrast(WHITE, background);
        int anchor = towardBlack ? BLACK : WHITE;
        for (int step = 1; step <= 24; step++) {
            int candidate = mix(foreground, anchor, step / 24.0);
            if (contrast(candidate, background) >= target) return candidate;
        }
        return anchor;
    }

    /** 叠一层半透明色（画在底色之上的灰阶、轨道、悬停层都用它）。 */
    public static int alpha(int alpha, int rgb) {
        return Math.clamp(alpha, 0, 255) << 24 | rgb & 0xFFFFFF;
    }

    private static int channel(int rgb, int shift) {
        return rgb >> shift & 0xFF;
    }
}
