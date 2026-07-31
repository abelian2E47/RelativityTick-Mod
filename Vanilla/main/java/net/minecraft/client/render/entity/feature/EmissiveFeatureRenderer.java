package net.minecraft.client.render.entity.feature;

import java.util.List;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class EmissiveFeatureRenderer<S extends LivingEntityRenderState, M extends EntityModel<S>> extends FeatureRenderer<S, M> {
    private final Identifier texture;
    private final EmissiveFeatureRenderer.AnimationAlphaAdjuster<S> animationAlphaAdjuster;
    private final EmissiveFeatureRenderer.ModelPartVisibility<S, M> modelPartVisibility;
    private final Function<Identifier, RenderLayer> renderLayerFunction;
    private final boolean ignoresInvisibility;

    public EmissiveFeatureRenderer(
        FeatureRendererContext<S, M> context,
        Identifier texture,
        EmissiveFeatureRenderer.AnimationAlphaAdjuster<S> animationAlphaAdjuster,
        EmissiveFeatureRenderer.ModelPartVisibility<S, M> modelPartVisibility,
        Function<Identifier, RenderLayer> renderLayerFunction,
        boolean ignoresInvisibility
    ) {
        super(context);
        this.texture = texture;
        this.animationAlphaAdjuster = animationAlphaAdjuster;
        this.modelPartVisibility = modelPartVisibility;
        this.renderLayerFunction = renderLayerFunction;
        this.ignoresInvisibility = ignoresInvisibility;
    }

    public void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, S livingEntityRenderState, float f, float g) {
        if (!livingEntityRenderState.invisible || this.ignoresInvisibility) {
            if (this.updateModelPartVisibility(livingEntityRenderState)) {
                VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(this.renderLayerFunction.apply(this.texture));
                float h = this.animationAlphaAdjuster.apply(livingEntityRenderState, livingEntityRenderState.age);
                int j = ColorHelper.getArgb(MathHelper.floor(h * 255.0F), 255, 255, 255);
                this.getContextModel().render(matrixStack, vertexConsumer, i, LivingEntityRenderer.getOverlay(livingEntityRenderState, 0.0F), j);
                this.unhideAllModelParts();
            }
        }
    }

    private boolean updateModelPartVisibility(S state) {
        List<ModelPart> list = this.modelPartVisibility.getPartsToDraw(this.getContextModel(), state);
        if (list.isEmpty()) {
            return false;
        }

        this.getContextModel().getParts().forEach(part -> part.hidden = true);
        list.forEach(part -> part.hidden = false);
        return true;
    }

    private void unhideAllModelParts() {
        this.getContextModel().getParts().forEach(part -> part.hidden = false);
    }

    @Environment(EnvType.CLIENT)
    public interface AnimationAlphaAdjuster<S extends LivingEntityRenderState> {
        float apply(S state, float tickDelta);
    }

    @Environment(EnvType.CLIENT)
    public interface ModelPartVisibility<S extends LivingEntityRenderState, M extends EntityModel<S>> {
        List<ModelPart> getPartsToDraw(M model, S state);
    }
}

