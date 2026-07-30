package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.state.ItemStackEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.OminousItemSpawnerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public class OminousItemSpawnerEntityRenderer extends EntityRenderer<OminousItemSpawnerEntity, ItemStackEntityRenderState> {
    private static final float field_50231 = 40.0F;
    private static final int field_50232 = 50;
    private final ItemModelManager itemModelManager;
    private final Random random = Random.create();

    protected OminousItemSpawnerEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemModelManager = context.getItemModelManager();
    }

    public ItemStackEntityRenderState createRenderState() {
        return new ItemStackEntityRenderState();
    }

    public void updateRenderState(OminousItemSpawnerEntity ominousItemSpawnerEntity, ItemStackEntityRenderState itemStackEntityRenderState, float f) {
        super.updateRenderState(ominousItemSpawnerEntity, itemStackEntityRenderState, f);
        ItemStack itemStack = ominousItemSpawnerEntity.getItem();
        itemStackEntityRenderState.update(ominousItemSpawnerEntity, itemStack, this.itemModelManager);
    }

    public void render(ItemStackEntityRenderState itemStackEntityRenderState, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        if (!itemStackEntityRenderState.itemRenderState.isEmpty()) {
            matrixStack.push();
            if (itemStackEntityRenderState.age <= 50.0F) {
                float f = Math.min(itemStackEntityRenderState.age, 50.0F) / 50.0F;
                matrixStack.scale(f, f, f);
            }

            float g = MathHelper.wrapDegrees(itemStackEntityRenderState.age * 40.0F);
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(g));
            ItemEntityRenderer.renderStack(matrixStack, vertexConsumerProvider, 15728880, itemStackEntityRenderState, this.random);
            matrixStack.pop();
        }
    }
}

