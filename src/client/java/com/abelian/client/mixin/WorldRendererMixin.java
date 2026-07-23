package com.abelian.client.mixin;

import com.abelian.client.ClientRegion;
import com.abelian.client.ClientRegionManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


import static com.abelian.client.RendererUtils.renderChunkLines;
import static com.abelian.client.RelativityTickClient.selectChunks;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Inject(
            method = "renderMain",
            at = @At(value = "TAIL")
    )
    private void renderRegionBoundaries(
            FrameGraphBuilder frameGraphBuilder,
            Frustum frustum,
            Camera camera,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            Fog fog,
            boolean renderBlockOutline,
            boolean renderEntityOutlines,
            RenderTickCounter renderTickCounter,
            Profiler profiler, CallbackInfo ci) {
        if (selectChunks.isEmpty() && ClientRegionManager.getRegions().isEmpty()) return;

        Vec3d camPos = camera.getPos();
        MatrixStack matrices = new MatrixStack();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        VertexConsumerProvider.Immediate consumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer lineConsumer = consumers.getBuffer(RenderLayer.getDebugLineStrip(5.0));
        //渲染选择中的区块
        if (!selectChunks.isEmpty()){
            float r1 = 1.0F, g1 = 0.5F, b1 = 0.0F;
            for (long posLong : selectChunks) {
                renderChunkLines(matrices, lineConsumer, posLong, -64, 320, r1, g1, b1, selectChunks);
            }
        }


        //渲染已选择的区域
        for (ClientRegion region : ClientRegionManager.getRegions()) {
            if (region.getChunkPositions() == null || region.getDimension().isEmpty()) continue;

            float r2, g2, b2;

            if (region.isRunning()) {
                //紫色
                r2 = 0.8f;
                g2 = 0.2f;
                b2 = 1.0f;
            } else if (region.isControlled()) {
                //蓝色
                r2 = 0.0f;
                g2 = 0.5f;
                b2 = 1.0f;
            } else {
                //绿色
                r2 = 0.0f;
                g2 = 1.0f;
                b2 = 0.0f;
            }

            for (long posLong : region.getChunkPositions()) {
                renderChunkLines(matrices, lineConsumer, posLong, -64, 320, r2, g2, b2, region.getChunkPositions());
            }
        }
    }

}

