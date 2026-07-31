package net.minecraft.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.VaultBlockEntity;
import net.minecraft.block.vault.VaultClientData;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemStackEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public class VaultBlockEntityRenderer implements BlockEntityRenderer<VaultBlockEntity> {
    private final ItemModelManager itemModelManager;
    private final Random random = Random.create();
    private final ItemStackEntityRenderState itemRenderState = new ItemStackEntityRenderState();

    public VaultBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        this.itemModelManager = context.getItemModelManager();
    }

    public void render(VaultBlockEntity vaultBlockEntity, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, int j) {
        if (VaultBlockEntity.Client.hasDisplayItem(vaultBlockEntity.getSharedData())) {
            World world = vaultBlockEntity.getWorld();
            if (world != null) {
                ItemStack itemStack = vaultBlockEntity.getSharedData().getDisplayItem();
                if (!itemStack.isEmpty()) {
                    this.itemModelManager.update(this.itemRenderState.itemRenderState, itemStack, ModelTransformationMode.GROUND, false, world, null, 0);
                    this.itemRenderState.renderedAmount = ItemStackEntityRenderState.getRenderedAmount(itemStack.getCount());
                    this.itemRenderState.seed = ItemStackEntityRenderState.getSeed(itemStack);
                    VaultClientData vaultClientData = vaultBlockEntity.getClientData();
                    matrixStack.push();
                    matrixStack.translate(0.5F, 0.4F, 0.5F);
                    matrixStack.multiply(
                        RotationAxis.POSITIVE_Y
                            .rotationDegrees(MathHelper.lerpAngleDegrees(f, vaultClientData.getPreviousDisplayRotation(), vaultClientData.getDisplayRotation()))
                    );
                    ItemEntityRenderer.renderStack(matrixStack, vertexConsumerProvider, i, this.itemRenderState, this.random);
                    matrixStack.pop();
                }
            }
        }
    }
}

