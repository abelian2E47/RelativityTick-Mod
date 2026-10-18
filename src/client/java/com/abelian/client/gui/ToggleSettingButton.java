package com.abelian.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

// 一行式开关：左边设置项名字，右边勾选框与「启用/禁用」，状态变化带过渡
public class ToggleSettingButton extends ButtonWidget {
    private final Text label;
    private final Text enabledTooltip;
    private final Text disabledTooltip;
    private boolean enabledState;
    /** 勾选状态的动画进度，用来插值颜色与勾的显隐。 */
    private float stateProgress;
    private float hoverProgress;

    public ToggleSettingButton(int x, int y, int width, int height, Text label, boolean enabledState,
                               Text narrationText, Text enabledTooltip, Text disabledTooltip, PressAction onPress) {
        super(x, y, width, height, narrationText, onPress, textSupplier -> textSupplier.get());
        this.label = label;
        this.enabledState = enabledState;
        this.enabledTooltip = enabledTooltip;
        this.disabledTooltip = disabledTooltip;
        this.stateProgress = enabledState ? 1f : 0f;
        applyTooltip();
    }

    public void setEnabledState(boolean enabledState) {
        if (this.enabledState == enabledState) return;
        this.enabledState = enabledState;
        applyTooltip();
    }

    public boolean isEnabledState() {
        return enabledState;
    }

    private void applyTooltip() {
        setTooltip(Tooltip.of(enabledState ? enabledTooltip : disabledTooltip));
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        RegionPanelStyle style = RegionPanelStyle.current();
        float seconds = delta / 20f;
        boolean highlighted = this.active && (isHovered() || isFocused());
        this.hoverProgress = PanelAnim.approach(this.hoverProgress, highlighted ? 1f : 0f, seconds, 14f);
        this.stateProgress = PanelAnim.approach(this.stateProgress, this.enabledState ? 1f : 0f, seconds, 12f);
        //服务端还没回包时先按旧值画，回包后这 0.1 秒的过渡就是"已生效"的反馈
        if (this.stateProgress != (this.enabledState ? 1f : 0f) && (this.stateProgress < 0.002f || this.stateProgress > 0.998f)) {
            this.stateProgress = this.enabledState ? 1f : 0f;
        }

        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        int fill = !this.active ? style.toggleDisabled()
                : RegionPanelDraw.blend(style.toggle(), style.toggleHover(), this.hoverProgress);
        int border = this.active
                ? RegionPanelDraw.blend(style.iconButtonBorder(), style.accent(), this.hoverProgress)
                : style.iconButtonBorder();
        RegionPanelDraw.roundRect(context, x, y, w, h, 4, fill);
        RegionPanelDraw.outline(context, x, y, w, h, 4, border);

        int boxSize = 12;
        int boxX = x + 8;
        int boxY = y + (h - boxSize) / 2;
        int onColor = this.active ? style.toggleOn() : style.disabled();
        int boxColor = RegionPanelDraw.blend(style.textDim(), onColor, this.stateProgress);
        RegionPanelDraw.roundRect(context, boxX, boxY, boxSize, boxSize, 3, style.toggleBox());
        RegionPanelDraw.outline(context, boxX, boxY, boxSize, boxSize, 3, boxColor);
        if (this.stateProgress > 0.05f) {
            //勾用一个 8x6 的字符画，超出进度的那部分按列裁掉，看起来像"画"上去的
            int visible = Math.max(1, Math.round(RegionPanelIcons.CHECK.width() * this.stateProgress));
            int alpha = (int) (255f * PanelAnim.clamp01(this.stateProgress * 1.4f));
            RegionPanelIcons.drawClipped(context, RegionPanelIcons.CHECK, boxX + 2, boxY + 3,
                    RegionPanelColor.alpha(alpha, onColor), visible);
        }

        Text stateText = Text.translatable(enabledState
                ? "screen.relativitytick.enabled" : "screen.relativitytick.disabled");
        int stateColor = !this.active ? style.textDim()
                : RegionPanelDraw.blend(style.textDim(), onColor, this.stateProgress);
        int labelColor = this.active ? style.text() : style.textDim();
        MinecraftClient client = MinecraftClient.getInstance();
        int textY = y + (h - 8) / 2;
        context.drawTextWithShadow(client.textRenderer, label, boxX + boxSize + 8, textY, labelColor);
        context.drawTextWithShadow(client.textRenderer, stateText,
                x + w - 8 - client.textRenderer.getWidth(stateText), textY, stateColor);
    }
}
