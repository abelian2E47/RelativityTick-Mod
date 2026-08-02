package com.abelian.client.mixin;

import com.abelian.client.clientRegionTick.ClientRegion;
import com.abelian.client.clientRegionTick.ClientRegionManager;
import net.minecraft.client.render.*;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


import static com.abelian.client.render.RendererUtils.renderChunkLines;
import static com.abelian.client.RelativityTickClient.selectChunks;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/debug/DebugRenderer;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;DDD)V"
            )
    )
    private void renderRegionBoundaries(
            DebugRenderer debugRenderer,
            MatrixStack matrices,
            VertexConsumerProvider.Immediate consumers,
            double cameraX,
            double cameraY,
            double cameraZ
    ) {
        debugRenderer.render(matrices, consumers, cameraX, cameraY, cameraZ);
        if (selectChunks.isEmpty() && ClientRegionManager.getRegions().isEmpty()) return;

        matrices.push();
        matrices.translate(-cameraX, -cameraY, -cameraZ);
        VertexConsumer lineConsumer = consumers.getBuffer(RenderLayer.getDebugLineStrip(5.0));

        if (!selectChunks.isEmpty()) {
            float r1 = 1.0F, g1 = 0.5F, b1 = 0.0F;
            for (long posLong : selectChunks) {
                renderChunkLines(matrices, lineConsumer, posLong, -64, 320, r1, g1, b1, selectChunks);
            }
        }

        for (ClientRegion region : ClientRegionManager.getRegions()) {
            if (region.getChunkPositions() == null || region.getDimension().isEmpty()) continue;

            float r2, g2, b2;
            if (region.isRunning()) {
                r2 = 0.8f;
                g2 = 0.2f;
                b2 = 1.0f;
            } else if (region.isControlled()) {
                r2 = 0.0f;
                g2 = 0.5f;
                b2 = 1.0f;
            } else {
                r2 = 0.0f;
                g2 = 1.0f;
                b2 = 0.0f;
            }

            for (long posLong : region.getChunkPositions()) {
                renderChunkLines(matrices, lineConsumer, posLong, -64, 320, r2, g2, b2, region.getChunkPositions());
            }
        }
        matrices.pop();
    }
}

