package net.minecraft.client.render.entity.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.ModelWithArms;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public class HeldItemFeatureRenderer<S extends ArmedEntityRenderState, M extends EntityModel<S> & ModelWithArms> extends FeatureRenderer<S, M> {
    public HeldItemFeatureRenderer(FeatureRendererContext<S, M> featureRendererContext) {
        super(featureRendererContext);
    }

    public void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, S armedEntityRenderState, float f, float g) {
        this.renderItem(armedEntityRenderState, armedEntityRenderState.rightHandItemState, Arm.RIGHT, matrixStack, vertexConsumerProvider, i);
        this.renderItem(armedEntityRenderState, armedEntityRenderState.leftHandItemState, Arm.LEFT, matrixStack, vertexConsumerProvider, i);
    }

    protected void renderItem(S entityState, ItemRenderState itemState, Arm arm, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (!itemState.isEmpty()) {
            matrices.push();
            this.getContextModel().setArmAngle(arm, matrices);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
            boolean bl = arm == Arm.LEFT;
            matrices.translate((bl ? -1 : 1) / 16.0F, 0.125F, -0.625F);
            itemState.render(matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV);
            matrices.pop();
        }
    }
}

