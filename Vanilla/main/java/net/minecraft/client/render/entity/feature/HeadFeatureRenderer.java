package net.minecraft.client.render.entity.feature;

import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.entity.model.ModelWithHead;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Util;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public class HeadFeatureRenderer<S extends LivingEntityRenderState, M extends EntityModel<S> & ModelWithHead> extends FeatureRenderer<S, M> {
    private static final float field_53209 = 0.625F;
    private static final float field_53210 = 1.1875F;
    private final HeadFeatureRenderer.HeadTransformation headTransformation;
    private final Function<SkullBlock.SkullType, SkullBlockEntityModel> headModels;

    public HeadFeatureRenderer(FeatureRendererContext<S, M> context, LoadedEntityModels models) {
        this(context, models, HeadFeatureRenderer.HeadTransformation.DEFAULT);
    }

    public HeadFeatureRenderer(FeatureRendererContext<S, M> context, LoadedEntityModels models, HeadFeatureRenderer.HeadTransformation headTransformation) {
        super(context);
        this.headTransformation = headTransformation;
        this.headModels = Util.memoize(type -> SkullBlockEntityRenderer.getModels(models, type));
    }

    public void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, S livingEntityRenderState, float f, float g) {
        if (!livingEntityRenderState.headItemRenderState.isEmpty() || livingEntityRenderState.wearingSkullType != null) {
            matrixStack.push();
            matrixStack.scale(this.headTransformation.horizontalScale(), 1.0F, this.headTransformation.horizontalScale());
            M entityModel = this.getContextModel();
            entityModel.getRootPart().rotate(matrixStack);
            entityModel.getHead().rotate(matrixStack);
            if (livingEntityRenderState.wearingSkullType != null) {
                matrixStack.translate(0.0F, this.headTransformation.skullYOffset(), 0.0F);
                matrixStack.scale(1.1875F, -1.1875F, -1.1875F);
                matrixStack.translate(-0.5, 0.0, -0.5);
                SkullBlock.SkullType skullType = livingEntityRenderState.wearingSkullType;
                SkullBlockEntityModel skullBlockEntityModel = this.headModels.apply(skullType);
                RenderLayer renderLayer = SkullBlockEntityRenderer.getRenderLayer(skullType, livingEntityRenderState.wearingSkullProfile);
                SkullBlockEntityRenderer.renderSkull(
                    null, 180.0F, livingEntityRenderState.headItemAnimationProgress, matrixStack, vertexConsumerProvider, i, skullBlockEntityModel, renderLayer
                );
            } else {
                translate(matrixStack, this.headTransformation);
                livingEntityRenderState.headItemRenderState.render(matrixStack, vertexConsumerProvider, i, OverlayTexture.DEFAULT_UV);
            }

            matrixStack.pop();
        }
    }

    public static void translate(MatrixStack matrices, HeadFeatureRenderer.HeadTransformation transformation) {
        matrices.translate(0.0F, -0.25F + transformation.yOffset(), 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
        matrices.scale(0.625F, -0.625F, -0.625F);
    }

    @Environment(EnvType.CLIENT)
    public record HeadTransformation(float yOffset, float skullYOffset, float horizontalScale) {
        public static final HeadFeatureRenderer.HeadTransformation DEFAULT = new HeadFeatureRenderer.HeadTransformation(0.0F, 0.0F, 1.0F);
    }
}

