package com.abelian.client.gui;

import net.minecraft.client.gui.DrawContext;

// 面板的字符画图标集；图标一律画成实心像素，每个 Glyph 是一组等宽行，井号为实心
public final class RegionPanelIcons {

    /** 一枚图标：等宽字符画。 */
    public static final class Glyph {
        private final String[] rows;

        Glyph(String... rows) {
            this.rows = rows;
        }

        public int width() {
            return rows.length == 0 ? 0 : rows[0].length();
        }

        public int height() {
            return rows.length;
        }
    }

    public static final Glyph MONITOR = new Glyph(
            "############",
            "#..........#",
            "#..#....#..#",
            "#..#....#..#",
            "#..######..#",
            "#..........#",
            "#..........#",
            "############",
            "...######...",
            "..########..");

    public static final Glyph GEAR = new Glyph(
            "..##..##..",
            ".########.",
            "..######..",
            "###....###",
            "##......##",
            "##......##",
            "###....###",
            "..######..",
            ".########.",
            "..##..##..");

    public static final Glyph PLAY = new Glyph(
            "#......",
            "###....",
            "#####..",
            "#######",
            "#######",
            "#######",
            "#####..",
            "###....",
            "#......");

    public static final Glyph PAUSE = new Glyph(
            "##...##",
            "##...##",
            "##...##",
            "##...##",
            "##...##",
            "##...##",
            "##...##",
            "##...##",
            "##...##");

    public static final Glyph SNOWFLAKE = new Glyph(
            "....#....",
            "#...#...#",
            ".#..#..#.",
            "..#.#.#..",
            "#########",
            "..#.#.#..",
            ".#..#..#.",
            "#...#...#",
            "....#....");

    public static final Glyph STEP = new Glyph(
            "#....",
            "##...",
            "###..",
            "####.",
            "#####",
            "####.",
            "###..",
            "##...",
            "#....");

    public static final Glyph SPRINT = new Glyph(
            "...####",
            "..####.",
            ".####..",
            "####...",
            "######.",
            ".####..",
            "..###..",
            "..##...",
            ".##....");

    public static final Glyph CLOSE = new Glyph(
            "##...##",
            "###.###",
            ".#####.",
            "..###..",
            ".#####.",
            "###.###",
            "##...##");

    public static final Glyph CHECK = new Glyph(
            "......##",
            ".....##.",
            "##...##.",
            "###.##..",
            ".#####..",
            "..###...");

    public static final Glyph CHEVRON_DOWN = new Glyph(
            "#########",
            ".#######.",
            "..#####..",
            "...###...",
            "....#....");

    public static final Glyph CHEVRON_UP = new Glyph(
            "....#....",
            "...###...",
            "..#####..",
            ".#######.",
            "#########");

    public static final Glyph ARROW_UP = new Glyph(
            "...#...",
            "..###..",
            ".#####.",
            "#######",
            "...#...",
            "...#...",
            "...#...");

    public static final Glyph ARROW_DOWN = new Glyph(
            "...#...",
            "...#...",
            "...#...",
            "#######",
            ".#####.",
            "..###..",
            "...#...");

    public static final Glyph SEARCH = new Glyph(
            ".#####..",
            "##...##.",
            "#.....#.",
            "#.....#.",
            "#.....#.",
            "##...##.",
            ".#####.#",
            "......##");

    public static final Glyph CHART = new Glyph(
            "........#",
            "........#",
            "....#...#",
            "....#...#",
            "..#.#...#",
            "..#.#.#.#",
            "#.#.#.#.#",
            "#########");

    public static final Glyph DOT = new Glyph(
            ".###.",
            "#####",
            "#####",
            "#####",
            ".###.");

    public static final Glyph CLOCK = new Glyph(
            "..#####..",
            ".#.....#.",
            "#...#...#",
            "#...#...#",
            "#...##..#",
            "#.......#",
            ".#.....#.",
            "..#####..");

    public static final Glyph GAUGE = new Glyph(
            "..#####..",
            ".#.....#.",
            "#.......#",
            "#.......#",
            "#.......#",
            "#.......#");

    private static final int TAG_WIDTH = 3;
    private static final int TAG_HEIGHT = 5;

    private RegionPanelIcons() {
    }

    public static void draw(DrawContext context, Glyph glyph, int x, int y, int color) {
        for (int row = 0; row < glyph.rows.length; row++) {
            String line = glyph.rows[row];
            for (int column = 0; column < line.length(); column++) {
                if (line.charAt(column) == '#') {
                    context.fill(x + column, y + row, x + column + 1, y + row + 1, color);
                }
            }
        }
    }

    // 只画字符画最左边的 columns 列，用于「勾正在画上去」的逐列揭示动画
    public static void drawClipped(DrawContext context, Glyph glyph, int x, int y, int color, int columns) {
        if (columns <= 0) return;
        for (int row = 0; row < glyph.rows.length; row++) {
            String line = glyph.rows[row];
            int limit = Math.min(columns, line.length());
            for (int column = 0; column < limit; column++) {
                if (line.charAt(column) == '#') {
                    context.fill(x + column, y + row, x + column + 1, y + row + 1, color);
                }
            }
        }
    }

    public static int centered(int start, int size, int content) {
        return start + (size - content) / 2;
    }

    public static void monitor(DrawContext context, int x, int y, int color) {
        draw(context, MONITOR, x, y, color);
    }

    public static void gear(DrawContext context, int x, int y, int color) {
        draw(context, GEAR, x, y, color);
    }

    public static int monitorWidth() {
        return MONITOR.width();
    }

    public static int monitorHeight() {
        return MONITOR.height();
    }

    public static int gearWidth() {
        return GEAR.width();
    }

    public static int gearHeight() {
        return GEAR.height();
    }

    public static void regionTag(DrawContext context, int x, int y, int color) {
        context.drawBorder(x, y, TAG_WIDTH, TAG_HEIGHT, color);
        context.fill(x + 1, y + 1, x + TAG_WIDTH - 1, y + TAG_HEIGHT - 1, color);
    }

    public static int tagWidth() {
        return TAG_WIDTH;
    }
}
