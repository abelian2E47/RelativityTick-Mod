package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.state.FlyingItemEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public class FlyingItemEntityRenderer<T extends Entity & FlyingItemEntity> extends EntityRenderer<T, FlyingItemEntityRenderState> {
    private final ItemModelManager itemModelManager;
    private final float scale;
    private final boolean lit;

    public FlyingItemEntityRenderer(EntityRendererFactory.Context ctx, float scale, boolean lit) {
        super(ctx);
        this.itemModelManager = ctx.getItemModelManager();
        this.scale = scale;
        this.lit = lit;
    }

    public FlyingItemEntityRenderer(EntityRendererFactory.Context context) {
        this(context, 1.0F, false);
    }

    @Override
    protected int getBlockLight(T entity, BlockPos pos) {
        return this.lit ? 15 : super.getBlockLight(entity, pos);
    }

    public void render(FlyingItemEntityRenderState flyingItemEntityRenderState, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        matrixStack.push();
        matrixStack.scale(this.scale, this.scale, this.scale);
        matrixStack.multiply(this.dispatcher.getRotation());
        flyingItemEntityRenderState.itemRenderState.render(matrixStack, vertexConsumerProvider, i, OverlayTexture.DEFAULT_UV);
        matrixStack.pop();
        super.render(flyingItemEntityRenderState, matrixStack, vertexConsumerProvider, i);
    }

    public FlyingItemEntityRenderState createRenderState() {
        return new FlyingItemEntityRenderState();
    }

    public void updateRenderState(T entity, FlyingItemEntityRenderState flyingItemEntityRenderState, float f) {
        super.updateRenderState(entity, flyingItemEntityRenderState, f);
        this.itemModelManager.updateForNonLivingEntity(flyingItemEntityRenderState.itemRenderState, entity.getStack(), ModelTransformationMode.GROUND, entity);
    }
}

