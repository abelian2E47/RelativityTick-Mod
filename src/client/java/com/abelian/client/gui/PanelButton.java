package com.abelian.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

// 面板上的文字按钮：自绘圆角 + 悬停渐变，悬停过渡走一阶滤波
public class PanelButton extends ButtonWidget {
    public enum Kind {
        /** 次要动作：默认样子。 */
        SECONDARY,
        /** 主要动作：用强调色实心底，视线应当先落在这里。 */
        PRIMARY
    }

    private final Kind kind;
    /** 0 → 1 的悬停进度，用来插值填充色与描边色。 */
    private float hoverProgress;

    public PanelButton(int x, int y, int width, int height, Text message, PressAction onPress, Kind kind) {
        this(x, y, width, height, message, message, onPress, kind);
    }

    public PanelButton(int x, int y, int width, int height, Text message, Text narration,
                       PressAction onPress, Kind kind) {
        super(x, y, width, height, message, onPress, textSupplier -> narration.copy());
        this.kind = kind;
    }

    protected Kind kind() {
        return kind;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        RegionPanelStyle style = RegionPanelStyle.current();
        boolean highlighted = this.active && (this.isHovered() || this.isFocused());
        //delta 是以 tick 计的帧间隔，换算成秒再喂给滤波，拖影快慢才不会跟着帧率跑
        this.hoverProgress = PanelAnim.approach(this.hoverProgress, highlighted ? 1f : 0f, delta / 20f, 14f);

        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        boolean primary = this.kind == Kind.PRIMARY;

        int fill = !this.active ? style.toggleDisabled()
                : RegionPanelDraw.blend(primary ? style.buttonPrimary() : style.iconButton(),
                        primary ? style.buttonPrimaryHover() : style.iconButtonHover(), this.hoverProgress);
        int border = !this.active ? style.iconButtonBorder()
                : RegionPanelDraw.blend(primary ? style.buttonPrimaryBorder() : style.iconButtonBorder(),
                        style.accent(), this.hoverProgress);

        RegionPanelDraw.roundRect(context, x, y, width, height, 4, fill);
        RegionPanelDraw.outline(context, x, y, width, height, 4, border);
        //顶部一条几乎看不见的高光，让扁平的填充看起来有一点厚度
        if (this.active) {
            context.fill(x + 4, y + 1, x + width - 4, y + 2, style.sheen());
        }

        Text message = getMessage();
        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
        int color = !this.active ? style.textDim()
                : primary ? style.textBright() : RegionPanelDraw.blend(style.text(), style.textBright(), this.hoverProgress);
        context.drawTextWithShadow(renderer, message,
                x + (width - renderer.getWidth(message)) / 2, y + (height - 8) / 2, color);
    }
}
