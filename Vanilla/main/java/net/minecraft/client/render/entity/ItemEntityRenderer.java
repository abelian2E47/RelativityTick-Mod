package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.render.entity.state.ItemStackEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public class ItemEntityRenderer extends EntityRenderer<ItemEntity, ItemEntityRenderState> {
    private static final float field_32924 = 0.15F;
    private static final float field_32929 = 0.0F;
    private static final float field_32930 = 0.0F;
    private static final float field_32931 = 0.09375F;
    private final ItemModelManager itemModelManager;
    private final Random random = Random.create();

    public ItemEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemModelManager = context.getItemModelManager();
        this.shadowRadius = 0.15F;
        this.shadowOpacity = 0.75F;
    }

    public ItemEntityRenderState createRenderState() {
        return new ItemEntityRenderState();
    }

    public void updateRenderState(ItemEntity itemEntity, ItemEntityRenderState itemEntityRenderState, float f) {
        super.updateRenderState(itemEntity, itemEntityRenderState, f);
        itemEntityRenderState.age = itemEntity.getItemAge() + f;
        itemEntityRenderState.uniqueOffset = itemEntity.uniqueOffset;
        itemEntityRenderState.update(itemEntity, itemEntity.getStack(), this.itemModelManager);
    }

    public void render(ItemEntityRenderState itemEntityRenderState, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        if (!itemEntityRenderState.itemRenderState.isEmpty()) {
            matrixStack.push();
            float f = 0.25F;
            float g = MathHelper.sin(itemEntityRenderState.age / 10.0F + itemEntityRenderState.uniqueOffset) * 0.1F + 0.1F;
            float h = itemEntityRenderState.itemRenderState.getTransformation().scale.y();
            matrixStack.translate(0.0F, g + 0.25F * h, 0.0F);
            float j = ItemEntity.getRotation(itemEntityRenderState.age, itemEntityRenderState.uniqueOffset);
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotation(j));
            renderStack(matrixStack, vertexConsumerProvider, i, itemEntityRenderState, this.random);
            matrixStack.pop();
            super.render(itemEntityRenderState, matrixStack, vertexConsumerProvider, i);
        }
    }

    public static void renderStack(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, ItemStackEntityRenderState state, Random random) {
        random.setSeed(state.seed);
        int i = state.renderedAmount;
        ItemRenderState itemRenderState = state.itemRenderState;
        boolean bl = itemRenderState.hasDepth();
        float f = itemRenderState.getTransformation().scale.x();
        float g = itemRenderState.getTransformation().scale.y();
        float h = itemRenderState.getTransformation().scale.z();
        if (!bl) {
            float j = -0.0F * (i - 1) * 0.5F * f;
            float k = -0.0F * (i - 1) * 0.5F * g;
            float l = -0.09375F * (i - 1) * 0.5F * h;
            matrices.translate(j, k, l);
        }

        for (int m = 0; m < i; m++) {
            matrices.push();
            if (m > 0) {
                if (bl) {
                    float n = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                    float o = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                    float p = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                    matrices.translate(n, o, p);
                } else {
                    float q = (random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                    float r = (random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                    matrices.translate(q, r, 0.0F);
                }
            }

            itemRenderState.render(matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV);
            matrices.pop();
            if (!bl) {
                matrices.translate(0.0F * f, 0.0F * g, 0.09375F * h);
            }
        }
    }
}

