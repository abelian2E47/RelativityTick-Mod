package net.minecraft.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.Direction;

@Environment(EnvType.CLIENT)
public class ShulkerBoxBlockEntityRenderer implements BlockEntityRenderer<ShulkerBoxBlockEntity> {
    private final ShulkerBoxBlockEntityRenderer.ShulkerBoxBlockModel model;

    public ShulkerBoxBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        this(ctx.getLoadedEntityModels());
    }

    public ShulkerBoxBlockEntityRenderer(LoadedEntityModels models) {
        this.model = new ShulkerBoxBlockEntityRenderer.ShulkerBoxBlockModel(models.getModelPart(EntityModelLayers.SHULKER_BOX));
    }

    public void render(
        ShulkerBoxBlockEntity shulkerBoxBlockEntity, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, int j
    ) {
        Direction direction = shulkerBoxBlockEntity.getCachedState().get(ShulkerBoxBlock.FACING, Direction.UP);
        DyeColor dyeColor = shulkerBoxBlockEntity.getColor();
        SpriteIdentifier spriteIdentifier;
        if (dyeColor == null) {
            spriteIdentifier = TexturedRenderLayers.SHULKER_TEXTURE_ID;
        } else {
            spriteIdentifier = TexturedRenderLayers.getShulkerBoxTextureId(dyeColor);
        }

        float g = shulkerBoxBlockEntity.getAnimationProgress(f);
        this.render(matrixStack, vertexConsumerProvider, i, j, direction, g, spriteIdentifier);
    }

    public void render(
        MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, Direction facing, float openness, SpriteIdentifier textureId
    ) {
        matrices.push();
        matrices.translate(0.5F, 0.5F, 0.5F);
        float f = 0.9995F;
        matrices.scale(0.9995F, 0.9995F, 0.9995F);
        matrices.multiply(facing.getRotationQuaternion());
        matrices.scale(1.0F, -1.0F, -1.0F);
        matrices.translate(0.0F, -1.0F, 0.0F);
        this.model.animateLid(openness);
        VertexConsumer vertexConsumer = textureId.getVertexConsumer(vertexConsumers, this.model::getLayer);
        this.model.render(matrices, vertexConsumer, light, overlay);
        matrices.pop();
    }

    @Environment(EnvType.CLIENT)
    static class ShulkerBoxBlockModel extends Model {
        private final ModelPart lid;

        public ShulkerBoxBlockModel(ModelPart root) {
            super(root, RenderLayer::getEntityCutoutNoCull);
            this.lid = root.getChild("lid");
        }

        public void animateLid(float openness) {
            this.lid.setPivot(0.0F, 24.0F - openness * 0.5F * 16.0F, 0.0F);
            this.lid.yaw = 270.0F * openness * (float) (Math.PI / 180.0);
        }
    }
}

