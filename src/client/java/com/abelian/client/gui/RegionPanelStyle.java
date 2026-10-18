package com.abelian.client.gui;

import com.abelian.regionTick.RegionTickManager;

import java.util.Locale;

// 面板的一套配色：底色、文字灰阶与一条低饱和强调色；只给用户六个可改色位（见 Slot）
public record RegionPanelStyle(
        String id,
        int panelBg,
        /** 卡片竖向渐变的上端（比底色亮一点，光从上面来）。 */
        int panelTop,
        /** 卡片竖向渐变的下端（比底色暗一档）。 */
        int panelBottom,
        int panelHeader,
        int headerTop,
        int headerBottom,
        int panelBorder,
        /** 卡片内侧那一圈几乎看不见的细线：让卡片边缘有厚度，而不是一块贴纸。 */
        int borderSoft,
        /** 卡片顶部那道白色高光（玻璃感）。 */
        int sheen,
        int accent,
        /** 强调色的极淡版本：选中行、章节标记的底色。 */
        int accentSoft,
        int divider,
        int text,
        int textBright,
        int textMuted,
        int textDim,
        int good,
        int warn,
        int bad,
        int info,
        int idle,
        int rowAlt,
        int rowHover,
        /** 选中行/当前行的底色（掺强调色）。 */
        int rowSelected,
        int track,
        int trackBorder,
        int barFill,
        /** 进度条渐变的亮端，让条子有立体感。 */
        int barFillTop,
        int hoverFill,
        int hoverBorder,
        int iconButton,
        int iconButtonHover,
        int iconButtonBorder,
        int disabled,
        int toggle,
        int toggleHover,
        int toggleDisabled,
        int toggleBox,
        int scrollTrack,
        int keyBg,
        int keyBorder,
        int keyBgFocus,
        int shadow,
        int shadowSoft,
        int buttonPrimary,
        int buttonPrimaryHover,
        int buttonPrimaryBorder,
        // 开关「开」的颜色：勾选框的边与勾、右侧「启用」两字（见 ToggleSettingButton）；第六个可改色位
        int toggleOn
) {
    /** 语义色的基准值：各套配色共用，只按底色做对比度兜底。 */
    private static final int GOOD_HUE = 0x6FA88B;
    private static final int WARN_HUE = 0xB99A62;
    private static final int BAD_HUE = 0xB57169;
    private static final int INFO_HUE = 0x7AA0B2;
    private static final int ENABLED_HUE = 0x82BCE4;

    /** 正文/次级/更弱三档的对比度下限。正文是 9:1 而不是 4.5:1——面板底色永远不是纯色，留够余量。 */
    private static final double TEXT_CONTRAST = 9.0;
    private static final double MUTED_CONTRAST = 6.0;
    private static final double DIM_CONTRAST = 4.5;
    private static final double ACCENT_CONTRAST = 3.0;

    /** 当前生效的主题；面板设置里改它就是整块界面换色。 */
    private static volatile RegionPanelStyle current = Theme.values()[0].style;

    // 用户可改的六个色位；枚举 id 兼作配置键名与面板设置行标签，顺序即显示顺序
    public enum Slot {
        BG("bg", "screen.relativitytick.config.color.bg"),
        HEADER("header", "screen.relativitytick.config.color.header"),
        BORDER("border", "screen.relativitytick.config.color.border"),
        ACCENT("accent", "screen.relativitytick.config.color.accent"),
        TEXT("text", "screen.relativitytick.config.color.text"),
        /** 开关"开"色：勾选框与「启用」。排在最后一行，和上面的结构色分开看。 */
        ENABLED("enabled", "screen.relativitytick.config.color.enabled");

        private final String id;
        private final String translationKey;

        Slot(String id, String translationKey) {
            this.id = id;
            this.translationKey = translationKey;
        }

        public String id() {
            return id;
        }

        public String translationKey() {
            return translationKey;
        }

        /** 这一格在当前配色里实际生效的值。 */
        public int valueOf(RegionPanelStyle style) {
            return switch (this) {
                case BG -> style.panelBg();
                case HEADER -> style.panelHeader();
                case BORDER -> style.panelBorder();
                case ACCENT -> style.accent();
                case TEXT -> style.text();
                case ENABLED -> style.toggleOn();
            };
        }
    }

    // 预设配色；第一项为默认项，defaultThemeId 与未知 id 都回落到它
    public enum Theme {
        /** 黛青：深蓝灰底 + 钢蓝强调，默认。 */
        DAIQING("daiqing", 0x16181D, 0x2E3542, 0x333B48, 0x6FA0D8, 0xE8EBF1),
        /** 玄夜：近黑中性灰 + 冷银强调，最安静的一套。 */
        XUANYE("xuanye", 0x0F1013, 0x1D2026, 0x2A2E36, 0x9BB0CC, 0xE9ECF1),
        /** 松烟：墨绿灰 + 苔绿强调。 */
        SONGYAN("songyan", 0x121712, 0x212A21, 0x2C3529, 0x86C08F, 0xE6EFE3),
        /** 藕荷：灰紫底 + 藕荷强调。 */
        OUHE("ouhe", 0x17131B, 0x2A2233, 0x342B3E, 0xC58FD8, 0xF0E9F3),
        /** 黄昏：暖褐底 + 琥珀强调。 */
        HUANGHUN("huanghun", 0x1A1613, 0x2E2620, 0x3A2F26, 0xD9A860, 0xF3ECE1),
        /** 朱砂：暗红底 + 朱砂强调。 */
        ZHUSHA("zhusha", 0x1A1315, 0x30222A, 0x3C2B33, 0xD4797F, 0xF3E9EB);

        private final String themeId;
        private final RegionPanelStyle style;
        private final int bg;
        private final int header;
        private final int border;
        private final int accent;
        private final int text;

        Theme(String id, int bg, int header, int border, int accent, int text) {
            this.themeId = id;
            this.bg = bg;
            this.header = header;
            this.border = border;
            this.accent = accent;
            this.text = text;
            this.style = RegionPanelStyle.of(id, bg, header, border, accent, text, ENABLED_HUE);
        }

        public String id() {
            return themeId;
        }

        /** 主题名（"黛青"）的翻译键，面板设置里的主题一栏用它。 */
        public String translationKey() {
            return "screen.relativitytick.theme." + themeId;
        }

        public RegionPanelStyle style() {
            return style;
        }

        public int color(Slot slot) {
            return slot.valueOf(style);
        }

        /** 主题的代表色：标题栏与强调色的混合，用来画主题选择器里的那个小色块。 */
        public int swatchColor() {
            //mix() 只混 RGB、alpha 恒为 0，色块要真的画出来必须先补上不透明度
            return RegionPanelColor.opaque(RegionPanelColor.mix(header, accent, 0.75));
        }
    }

    // 由六个可改色位推出整套配色的派生值
    public static RegionPanelStyle of(String id, int bg, int header, int border, int accent, int text, int toggleOn) {
        boolean light = !RegionPanelColor.isDark(bg);
        int raised = RegionPanelColor.WHITE;
        int sunken = RegionPanelColor.BLACK;
        int line = light ? sunken : raised;

        int strong = RegionPanelColor.ensureContrast(text, bg, TEXT_CONTRAST);
        int bright = RegionPanelColor.ensureContrast(RegionPanelColor.mix(strong, RegionPanelColor.WHITE, 0.18),
                bg, TEXT_CONTRAST);
        int muted = RegionPanelColor.ensureContrast(RegionPanelColor.mix(strong, bg, 0.40), bg, MUTED_CONTRAST);
        int dim = RegionPanelColor.ensureContrast(RegionPanelColor.mix(strong, bg, 0.62), bg, DIM_CONTRAST);
        int accentColor = RegionPanelColor.ensureContrast(accent, bg, ACCENT_CONTRAST);

        //竖向渐变的两端：亮端掺白、暗端掺黑，都在底色的同一个色相上走，不会串色
        int panelTop = RegionPanelColor.mix(bg, raised, light ? 0.04 : 0.06);
        int panelBottom = RegionPanelColor.mix(bg, sunken, light ? 0.05 : 0.30);
        int headerTop = RegionPanelColor.mix(header, raised, light ? 0.03 : 0.08);
        int headerBottom = RegionPanelColor.mix(header, bg, light ? 0.20 : 0.45);

        return new RegionPanelStyle(id,
                RegionPanelColor.opaque(bg),
                RegionPanelColor.opaque(panelTop), RegionPanelColor.opaque(panelBottom),
                RegionPanelColor.opaque(header),
                RegionPanelColor.opaque(headerTop), RegionPanelColor.opaque(headerBottom),
                RegionPanelColor.opaque(border),
                RegionPanelColor.alpha(light ? 0x14 : 0x1E, line),
                RegionPanelColor.alpha(light ? 0x0A : 0x0D, raised),
                RegionPanelColor.opaque(accentColor),
                RegionPanelColor.alpha(0x2E, accentColor),
                RegionPanelColor.alpha(light ? 0x1E : 0x24, line),
                RegionPanelColor.opaque(strong), RegionPanelColor.opaque(bright),
                RegionPanelColor.opaque(muted), RegionPanelColor.opaque(dim),
                RegionPanelColor.opaque(RegionPanelColor.ensureContrast(GOOD_HUE, bg, DIM_CONTRAST)),
                RegionPanelColor.opaque(RegionPanelColor.ensureContrast(WARN_HUE, bg, DIM_CONTRAST)),
                RegionPanelColor.opaque(RegionPanelColor.ensureContrast(BAD_HUE, bg, DIM_CONTRAST)),
                RegionPanelColor.opaque(RegionPanelColor.ensureContrast(INFO_HUE, bg, DIM_CONTRAST)),
                RegionPanelColor.opaque(dim),
                RegionPanelColor.alpha(0x0F, raised), RegionPanelColor.alpha(0x1A, raised),
                RegionPanelColor.alpha(0x24, accentColor),
                RegionPanelColor.alpha(0x1F, raised), RegionPanelColor.alpha(0x33, line),
                RegionPanelColor.opaque(accentColor),
                RegionPanelColor.opaque(RegionPanelColor.mix(accentColor, RegionPanelColor.WHITE, 0.22)),
                RegionPanelColor.alpha(0x1F, raised), RegionPanelColor.alpha(0x40, line),
                RegionPanelColor.alpha(0x12, raised), RegionPanelColor.alpha(0x2A, raised),
                RegionPanelColor.alpha(0x33, line),
                RegionPanelColor.opaque(RegionPanelColor.ensureContrast(RegionPanelColor.mix(strong, bg, 0.72),
                        bg, ACCENT_CONTRAST)),
                RegionPanelColor.alpha(0x14, raised), RegionPanelColor.alpha(0x26, raised),
                RegionPanelColor.alpha(0x0A, raised), RegionPanelColor.alpha(0x33, sunken),
                RegionPanelColor.alpha(0x33, sunken), RegionPanelColor.alpha(0x59, sunken),
                RegionPanelColor.alpha(0x59, line),
                RegionPanelColor.alpha(0x3A, sunken),
                RegionPanelColor.alpha(light ? 0x1C : 0x4A, sunken),
                RegionPanelColor.alpha(light ? 0x10 : 0x24, sunken),
                RegionPanelColor.alpha(0x3A, accentColor), RegionPanelColor.alpha(0x58, accentColor),
                RegionPanelColor.alpha(0x99, accentColor),
                RegionPanelColor.opaque(RegionPanelColor.ensureContrast(toggleOn, bg, DIM_CONTRAST)));
    }

    public static RegionPanelStyle current() {
        return current;
    }

    public static void setCurrent(RegionPanelStyle style) {
        if (style != null) current = style;
    }

    /** 按主题 id 取样式，未知 id 回落到首个主题。 */
    public static Theme byId(String id) {
        Theme theme = preset(id);
        return theme != null ? theme : Theme.values()[0];
    }

    /** 按 id 取预设；{@code null} 表示这不是预设（自定义配色或坏数据）。 */
    public static Theme preset(String id) {
        if (id == null) return null;
        for (Theme theme : Theme.values()) {
            if (theme.themeId.equals(id)) return theme;
        }
        return null;
    }

    public static String defaultThemeId() {
        return Theme.values()[0].themeId;
    }

    /** 全部预设，面板设置里按这个顺序铺一排。 */
    public static Theme[] presets() {
        return Theme.values();
    }

    /** 状态徽章的文字颜色，同时也是徽章圆点的颜色。 */
    public int stateColor(RegionTickManager.RegionState state) {
        return switch (state) {
            case RUNNING -> good;
            case SPRINTING -> warn;
            case FROZEN -> text;
            case RELEASED -> idle;
        };
    }

    /** 状态徽章的底：状态色的极淡版本，让徽章是一块"牌子"而不是一行字。 */
    public int stateFill(RegionTickManager.RegionState state) {
        return RegionPanelColor.alpha(0x24, stateColor(state));
    }

    public static String stateKey(RegionTickManager.RegionState state) {
        return "screen.relativitytick.state." + state.name().toLowerCase(Locale.ROOT);
    }

    /** 负载比例（耗时 / 上限）对应的颜色，阈值与服务端 status 命令保持一致。 */
    public int loadColor(double ratio) {
        return ratio >= 0.9 ? bad : ratio >= 0.6 ? warn : good;
    }

    /** 一行在列表里的底色：选中 > 悬停 > 隔行。返回 0 表示不铺。 */
    public int rowBackground(boolean selected, boolean hovered, boolean alternate) {
        if (selected) return rowSelected;
        if (hovered) return rowHover;
        return alternate ? rowAlt : 0;
    }
}
