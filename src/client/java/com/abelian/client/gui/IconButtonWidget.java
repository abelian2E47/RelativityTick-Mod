package com.abelian.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

// 只放一个图标的方形按钮（列表行里的齿轮、监测）；图标是字符画（见 RegionPanelIcons）
public class IconButtonWidget extends ButtonWidget {
    private final Icon icon;
    private float hoverProgress;

    public enum Icon {
        MONITOR, GEAR
    }

    public IconButtonWidget(int x, int y, int width, int height, Icon icon, Text narration, PressAction onPress) {
        super(x, y, width, height, narration, onPress, textSupplier -> textSupplier.get());
        this.icon = icon;
    }

    @Override
    public void setMessage(Text message) {
        super.setMessage(message);
        setTooltip(Tooltip.of(message));
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        RegionPanelStyle style = RegionPanelStyle.current();
        boolean highlighted = this.active && (isHovered() || isFocused());
        this.hoverProgress = PanelAnim.approach(this.hoverProgress, highlighted ? 1f : 0f, delta / 20f, 14f);

        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        int fill = !this.active ? style.toggleDisabled()
                : RegionPanelDraw.blend(style.iconButton(), style.accentSoft(), this.hoverProgress);
        int border = this.active
                ? RegionPanelDraw.blend(style.iconButtonBorder(), style.accent(), this.hoverProgress)
                : style.iconButtonBorder();
        int color = !this.active ? style.disabled()
                : RegionPanelDraw.blend(style.textMuted(), style.accent(), this.hoverProgress);

        RegionPanelDraw.roundRect(context, x, y, w, h, 4, fill);
        RegionPanelDraw.outline(context, x, y, w, h, 4, border);
        switch (icon) {
            case MONITOR -> RegionPanelIcons.monitor(context,
                    centered(x, w, RegionPanelIcons.monitorWidth()),
                    centered(y, h, RegionPanelIcons.monitorHeight()), color);
            case GEAR -> RegionPanelIcons.gear(context,
                    centered(x, w, RegionPanelIcons.gearWidth()),
                    centered(y, h, RegionPanelIcons.gearHeight()), color);
        }
    }

    private static int centered(int start, int size, int content) {
        return start + (size - content) / 2;
    }
}
