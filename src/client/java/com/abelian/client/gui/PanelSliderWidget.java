package com.abelian.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;

// 对数刻度的数值滑块，用于速率这类跨数量级（0.1~10000）的取值
public class PanelSliderWidget extends SliderWidget {
    private static final int HANDLE_WIDTH = 8;
    private static final int INSET = HANDLE_WIDTH / 2;
    private static final int GRAB_RADIUS = HANDLE_WIDTH + 2;
    private static final double FINE_SCALE = 0.25;
    private static final int HANDLE_GAP = 6;
    private static final int TEXT_MARGIN = 5;
    private static final int TRACK_HEIGHT = 6;

    private final double min;
    private final double max;
    private final DoubleUnaryOperator quantizer;
    private final Function<Double, Text> labelFactory;
    private final DoubleConsumer committer;
    private final DoubleConsumer preview;
    private double lastCommitted;
    private double grabOffset;
    private boolean dragging;
    private Text label = Text.empty();
    /** 手柄的悬停/拖动放大进度。 */
    private float handleProgress;

    public PanelSliderWidget(int x, int y, int width, double min, double max, double value,
                             DoubleUnaryOperator quantizer, Function<Double, Text> labelFactory,
                             DoubleConsumer committer) {
        this(x, y, width, min, max, value, quantizer, labelFactory, committer, null);
    }

    public PanelSliderWidget(int x, int y, int width, double min, double max, double value,
                             DoubleUnaryOperator quantizer, Function<Double, Text> labelFactory,
                             DoubleConsumer committer, DoubleConsumer preview) {
        super(x, y, width, 20, Text.empty(), valueToSlider(min, max, value));
        this.min = min;
        this.max = max;
        this.quantizer = quantizer;
        this.labelFactory = labelFactory;
        this.committer = committer;
        this.preview = preview;
        this.lastCommitted = quantized();
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        label = labelFactory.apply(quantized());
        //message 交给 drawMessage 自己画（要避让手柄），这里必须留空，否则原版会把文字盖在轨道中间
        setMessage(Text.empty());
    }

    @Override
    protected MutableText getNarrationMessage() {
        return Text.translatable("gui.narrate.slider", label);
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int height = getHeight();
        RegionPanelStyle style = RegionPanelStyle.current();
        boolean highlighted = this.active && (isHovered() || isFocused() || dragging);
        this.handleProgress = PanelAnim.approach(this.handleProgress, highlighted ? 1f : 0f, delta / 20f, 16f);

        int trackY = y + (height - TRACK_HEIGHT) / 2;
        int handleX = handleX();
        //填充的右界落在手柄中缝：按整条轨道宽乘比例算会差出半个手柄，看着像"没拖到底就满了"
        RegionPanelDraw.barPixels(context, x, trackY, getWidth(), TRACK_HEIGHT,
                handleX + HANDLE_WIDTH / 2 - x, this.active ? style.barFill() : style.idle());

        int handleHeight = height - 6 + Math.round(this.handleProgress * 3f);
        int handleY = y + (height - handleHeight) / 2;
        //先垫一层卡片底色，遮掉手柄底下那段轨道；不遮的话手柄会透出轨道边缘
        RegionPanelDraw.roundRect(context, handleX - 1, y + 1, HANDLE_WIDTH + 2, height - 2, 4, style.panelBg());
        int handleColor = !this.active ? style.disabled()
                : dragging ? style.accent()
                : RegionPanelDraw.blend(style.text(), style.textBright(), this.handleProgress);
        RegionPanelDraw.roundRect(context, handleX, handleY, HANDLE_WIDTH, handleHeight, 3, handleColor);
        RegionPanelDraw.outline(context, handleX, handleY, HANDLE_WIDTH, handleHeight, 3,
                this.active ? style.panelBorder() : style.iconButtonBorder());

        drawMessage(context, MinecraftClient.getInstance().textRenderer, 0);
    }

    /** 把数值画在轨道上，并且避开手柄：左边放不下就挪到手柄右边，两边都放不下就靠右并垫一层底。 */
    public void drawMessage(DrawContext context, TextRenderer textRenderer, int color) {
        RegionPanelStyle style = RegionPanelStyle.current();
        int alpha = MathHelper.ceil(this.alpha * 255.0F) << 24;
        int textColor = (this.active ? style.text() : style.idle()) | alpha;
        int textWidth = textRenderer.getWidth(label);
        int left = getX() + TEXT_MARGIN;
        int rightAlignedX = Math.max(left, getX() + getWidth() - TEXT_MARGIN - textWidth);
        int handleX = handleX();
        int handleLeft = handleX - HANDLE_GAP;
        int handleRight = handleX + HANDLE_WIDTH + HANDLE_GAP;
        int textX;
        if (handleLeft - left >= textWidth) {
            textX = left + (handleLeft - left - textWidth) / 2;
        } else if (rightAlignedX + textWidth - handleRight >= textWidth) {
            textX = handleRight + (rightAlignedX + textWidth - handleRight - textWidth) / 2;
        } else {
            textX = rightAlignedX;
        }
        textX = MathHelper.clamp(textX, left, rightAlignedX);
        int fillRight = getX() + INSET + (int) (this.value * (getWidth() - HANDLE_WIDTH));
        if (textX < fillRight) {
            int pad = 1;
            RegionPanelDraw.roundRect(context, textX - pad, getY() + 2,
                    Math.min(textX + textWidth + pad, fillRight) - (textX - pad), getHeight() - 4, 3,
                    style.panelBg());
        }
        context.drawTextWithShadow(textRenderer, label, textX, getY() + (getHeight() - 8) / 2, textColor);
    }

    /** 故意空实现，见类注释。 */
    @Override
    protected void applyValue() {
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        dragging = true;
        if (Math.abs(mouseX - (handleX() + HANDLE_WIDTH / 2.0)) <= GRAB_RADIUS) {
            grabOffset = this.value - positionFromMouse(mouseX);
        } else {
            grabOffset = 0.0;
            moveTo(positionFromMouse(mouseX));
        }
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (Screen.hasShiftDown()) {
            moveTo(this.value + deltaX * FINE_SCALE / (getWidth() - HANDLE_WIDTH));
        } else {
            moveTo(positionFromMouse(mouseX) + grabOffset);
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        endDrag();
        super.onRelease(mouseX, mouseY);
        commit();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!this.active || verticalAmount == 0.0) return false;
        moveTo(this.value + Math.signum(verticalAmount) / (getWidth() - HANDLE_WIDTH));
        commit();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
        if (handled) commit();
        return handled;
    }

    public boolean isDragging() {
        return dragging;
    }

    public int handleCenterX() {
        return handleX() + HANDLE_WIDTH / 2;
    }

    public double getSliderValue() {
        return quantized();
    }

    /** 服务端回包/初始化时外部写入。会同步 {@code lastCommitted}，避免把外部写入又当成一次用户操作提交回去。 */
    public void setSliderValue(double value) {
        double position = valueToSlider(min, max, value);
        if (this.value != position) {
            this.value = position;
            updateMessage();
        }
        this.lastCommitted = quantized();
    }

    private int handleX() {
        return getX() + (int) (this.value * (getWidth() - HANDLE_WIDTH));
    }

    private double positionFromMouse(double mouseX) {
        return (mouseX - (getX() + INSET)) / (getWidth() - HANDLE_WIDTH);
    }

    private void moveTo(double position) {
        double clamped = MathHelper.clamp(position, 0.0, 1.0);
        if (clamped == this.value) return;
        this.value = clamped;
        updateMessage();
        if (preview != null) preview.accept(quantized());
    }

    private void endDrag() {
        dragging = false;
        grabOffset = 0.0;
    }

    private void commit() {
        double value = quantized();
        if (value == lastCommitted) return;
        lastCommitted = value;
        committer.accept(value);
        //回调里可能同步触发 setSliderValue（乐观确认），所以提交后要再对齐一次
        lastCommitted = value;
    }

    private double quantized() {
        return quantizer.applyAsDouble(sliderToValue(min, max, this.value));
    }

    private static double valueToSlider(double min, double max, double value) {
        double clamped = Math.clamp(value, min, max);
        return Math.clamp(Math.log(clamped / min) / Math.log(max / min), 0.0, 1.0);
    }

    private static double sliderToValue(double min, double max, double position) {
        return min * Math.pow(max / min, Math.clamp(position, 0.0, 1.0));
    }
}
