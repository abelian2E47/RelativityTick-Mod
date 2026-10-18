package com.abelian.client;

import com.abelian.client.config.RelativityTickClientConfig;
import com.abelian.client.gui.KeyCaptureButton;
import com.abelian.client.gui.PanelButton;
import com.abelian.client.gui.PanelTextField;
import com.abelian.client.gui.RegionPanelColor;
import com.abelian.client.gui.RegionPanelDraw;
import com.abelian.client.gui.RegionPanelScreen;
import com.abelian.client.gui.RegionPanelStyle;
import com.abelian.client.gui.RegionPanelText;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

// 面板外观设置：面板自己的主题与操作节奏，和具体区域无关
public class PanelConfigScreen extends RegionPanelScreen {
    private static final int ROW_HEIGHT = 24;
    /** 窗口矮（比如 GUI 缩放 4）时用的紧凑行高：九行 + 标题 + 页脚要塞进 270 逻辑像素。 */
    private static final int DENSE_ROW_HEIGHT = 20;
    private static final int ROW_GAP = 4;
    private static final int DENSE_ROW_GAP = 2;
    // 第三档（GUI 缩放 2 的 427x240）：行高压到 18、去掉行间距、控件压到 16
    private static final int COMPACT_ROW_HEIGHT = 18;
    private static final int COMPACT_ROW_GAP = 0;
    private static final int COMPACT_WIDGET_HEIGHT = 16;
    private static final int KEY_WIDTH = 96;
    private static final int LABEL_GAP = 10;
    /** 色块：输入框左边那一小块当前生效的颜色。 */
    private static final int SWATCH_SIZE = 10;
    private static final int SWATCH_GAP = 6;
    // 步进刻数是数字输入 + 两个微调按钮，不是滑块
    private static final int STEP_BUTTON_WIDTH = 20;
    private static final int STEP_GAP = 4;

    // 主题是一排只画色块的按钮，名字进 tooltip，当前生效的那套写在色块右边
    private static final int THEME_CHIP_WIDTH = 26;
    private static final int THEME_CHIP_GAP = 4;
    private static final int THEME_SWATCH_SIZE = 12;
    /** 页脚左边那个按钮的最小宽度：中文六个字和英文 "Restore default palette" 都得放得下。 */
    private static final int RESTORE_BUTTON_MIN_WIDTH = 96;
    private static final int CLEAR_BUTTON_MIN_WIDTH = 40;

    private static final int THEME_ROW = 0;
    private static final int COLOR_ROW = THEME_ROW + 1;
    /** 六个色位占 1..6 行，后面的行都排在分隔线下面。 */
    private static final int STEP_ROW = COLOR_ROW + RegionPanelStyle.Slot.values().length;
    private static final int KEY_ROW = STEP_ROW + 1;
    private static final int ROW_COUNT = KEY_ROW + 1;

    private final Screen parent;
    private final Map<RegionPanelStyle.Slot, TextFieldWidget> colorFields =
            new EnumMap<>(RegionPanelStyle.Slot.class);
    /** 主题列表取自基础库：新增预设只改那一处，这屏不用跟着动。 */
    private final RegionPanelStyle.Theme[] themes = RegionPanelStyle.presets();
    private final PanelButton[] themeChips = new PanelButton[RegionPanelStyle.presets().length];

    private Layout layout = Layout.EMPTY;
    private PanelTextField stepField;
    private KeyCaptureButton keyButton;
    private PanelButton clearKeyButton;
    private PanelButton presetButton;
    /** 我们自己改写输入框内容时置起，避免 setText 触发的监听又一次提交。 */
    private boolean updatingFields;

    public PanelConfigScreen(Screen parent) {
        super(Text.translatable("screen.relativitytick.panel_config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        layout = computeLayout();
        buildWidgets();
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private void buildWidgets() {
        //主题：一排小色块。空文案是故意的——色块本身就是内容，文字由 drawThemeChips 画在旁边
        int chipX = layout.controlX();
        for (int i = 0; i < themes.length; i++) {
            RegionPanelStyle.Theme theme = themes[i];
            PanelButton chip = new PanelButton(chipX, widgetY(THEME_ROW), THEME_CHIP_WIDTH, layout.widgetHeight(),
                    Text.empty(), Text.translatable(theme.translationKey()),
                    button -> selectTheme(theme), PanelButton.Kind.SECONDARY);
            chip.setTooltip(Tooltip.of(Text.translatable("screen.relativitytick.config.theme.tooltip",
                    Text.translatable(theme.translationKey()))));
            themeChips[i] = addDrawableChild(chip);
            chipX += THEME_CHIP_WIDTH + THEME_CHIP_GAP;
        }

        for (RegionPanelStyle.Slot slot : RegionPanelStyle.Slot.values()) {
            int row = COLOR_ROW + slot.ordinal();
            PanelTextField field = addDrawableChild(new PanelTextField(textRenderer, layout.fieldX(),
                    widgetY(row), FIELD_WIDTH, layout.widgetHeight(), Text.translatable(slot.translationKey())));
            field.setMaxLength(7);
            field.setTextPredicate(text -> text.matches("[0-9a-fA-F#]*"));
            field.setText(RelativityTickClientConfig.getPanelColorHex(slot));
            field.setTooltip(Tooltip.of(Text.translatable("screen.relativitytick.config.color.tooltip")));
            field.setChangedListener(text -> {
                Integer rgb = RegionPanelColor.parse(text);
                //输入中途（"#16"）解析不出来就什么都不做：只有填满 6 位才提交，不必按回车
                if (!updatingFields && rgb != null) submitColor(slot, rgb);
            });
            colorFields.put(slot, field);
        }

        //步长：数字框在中间，两边是 -1 / +1 的微调键；右边缘和其它行对齐
        stepField = addDrawableChild(new PanelTextField(textRenderer, layout.stepFieldX(), widgetY(STEP_ROW),
                FIELD_WIDTH, layout.widgetHeight(), Text.translatable("screen.relativitytick.config.step")));
        stepField.setMaxLength(4);
        stepField.setTextPredicate(text -> text.matches("[0-9]*"));
        stepField.setText(RegionPanelText.integer(RelativityTickClientConfig.getStepSize()));
        stepField.setTooltip(Tooltip.of(Text.translatable("screen.relativitytick.config.step.tooltip")));
        stepField.setChangedListener(text -> {
            Integer value = parseStep(text);
            //边填边生效：敲到哪一位都先按那个数步进，回车/点走时再归位到合法值
            if (!updatingFields && value != null) submitStepSize(value);
        });

        addDrawableChild(new PanelButton(layout.stepMinusX(), widgetY(STEP_ROW), STEP_BUTTON_WIDTH, layout.widgetHeight(),
                Text.literal("-"), Text.translatable("screen.relativitytick.config.step.decrease"),
                button -> nudgeStep(-1), PanelButton.Kind.SECONDARY));
        addDrawableChild(new PanelButton(layout.stepPlusX(), widgetY(STEP_ROW), STEP_BUTTON_WIDTH, layout.widgetHeight(),
                Text.literal("+"), Text.translatable("screen.relativitytick.config.step.increase"),
                button -> nudgeStep(1), PanelButton.Kind.SECONDARY));

        keyButton = addDrawableChild(new KeyCaptureButton(layout.controlRight() - KEY_WIDTH,
                widgetY(KEY_ROW), KEY_WIDTH, layout.widgetHeight(),
                parseKey(RelativityTickClientConfig.getStepKey()), this::submitStepKey));

        // 解绑按钮放在录制键左边同一行，负责把录制键显示回「未绑定」
        Text clearLabel = Text.translatable("screen.relativitytick.config.key.clear");
        int clearWidth = Math.max(CLEAR_BUTTON_MIN_WIDTH, textRenderer.getWidth(clearLabel) + 16);
        clearKeyButton = new PanelButton(layout.controlRight() - KEY_WIDTH - STEP_GAP - clearWidth,
                widgetY(KEY_ROW), clearWidth, layout.widgetHeight(), clearLabel,
                Text.translatable("screen.relativitytick.config.key.clear.narration"),
                button -> clearStepKey(), PanelButton.Kind.SECONDARY);
        clearKeyButton.setTooltip(Tooltip.of(Text.translatable("screen.relativitytick.config.key.clear.tooltip")));
        addDrawableChild(clearKeyButton);

        // 「回到默认配色」放在页脚左边，已是默认配色时置灰
        presetButton = new PanelButton(layout.innerX(), layout.footerY(),
                Math.max(RESTORE_BUTTON_MIN_WIDTH, textRenderer.getWidth(presetLabel()) + 20), layout.widgetHeight(),
                presetLabel(), button -> resetToPreset(), PanelButton.Kind.SECONDARY);
        presetButton.setTooltip(Tooltip.of(Text.translatable("screen.relativitytick.config.preset.tooltip")));
        addDrawableChild(presetButton);
        refreshPresetButton();
        refreshClearButton();

        PanelButton close = new PanelButton(layout.controlRight() - FOOTER_BUTTON_WIDTH, layout.footerY(),
                FOOTER_BUTTON_WIDTH, layout.widgetHeight(),
                Text.translatable("screen.relativitytick.close"), button -> close(), PanelButton.Kind.SECONDARY);
        close.setTooltip(Tooltip.of(Text.translatable("screen.relativitytick.close.tooltip")));
        addDrawableChild(close);
    }

    private record Layout(int x, int y, int width, int height, int rowTop, int rowHeight, int rowGap,
                         int dividerY, int footerY, int labelX, int controlX, int controlRight,
                         int fieldX, int stepMinusX, int stepFieldX, int stepPlusX,
                         int widgetHeight, int ruleGap) {
        static final Layout EMPTY = new Layout(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        int innerX() { return x + RegionPanelDraw.PADDING; }
        int innerRight() { return x + width - RegionPanelDraw.PADDING; }
        int innerWidth() { return width - RegionPanelDraw.PADDING * 2; }

        /** 分隔线上面一段有 1 + 六个色位共七行，第 7 行起要往下让出分隔线的高度。 */
        int rowY(int index) {
            int band = index >= STEP_ROW ? (rowGap * 2 + 1) : 0;
            return rowTop + index * (rowHeight + rowGap) + band;
        }

        int labelY(int index) {
            return rowY(index) + (rowHeight - 8) / 2;
        }

        int swatchX() {
            return fieldX - SWATCH_GAP - SWATCH_SIZE;
        }

        int swatchY(int index) {
            return rowY(index) + (rowHeight - SWATCH_SIZE) / 2;
        }
    }

    /** 行内控件在该行垂直居中。 */
    private int widgetY(int row) {
        return layout.rowY(row) + (layout.rowHeight() - layout.widgetHeight()) / 2;
    }

    /** 一档行距方案：窗口装不下时整档往紧里换，而不是把页脚挤出屏幕。 */
    private record Profile(int rowHeight, int rowGap, int topGap, int footerGap, int ruleGap, int widgetHeight) {}

    private static final Profile[] PROFILES = {
            new Profile(ROW_HEIGHT, ROW_GAP, 12, 12, 8, WIDGET_HEIGHT),
            new Profile(DENSE_ROW_HEIGHT, DENSE_ROW_GAP, 8, 8, 8, WIDGET_HEIGHT),
            new Profile(COMPACT_ROW_HEIGHT, COMPACT_ROW_GAP, 6, 6, 4, COMPACT_WIDGET_HEIGHT),
    };

    private Layout computeLayout() {
        int panelWidth = panelWidth();
        int innerX = panelX() + RegionPanelDraw.PADDING;
        int innerRight = panelX() + panelWidth - RegionPanelDraw.PADDING;
        int controlX = innerX + labelWidth() + LABEL_GAP;

        //九行 + 标题 + 页脚，从宽到紧挑第一档装得下的。矮窗口里宁可行矮一点，也不要页脚被裁到屏幕外
        Profile profile = PROFILES[PROFILES.length - 1];
        for (Profile candidate : PROFILES) {
            if (panelHeight(candidate) <= height - 8) {
                profile = candidate;
                break;
            }
        }
        int rowHeight = profile.rowHeight();
        int rowGap = profile.rowGap();
        int panelHeight = panelHeight(profile);

        int rowTop = RegionPanelDraw.HEADER_HEIGHT + profile.topGap();
        int advance = rowHeight + rowGap;
        int band = rowGap * 2 + 1;
        //即使最紧一档还装不下，也不让面板顶出屏幕上边
        int panelY = Math.max(4, Math.min((height - panelHeight) / 2, height - 8 - panelHeight));
        //分隔线画在"最后一个色位行"与"步进行"之间那段空档的中间
        int dividerY = rowTop + STEP_ROW * advance + rowGap;
        int footerY = rowTop + (ROW_COUNT - 1) * advance + band + rowHeight + profile.footerGap();

        //步长组（- / 数字框 / +）整体右对齐，右边缘与色位输入框、关闭键连成一条线
        int stepPlusX = innerRight - STEP_BUTTON_WIDTH;
        int stepFieldX = stepPlusX - STEP_GAP - FIELD_WIDTH;
        int stepMinusX = stepFieldX - STEP_GAP - STEP_BUTTON_WIDTH;

        return new Layout(panelX(), panelY, panelWidth, panelHeight, panelY + rowTop, rowHeight, rowGap,
                panelY + dividerY, panelY + footerY, innerX, controlX, innerRight,
                innerRight - FIELD_WIDTH, stepMinusX, stepFieldX, stepPlusX,
                profile.widgetHeight(), profile.ruleGap());
    }

    private static int panelHeight(Profile profile) {
        int rowTop = RegionPanelDraw.HEADER_HEIGHT + profile.topGap();
        int band = profile.rowGap() * 2 + 1;
        //最后一行之上还要让出分隔线，页脚再往下留一段
        int footerY = rowTop + (ROW_COUNT - 1) * (profile.rowHeight() + profile.rowGap())
                + band + profile.rowHeight() + profile.footerGap();
        return footerY + profile.widgetHeight() + RegionPanelDraw.PADDING;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        RegionPanelDraw.card(context, layout.x(), layout.y(), layout.width(), layout.height());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        RegionPanelDraw.header(context, textRenderer, layout.x(), layout.y(), layout.width(), title, null);

        drawLabel(context, "screen.relativitytick.config.theme", THEME_ROW);
        drawThemeChips(context);
        for (RegionPanelStyle.Slot slot : RegionPanelStyle.Slot.values()) {
            drawLabel(context, slot.translationKey(), COLOR_ROW + slot.ordinal());
            //色块读的是"当前生效"的值：输入框里可能是刚敲进去、还没满 6 位的半截文本
            RegionPanelDraw.swatch(context, layout.swatchX(), layout.swatchY(COLOR_ROW + slot.ordinal()),
                    SWATCH_SIZE, slot.valueOf(RegionPanelStyle.current()), false);
        }
        drawLabel(context, "screen.relativitytick.config.step", STEP_ROW);
        drawLabel(context, "screen.relativitytick.config.key", KEY_ROW);

        RegionPanelDraw.divider(context, layout.innerX(), layout.dividerY(), layout.innerWidth());
        RegionPanelDraw.footerRule(context, layout.x(), layout.footerY() - layout.ruleGap(), layout.width());
    }

    // 主题色块：当前生效的那块描一圈强调色，旁边写出它的名字
    private void drawThemeChips(DrawContext context) {
        RegionPanelStyle style = RegionPanelStyle.current();
        String activeId = RelativityTickClientConfig.getPanelTheme();
        for (int i = 0; i < themeChips.length; i++) {
            PanelButton chip = themeChips[i];
            if (chip == null) continue;
            RegionPanelStyle.Theme theme = themes[i];
            boolean selected = theme.id().equals(activeId);
            int x = chip.getX() + (chip.getWidth() - THEME_SWATCH_SIZE) / 2;
            int y = chip.getY() + (chip.getHeight() - THEME_SWATCH_SIZE) / 2;
            RegionPanelDraw.swatch(context, x, y, THEME_SWATCH_SIZE, theme.swatchColor(), selected);
            if (selected) {
                RegionPanelDraw.outline(context, chip.getX(), chip.getY(), chip.getWidth(), chip.getHeight(),
                        RegionPanelDraw.CORNER_SMALL, style.accent());
            }
        }
        PanelButton last = themeChips.length == 0 ? null : themeChips[themeChips.length - 1];
        int textX = last == null ? layout.controlX() : last.getX() + last.getWidth() + 8;
        context.drawTextWithShadow(textRenderer,
                Text.translatable("screen.relativitytick.config.theme.current", currentThemeName()),
                textX, layout.labelY(THEME_ROW), style.textMuted());
    }

    private Text currentThemeName() {
        RegionPanelStyle.Theme theme = RegionPanelStyle.preset(RelativityTickClientConfig.getPanelTheme());
        return theme != null
                ? Text.translatable(theme.translationKey())
                : Text.translatable("screen.relativitytick.config.theme.custom");
    }

    private void drawLabel(DrawContext context, String key, int row) {
        context.drawTextWithShadow(textRenderer, Text.translatable(key), layout.labelX(), layout.labelY(row),
                RegionPanelStyle.current().textMuted());
    }

    /** 标签列宽按当前语言实测：中文「步进快捷键」和英文 "Step hotkey" 都不改代码。 */
    private int labelWidth() {
        int width = textRenderer.getWidth(Text.translatable("screen.relativitytick.config.theme"));
        for (RegionPanelStyle.Slot slot : RegionPanelStyle.Slot.values()) {
            width = Math.max(width, textRenderer.getWidth(Text.translatable(slot.translationKey())));
        }
        return Math.max(width, Math.max(textRenderer.getWidth(Text.translatable("screen.relativitytick.config.step")),
                textRenderer.getWidth(Text.translatable("screen.relativitytick.config.key"))));
    }

    // 「回到默认配色」：把六个色位一起覆盖回预设值，已是默认配色时置灰
    private Text presetLabel() {
        return Text.translatable("screen.relativitytick.config.preset.restore");
    }

    private void refreshPresetButton() {
        if (presetButton == null) return;
        presetButton.setMessage(presetLabel());
        presetButton.active = !RelativityTickClientConfig.DEFAULT_PANEL_THEME
                .equals(RelativityTickClientConfig.getPanelTheme());
    }

    private void resetToPreset() {
        selectTheme(themes[0]);
    }

    // 换一套预设：setPanelTheme 会同步六个色位并立刻生效，之后刷新框与按钮显示
    private void selectTheme(RegionPanelStyle.Theme theme) {
        try {
            RelativityTickClientConfig.setPanelTheme(theme.id());
        } catch (IOException | RuntimeException ignored) {
            //写盘失败只影响下次启动：界面已经换成新配色，不该因为这个弹回旧主题
        }
        //预设覆盖了六个色位，输入框得跟着回到预设值，不然框里还留着上一套的颜色
        refreshColorFields();
        refreshPresetButton();
    }

    private void submitColor(RegionPanelStyle.Slot slot, int rgb) {
        try {
            RelativityTickClientConfig.setPanelColor(slot, rgb);
        } catch (IOException ignored) {
        }
        //配色一变就是自定义了，"回到默认配色"这一刻才有东西可还原，按钮跟着亮起来
        refreshPresetButton();
    }

    private void refreshColorFields() {
        for (Map.Entry<RegionPanelStyle.Slot, TextFieldWidget> entry : colorFields.entrySet()) {
            setFieldText(entry.getValue(), RelativityTickClientConfig.getPanelColorHex(entry.getKey()));
        }
    }

    private void setFieldText(TextFieldWidget field, String text) {
        if (field.getText().equals(text)) return;
        updatingFields = true;
        field.setText(text);
        updatingFields = false;
    }

    /** 提交（或回退）当前聚焦的色位输入框：填的不是 6 位十六进制就退回它现在的值，不发任何东西。 */
    private RegionPanelStyle.Slot focusedSlot(TextFieldWidget field) {
        for (Map.Entry<RegionPanelStyle.Slot, TextFieldWidget> entry : colorFields.entrySet()) {
            if (entry.getValue() == field) return entry.getKey();
        }
        return null;
    }

    private void commitColorField(TextFieldWidget field) {
        RegionPanelStyle.Slot slot = focusedSlot(field);
        if (slot == null) return;
        if (RegionPanelColor.parse(field.getText()) != null) return;
        refreshOne(slot);
    }

    private void refreshOne(RegionPanelStyle.Slot slot) {
        TextFieldWidget field = colorFields.get(slot);
        if (field != null) setFieldText(field, RelativityTickClientConfig.getPanelColorHex(slot));
    }

    private void submitStepSize(int value) {
        try {
            RelativityTickClientConfig.setStepSize(MathHelper.clamp(value,
                    RelativityTickClientConfig.STEP_SIZE_MIN, RelativityTickClientConfig.STEP_SIZE_MAX));
        } catch (IOException ignored) {
        }
    }

    /** 微调按钮：在生效值上加减 1，越界就停在边界上。 */
    private void nudgeStep(int delta) {
        int value = MathHelper.clamp(RelativityTickClientConfig.getStepSize() + delta,
                RelativityTickClientConfig.STEP_SIZE_MIN, RelativityTickClientConfig.STEP_SIZE_MAX);
        submitStepSize(value);
        setFieldText(stepField, RegionPanelText.integer(value));
    }

    // 回车 / 点走时收尾：空文本或越界值都归位到生效值
    private void commitStepField() {
        Integer parsed = parseStep(stepField.getText());
        int value = parsed == null ? RelativityTickClientConfig.getStepSize()
                : MathHelper.clamp(parsed, RelativityTickClientConfig.STEP_SIZE_MIN,
                        RelativityTickClientConfig.STEP_SIZE_MAX);
        submitStepSize(value);
        setFieldText(stepField, RegionPanelText.integer(value));
    }

    private static Integer parseStep(String text) {
        if (text == null || text.isEmpty()) return null;
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void submitStepKey(InputUtil.Key key) {
        try {
            RelativityTickClientConfig.setStepKey(key == null ? "" : key.getTranslationKey());
        } catch (IOException ignored) {
        }
        RelativityTickClient.applyStepKey();
        refreshClearButton();
    }

    /** 解绑：清空生效值，并让录制按钮立刻显示回「未绑定」（它自己只在录制流程里改文案）。 */
    private void clearStepKey() {
        submitStepKey(null);
        if (keyButton != null) {
            keyButton.setMessage(Text.translatable("screen.relativitytick.config.key.none"));
        }
    }

    /** 没绑快捷键时"清除"无事可做，灰掉——按钮能不能点本身就是当前状态的读数。 */
    private void refreshClearButton() {
        if (clearKeyButton == null) return;
        String key = RelativityTickClientConfig.getStepKey();
        clearKeyButton.active = key != null && !key.isEmpty();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        //捕获态下 ESC 是"取消这次捕获"，不能顺手把整块面板关掉
        if (keyButton != null && keyButton.isWaiting()) {
            return keyButton.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (getFocused() instanceof TextFieldWidget field) {
                if (focusedSlot(field) != null) {
                    commitColorField(field);
                    return true;
                }
                if (field == stepField) {
                    commitStepField();
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        //点走 = 收尾：色位里半截的色号退回原值，步长的空框/越界值归位到生效值
        if (getFocused() instanceof TextFieldWidget field && !field.isMouseOver(mouseX, mouseY)) {
            if (focusedSlot(field) != null) {
                commitColorField(field);
            } else if (field == stepField) {
                commitStepField();
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
