package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.state.FireworkRocketEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public class FireworkRocketEntityRenderer extends EntityRenderer<FireworkRocketEntity, FireworkRocketEntityRenderState> {
    private final ItemModelManager itemModelManager;

    public FireworkRocketEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemModelManager = context.getItemModelManager();
    }

    public void render(
        FireworkRocketEntityRenderState fireworkRocketEntityRenderState, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i
    ) {
        matrixStack.push();
        matrixStack.multiply(this.dispatcher.getRotation());
        if (fireworkRocketEntityRenderState.shotAtAngle) {
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
        }

        fireworkRocketEntityRenderState.stack.render(matrixStack, vertexConsumerProvider, i, OverlayTexture.DEFAULT_UV);
        matrixStack.pop();
        super.render(fireworkRocketEntityRenderState, matrixStack, vertexConsumerProvider, i);
    }

    public FireworkRocketEntityRenderState createRenderState() {
        return new FireworkRocketEntityRenderState();
    }

    public void updateRenderState(FireworkRocketEntity fireworkRocketEntity, FireworkRocketEntityRenderState fireworkRocketEntityRenderState, float f) {
        super.updateRenderState(fireworkRocketEntity, fireworkRocketEntityRenderState, f);
        fireworkRocketEntityRenderState.shotAtAngle = fireworkRocketEntity.wasShotAtAngle();
        this.itemModelManager
            .updateForNonLivingEntity(
                fireworkRocketEntityRenderState.stack, fireworkRocketEntity.getStack(), ModelTransformationMode.GROUND, fireworkRocketEntity
            );
    }
}

