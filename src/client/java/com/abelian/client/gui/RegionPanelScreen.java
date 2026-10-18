package com.abelian.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

// 四块面板的公共基类：面板尺寸、发命令、以及整屏背景（不调 super.renderBackground）
public abstract class RegionPanelScreen extends Screen {
    /** 面板宽度上限；窗口更窄时收到 {@link #PANEL_MIN_WIDTH}，再窄就贴着窗口。 */
    protected static final int PANEL_WIDTH = 460;
    protected static final int PANEL_MIN_WIDTH = 400;
    protected static final int FOOTER_BUTTON_WIDTH = 70;
    /** 控件标准高度。 */
    protected static final int WIDGET_HEIGHT = 20;
    /** 数值输入框宽度：够放 "10000.0" 这种最长的一档。 */
    protected static final int FIELD_WIDTH = 56;

    protected RegionPanelScreen(Text title) {
        super(title);
    }

    /** 面板是"边看边调"的工具，不该把单人游戏暂停掉。 */
    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // 先糊掉世界（原版 applyBlur），必须在落笔之前，不在世界里时跳过
        if (this.client != null && this.client.world != null) {
            this.applyBlur();
        }
        RegionPanelDraw.backdrop(context, this.width, this.height);
    }

    /** 发一条聊天命令。单人/未进世界（player 为 null）时静默丢弃，不抛异常。 */
    protected static void sendCommand(String command) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.player.networkHandler == null) return;
        client.player.networkHandler.sendChatCommand(command);
    }

    // 把值包成命令参数里的字符串字面量，转义引号与反斜杠
    protected static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /** 翻译键 → 按键。空串或无法识别时返回 null（表示"未绑定"）。 */
    protected static InputUtil.Key parseKey(String translationKey) {
        if (translationKey == null || translationKey.isEmpty()) return null;
        try {
            return InputUtil.fromTranslationKey(translationKey);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    protected int panelWidth() {
        return MathHelper.clamp(width - 24, PANEL_MIN_WIDTH, PANEL_WIDTH);
    }

    protected int panelX() {
        return (width - panelWidth()) / 2;
    }
}
