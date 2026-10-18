package com.abelian.client;

import com.abelian.client.clientRegionTick.ClientRegion;
import com.abelian.client.clientRegionTick.ClientRegionManager;
import com.abelian.client.gui.PanelButton;
import com.abelian.client.gui.PanelSliderWidget;
import com.abelian.client.gui.PanelTextField;
import com.abelian.client.gui.RegionPanelDraw;
import com.abelian.client.gui.RegionPanelScreen;
import com.abelian.client.gui.RegionPanelStyle;
import com.abelian.client.gui.RegionPanelText;
import com.abelian.client.gui.ToggleSettingButton;
import com.abelian.network.RegionStatusPayload;
import com.abelian.regionTick.RegionTickManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

// 单个区域的设置：目标速率、耗时上限、区域内三项 tick 开关；无「应用」按钮，改动即时提交
public class RegionConfigScreen extends RegionPanelScreen {
    private static final int STATUS_INTERVAL_TICKS = 4;
    /** 本地提交后等回包确认的拍数上限（20 tick ≈ 1 秒）：过了就不管服务端答不答应，以它为准。 */
    private static final int CONFIRM_HOLD_TICKS = 20;
    private static final int LABEL_GAP = 10;
    private static final int GAP = 8;
    private static final int ROW_HEIGHT = 24;

    private static final double RATE_MIN = 0.1;
    private static final double RATE_MAX = 10000.0;
    private static final double DURATION_MIN = 1.0;
    private static final double DURATION_MAX = 1000.0;

    /** 快捷速率的基准：1 倍就是原版节奏，每秒 20 刻（也是服务端给新区域的默认值）。 */
    private static final double RATE_PRESET_UNIT = 20.0;
    private static final double[] RATE_PRESETS = {0.5, 1.0, 2.0, 10.0};
    private static final String[] RATE_PRESET_KEYS = {
            "screen.relativitytick.config.rate.preset.half",
            "screen.relativitytick.config.rate.preset.normal",
            "screen.relativitytick.config.rate.preset.double",
            "screen.relativitytick.config.rate.preset.fast"
    };

    // 快捷速率做成 16 高的小胶囊，摆在状态那一行的右侧
    private static final int PRESET_CHIP_HEIGHT = 16;
    private static final int PRESET_CHIP_GAP = 6;

    private final String regionId;
    private final Screen parent;
    /** 每次进面板取一次当前主题；配色在面板设置里改，回到这里会重新取。 */
    private RegionPanelStyle style = RegionPanelStyle.current();

    private Layout layout = Layout.EMPTY;
    private PanelSliderWidget rateSlider;
    private TextFieldWidget rateField;
    private PanelSliderWidget durationSlider;
    private TextFieldWidget durationField;
    private ToggleSettingButton hopperToggle;
    private ToggleSettingButton entityToggle;
    private ToggleSettingButton observerToggle;
    private final PanelButton[] ratePresetButtons = new PanelButton[RATE_PRESETS.length];

    private int statusCountdown;
    private boolean updatingFields;
    private final Pending pendingRate = new Pending();
    private final Pending pendingDuration = new Pending();

    public RegionConfigScreen(String regionId, Screen parent) {
        super(Text.translatable("screen.relativitytick.config"));
        this.regionId = regionId;
        this.parent = parent;
    }

    @Override
    protected void init() {
        layout = computeLayout();
        style = RegionPanelStyle.current();
        buildWidgets();
        statusCountdown = STATUS_INTERVAL_TICKS;
        ClientRegionManager.requestLiveStatus(regionId);
        refreshFromServer(true);
    }

    @Override
    public void tick() {
        if (--statusCountdown <= 0) {
            statusCountdown = STATUS_INTERVAL_TICKS;
            ClientRegionManager.requestLiveStatus(regionId);
        }
        refreshFromServer(false);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private void buildWidgets() {
        ClientRegion region = ClientRegionManager.getRegion(regionId);
        RegionStatusPayload status = liveStatus();
        double rate = region == null ? RATE_PRESET_UNIT : region.getRate();
        double limit = status == null ? DURATION_MIN : status.tickWindowLimitMs();

        // 初始文本交给 refreshFromServer(true) 填，避免留下服务端没答应过的值
        rateField = numberField(layout.fieldX(), layout.rateRowY(), Text.translatable("screen.relativitytick.config.rate"));
        rateField.setTooltip(Tooltip.of(Text.translatable("screen.relativitytick.config.rate.field")));
        rateField.setChangedListener(text -> {
            Double parsed = parse(text);
            if (!updatingFields && parsed != null) submitRate(parsed);
        });
        rateSlider = addDrawableChild(new PanelSliderWidget(layout.sliderX(), layout.rateRowY(), layout.sliderWidth(),
                RATE_MIN, RATE_MAX, rate, RegionConfigScreen::quantizeRate,
                value -> Text.translatable("screen.relativitytick.config.rate.value", RegionPanelText.number(value)),
                value -> submitRate(value),
                value -> preview(rateField, value)));

        durationField = numberField(layout.fieldX(), layout.durationRowY(), Text.translatable("screen.relativitytick.config.limit"));
        durationField.setTooltip(Tooltip.of(Text.translatable("screen.relativitytick.config.limit.field")));
        durationField.setChangedListener(text -> {
            Double parsed = parse(text);
            if (!updatingFields && parsed != null) submitDuration(parsed);
        });
        durationSlider = addDrawableChild(new PanelSliderWidget(layout.sliderX(), layout.durationRowY(), layout.sliderWidth(),
                DURATION_MIN, DURATION_MAX, limit, RegionConfigScreen::quantizeDuration,
                value -> Text.translatable("screen.relativitytick.config.limit.value", RegionPanelText.number(value)),
                value -> submitDuration(value),
                value -> preview(durationField, value)));

        // 快捷速率：点击走滑块松手那条提交路径；从右往左摆，最后一个贴输入框列右缘
        int chipRight = layout.innerRight();
        for (int i = RATE_PRESETS.length - 1; i >= 0; i--) {
            Text label = Text.translatable(RATE_PRESET_KEYS[i]);
            int chipWidth = Math.max(24, textRenderer.getWidth(label) + 12);
            int chipX = chipRight - chipWidth;
            double value = RATE_PRESET_UNIT * RATE_PRESETS[i];
            PanelButton chip = new PanelButton(chipX, layout.statusY() - 3, chipWidth, PRESET_CHIP_HEIGHT, label,
                    Text.translatable("screen.relativitytick.config.rate.preset.narration", label,
                            RegionPanelText.integer(Math.round(value))),
                    button -> submitRate(value), PanelButton.Kind.SECONDARY);
            chip.setTooltip(Tooltip.of(Text.translatable("screen.relativitytick.config.rate.preset.tooltip",
                    label, RegionPanelText.number(value))));
            ratePresetButtons[i] = addDrawableChild(chip);
            chipRight = chipX - PRESET_CHIP_GAP;
        }

        int toggleY = layout.toggleY();
        hopperToggle = addDrawableChild(settingToggle(toggleY, "screen.relativitytick.setting.hopper", "hopperTick"));
        entityToggle = addDrawableChild(settingToggle(toggleY + layout.toggleRow(),
                "screen.relativitytick.setting.entity", "entityTick"));
        observerToggle = addDrawableChild(settingToggle(toggleY + layout.toggleRow() * 2,
                "screen.relativitytick.setting.observer", "observerScheduledTick"));

        PanelButton close = new PanelButton(panelX() + panelWidth() - RegionPanelDraw.PADDING - FOOTER_BUTTON_WIDTH,
                layout.footerY(), FOOTER_BUTTON_WIDTH, WIDGET_HEIGHT,
                Text.translatable("screen.relativitytick.close"), button -> close(), PanelButton.Kind.SECONDARY);
        close.setTooltip(Tooltip.of(Text.translatable("screen.relativitytick.close.tooltip")));
        addDrawableChild(close);
    }

    private TextFieldWidget numberField(int x, int y, Text label) {
        PanelTextField field = addDrawableChild(new PanelTextField(textRenderer, x, y, FIELD_WIDTH,
                WIDGET_HEIGHT, label));
        field.setMaxLength(12);
        //只放数字和小数点进来：非法字符根本进不了框，就不用猜玩家想输什么
        field.setTextPredicate(text -> text.matches("[0-9]*\\.?[0-9]*"));
        return field;
    }

    private ToggleSettingButton settingToggle(int y, String labelKey, String setting) {
        return new ToggleSettingButton(layout.innerX(), y, layout.innerWidth(), layout.toggleHeight(),
                Text.translatable(labelKey), true,
                Text.translatable("screen.relativitytick.setting.narration",
                        Text.translatable(labelKey), Text.translatable("screen.relativitytick.enabled")),
                Text.translatable("screen.relativitytick.setting.keep", Text.translatable(labelKey)),
                Text.translatable("screen.relativitytick.setting.skip", Text.translatable(labelKey)),
                button -> toggleSetting((ToggleSettingButton) button, setting));
    }

    // 用服务端回包刷新界面；force 为 true 时连正在编辑的输入框也一起覆盖
    private void refreshFromServer(boolean force) {
        ClientRegion region = ClientRegionManager.getRegion(regionId);
        RegionStatusPayload status = liveStatus();
        boolean exists = region != null || status != null;
        boolean hasLimit = status != null;

        rateSlider.active = exists;
        rateField.setEditable(exists);
        durationSlider.active = hasLimit;
        durationField.setEditable(hasLimit);
        hopperToggle.active = exists;
        entityToggle.active = exists;
        observerToggle.active = exists;
        //区域都没了的话，快捷速率点下去也只会发一条必然被拒的命令，一起灰掉
        refreshPresetButtons(exists);
        if (!exists) return;

        //settle 每 tick 都要走一次（等超时靠它递减），所以放在条件外面
        if (hasLimit) {
            double limit = status.tickWindowLimitMs();
            boolean trusted = pendingDuration.settle(limit);
            if (force || (trusted && !durationField.isFocused() && !durationSlider.isDragging())) {
                durationSlider.setSliderValue(limit);
                setFieldText(durationField, RegionPanelText.number(limit));
            }
        } else if (force || !durationField.isFocused()) {
            //没有快照就不知道服务端真实上限，宁可留空也不显示一个假数字
            setFieldText(durationField, "");
        }

        double rate = status != null ? status.rate() : region.getRate();
        boolean trusted = pendingRate.settle(rate);
        if (force || (trusted && !rateField.isFocused() && !rateSlider.isDragging())) {
            rateSlider.setSliderValue(rate);
            setFieldText(rateField, RegionPanelText.number(rate));
        }

        hopperToggle.setEnabledState(!(status != null ? status.disableHopperTick() : region.isDisableHopperTick()));
        entityToggle.setEnabledState(!(status != null ? status.disableEntityTick() : region.isDisableEntityTick()));
        observerToggle.setEnabledState(!(status != null ? status.disableObserverTick() : region.isDisableObserverTick()));
    }

    private void refreshPresetButtons(boolean active) {
        for (PanelButton chip : ratePresetButtons) {
            if (chip != null) chip.active = active;
        }
    }

    private void setFieldText(TextFieldWidget field, String text) {
        if (field.getText().equals(text)) return;
        updatingFields = true;
        field.setText(text);
        updatingFields = false;
    }

    /** 拖动中的实时回显：只改输入框里的数字，不发命令（命令等松手，见 {@link PanelSliderWidget}）。 */
    private void preview(TextFieldWidget field, double value) {
        if (field.isFocused()) return;
        setFieldText(field, RegionPanelText.number(value));
    }

    private static final class Pending {
        private static final double NONE = Double.NaN;
        private static final double EPSILON = 1e-4;

        private double value = NONE;
        private int hold;

        void arm(double value) {
            this.value = value;
            this.hold = CONFIRM_HOLD_TICKS;
        }

        // 每 tick 调一次
        boolean settle(double serverValue) {
            if (Double.isNaN(this.value)) return true;
            if (Math.abs(serverValue - this.value) < EPSILON) {
                this.value = NONE;
                return true;
            }
            if (--this.hold <= 0) {
                this.value = NONE;
                return true;
            }
            return false;
        }
    }

    private record Layout(int x, int y, int width, int height, int statusY, int rateRowY, int durationRowY,
                          int speedY, int loadLabelY, int loadY, int dividerY, int toggleY, int footerY,
                          int sliderX, int sliderWidth, int fieldX, boolean dense,
                          int rowSpacing, int toggleRow, int toggleHeight, boolean compactLoad) {
        static final Layout EMPTY = new Layout(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false,
                ROW_HEIGHT, ROW_HEIGHT, WIDGET_HEIGHT, false);

        int innerX() { return x + RegionPanelDraw.PADDING; }
        int innerRight() { return x + width - RegionPanelDraw.PADDING; }
        int innerWidth() { return width - RegionPanelDraw.PADDING * 2; }
    }

    // 纵向排布的一档：窗口越矮换更紧凑的一档；这些是每段真正占的像素
    private record Profile(int statusBlock, int rowSpacing, int speedGap, int speedRow, int loadLabelGap,
                           int loadHeight, int loadBlockGap, int toggleGap, int toggleRow, int toggleHeight,
                           int footerGap) {
    }

    /** 常规 → 紧凑 → 极简。前两档功能齐全，只在段高与间距上收紧；极简档把「上限占用」收成一行。 */
    private static final Profile[] PROFILES = {
            new Profile(32, 24, 12, 10, 20, RegionPanelDraw.LOAD_HEIGHT, 14, 10, 24, WIDGET_HEIGHT, 12),
            new Profile(28, 22, 8, 9, 10, RegionPanelDraw.LOAD_HEIGHT, 8, 6, 21, 19, 8),
            new Profile(26, 21, 6, 9, 6, RegionPanelDraw.LOAD_HEIGHT_COMPACT, 6, 5, 19, 18, 10)
    };
    /** 页脚占的高度：关闭键 20 + 卡片下沿留白。 */
    private static final int FOOTER_BLOCK = 26;

    /** 一档排布从卡片顶到页脚顶一共多高。 */
    private static int extent(Profile profile) {
        return RegionPanelDraw.HEADER_HEIGHT + profile.statusBlock() + profile.rowSpacing() * 2
                + profile.speedGap() + profile.speedRow() + profile.loadLabelGap()
                + profile.loadHeight() + profile.loadBlockGap() + profile.toggleGap()
                + profile.toggleRow() * 3 + profile.footerGap();
    }

    private Layout computeLayout() {
        int panelWidth = panelWidth();
        int innerWidth = panelWidth - RegionPanelDraw.PADDING * 2;
        int innerX = panelX() + RegionPanelDraw.PADDING;

        int labelWidth = labelWidth();
        int sliderX = innerX + labelWidth + LABEL_GAP;
        int sliderWidth = innerWidth - labelWidth - LABEL_GAP - FIELD_WIDTH - GAP;
        int fieldX = sliderX + sliderWidth + GAP;

        // 先挑一档装得下的，三档都装不下就用最紧凑档，多出的几像素由 panelY 夹住
        Profile profile = PROFILES[PROFILES.length - 1];
        for (Profile candidate : PROFILES) {
            if (extent(candidate) + FOOTER_BLOCK <= height - 8) {
                profile = candidate;
                break;
            }
        }
        int footerY = extent(profile);

        int top = RegionPanelDraw.HEADER_HEIGHT;
        int statusY = top + 8;
        int rateRowY = top + profile.statusBlock();
        int durationRowY = rateRowY + profile.rowSpacing();
        int speedY = durationRowY + profile.rowSpacing() + profile.speedGap();
        int loadLabelY = speedY + profile.speedRow() + profile.loadLabelGap();
        int loadY = loadLabelY + profile.loadHeight();
        int dividerY = loadY + profile.loadBlockGap();
        int toggleY = dividerY + profile.toggleGap();
        int panelHeight = footerY + FOOTER_BLOCK;

        int panelY = Math.max(4, Math.min((height - panelHeight) / 2, height - 8 - panelHeight));
        return new Layout(panelX(), panelY, panelWidth, panelHeight,
                panelY + statusY, panelY + rateRowY, panelY + durationRowY,
                panelY + speedY, panelY + loadLabelY, panelY + loadY,
                panelY + dividerY, panelY + toggleY, panelY + footerY,
                sliderX, sliderWidth, fieldX, profile != PROFILES[0],
                profile.rowSpacing(), profile.toggleRow(), profile.toggleHeight(),
                profile.loadHeight() < RegionPanelDraw.LOAD_HEIGHT);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        RegionPanelDraw.card(context, layout.x(), layout.y(), layout.width(), layout.height());
    }

    // 滑块那一行底下垫一块内嵌玻璃面，鼠标落在行内时整块一起亮；必须画在控件之前
    private void drawRowBackdrop(DrawContext context, int rowY, int mouseX, int mouseY) {
        int x = layout.innerX() - 6;
        int y = rowY - 3;
        int w = layout.innerWidth() + 12;
        int h = layout.rowSpacing();
        boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        RegionPanelDraw.roundRect(context, x, y, w, h, RegionPanelDraw.CORNER_SMALL,
                hovered ? style.rowHover() : style.rowAlt());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        drawRowBackdrop(context, layout.rateRowY(), mouseX, mouseY);
        drawRowBackdrop(context, layout.durationRowY(), mouseX, mouseY);
        super.render(context, mouseX, mouseY, delta);
        RegionPanelDraw.header(context, textRenderer, layout.x(), layout.y(), layout.width(),
                title, Text.literal(textRenderer.trimToWidth(regionId, layout.innerWidth() / 2)));

        RegionStatusPayload status = liveStatus();
        ClientRegion region = ClientRegionManager.getRegion(regionId);
        if (region == null && status == null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.relativitytick.no_region"),
                    layout.x() + layout.width() / 2, layout.statusY() + 4, style.warn());
            return;
        }

        drawStatus(context, status, region, mouseX, mouseY);
        drawRowLabel(context, "screen.relativitytick.config.rate", layout.rateRowY() + 6);
        drawRowLabel(context, "screen.relativitytick.config.limit", layout.durationRowY() + 6);
        drawSpeed(context, status, region);
        drawLoadBar(context, status);
        RegionPanelDraw.footerRule(context, layout.x(), layout.footerY() - 9, layout.width());
        RegionPanelDraw.divider(context, layout.innerX(), layout.dividerY(), layout.innerWidth());
        //当前速率正好落在某一档快捷值上时给它描一圈强调色：一眼看得出"现在在哪一档"
        drawPresetHighlight(context, status != null ? status.rate() : region.getRate());
    }

    private void drawRowLabel(DrawContext context, String key, int y) {
        context.drawTextWithShadow(textRenderer, Text.translatable(key), layout.innerX(), y,
                style.textMuted());
    }

    private int labelWidth() {
        return Math.max(textRenderer.getWidth(Text.translatable("screen.relativitytick.config.rate")),
                textRenderer.getWidth(Text.translatable("screen.relativitytick.config.limit")));
    }

    private void drawStatus(DrawContext context, RegionStatusPayload status, ClientRegion region,
                            int mouseX, int mouseY) {
        RegionTickManager.RegionState state = status != null ? status.state() : region.getRegionState();
        Text stateText = Text.translatable(RegionPanelStyle.stateKey(state));
        int badgeWidth = RegionPanelDraw.badgeWidth(textRenderer, stateText);
        boolean hovered = mouseX >= layout.innerX() - 4 && mouseX <= layout.innerX() + badgeWidth
                && mouseY >= layout.statusY() - 2 && mouseY <= layout.statusY() + 12;
        RegionPanelDraw.badge(context, textRenderer, stateText,
                layout.innerX(), layout.statusY(), style.stateColor(state), hovered);
    }

    private void drawPresetHighlight(DrawContext context, double rate) {
        for (int i = 0; i < ratePresetButtons.length; i++) {
            PanelButton chip = ratePresetButtons[i];
            if (chip == null || !chip.active) continue;
            if (Math.abs(rate - RATE_PRESET_UNIT * RATE_PRESETS[i]) > 1e-4) continue;
            RegionPanelDraw.outline(context, chip.getX(), chip.getY(), chip.getWidth(), chip.getHeight(),
                    RegionPanelDraw.CORNER_SMALL, style.accent());
        }
    }

    /** 实际速率读数：和监测面板同一套写法（标签 + 计数），两处不要各说各的。 */
    private void drawSpeed(DrawContext context, RegionStatusPayload status, ClientRegion region) {
        double rate = status != null ? status.rate() : region.getRate();
        boolean sampled = status != null && status.tpsSampleReady();
        Text value = sampled
                ? Text.literal(RegionPanelText.integer(Math.round(status.tps())))
                : Text.translatable("screen.relativitytick.unknown");
        int color = !sampled ? style.textDim() : status.tps() >= rate * 0.9 ? style.good() : style.warn();
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.relativitytick.metric.tps"),
                layout.innerX(), layout.speedY(), style.textMuted());
        context.drawTextWithShadow(textRenderer, value, layout.innerRight() - textRenderer.getWidth(value),
                layout.speedY(), color);
    }

    // 上限占用这一段交给 RegionPanelDraw#load，与监测面板共用
    private void drawLoadBar(DrawContext context, RegionStatusPayload status) {
        double used = status != null ? status.tickWindowMs() : 0.0;
        double limit = status != null ? status.tickWindowLimitMs() : 0.0;
        RegionPanelDraw.load(context, textRenderer, layout.innerX(),
                //条右界对齐上面那两行输入框的右边缘，读数行才跟条一样宽
                Math.max(layout.innerX() + 32, layout.fieldX() + FIELD_WIDTH),
                layout.loadLabelY(), used, limit, status != null && limit > 0.0, layout.compactLoad());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (getFocused() == rateField || getFocused() == durationField) {
                submitFocusedField();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        TextFieldWidget focused = getFocused() == rateField ? rateField
                : getFocused() == durationField ? durationField : null;
        if (focused != null && !focused.isMouseOver(mouseX, mouseY)) {
            submitFocusedField();
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void submitFocusedField() {
        if (getFocused() == rateField) {
            Double parsed = parse(rateField.getText());
            if (parsed == null) restore(rateField, rateSlider.getSliderValue());
            else submitRate(parsed);
        } else if (getFocused() == durationField) {
            Double parsed = parse(durationField.getText());
            if (parsed == null) restore(durationField, durationSlider.getSliderValue());
            else submitDuration(parsed);
        }
    }

    private void restore(TextFieldWidget field, double value) {
        if (field.getText().isEmpty()) return;
        setFieldText(field, RegionPanelText.number(value));
    }

    private void submitRate(double value) {
        double clamped = MathHelper.clamp(value, RATE_MIN, RATE_MAX);
        rateSlider.setSliderValue(clamped);
        setFieldText(rateField, RegionPanelText.number(clamped));
        pendingRate.arm(clamped);
        sendCommand("regionTick rate " + quote(regionId) + " " + RegionPanelText.command(clamped));
    }

    private void submitDuration(double value) {
        double clamped = MathHelper.clamp(value, DURATION_MIN, DURATION_MAX);
        durationSlider.setSliderValue(clamped);
        setFieldText(durationField, RegionPanelText.number(clamped));
        pendingDuration.arm(clamped);
        sendCommand("regionManager parameter tickDurationLimit " + quote(regionId) + " " + RegionPanelText.command(clamped));
    }

    private void toggleSetting(ToggleSettingButton button, String setting) {
        boolean enabled = !button.isEnabledState();
        button.setEnabledState(enabled);
        sendCommand("regionManager setting " + setting + " " + quote(regionId) + " " + (enabled ? "enable" : "disable"));
    }

    private static Double parse(String text) {
        try {
            double value = Double.parseDouble(text.trim());
            return Double.isFinite(value) ? value : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private RegionStatusPayload liveStatus() {
        return ClientRegionManager.getLiveStatus(regionId);
    }

    private static double quantizeRate(double value) {
        if (value >= 100) return Math.round(value);
        if (value >= 10) return Math.round(value * 10) / 10.0;
        if (value >= 1) return Math.round(value * 100) / 100.0;
        return Math.round(value * 1000) / 1000.0;
    }

    private static double quantizeDuration(double value) {
        if (value >= 100) return Math.round(value);
        return Math.round(value * 10) / 10.0;
    }
}
