package com.abelian.client.mixin;

import com.abelian.client.clientRegionTick.ClientRegion;
import com.abelian.client.clientRegionTick.ClientRegionManager;
import com.abelian.client.clientRegionTick.ClientScheduledTickManager;
import com.abelian.client.clientRegionTick.ClientScheduledTickManager.ScheduledTickDisplay;
import com.abelian.client.config.RelativityTickClientConfig;
import com.abelian.client.render.RendererUtils;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.world.debug.gizmo.TextGizmo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.abelian.client.render.RendererUtils.renderChunkLines;
import static com.abelian.client.RelativityTickClient.selectChunks;

@Mixin(DebugRenderer.class)
public abstract class DebugRendererMixin {
    //选区橙色
    private static final int SELECTION_COLOR = 0xFFFF8000;
    //运行中紫色 / 受控蓝色 / 已释放绿色
    private static final int RUNNING_COLOR = 0xFFCC33FF;
    private static final int CONTROLLED_COLOR = 0xFF0080FF;
    private static final int RELEASED_COLOR = 0xFF00FF00;
    //计划刻三行文本的颜色，对应原来的 BLUE / GREEN / YELLOW
    private static final int REMAINING_COLOR = 0xFF5555FF;
    private static final int SUB_ORDER_COLOR = 0xFF55FF55;
    private static final int PRIORITY_COLOR = 0xFFFFFF55;

    //1.21.11 的调试渲染改为世界坐标 gizmo（无 MatrixStack / VertexConsumerProvider），此注入点仍在收集作用域内，同帧被绘制
    @Inject(method = "render", at = @At("TAIL"))
    private void renderRegionOverlay(Frustum frustum, double cameraX, double cameraY, double cameraZ, float tickProgress, CallbackInfo ci) {
        float lineWidth = (float) RelativityTickClientConfig.getRegionLineWidth();

        //区域边界框
        if (!selectChunks.isEmpty()) {
            for (long posLong : selectChunks) {
                renderChunkLines(posLong, -64, 320, SELECTION_COLOR, lineWidth, selectChunks);
            }
        }

        for (ClientRegion region : ClientRegionManager.getRegions()) {
            if (region.getChunkPositions() == null || region.getDimension().isEmpty()) continue;

            int color;
            if (region.isRunning()) {
                color = RUNNING_COLOR;
            } else if (region.isControlled()) {
                color = CONTROLLED_COLOR;
            } else {
                color = RELEASED_COLOR;
            }

            for (long posLong : region.getChunkPositions()) {
                renderChunkLines(posLong, -64, 320, color, lineWidth, region.getChunkPositions());
            }
        }

        List<ScheduledTickDisplay> displays = RelativityTickClientConfig.isRenderScheduledTicksEnabled()
                ? ClientScheduledTickManager.getDisplayData()
                : List.of();
        if (!displays.isEmpty()) {
            float scale = (float) RelativityTickClientConfig.getScheduledTickTextScale();
            Map<Vec3d, List<ScheduledTickDisplay>> displaysByPos = new LinkedHashMap<>();
            for (ScheduledTickDisplay display : displays) {
                displaysByPos.computeIfAbsent(display.pos(), p -> new ArrayList<>()).add(display);
            }
            for (Map.Entry<Vec3d, List<ScheduledTickDisplay>> entry : displaysByPos.entrySet()) {
                renderScheduledTickTexts(entry.getKey(), entry.getValue(), scale);
            }
        }
    }

    //每个计划刻画三行（剩余刻 / 次序 / 优先级），自下而上排布，颜色与旧实现一致
    private static void renderScheduledTickTexts(Vec3d pos, List<ScheduledTickDisplay> displays, float scale) {
        double lineStep = 10.0 * scale;
        int line = 0;
        for (int i = displays.size() - 1; i >= 0; i--) {
            ScheduledTickDisplay display = displays.get(i);
            drawLabel(pos, lineStep, line++, Text.translatable("relativitytick.scheduled_tick.priority", display.priority()).getString(), PRIORITY_COLOR, scale);
            drawLabel(pos, lineStep, line++, Text.translatable("relativitytick.scheduled_tick.sub_order", display.subOrderRank()).getString(), SUB_ORDER_COLOR, scale);
            drawLabel(pos, lineStep, line++, Text.translatable("relativitytick.scheduled_tick.remaining", display.remainingTick()).getString(), REMAINING_COLOR, scale);
        }
    }

    private static void drawLabel(Vec3d pos, double lineStep, int line, String text, int color, float scale) {
        Vec3d linePos = new Vec3d(pos.x, pos.y + line * lineStep, pos.z);
        GizmoDrawing.text(text, linePos, TextGizmo.Style.centered(color).scaled(scale)).ignoreOcclusion();
    }
}
