package net.minecraft.client.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.text.Style;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class BakedGlyph {
    public static final float field_55098 = 0.001F;
    private final TextRenderLayerSet textRenderLayers;
    private final float minU;
    private final float maxU;
    private final float minV;
    private final float maxV;
    private final float minX;
    private final float maxX;
    private final float minY;
    private final float maxY;

    public BakedGlyph(TextRenderLayerSet textRenderLayers, float minU, float maxU, float minV, float maxV, float minX, float maxX, float minY, float maxY) {
        this.textRenderLayers = textRenderLayers;
        this.minU = minU;
        this.maxU = maxU;
        this.minV = minV;
        this.maxV = maxV;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
    }

    public void draw(BakedGlyph.DrawnGlyph glyph, Matrix4f matrix, VertexConsumer vertexConsumer, int light) {
        Style style = glyph.style();
        boolean bl = style.isItalic();
        float f = glyph.x();
        float g = glyph.y();
        int i = glyph.color();
        int j = glyph.shadowColor();
        boolean bl2 = style.isBold();
        if (glyph.hasShadow()) {
            this.draw(bl, f + glyph.shadowOffset(), g + glyph.shadowOffset(), matrix, vertexConsumer, j, bl2, light);
            this.draw(bl, f, g, 0.03F, matrix, vertexConsumer, i, bl2, light);
        } else {
            this.draw(bl, f, g, matrix, vertexConsumer, i, bl2, light);
        }

        if (bl2) {
            if (glyph.hasShadow()) {
                this.draw(bl, f + glyph.boldOffset() + glyph.shadowOffset(), g + glyph.shadowOffset(), 0.001F, matrix, vertexConsumer, j, true, light);
                this.draw(bl, f + glyph.boldOffset(), g, 0.03F, matrix, vertexConsumer, i, true, light);
            } else {
                this.draw(bl, f + glyph.boldOffset(), g, matrix, vertexConsumer, i, true, light);
            }
        }
    }

    private void draw(boolean italic, float x, float y, Matrix4f matrix, VertexConsumer vertexConsumer, int color, boolean bold, int light) {
        this.draw(italic, x, y, 0.0F, matrix, vertexConsumer, color, bold, light);
    }

    private void draw(boolean italic, float x, float y, float z, Matrix4f matrix, VertexConsumer vertexConsumer, int color, boolean bold, int light) {
        float f = x + this.minX;
        float g = x + this.maxX;
        float h = y + this.minY;
        float i = y + this.maxY;
        float j = italic ? 1.0F - 0.25F * this.minY : 0.0F;
        float k = italic ? 1.0F - 0.25F * this.maxY : 0.0F;
        float l = bold ? 0.1F : 0.0F;
        vertexConsumer.vertex(matrix, f + j - l, h - l, z).color(color).texture(this.minU, this.minV).light(light);
        vertexConsumer.vertex(matrix, f + k - l, i + l, z).color(color).texture(this.minU, this.maxV).light(light);
        vertexConsumer.vertex(matrix, g + k + l, i + l, z).color(color).texture(this.maxU, this.maxV).light(light);
        vertexConsumer.vertex(matrix, g + j + l, h - l, z).color(color).texture(this.maxU, this.minV).light(light);
    }

    public void drawRectangle(BakedGlyph.Rectangle rectangle, Matrix4f matrix, VertexConsumer vertexConsumer, int light) {
        if (rectangle.hasShadow()) {
            this.drawRectangle(rectangle, rectangle.shadowOffset(), 0.0F, rectangle.shadowColor(), vertexConsumer, light, matrix);
            this.drawRectangle(rectangle, 0.0F, 0.03F, rectangle.color, vertexConsumer, light, matrix);
        } else {
            this.drawRectangle(rectangle, 0.0F, 0.0F, rectangle.color, vertexConsumer, light, matrix);
        }
    }

    private void drawRectangle(
        BakedGlyph.Rectangle rectangle, float shadowOffset, float zOffset, int color, VertexConsumer vertexConsumer, int light, Matrix4f matrix
    ) {
        vertexConsumer.vertex(matrix, rectangle.minX + shadowOffset, rectangle.minY + shadowOffset, rectangle.zIndex + zOffset)
            .color(color)
            .texture(this.minU, this.minV)
            .light(light);
        vertexConsumer.vertex(matrix, rectangle.maxX + shadowOffset, rectangle.minY + shadowOffset, rectangle.zIndex + zOffset)
            .color(color)
            .texture(this.minU, this.maxV)
            .light(light);
        vertexConsumer.vertex(matrix, rectangle.maxX + shadowOffset, rectangle.maxY + shadowOffset, rectangle.zIndex + zOffset)
            .color(color)
            .texture(this.maxU, this.maxV)
            .light(light);
        vertexConsumer.vertex(matrix, rectangle.minX + shadowOffset, rectangle.maxY + shadowOffset, rectangle.zIndex + zOffset)
            .color(color)
            .texture(this.maxU, this.minV)
            .light(light);
    }

    public RenderLayer getLayer(TextRenderer.TextLayerType layerType) {
        return this.textRenderLayers.getRenderLayer(layerType);
    }

    @Environment(EnvType.CLIENT)
    public record DrawnGlyph(float x, float y, int color, int shadowColor, BakedGlyph glyph, Style style, float boldOffset, float shadowOffset) {
        boolean hasShadow() {
            return this.shadowColor() != 0;
        }
    }

    @Environment(EnvType.CLIENT)
    public record Rectangle(float minX, float minY, float maxX, float maxY, float zIndex, int color, int shadowColor, float shadowOffset) {
        public Rectangle(float minX, float minY, float maxX, float maxY, float zIndex, int color) {
            this(minX, minY, maxX, maxY, zIndex, color, 0, 0.0F);
        }

        boolean hasShadow() {
            return this.shadowColor() != 0;
        }
    }
}

