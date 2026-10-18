package com.abelian.client.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

// 面板绘制原语：背景、卡片、圆角、胶囊、徽章、进度条、指标行、迷你曲线，全部由圆角矩形逐行拼出
public final class RegionPanelDraw {
    /** 卡片标题栏高度。标题里的字与右上角的状态都对齐到它。 */
    public static final int HEADER_HEIGHT = 30;
    /** 卡片左右统一的留白：标题、分隔线、行内容都对齐到它。 */
    public static final int PADDING = 14;
    /** 常用圆角：卡片大一点，控件小一点。 */
    public static final int CORNER = 8;
    public static final int CORNER_SMALL = 5;
    /** 控件的标准高度，四个面板共用。 */
    public static final int ROW_HEIGHT = 22;

    private RegionPanelDraw() {
    }

    // ---------------------------------------------------------------- 背景

    // 整屏背景：竖向渐变加中点晕影与四周压暗
    public static void backdrop(DrawContext context, int width, int height) {
        RegionPanelStyle style = RegionPanelStyle.current();
        // 背景留一点透明度，透出底下模糊过的世界；卡片本体仍不透明
        int top = RegionPanelColor.alpha(0xC4, RegionPanelColor.mix(style.panelBg(), RegionPanelColor.BLACK, 0.45));
        int bottom = RegionPanelColor.alpha(0xD4, RegionPanelColor.mix(style.panelBg(), RegionPanelColor.BLACK, 0.62));
        context.fillGradient(0, 0, width, height, top, bottom);

        int veil = RegionPanelColor.alpha(0x44, RegionPanelColor.BLACK);
        int clear = RegionPanelColor.alpha(0x00, RegionPanelColor.BLACK);
        context.fillGradient(0, 0, width, Math.max(1, height / 3), veil, clear);
        context.fillGradient(0, height - Math.max(1, height / 3), width, height, clear, veil);
        //左右两侧各一条竖向压暗：逐列填充，只有最外侧这些列需要
        int edge = Math.min(140, width / 4);
        for (int i = 0; i < edge; i++) {
            int color = RegionPanelColor.alpha((int) (0x30 * (1.0 - (double) i / edge)), RegionPanelColor.BLACK);
            if (color == 0) continue;
            context.fill(i, 0, i + 1, height, color);
            context.fill(width - i - 1, 0, width - i, height, color);
        }
    }

    // 卡片后面那圈强调色光晕，层数由 spread 给，越往外越淡
    public static void glow(DrawContext context, int x, int y, int w, int h, int spread) {
        int accent = RegionPanelStyle.current().accent();
        for (int i = spread; i >= 1; i--) {
            int alpha = Math.max(2, 0x14 - i * 2);
            roundRect(context, x - i * 2, y - i * 2, w + i * 4, h + i * 4, CORNER + i * 2,
                    RegionPanelColor.alpha(alpha, accent));
        }
    }

    // ---------------------------------------------------------------- 卡片

    /** 卡片本体：投影 → 竖向渐变底 → 顶部高光 → 标题栏渐变 → 强调线 → 描边。 */
    public static void card(DrawContext context, int x, int y, int w, int h) {
        RegionPanelStyle style = RegionPanelStyle.current();
        shadow(context, x, y, w, h, CORNER, 5, 3);
        roundRectGradient(context, x, y, w, h, CORNER, style.panelTop(), style.panelBottom());
        roundRectGradient(context, x, y, w, HEADER_HEIGHT, CORNER, style.headerTop(), style.headerBottom());
        //标题栏与正文之间那条细线：两块实色直接相接时边界会糊在一起
        context.fill(x + 1, y + HEADER_HEIGHT, x + w - 1, y + HEADER_HEIGHT + 1, style.divider());
        //顶部强调线：整块卡片唯一的饱和色，界面的"品牌位"
        roundRect(context, x + CORNER, y, w - CORNER * 2, 2, 1, style.accent());
        //玻璃高光：标题栏上缘一条几乎看不见的白
        context.fill(x + CORNER, y + 1, x + w - CORNER, y + 2, style.sheen());
        outline(context, x, y, w, h, CORNER, style.panelBorder());
        outline(context, x + 1, y + 1, w - 2, h - 2, CORNER - 1, style.borderSoft());
    }

    /** 左上角一枚强调色竖标 + 标题，右上角 subtitle。标题放不下时先截标题，subtitle 永远完整。 */
    public static void header(DrawContext context, TextRenderer textRenderer, int x, int y, int w,
                              Text title, Text subtitle) {
        RegionPanelStyle style = RegionPanelStyle.current();
        int textY = y + (HEADER_HEIGHT - 8) / 2 + 1;
        roundRect(context, x + PADDING, textY + 1, 3, 8, 2, style.accent());

        int cursor = x + PADDING + 9;
        if (subtitle != null) {
            int subtitleWidth = textRenderer.getWidth(subtitle);
            int chipWidth = subtitleWidth + 12;
            int chipX = x + w - PADDING - chipWidth;
            chip(context, textRenderer, chipX, textY - 3, subtitle, style.textMuted(),
                    style.track(), style.trackBorder());
            int room = chipX - 10 - cursor;
            context.drawTextWithShadow(textRenderer,
                    Text.literal(textRenderer.trimToWidth(title.getString(), Math.max(0, room))),
                    cursor, textY, style.textBright());
            return;
        }
        context.drawTextWithShadow(textRenderer, title, cursor, textY, style.textBright());
    }

    /** 页脚上缘的一条横线：页脚与内容分开，但不用再画一条完整的卡片边。 */
    public static void footerRule(DrawContext context, int x, int y, int w) {
        RegionPanelStyle style = RegionPanelStyle.current();
        context.fill(x + PADDING, y, x + w - PADDING, y + 1, style.divider());
    }

    public static void divider(DrawContext context, int x, int y, int w) {
        context.fill(x, y, x + w, y + 1, RegionPanelStyle.current().divider());
    }

    /** 章节小标题：左边一小段强调色，后面跟一行弱灰的字。 */
    public static void sectionLabel(DrawContext context, TextRenderer textRenderer, int x, int y, Text label) {
        RegionPanelStyle style = RegionPanelStyle.current();
        roundRect(context, x, y + 2, 2, 6, 1, style.accent());
        context.drawTextWithShadow(textRenderer, label, x + 7, y, style.textMuted());
    }

    // ---------------------------------------------------------------- 徽章 / 胶囊

    /** 色块：面板设置里色位左边那一小块，用来"看见"这一格现在生效的是什么颜色。 */
    public static void swatch(DrawContext context, int x, int y, int size, int color, boolean selected) {
        RegionPanelStyle style = RegionPanelStyle.current();
        roundRect(context, x, y, size, size, 3, color);
        outline(context, x, y, size, size, 3, selected ? style.accent() : style.trackBorder());
    }

    /** 圆角小牌子：标题栏右上角的副标题、监测屏的新鲜度都用它。 */
    public static int chip(DrawContext context, TextRenderer textRenderer, int x, int y, Text text,
                           int textColor, int fill, int border) {
        int width = chipWidth(textRenderer, text);
        roundRect(context, x, y, width, 14, 4, fill);
        if (border != 0) outline(context, x, y, width, 14, 4, border);
        context.drawTextWithShadow(textRenderer, text, x + 6, y + 3, textColor);
        return width;
    }

    public static int chipWidth(TextRenderer textRenderer, Text text) {
        return textRenderer.getWidth(text) + 12;
    }

    /** 状态色圆点 + 同色文字，返回占用宽度；悬停时垫一层底并描边（它是"进监测"的点击区）。 */
    public static int badge(DrawContext context, TextRenderer textRenderer, Text text, int x, int y, int color, boolean hovered) {
        RegionPanelStyle style = RegionPanelStyle.current();
        int width = badgeWidth(textRenderer, text);
        int fill = hovered ? style.hoverFill() : RegionPanelColor.alpha(0x18, color);
        roundRect(context, x - 4, y - 2, width + 8, 14, 7, fill);
        if (hovered) outline(context, x - 4, y - 2, width + 8, 14, 7, style.hoverBorder());
        roundRect(context, x, y + 3, 5, 5, 2, color);
        context.drawTextWithShadow(textRenderer, text, x + 9, y + 2, color);
        return width;
    }

    public static int badgeWidth(TextRenderer textRenderer, Text text) {
        return 9 + textRenderer.getWidth(text);
    }

    public static void pill(DrawContext context, int x, int y, int w, int h, int color) {
        roundRect(context, x, y, w, h, h / 2, color);
    }

    /** 列表行左侧那根悬停竖条。 */
    public static void rowAccent(DrawContext context, int x, int y, int height) {
        roundRect(context, x, y + 2, 2, height - 4, 1, RegionPanelStyle.current().accent());
    }

    // ---------------------------------------------------------------- 条 / 读数

    /** 细进度条：圆角槽底 + 按比例填充 + 1px 描边。ratio 会被裁到 [0,1]。 */
    public static void bar(DrawContext context, int x, int y, int w, int h, double ratio, int color) {
        barPixels(context, x, y, w, h, (int) Math.round(w * Math.clamp(ratio, 0.0, 1.0)), color);
    }

    // 与 bar 同一套线，但填充量直接给像素，供滑块把填充右界落在手柄中缝上
    public static void barPixels(DrawContext context, int x, int y, int w, int h, int filled, int color) {
        if (w <= 0 || h <= 0) return;
        RegionPanelStyle style = RegionPanelStyle.current();
        roundRect(context, x, y, w, h, h / 2, style.track());
        int fillWidth = Math.clamp(filled, 0, w);
        if (fillWidth > 0) {
            roundRectGradient(context, x, y, fillWidth, h, h / 2,
                    RegionPanelColor.opaque(RegionPanelColor.mix(color, RegionPanelColor.WHITE, 0.18)), color);
        }
        outline(context, x, y, w, h, h / 2, style.trackBorder());
    }

    /** 一行"标签 …… 值"：标签左对齐并在剩余空间里截断，值右对齐到 right。 */
    public static void metric(DrawContext context, TextRenderer textRenderer, int x, int right, int y,
                              Text label, Text value, int valueColor) {
        RegionPanelStyle style = RegionPanelStyle.current();
        int valueWidth = textRenderer.getWidth(value);
        int labelRoom = right - x - valueWidth - 8;
        context.drawTextWithShadow(textRenderer,
                Text.literal(textRenderer.trimToWidth(label.getString(), Math.max(0, labelRoom))),
                x, y, style.textMuted());
        context.drawTextWithShadow(textRenderer, value, right - valueWidth, y, valueColor);
    }

    // 一段「标签 + 读数」的卡片格：标签在上、数值在下且用大一号亮色
    public static void tile(DrawContext context, TextRenderer textRenderer, int x, int y, int w, Text label,
                            Text value, int valueColor, boolean highlighted) {
        RegionPanelStyle style = RegionPanelStyle.current();
        roundRect(context, x, y, w, 26, CORNER_SMALL, highlighted ? style.track() : style.rowAlt());
        outline(context, x, y, w, 26, CORNER_SMALL, highlighted ? style.trackBorder() : 0);
        context.drawTextWithShadow(textRenderer,
                Text.literal(textRenderer.trimToWidth(label.getString(), w - 12)), x + 6, y + 4, style.textMuted());
        context.drawTextWithShadow(textRenderer,
                Text.literal(textRenderer.trimToWidth(value.getString(), w - 12)), x + 6, y + 14, valueColor);
    }

    // 「上限占用」这一段的纵向高度：标签行 + 空档 + 条 + 空档 + 读数行
    public static final int LOAD_HEIGHT = 34;
    /** 挤到极限时的「上限占用」：只留标签 + 右端一个百分比，占一行。 */
    public static final int LOAD_HEIGHT_COMPACT = 12;

    private static final int LOAD_BAR_TOP = 13;
    private static final int LOAD_BAR_HEIGHT = 6;
    private static final int LOAD_READING_TOP = 24;

    // 「上限占用」一整段：标签行 → 占用条 → 条下「实测 / 上限」与右端彩色百分比
    public static void load(DrawContext context, TextRenderer textRenderer, int x, int right, int y,
                            double usedMs, double limitMs, boolean known) {
        load(context, textRenderer, x, right, y, usedMs, limitMs, known, false);
    }

    // 「上限占用」紧凑版：只画标签和右端百分比，不画条与读数行
    public static void load(DrawContext context, TextRenderer textRenderer, int x, int right, int y,
                            double usedMs, double limitMs, boolean known, boolean compact) {
        RegionPanelStyle style = RegionPanelStyle.current();
        if (compact) {
            Text label = Text.translatable("screen.relativitytick.metric.load");
            context.drawTextWithShadow(textRenderer, label, x, y, style.textMuted());
            Text tail = known && limitMs > 0.0
                    ? Text.translatable("screen.relativitytick.percent", Math.round(usedMs / limitMs * 100))
                    : Text.translatable("screen.relativitytick.unknown");
            int color = known && limitMs > 0.0 ? style.loadColor(usedMs / limitMs) : style.textDim();
            context.drawTextWithShadow(textRenderer, tail, right - textRenderer.getWidth(tail), y, color);
            return;
        }
        Text label = Text.translatable("screen.relativitytick.metric.load");
        context.drawTextWithShadow(textRenderer, label, x, y, style.textMuted());
        if (!known || limitMs <= 0.0) {
            Text unknown = Text.translatable("screen.relativitytick.unknown");
            context.drawTextWithShadow(textRenderer, unknown, right - textRenderer.getWidth(unknown), y, style.textDim());
            return;
        }

        double ratio = usedMs / limitMs;
        int color = style.loadColor(ratio);
        bar(context, x, y + LOAD_BAR_TOP, right - x, LOAD_BAR_HEIGHT, ratio, color);

        int readingY = y + LOAD_READING_TOP;
        context.drawTextWithShadow(textRenderer,
                Text.translatable("screen.relativitytick.load.value", RegionPanelText.number(usedMs),
                        RegionPanelText.number(limitMs)),
                x, readingY, style.textMuted());
        //百分比单独右对齐并且带阈值色：这条读数里唯一会变色、也是唯一要一眼看到的就是它
        Text percent = Text.translatable("screen.relativitytick.percent", Math.round(ratio * 100));
        context.drawTextWithShadow(textRenderer, percent, right - textRenderer.getWidth(percent), readingY, color);
    }

    /** 右对齐的细滚动条；内容不足一屏时不画。 */
    public static void scrollbar(DrawContext context, int x, int top, int bottom, int first, int visible, int total) {
        if (total <= visible) return;
        RegionPanelStyle style = RegionPanelStyle.current();
        int trackHeight = bottom - top;
        pill(context, x, top, 3, trackHeight, style.scrollTrack());
        int thumbHeight = Math.max(16, (int) (trackHeight * (double) visible / total));
        int maxFirst = total - visible;
        int offset = maxFirst <= 0 ? 0 : (int) ((trackHeight - thumbHeight) * (double) first / maxFirst);
        pill(context, x, top + offset, 3, thumbHeight, style.accent());
    }

    // 迷你曲线：把一串历史读数画成折线（下面带一层极淡面积），取 samples 前 count 个按旧→新绘制
    public static void sparkline(DrawContext context, TextRenderer textRenderer, int x, int y, int w, int h,
                                 double[] samples, int count, double min, double max, double reference, int color, Text emptyHint) {
        if (w <= 1 || h <= 1) return;
        RegionPanelStyle style = RegionPanelStyle.current();
        roundRect(context, x, y, w, h, 3, style.rowAlt());
        double span = Math.max(1.0e-9, max - min);
        int base = y + h;
        //参照线：目标速率 / mspt 上限，虚线画在曲线下面
        if (Double.isFinite(reference) && reference >= min && reference <= max) {
            int refY = base - 1 - (int) Math.round(Math.clamp((reference - min) / span, 0.0, 1.0) * (h - 3));
            for (int column = 0; column < w; column += 4) {
                context.fill(x + column, refY, x + Math.min(w, column + 2), refY + 1, style.trackBorder());
            }
        }
        if (count <= 0) {
            //还没有样本：留一条基线和一句说明，空框会被当成"画坏了"
            context.fill(x + 2, base - 3, x + w - 2, base - 2, style.trackBorder());
            context.drawTextWithShadow(textRenderer, emptyHint,
                    x + (w - textRenderer.getWidth(emptyHint)) / 2, y + (h - 8) / 2, style.textDim());
            outline(context, x, y, w, h, 3, style.trackBorder());
            return;
        }
        //面积填充走竖向渐变，曲线贴顶时才不会看起来"填满了"
        int areaTop = RegionPanelColor.alpha(0x00, color);
        int areaBottom = RegionPanelColor.alpha(0x33, color);
        int previous = -1;
        for (int column = 0; column < w; column++) {
            int index = count <= 1 ? 0 : (int) ((long) column * (count - 1) / Math.max(1, w - 1));
            double value = samples[Math.min(index, count - 1)];
            double normalized = Math.clamp((value - min) / span, 0.0, 1.0);
            int pointY = base - 1 - (int) Math.round(normalized * (h - 3));
            if (pointY < base) {
                context.fillGradient(x + column, pointY, x + column + 1, base, areaTop, areaBottom);
            }
            //相邻两列之间补上竖直的一段，斜线才不会断成虚线
            if (previous >= 0) {
                int from = Math.min(previous, pointY);
                int to = Math.max(previous, pointY) + 1;
                context.fill(x + column, from, x + column + 1, to, color);
            } else {
                context.fill(x + column, pointY, x + column + 1, pointY + 1, color);
            }
            previous = pointY;
        }
        outline(context, x, y, w, h, 3, style.trackBorder());
    }

    // ---------------------------------------------------------------- 低层形状

    /** 圆角矩形。同一缩进的行合并成一次 fill，圆角卡片的代价只有十来次填充。 */
    public static void roundRect(DrawContext context, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0 || (color >>> 24) == 0) return;
        int r = clampRadius(radius, w, h);
        if (r == 0) {
            context.fill(x, y, x + w, y + h, color);
            return;
        }
        int runStart = 0;
        int runInset = insetFor(0, h, r);
        for (int row = 1; row <= h; row++) {
            int inset = row < h ? insetFor(row, h, r) : -1;
            if (inset != runInset) {
                context.fill(x + runInset, y + runStart, x + w - runInset, y + row, color);
                runStart = row;
                runInset = inset;
            }
        }
    }

    /** 竖向渐变的圆角矩形。逐行画，一行一个颜色。 */
    public static void roundRectGradient(DrawContext context, int x, int y, int w, int h, int radius,
                                         int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        int r = clampRadius(radius, w, h);
        for (int row = 0; row < h; row++) {
            int inset = r == 0 ? 0 : insetFor(row, h, r);
            context.fill(x + inset, y + row, x + w - inset, y + row + 1,
                    h <= 1 ? top : blend(top, bottom, (double) row / (h - 1)));
        }
    }

    /** 圆角描边（1px）。虚线感的斜角由逐行错位自然形成。 */
    public static void outline(DrawContext context, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0 || (color >>> 24) == 0) return;
        int r = clampRadius(radius, w, h);
        if (r == 0) {
            context.drawBorder(x, y, w, h, color);
            return;
        }
        for (int row = 0; row < h; row++) {
            int inset = insetFor(row, h, r);
            int left = x + inset;
            int right = x + w - inset;
            if (row == 0 || row == h - 1) {
                context.fill(left + 1, y + row, right - 1, y + row + 1, color);
            } else {
                context.fill(left, y + row, left + 1, y + row + 1, color);
                context.fill(right - 1, y + row, right, y + row + 1, color);
            }
        }
    }

    /** 卡片投影：几圈由外向内越来越深的圆角矩形，整体下移一点（光从上面来）。 */
    public static void shadow(DrawContext context, int x, int y, int w, int h, int radius, int spread, int offsetY) {
        RegionPanelStyle style = RegionPanelStyle.current();
        for (int i = spread; i >= 1; i--) {
            roundRect(context, x - i, y - i + offsetY, w + i * 2, h + i * 2, radius + i,
                    i <= 2 ? style.shadow() : style.shadowSoft());
        }
    }

    // 两个 ARGB 颜色按 t 插值（含 alpha）
    public static int blend(int from, int to, double t) {
        double amount = Math.clamp(t, 0.0, 1.0);
        if (amount <= 0.0) return from;
        if (amount >= 1.0) return to;
        int a = blendChannel(from >>> 24, to >>> 24, amount);
        int r = blendChannel(from >> 16 & 0xFF, to >> 16 & 0xFF, amount);
        int g = blendChannel(from >> 8 & 0xFF, to >> 8 & 0xFF, amount);
        int b = blendChannel(from & 0xFF, to & 0xFF, amount);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static int blendChannel(int from, int to, double amount) {
        return (int) Math.round(from + (to - from) * amount);
    }

    private static int clampRadius(int radius, int w, int h) {
        return Math.max(0, Math.min(radius, Math.min(w, h) / 2));
    }

    /** 第 row 行为了凑出圆角要向内缩进多少像素。 */
    private static int insetFor(int row, int h, int r) {
        int dy;
        if (row < r) {
            dy = r - 1 - row;
        } else if (row >= h - r) {
            dy = row - (h - r);
        } else {
            dy = 0;
        }
        if (dy <= 0) return 0;
        double inset = r - Math.sqrt((double) r * r - (double) dy * dy);
        return (int) Math.round(inset);
    }
}
