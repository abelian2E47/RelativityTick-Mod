package net.minecraft.client.render.entity.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.FoxEntityModel;
import net.minecraft.client.render.entity.state.FoxEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public class FoxHeldItemFeatureRenderer extends FeatureRenderer<FoxEntityRenderState, FoxEntityModel> {
    public FoxHeldItemFeatureRenderer(FeatureRendererContext<FoxEntityRenderState, FoxEntityModel> featureRendererContext) {
        super(featureRendererContext);
    }

    public void render(
        MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, FoxEntityRenderState foxEntityRenderState, float f, float g
    ) {
        ItemRenderState itemRenderState = foxEntityRenderState.itemRenderState;
        if (!itemRenderState.isEmpty()) {
            boolean bl = foxEntityRenderState.sleeping;
            boolean bl2 = foxEntityRenderState.baby;
            matrixStack.push();
            matrixStack.translate(
                this.getContextModel().head.pivotX / 16.0F, this.getContextModel().head.pivotY / 16.0F, this.getContextModel().head.pivotZ / 16.0F
            );
            if (bl2) {
                float h = 0.75F;
                matrixStack.scale(0.75F, 0.75F, 0.75F);
            }

            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotation(foxEntityRenderState.headRoll));
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f));
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g));
            if (foxEntityRenderState.baby) {
                if (bl) {
                    matrixStack.translate(0.4F, 0.26F, 0.15F);
                } else {
                    matrixStack.translate(0.06F, 0.26F, -0.5F);
                }
            } else if (bl) {
                matrixStack.translate(0.46F, 0.26F, 0.22F);
            } else {
                matrixStack.translate(0.06F, 0.27F, -0.5F);
            }

            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
            if (bl) {
                matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90.0F));
            }

            itemRenderState.render(matrixStack, vertexConsumerProvider, i, OverlayTexture.DEFAULT_UV);
            matrixStack.pop();
        }
    }
}

