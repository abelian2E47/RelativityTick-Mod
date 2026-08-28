package com.abelian.client.mixin;

import com.abelian.client.clientRegionTick.ClientRegion;
import com.abelian.client.clientRegionTick.ClientRegionManager;
import com.abelian.client.clientRegionTick.ClientScheduledTickManager;
import com.abelian.client.clientRegionTick.ClientScheduledTickManager.ScheduledTickDisplay;
import com.abelian.client.config.RelativityTickClientConfig;
import com.abelian.client.render.RendererUtils;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.abelian.client.render.RendererUtils.renderChunkLines;
import static com.abelian.client.render.RendererUtils.renderTexts;
import static com.abelian.client.RelativityTickClient.selectChunks;

@Mixin(DebugRenderer.class)
public abstract class DebugRendererMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void renderRegionOverlay(
            MatrixStack matrices,
            Frustum frustum,
            VertexConsumerProvider.Immediate vertexConsumers,
            double cameraX, double cameraY, double cameraZ,
            CallbackInfo ci) {
        Vec3d camPos = new Vec3d(cameraX, cameraY, cameraZ);
        MatrixStack overlayMatrices = new MatrixStack();
        overlayMatrices.translate(-camPos.x, -camPos.y, -camPos.z);

        //区域边界框
        if (!selectChunks.isEmpty() || !ClientRegionManager.getRegions().isEmpty()) {
            RenderLayer regionLinesLayer = RendererUtils.getRegionLinesLayer(RelativityTickClientConfig.getRegionLineWidth());
            VertexConsumer lineConsumer = vertexConsumers.getBuffer(regionLinesLayer);
            if (!selectChunks.isEmpty()) {
                float r1 = 1.0F, g1 = 0.5F, b1 = 0.0F;
                for (long posLong : selectChunks) {
                    renderChunkLines(overlayMatrices, lineConsumer, posLong, -64, 320, r1, g1, b1, selectChunks);
                }
            }

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
                    renderChunkLines(overlayMatrices, lineConsumer, posLong, -64, 320, r2, g2, b2, region.getChunkPositions());
                }
            }
            vertexConsumers.draw(regionLinesLayer);
        }

        List<ScheduledTickDisplay> displays = RelativityTickClientConfig.isRenderScheduledTicksEnabled()
                ? ClientScheduledTickManager.getDisplayData()
                : List.of();
        if (!displays.isEmpty()) {
            Map<Vec3d, List<MutableText>> textsByPos = new LinkedHashMap<>();
            for (ScheduledTickDisplay display : displays) {
                List<MutableText> lines = List.of(
                        Text.translatable("relativitytick.scheduled_tick.remaining", display.remainingTick()).formatted(Formatting.RED),
                        Text.translatable("relativitytick.scheduled_tick.sub_order", display.subOrderRank()).formatted(Formatting.GREEN),
                        Text.translatable("relativitytick.scheduled_tick.priority", display.priority()).formatted(Formatting.YELLOW));
                textsByPos.computeIfAbsent(display.pos(), p -> new ArrayList<>()).addAll(lines);
            }
            for (Map.Entry<Vec3d, List<MutableText>> entry : textsByPos.entrySet()) {
                renderTexts(entry.getValue(), entry.getKey(), 0f, overlayMatrices, vertexConsumers);
            }
            vertexConsumers.drawCurrentLayer();
        }
    }
}
