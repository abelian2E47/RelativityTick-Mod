package net.minecraft.client.render.entity.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.DolphinEntityModel;
import net.minecraft.client.render.entity.state.DolphinEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class DolphinHeldItemFeatureRenderer extends FeatureRenderer<DolphinEntityRenderState, DolphinEntityModel> {
    public DolphinHeldItemFeatureRenderer(FeatureRendererContext<DolphinEntityRenderState, DolphinEntityModel> featureRendererContext) {
        super(featureRendererContext);
    }

    public void render(
        MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, DolphinEntityRenderState dolphinEntityRenderState, float f, float g
    ) {
        ItemRenderState itemRenderState = dolphinEntityRenderState.itemRenderState;
        if (!itemRenderState.isEmpty()) {
            matrixStack.push();
            float h = 1.0F;
            float j = -1.0F;
            float k = MathHelper.abs(dolphinEntityRenderState.pitch) / 60.0F;
            if (dolphinEntityRenderState.pitch < 0.0F) {
                matrixStack.translate(0.0F, 1.0F - k * 0.5F, -1.0F + k * 0.5F);
            } else {
                matrixStack.translate(0.0F, 1.0F + k * 0.8F, -1.0F + k * 0.2F);
            }

            itemRenderState.render(matrixStack, vertexConsumerProvider, i, OverlayTexture.DEFAULT_UV);
            matrixStack.pop();
        }
    }
}

