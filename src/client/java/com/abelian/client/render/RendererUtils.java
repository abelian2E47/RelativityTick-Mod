package com.abelian.client.render;

import com.abelian.client.config.RelativityTickClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.Function;

public class RendererUtils {
    //区域线框渲染层：LINES 模式 + 可配置线宽（宽度为屏幕像素，与视角无关）
    private static final Function<Double, RenderLayer> REGION_LINES_LAYER = Util.memoize(width -> RenderLayer.of(
            "region_lines",
            VertexFormats.LINES,
            VertexFormat.DrawMode.LINES,
            1536,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.LINES_PROGRAM)
                    .lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(width)))
                    .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .target(RenderPhase.ITEM_ENTITY_TARGET)
                    .writeMaskState(RenderPhase.ALL_MASK)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .build(false)
    ));

    public static RenderLayer getRegionLinesLayer(double width) {
        return REGION_LINES_LAYER.apply(width);
    }
    public static void renderTexts(List<MutableText> infoTexts, Vec3d worldPos, float height, MatrixStack matrices, VertexConsumerProvider vertexConsumer) {
        if (infoTexts.isEmpty()) return;
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        float y = 10 - (infoTexts.size() * 10);

        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        float cameraYaw = camera.getYaw();
        float cameraPitch = camera.getPitch();

        matrices.push();
        matrices.translate(worldPos.x, worldPos.y + height, worldPos.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-cameraYaw + 180));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-cameraPitch));
        float scale = (float) RelativityTickClientConfig.getScheduledTickTextScale();
        matrices.scale(scale, -scale, scale);
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();

        float y1 = y;
        for (Text infoText : infoTexts) {
            float x = -textRenderer.getWidth(infoText) / 2f;
            textRenderer.draw(
                    infoText, x, y1, -2130706433, false,
                    matrix4f, vertexConsumer,
                    TextRenderer.TextLayerType.SEE_THROUGH,
                    0x4CC8C8C8,
                    0xF000F0
            );
            y1 += 10;
        }


        float y2 = y;
        for (Text infoText : infoTexts) {
            float x = -textRenderer.getWidth(infoText) / 2f;
            textRenderer.draw(
                    infoText, x, y2, 0xFFFFFFFF, false,
                    matrix4f, vertexConsumer,
                    TextRenderer.TextLayerType.SEE_THROUGH,
                    0, 0xF000F0
            );
            y2 += 10;
        }

        matrices.pop();

    }

    public static void renderChunkLines(MatrixStack matrices, VertexConsumer lineConsumer, long currentPos, float minY, float maxY, float r,float g, float b,Set<Long> chunks) {
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();

        int chunkX = ChunkPos.getPackedX(currentPos);
        int chunkZ = ChunkPos.getPackedZ(currentPos);
        float x = (float)(chunkX << 4);
        float z = (float)(chunkZ << 4);

        boolean hasNorth = chunks.contains(ChunkPos.toLong(chunkX, chunkZ - 1));
        boolean hasSouth = chunks.contains(ChunkPos.toLong(chunkX, chunkZ + 1));
        boolean hasWest  = chunks.contains(ChunkPos.toLong(chunkX - 1, chunkZ));
        boolean hasEast  = chunks.contains(ChunkPos.toLong(chunkX + 1, chunkZ));

        drawVerticalIfNecessary(matrix4f, lineConsumer, x, z, minY, maxY, r, g, b, hasWest, hasNorth);
        drawVerticalIfNecessary(matrix4f, lineConsumer, x + 16, z, minY, maxY, r, g, b, hasEast, hasNorth);
        drawVerticalIfNecessary(matrix4f, lineConsumer, x, z + 16, minY, maxY, r, g, b, hasWest, hasSouth);
        drawVerticalIfNecessary(matrix4f, lineConsumer, x + 16, z + 16, minY, maxY, r, g, b, hasEast, hasSouth);

        for (float y = minY; y <= maxY; y += 4) {
            if (!hasNorth) drawLine(matrix4f, lineConsumer, x, y, z, x + 16, y, z, r, g, b);
            if (!hasSouth) drawLine(matrix4f, lineConsumer, x, y, z + 16, x + 16, y, z + 16, r, g, b);
            if (!hasWest)  drawLine(matrix4f, lineConsumer, x, y, z, x, y, z + 16, r, g, b);
            if (!hasEast)  drawLine(matrix4f, lineConsumer, x + 16, y, z, x + 16, y, z + 16, r, g, b);
        }
    }

    private static void drawVerticalIfNecessary(Matrix4f matrix, VertexConsumer consumer, float x, float z, float minY, float maxY, float r, float g, float b, boolean n1, boolean n2) {
        if (!(n1 && n2)) {
            consumer.vertex(matrix, x, minY, z).color(r, g, b, 1.0F).normal(0.0F, 1.0F, 0.0F);
            consumer.vertex(matrix, x, maxY, z).color(r, g, b, 1.0F).normal(0.0F, 1.0F, 0.0F);
        }
    }

    private static void drawLine(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b) {
        float nx = x2 - x1;
        float ny = y2 - y1;
        float nz = z2 - z1;
        float length = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= length;
        ny /= length;
        nz /= length;
        consumer.vertex(matrix, x1, y1, z1).color(r, g, b, 1.0F).normal(nx, ny, nz);
        consumer.vertex(matrix, x2, y2, z2).color(r, g, b, 1.0F).normal(nx, ny, nz);
    }



}
