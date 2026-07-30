package net.minecraft.client.render.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ColorHelper;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class ChunkBorderDebugRenderer implements DebugRenderer.Renderer {
    private final MinecraftClient client;
    private static final int DARK_CYAN = ColorHelper.getArgb(255, 0, 155, 155);
    private static final int YELLOW = ColorHelper.getArgb(255, 255, 255, 0);

    public ChunkBorderDebugRenderer(MinecraftClient client) {
        this.client = client;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, double cameraX, double cameraY, double cameraZ) {
        Entity entity = this.client.gameRenderer.getCamera().getFocusedEntity();
        float f = (float)(this.client.world.getBottomY() - cameraY);
        float g = (float)(this.client.world.getTopYInclusive() + 1 - cameraY);
        ChunkPos chunkPos = entity.getChunkPos();
        float h = (float)(chunkPos.getStartX() - cameraX);
        float i = (float)(chunkPos.getStartZ() - cameraZ);
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getDebugLineStrip(1.0));
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();

        for (int j = -16; j <= 32; j += 16) {
            for (int k = -16; k <= 32; k += 16) {
                vertexConsumer.vertex(matrix4f, h + j, f, i + k).color(1.0F, 0.0F, 0.0F, 0.0F);
                vertexConsumer.vertex(matrix4f, h + j, f, i + k).color(1.0F, 0.0F, 0.0F, 0.5F);
                vertexConsumer.vertex(matrix4f, h + j, g, i + k).color(1.0F, 0.0F, 0.0F, 0.5F);
                vertexConsumer.vertex(matrix4f, h + j, g, i + k).color(1.0F, 0.0F, 0.0F, 0.0F);
            }
        }

        for (int l = 2; l < 16; l += 2) {
            int m = l % 4 == 0 ? DARK_CYAN : YELLOW;
            vertexConsumer.vertex(matrix4f, h + l, f, i).color(1.0F, 1.0F, 0.0F, 0.0F);
            vertexConsumer.vertex(matrix4f, h + l, f, i).color(m);
            vertexConsumer.vertex(matrix4f, h + l, g, i).color(m);
            vertexConsumer.vertex(matrix4f, h + l, g, i).color(1.0F, 1.0F, 0.0F, 0.0F);
            vertexConsumer.vertex(matrix4f, h + l, f, i + 16.0F).color(1.0F, 1.0F, 0.0F, 0.0F);
            vertexConsumer.vertex(matrix4f, h + l, f, i + 16.0F).color(m);
            vertexConsumer.vertex(matrix4f, h + l, g, i + 16.0F).color(m);
            vertexConsumer.vertex(matrix4f, h + l, g, i + 16.0F).color(1.0F, 1.0F, 0.0F, 0.0F);
        }

        for (int n = 2; n < 16; n += 2) {
            int o = n % 4 == 0 ? DARK_CYAN : YELLOW;
            vertexConsumer.vertex(matrix4f, h, f, i + n).color(1.0F, 1.0F, 0.0F, 0.0F);
            vertexConsumer.vertex(matrix4f, h, f, i + n).color(o);
            vertexConsumer.vertex(matrix4f, h, g, i + n).color(o);
            vertexConsumer.vertex(matrix4f, h, g, i + n).color(1.0F, 1.0F, 0.0F, 0.0F);
            vertexConsumer.vertex(matrix4f, h + 16.0F, f, i + n).color(1.0F, 1.0F, 0.0F, 0.0F);
            vertexConsumer.vertex(matrix4f, h + 16.0F, f, i + n).color(o);
            vertexConsumer.vertex(matrix4f, h + 16.0F, g, i + n).color(o);
            vertexConsumer.vertex(matrix4f, h + 16.0F, g, i + n).color(1.0F, 1.0F, 0.0F, 0.0F);
        }

        for (int p = this.client.world.getBottomY(); p <= this.client.world.getTopYInclusive() + 1; p += 2) {
            float q = (float)(p - cameraY);
            int r = p % 8 == 0 ? DARK_CYAN : YELLOW;
            vertexConsumer.vertex(matrix4f, h, q, i).color(1.0F, 1.0F, 0.0F, 0.0F);
            vertexConsumer.vertex(matrix4f, h, q, i).color(r);
            vertexConsumer.vertex(matrix4f, h, q, i + 16.0F).color(r);
            vertexConsumer.vertex(matrix4f, h + 16.0F, q, i + 16.0F).color(r);
            vertexConsumer.vertex(matrix4f, h + 16.0F, q, i).color(r);
            vertexConsumer.vertex(matrix4f, h, q, i).color(r);
            vertexConsumer.vertex(matrix4f, h, q, i).color(1.0F, 1.0F, 0.0F, 0.0F);
        }

        vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getDebugLineStrip(2.0));

        for (int s = 0; s <= 16; s += 16) {
            for (int t = 0; t <= 16; t += 16) {
                vertexConsumer.vertex(matrix4f, h + s, f, i + t).color(0.25F, 0.25F, 1.0F, 0.0F);
                vertexConsumer.vertex(matrix4f, h + s, f, i + t).color(0.25F, 0.25F, 1.0F, 1.0F);
                vertexConsumer.vertex(matrix4f, h + s, g, i + t).color(0.25F, 0.25F, 1.0F, 1.0F);
                vertexConsumer.vertex(matrix4f, h + s, g, i + t).color(0.25F, 0.25F, 1.0F, 0.0F);
            }
        }

        for (int u = this.client.world.getBottomY(); u <= this.client.world.getTopYInclusive() + 1; u += 16) {
            float v = (float)(u - cameraY);
            vertexConsumer.vertex(matrix4f, h, v, i).color(0.25F, 0.25F, 1.0F, 0.0F);
            vertexConsumer.vertex(matrix4f, h, v, i).color(0.25F, 0.25F, 1.0F, 1.0F);
            vertexConsumer.vertex(matrix4f, h, v, i + 16.0F).color(0.25F, 0.25F, 1.0F, 1.0F);
            vertexConsumer.vertex(matrix4f, h + 16.0F, v, i + 16.0F).color(0.25F, 0.25F, 1.0F, 1.0F);
            vertexConsumer.vertex(matrix4f, h + 16.0F, v, i).color(0.25F, 0.25F, 1.0F, 1.0F);
            vertexConsumer.vertex(matrix4f, h, v, i).color(0.25F, 0.25F, 1.0F, 1.0F);
            vertexConsumer.vertex(matrix4f, h, v, i).color(0.25F, 0.25F, 1.0F, 0.0F);
        }
    }
}

