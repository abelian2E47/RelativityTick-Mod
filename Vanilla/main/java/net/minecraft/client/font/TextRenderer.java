package net.minecraft.client.font;

import com.google.common.collect.Lists;
import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.TextVisitFactory;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix4f;

/**
 * Manages the rendering of text.
 * 
 * <p>The current instance used by the client can be obtained by
 * {@code MinecraftClient.getInstance().textRenderer}.
 * 
 * @see net.minecraft.client.MinecraftClient#textRenderer
 */
@Environment(EnvType.CLIENT)
public class TextRenderer {
    private static final float Z_INDEX = 0.01F;
    public static final float FORWARD_SHIFT = 0.03F;
    public static final int field_55090 = 0;
    public static final int ARABIC_SHAPING_LETTERS_SHAPE = 8;
    /**
     * The font height of the text that is rendered by the text renderer.
     */
    public final int fontHeight = 9;
    public final Random random = Random.create();
    private final Function<Identifier, FontStorage> fontStorageAccessor;
    final boolean validateAdvance;
    private final TextHandler handler;

    public TextRenderer(Function<Identifier, FontStorage> fontStorageAccessor, boolean validateAdvance) {
        this.fontStorageAccessor = fontStorageAccessor;
        this.validateAdvance = validateAdvance;
        this.handler = new TextHandler(
            (codePoint, style) -> this.getFontStorage(style.getFont()).getGlyph(codePoint, this.validateAdvance).getAdvance(style.isBold())
        );
    }

    FontStorage getFontStorage(Identifier id) {
        return this.fontStorageAccessor.apply(id);
    }

    public String mirror(String text) {
        try {
            Bidi bidi = new Bidi(new ArabicShaping(8).shape(text), 127);
            bidi.setReorderingMode(0);
            return bidi.writeReordered(2);
        } catch (ArabicShapingException var3) {
            return text;
        }
    }

    /**
     * @param color the text color in the 0xAARRGGBB format
     */
    public int draw(
        String text,
        float x,
        float y,
        int color,
        boolean shadow,
        Matrix4f matrix,
        VertexConsumerProvider vertexConsumers,
        TextRenderer.TextLayerType layerType,
        int backgroundColor,
        int light
    ) {
        if (this.isRightToLeft()) {
            text = this.mirror(text);
        }

        return this.drawInternal(text, x, y, color, shadow, matrix, vertexConsumers, layerType, backgroundColor, light, true);
    }

    /**
     * @param color the text color in the 0xAARRGGBB format
     */
    public int draw(
        Text text,
        float x,
        float y,
        int color,
        boolean shadow,
        Matrix4f matrix,
        VertexConsumerProvider vertexConsumers,
        TextRenderer.TextLayerType layerType,
        int backgroundColor,
        int light
    ) {
        return this.draw(text, x, y, color, shadow, matrix, vertexConsumers, layerType, backgroundColor, light, true);
    }

    /**
     * @param color the text color in the 0xAARRGGBB format
     */
    public int draw(
        Text text,
        float x,
        float y,
        int color,
        boolean shadow,
        Matrix4f matrix,
        VertexConsumerProvider vertexConsumers,
        TextRenderer.TextLayerType layerType,
        int backgroundColor,
        int light,
        boolean swapZIndex
    ) {
        return this.drawInternal(text.asOrderedText(), x, y, color, shadow, matrix, vertexConsumers, layerType, backgroundColor, light, swapZIndex);
    }

    /**
     * @param color the text color in the 0xAARRGGBB format
     */
    public int draw(
        OrderedText text,
        float x,
        float y,
        int color,
        boolean shadow,
        Matrix4f matrix,
        VertexConsumerProvider vertexConsumers,
        TextRenderer.TextLayerType layerType,
        int backgroundColor,
        int light
    ) {
        return this.drawInternal(text, x, y, color, shadow, matrix, vertexConsumers, layerType, backgroundColor, light, true);
    }

    /**
     * @param color the text color in 0xAARRGGBB
     * @param outlineColor the outline color in 0xAARRGGBB
     */
    public void drawWithOutline(
        OrderedText text, float x, float y, int color, int outlineColor, Matrix4f matrix, VertexConsumerProvider vertexConsumers, int light
    ) {
        int i = tweakTransparency(outlineColor);
        TextRenderer.Drawer drawer = new TextRenderer.Drawer(vertexConsumers, 0.0F, 0.0F, i, false, matrix, TextRenderer.TextLayerType.NORMAL, light);

        for (int j = -1; j <= 1; j++) {
            for (int k = -1; k <= 1; k++) {
                if (j != 0 || k != 0) {
                    float[] fs = new float[]{x};
                    int l = j;
                    int m = k;
                    text.accept((index, style, codePoint) -> {
                        boolean bl = style.isBold();
                        FontStorage fontStorage = this.getFontStorage(style.getFont());
                        Glyph glyph = fontStorage.getGlyph(codePoint, this.validateAdvance);
                        drawer.x = fs[0] + l * glyph.getShadowOffset();
                        drawer.y = y + m * glyph.getShadowOffset();
                        fs[0] += glyph.getAdvance(bl);
                        return drawer.accept(index, style.withColor(i), codePoint);
                    });
                }
            }
        }

        drawer.drawGlyphs();
        TextRenderer.Drawer drawer2 = new TextRenderer.Drawer(
            vertexConsumers, x, y, tweakTransparency(color), false, matrix, TextRenderer.TextLayerType.POLYGON_OFFSET, light
        );
        text.accept(drawer2);
        drawer2.drawLayer(x);
    }

    private static int tweakTransparency(int argb) {
        return (argb & -67108864) == 0 ? ColorHelper.fullAlpha(argb) : argb;
    }

    private int drawInternal(
        String text,
        float x,
        float y,
        int color,
        boolean shadow,
        Matrix4f matrix,
        VertexConsumerProvider vertexConsumers,
        TextRenderer.TextLayerType layerType,
        int backgroundColor,
        int light,
        boolean mirror
    ) {
        color = tweakTransparency(color);
        x = this.drawLayer(text, x, y, color, shadow, matrix, vertexConsumers, layerType, backgroundColor, light, mirror);
        return (int)x + (shadow ? 1 : 0);
    }

    private int drawInternal(
        OrderedText text,
        float x,
        float y,
        int color,
        boolean shadow,
        Matrix4f matrix,
        VertexConsumerProvider vertexConsumerProvider,
        TextRenderer.TextLayerType layerType,
        int backgroundColor,
        int light,
        boolean swapZIndex
    ) {
        color = tweakTransparency(color);
        x = this.drawLayer(text, x, y, color, shadow, matrix, vertexConsumerProvider, layerType, backgroundColor, light, swapZIndex);
        return (int)x + (shadow ? 1 : 0);
    }

    private float drawLayer(
        String text,
        float x,
        float y,
        int color,
        boolean shadow,
        Matrix4f matrix,
        VertexConsumerProvider vertexConsumerProvider,
        TextRenderer.TextLayerType layerType,
        int backgroundColor,
        int light,
        boolean swapZIndex
    ) {
        TextRenderer.Drawer drawer = new TextRenderer.Drawer(vertexConsumerProvider, x, y, color, backgroundColor, shadow, matrix, layerType, light, swapZIndex);
        TextVisitFactory.visitFormatted(text, Style.EMPTY, drawer);
        return drawer.drawLayer(x);
    }

    private float drawLayer(
        OrderedText text,
        float x,
        float y,
        int color,
        boolean shadow,
        Matrix4f matrix,
        VertexConsumerProvider vertexConsumerProvider,
        TextRenderer.TextLayerType layerType,
        int backgroundColor,
        int light,
        boolean swapZIndex
    ) {
        TextRenderer.Drawer drawer = new TextRenderer.Drawer(vertexConsumerProvider, x, y, color, backgroundColor, shadow, matrix, layerType, light, swapZIndex);
        text.accept(drawer);
        return drawer.drawLayer(x);
    }

    /**
     * Gets the width of some text when rendered.
     * 
     * @param text the text
     */
    public int getWidth(String text) {
        return MathHelper.ceil(this.handler.getWidth(text));
    }

    /**
     * Gets the width of some text when rendered.
     * 
     * @param text the text
     */
    public int getWidth(StringVisitable text) {
        return MathHelper.ceil(this.handler.getWidth(text));
    }

    /**
     * Gets the width of some text when rendered.
     */
    public int getWidth(OrderedText text) {
        return MathHelper.ceil(this.handler.getWidth(text));
    }

    /**
     * Trims a string to be at most {@code maxWidth} wide.
     * 
     * @return the trimmed string
     */
    public String trimToWidth(String text, int maxWidth, boolean backwards) {
        return backwards ? this.handler.trimToWidthBackwards(text, maxWidth, Style.EMPTY) : this.handler.trimToWidth(text, maxWidth, Style.EMPTY);
    }

    /**
     * Trims a string to be at most {@code maxWidth} wide.
     * 
     * @return the trimmed string
     * @see TextHandler#trimToWidth(String, int, Style)
     */
    public String trimToWidth(String text, int maxWidth) {
        return this.handler.trimToWidth(text, maxWidth, Style.EMPTY);
    }

    /**
     * Trims a string to be at most {@code maxWidth} wide.
     * 
     * @return the text
     * @see TextHandler#trimToWidth(StringVisitable, int, Style)
     */
    public StringVisitable trimToWidth(StringVisitable text, int width) {
        return this.handler.trimToWidth(text, width, Style.EMPTY);
    }

    /**
     * Gets the height of the text when it has been wrapped.
     * 
     * @return the height of the wrapped text
     * @see TextRenderer#wrapLines(StringVisitable, int)
     * @see #getWrappedLinesHeight(StringVisitable, int)
     */
    public int getWrappedLinesHeight(String text, int maxWidth) {
        return 9 * this.handler.wrapLines(text, maxWidth, Style.EMPTY).size();
    }

    /**
     * {@return the height of the text, after it has been wrapped, in pixels}
     * @see TextRenderer#wrapLines(StringVisitable, int)
     * @see #getWrappedLinesHeight(String, int)
     */
    public int getWrappedLinesHeight(StringVisitable text, int maxWidth) {
        return 9 * this.handler.wrapLines(text, maxWidth, Style.EMPTY).size();
    }

    /**
     * Wraps text when the rendered width of text exceeds the {@code width}.
     * 
     * @return a list of ordered text which has been wrapped
     */
    public List<OrderedText> wrapLines(StringVisitable text, int width) {
        return Language.getInstance().reorder(this.handler.wrapLines(text, width, Style.EMPTY));
    }

    /**
     * Checks if the currently set language uses right to left writing.
     */
    public boolean isRightToLeft() {
        return Language.getInstance().isRightToLeft();
    }

    public TextHandler getTextHandler() {
        return this.handler;
    }

    @Environment(EnvType.CLIENT)
    class Drawer implements CharacterVisitor {
        final VertexConsumerProvider vertexConsumers;
        private final boolean shadow;
        private final int color;
        private final int backgroundColor;
        private final Matrix4f matrix;
        private final TextRenderer.TextLayerType layerType;
        private final int light;
        private final boolean swapZIndex;
        float x;
        float y;
        private final List<BakedGlyph.DrawnGlyph> glyphs = new ArrayList<>();
        @Nullable
        private List<BakedGlyph.Rectangle> rectangles;

        private void addRectangle(BakedGlyph.Rectangle rectangle) {
            if (this.rectangles == null) {
                this.rectangles = Lists.newArrayList();
            }

            this.rectangles.add(rectangle);
        }

        public Drawer(
            final VertexConsumerProvider vertexConsumers,
            final float x,
            final float y,
            final int color,
            final boolean shadow,
            final Matrix4f matrix,
            final TextRenderer.TextLayerType layerType,
            final int light
        ) {
            this(vertexConsumers, x, y, color, 0, shadow, matrix, layerType, light, true);
        }

        public Drawer(
            final VertexConsumerProvider vertexConsumers,
            final float x,
            final float y,
            final int color,
            final int backgroundColor,
            final boolean shadow,
            final Matrix4f matrix,
            final TextRenderer.TextLayerType layerType,
            final int light,
            final boolean swapZIndex
        ) {
            this.vertexConsumers = vertexConsumers;
            this.x = x;
            this.y = y;
            this.shadow = shadow;
            this.color = color;
            this.backgroundColor = backgroundColor;
            this.matrix = matrix;
            this.layerType = layerType;
            this.light = light;
            this.swapZIndex = swapZIndex;
        }

        @Override
        public boolean accept(int index, Style style, int codePoint) {
            FontStorage fontStorage = TextRenderer.this.getFontStorage(style.getFont());
            Glyph glyph = fontStorage.getGlyph(codePoint, TextRenderer.this.validateAdvance);
            BakedGlyph bakedGlyph = style.isObfuscated() && codePoint != 32 ? fontStorage.getObfuscatedBakedGlyph(glyph) : fontStorage.getBaked(codePoint);
            boolean bl = style.isBold();
            TextColor textColor = style.getColor();
            int i = this.getRenderColor(textColor);
            int j = this.getShadowColor(style, i);
            float f = glyph.getAdvance(bl);
            float g = index == 0 ? this.x - 1.0F : this.x;
            float h = glyph.getShadowOffset();
            if (!(bakedGlyph instanceof EmptyBakedGlyph)) {
                float k = bl ? glyph.getBoldOffset() : 0.0F;
                this.glyphs.add(new BakedGlyph.DrawnGlyph(this.x, this.y, i, j, bakedGlyph, style, k, h));
            }

            if (style.isStrikethrough()) {
                this.addRectangle(new BakedGlyph.Rectangle(g, this.y + 4.5F, this.x + f, this.y + 4.5F - 1.0F, this.getForegroundZIndex(), i, j, h));
            }

            if (style.isUnderlined()) {
                this.addRectangle(new BakedGlyph.Rectangle(g, this.y + 9.0F, this.x + f, this.y + 9.0F - 1.0F, this.getForegroundZIndex(), i, j, h));
            }

            this.x += f;
            return true;
        }

        float drawLayer(float x) {
            BakedGlyph bakedGlyph = null;
            if (this.backgroundColor != 0) {
                BakedGlyph.Rectangle rectangle = new BakedGlyph.Rectangle(
                    x - 1.0F, this.y + 9.0F, this.x, this.y - 1.0F, this.getBackgroundZIndex(), this.backgroundColor
                );
                bakedGlyph = TextRenderer.this.getFontStorage(Style.DEFAULT_FONT_ID).getRectangleBakedGlyph();
                VertexConsumer vertexConsumer = this.vertexConsumers.getBuffer(bakedGlyph.getLayer(this.layerType));
                bakedGlyph.drawRectangle(rectangle, this.matrix, vertexConsumer, this.light);
            }

            this.drawGlyphs();
            if (this.rectangles != null) {
                if (bakedGlyph == null) {
                    bakedGlyph = TextRenderer.this.getFontStorage(Style.DEFAULT_FONT_ID).getRectangleBakedGlyph();
                }

                VertexConsumer vertexConsumer2 = this.vertexConsumers.getBuffer(bakedGlyph.getLayer(this.layerType));

                for (BakedGlyph.Rectangle rectangle2 : this.rectangles) {
                    bakedGlyph.drawRectangle(rectangle2, this.matrix, vertexConsumer2, this.light);
                }
            }

            return this.x;
        }

        private int getRenderColor(@Nullable TextColor override) {
            if (override != null) {
                int i = ColorHelper.getAlpha(this.color);
                int j = override.getRgb();
                return ColorHelper.withAlpha(i, j);
            } else {
                return this.color;
            }
        }

        private int getShadowColor(Style style, int textColor) {
            Integer integer = style.getShadowColor();
            if (integer != null) {
                float f = ColorHelper.getAlphaFloat(textColor);
                float g = ColorHelper.getAlphaFloat(integer);
                return f != 1.0F ? ColorHelper.withAlpha(ColorHelper.channelFromFloat(f * g), integer) : integer;
            } else {
                return this.shadow ? ColorHelper.scaleRgb(textColor, 0.25F) : 0;
            }
        }

        void drawGlyphs() {
            for (BakedGlyph.DrawnGlyph drawnGlyph : this.glyphs) {
                BakedGlyph bakedGlyph = drawnGlyph.glyph();
                VertexConsumer vertexConsumer = this.vertexConsumers.getBuffer(bakedGlyph.getLayer(this.layerType));
                bakedGlyph.draw(drawnGlyph, this.matrix, vertexConsumer, this.light);
            }
        }

        private float getForegroundZIndex() {
            return this.swapZIndex ? 0.01F : -0.01F;
        }

        private float getBackgroundZIndex() {
            return this.swapZIndex ? -0.01F : 0.01F;
        }
    }

    @Environment(EnvType.CLIENT)
    public enum TextLayerType {
        NORMAL,
        SEE_THROUGH,
        POLYGON_OFFSET;
    }
}

