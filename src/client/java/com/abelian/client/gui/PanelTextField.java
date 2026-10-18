package com.abelian.client.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

// 卡片里的输入框：关掉原版黑底背景，自绘圆角外框，内层文字位置保持不变
public class PanelTextField extends TextFieldWidget {
    private static final int INSET = 4;
    private static final int LINE_HEIGHT = 8;
    private final int boxX;
    private final int boxY;
    private final int boxWidth;
    private final int boxHeight;
    private boolean editableState = true;

    public PanelTextField(TextRenderer textRenderer, int x, int y, int width, int height, Text narration) {
        super(textRenderer, x + INSET, y + (height - LINE_HEIGHT) / 2, width - INSET * 2, LINE_HEIGHT, narration);
        this.boxX = x;
        this.boxY = y;
        this.boxWidth = width;
        this.boxHeight = height;
        setDrawsBackground(false);
    }

    @Override
    public void setEditable(boolean editable) {
        super.setEditable(editable);
        this.editableState = editable;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.visible
                && mouseX >= boxX && mouseX < boxX + boxWidth
                && mouseY >= boxY && mouseY < boxY + boxHeight;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        RegionPanelStyle style = RegionPanelStyle.current();
        setEditableColor(style.text());
        setUneditableColor(style.textDim());
        boolean focused = this.isFocused() && this.editableState;
        int fill = !this.editableState ? style.toggleDisabled() : focused ? style.keyBgFocus() : style.keyBg();
        RegionPanelDraw.roundRect(context, boxX, boxY, boxWidth, boxHeight, 4, fill);
        //聚焦时先描一圈更宽的同色轮廓：光标在里面闪，外框得跟着亮一下才看得出焦点在哪
        if (focused) {
            RegionPanelDraw.outline(context, boxX - 1, boxY - 1, boxWidth + 2, boxHeight + 2, 5, style.accentSoft());
        }
        super.renderWidget(context, mouseX, mouseY, delta);
        RegionPanelDraw.outline(context, boxX, boxY, boxWidth, boxHeight, 4,
                !this.editableState ? style.iconButtonBorder() : focused ? style.accent() : style.keyBorder());
    }
}
