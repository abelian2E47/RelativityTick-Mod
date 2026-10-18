package com.abelian.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

// 快捷键捕获按钮：点一下进入等待按键，再按任意键绑定；等待态静音并让描边脉动
public class KeyCaptureButton extends PanelButton {
    private static final Text WAITING = Text.translatable("screen.relativitytick.config.key.waiting");

    private final Consumer<InputUtil.Key> captureTarget;
    private InputUtil.Key bound;
    private boolean waiting;

    public KeyCaptureButton(int x, int y, int width, int height, InputUtil.Key bound, Consumer<InputUtil.Key> onCapture) {
        super(x, y, width, height, label(bound), KeyCaptureButton::toggleCapture, Kind.SECONDARY);
        this.bound = bound;
        this.captureTarget = onCapture;
        this.setTooltip(Tooltip.of(Text.translatable("screen.relativitytick.config.key.tooltip")));
    }

    private static void toggleCapture(ButtonWidget button) {
        KeyCaptureButton self = (KeyCaptureButton) button;
        self.waiting = !self.waiting;
        self.setMessage(self.waiting ? WAITING : label(self.bound));
    }

    public boolean isWaiting() {
        return waiting;
    }

    private static Text label(InputUtil.Key bound) {
        return bound == null
                ? Text.translatable("screen.relativitytick.config.key.none")
                : bound.getLocalizedText();
    }

    @Override
    protected Kind kind() {
        return waiting ? Kind.PRIMARY : Kind.SECONDARY;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderWidget(context, mouseX, mouseY, delta);
        if (waiting) {
            RegionPanelStyle style = RegionPanelStyle.current();
            //0.75 秒一个来回的呼吸灯：脉动只改描边，不改尺寸，免得按钮在等待时"跳"
            float phase = 0.5f + 0.5f * PanelAnim.pulse(PanelAnim.nowMillis(), 750f);
            int alpha = (int) (140 + 115 * phase);
            RegionPanelDraw.outline(context, getX() - 1, getY() - 1, getWidth() + 2, getHeight() + 2, 5,
                    RegionPanelColor.alpha(alpha, style.accent()));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!waiting) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE -> cancel();
            case GLFW.GLFW_KEY_DELETE, GLFW.GLFW_KEY_BACKSPACE -> capture(null);
            default -> capture(InputUtil.fromKeyCode(keyCode, scanCode));
        }
        return true;
    }

    private void cancel() {
        waiting = false;
        setMessage(label(bound));
    }

    private void capture(InputUtil.Key key) {
        waiting = false;
        bound = key;
        setMessage(label(key));
        if (captureTarget != null) {
            captureTarget.accept(key);
        }
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        if (!waiting) {
            super.playDownSound(soundManager);
        }
    }
}
