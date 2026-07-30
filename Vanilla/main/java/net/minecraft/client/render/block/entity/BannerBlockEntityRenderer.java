package net.minecraft.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BannerBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.WallBannerBlock;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.model.BannerBlockModel;
import net.minecraft.client.render.block.entity.model.BannerFlagBlockModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.RotationPropertyHelper;

@Environment(EnvType.CLIENT)
public class BannerBlockEntityRenderer implements BlockEntityRenderer<BannerBlockEntity> {
    private static final int ROTATIONS = 16;
    private static final float field_55282 = 0.6666667F;
    private final BannerBlockModel standingModel;
    private final BannerBlockModel wallModel;
    private final BannerFlagBlockModel standingFlagModel;
    private final BannerFlagBlockModel wallFlagModel;

    public BannerBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        this(context.getLoadedEntityModels());
    }

    public BannerBlockEntityRenderer(LoadedEntityModels models) {
        this.standingModel = new BannerBlockModel(models.getModelPart(EntityModelLayers.STANDING_BANNER));
        this.wallModel = new BannerBlockModel(models.getModelPart(EntityModelLayers.WALL_BANNER));
        this.standingFlagModel = new BannerFlagBlockModel(models.getModelPart(EntityModelLayers.STANDING_BANNER_FLAG));
        this.wallFlagModel = new BannerFlagBlockModel(models.getModelPart(EntityModelLayers.WALL_BANNER_FLAG));
    }

    public void render(BannerBlockEntity bannerBlockEntity, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, int j) {
        BlockState blockState = bannerBlockEntity.getCachedState();
        BannerBlockModel bannerBlockModel;
        BannerFlagBlockModel bannerFlagBlockModel;
        float g;
        if (blockState.getBlock() instanceof BannerBlock) {
            g = -RotationPropertyHelper.toDegrees(blockState.get(BannerBlock.ROTATION));
            bannerBlockModel = this.standingModel;
            bannerFlagBlockModel = this.standingFlagModel;
        } else {
            g = -blockState.get(WallBannerBlock.FACING).getPositiveHorizontalDegrees();
            bannerBlockModel = this.wallModel;
            bannerFlagBlockModel = this.wallFlagModel;
        }

        long l = bannerBlockEntity.getWorld().getTime();
        BlockPos blockPos = bannerBlockEntity.getPos();
        float k = ((float)Math.floorMod(blockPos.getX() * 7 + blockPos.getY() * 9 + blockPos.getZ() * 13 + l, 100L) + f) / 100.0F;
        render(
            matrixStack,
            vertexConsumerProvider,
            i,
            j,
            g,
            bannerBlockModel,
            bannerFlagBlockModel,
            k,
            bannerBlockEntity.getColorForState(),
            bannerBlockEntity.getPatterns()
        );
    }

    public void renderAsItem(
        MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, DyeColor baseColor, BannerPatternsComponent patterns
    ) {
        render(matrices, vertexConsumers, light, overlay, 0.0F, this.standingModel, this.standingFlagModel, 0.0F, baseColor, patterns);
    }

    private static void render(
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light,
        int overlay,
        float rotation,
        BannerBlockModel model,
        BannerFlagBlockModel flagModel,
        float sway,
        DyeColor baseColor,
        BannerPatternsComponent patterns
    ) {
        matrices.push();
        matrices.translate(0.5F, 0.0F, 0.5F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
        matrices.scale(0.6666667F, -0.6666667F, -0.6666667F);
        model.render(matrices, ModelBaker.BANNER_BASE.getVertexConsumer(vertexConsumers, RenderLayer::getEntitySolid), light, overlay);
        flagModel.sway(sway);
        renderCanvas(matrices, vertexConsumers, light, overlay, flagModel.getRootPart(), ModelBaker.BANNER_BASE, true, baseColor, patterns);
        matrices.pop();
    }

    public static void renderCanvas(
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light,
        int overlay,
        ModelPart canvas,
        SpriteIdentifier baseSprite,
        boolean isBanner,
        DyeColor color,
        BannerPatternsComponent patterns
    ) {
        renderCanvas(matrices, vertexConsumers, light, overlay, canvas, baseSprite, isBanner, color, patterns, false, true);
    }

    public static void renderCanvas(
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light,
        int overlay,
        ModelPart canvas,
        SpriteIdentifier baseSprite,
        boolean isBanner,
        DyeColor color,
        BannerPatternsComponent patterns,
        boolean glint,
        boolean solid
    ) {
        canvas.render(matrices, baseSprite.getVertexConsumer(vertexConsumers, RenderLayer::getEntitySolid, solid, glint), light, overlay);
        renderLayer(matrices, vertexConsumers, light, overlay, canvas, isBanner ? TexturedRenderLayers.BANNER_BASE : TexturedRenderLayers.SHIELD_BASE, color);

        for (int i = 0; i < 16 && i < patterns.layers().size(); i++) {
            BannerPatternsComponent.Layer layer = patterns.layers().get(i);
            SpriteIdentifier spriteIdentifier = isBanner
                ? TexturedRenderLayers.getBannerPatternTextureId(layer.pattern())
                : TexturedRenderLayers.getShieldPatternTextureId(layer.pattern());
            renderLayer(matrices, vertexConsumers, light, overlay, canvas, spriteIdentifier, layer.color());
        }
    }

    private static void renderLayer(
        MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, ModelPart canvas, SpriteIdentifier textureId, DyeColor color
    ) {
        int i = color.getEntityColor();
        canvas.render(matrices, textureId.getVertexConsumer(vertexConsumers, RenderLayer::getEntityNoOutline), light, overlay, i);
    }
}

