package net.minecraft.client.render.block.entity;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.WallSkullBlock;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.DragonHeadEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.entity.model.PiglinHeadEntityModel;
import net.minecraft.client.render.entity.model.SkullEntityModel;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationPropertyHelper;

@Environment(EnvType.CLIENT)
public class SkullBlockEntityRenderer implements BlockEntityRenderer<SkullBlockEntity> {
    private final Function<SkullBlock.SkullType, SkullBlockEntityModel> models;
    private static final Map<SkullBlock.SkullType, Identifier> TEXTURES = Util.make(Maps.newHashMap(), map -> {
        map.put(SkullBlock.Type.SKELETON, Identifier.ofVanilla("textures/entity/skeleton/skeleton.png"));
        map.put(SkullBlock.Type.WITHER_SKELETON, Identifier.ofVanilla("textures/entity/skeleton/wither_skeleton.png"));
        map.put(SkullBlock.Type.ZOMBIE, Identifier.ofVanilla("textures/entity/zombie/zombie.png"));
        map.put(SkullBlock.Type.CREEPER, Identifier.ofVanilla("textures/entity/creeper/creeper.png"));
        map.put(SkullBlock.Type.DRAGON, Identifier.ofVanilla("textures/entity/enderdragon/dragon.png"));
        map.put(SkullBlock.Type.PIGLIN, Identifier.ofVanilla("textures/entity/piglin/piglin.png"));
        map.put(SkullBlock.Type.PLAYER, DefaultSkinHelper.getTexture());
    });

    @Nullable
    public static SkullBlockEntityModel getModels(LoadedEntityModels models, SkullBlock.SkullType type) {
        if (type instanceof SkullBlock.Type type2) {
            return switch (type2) {
                case SKELETON -> new SkullEntityModel(models.getModelPart(EntityModelLayers.SKELETON_SKULL));
                case WITHER_SKELETON -> new SkullEntityModel(models.getModelPart(EntityModelLayers.WITHER_SKELETON_SKULL));
                case PLAYER -> new SkullEntityModel(models.getModelPart(EntityModelLayers.PLAYER_HEAD));
                case ZOMBIE -> new SkullEntityModel(models.getModelPart(EntityModelLayers.ZOMBIE_HEAD));
                case CREEPER -> new SkullEntityModel(models.getModelPart(EntityModelLayers.CREEPER_HEAD));
                case DRAGON -> new DragonHeadEntityModel(models.getModelPart(EntityModelLayers.DRAGON_SKULL));
                case PIGLIN -> new PiglinHeadEntityModel(models.getModelPart(EntityModelLayers.PIGLIN_HEAD));
            };
        } else {
            return null;
        }
    }

    public SkullBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        LoadedEntityModels loadedEntityModels = context.getLoadedEntityModels();
        this.models = Util.memoize(type -> getModels(loadedEntityModels, type));
    }

    public void render(SkullBlockEntity skullBlockEntity, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, int j) {
        float g = skullBlockEntity.getPoweredTicks(f);
        BlockState blockState = skullBlockEntity.getCachedState();
        boolean bl = blockState.getBlock() instanceof WallSkullBlock;
        Direction direction = bl ? blockState.get(WallSkullBlock.FACING) : null;
        int k = bl ? RotationPropertyHelper.fromDirection(direction.getOpposite()) : blockState.get(SkullBlock.ROTATION);
        float h = RotationPropertyHelper.toDegrees(k);
        SkullBlock.SkullType skullType = ((AbstractSkullBlock)blockState.getBlock()).getSkullType();
        SkullBlockEntityModel skullBlockEntityModel = this.models.apply(skullType);
        RenderLayer renderLayer = getRenderLayer(skullType, skullBlockEntity.getOwner());
        renderSkull(direction, h, g, matrixStack, vertexConsumerProvider, i, skullBlockEntityModel, renderLayer);
    }

    public static void renderSkull(
        @Nullable Direction direction,
        float yaw,
        float animationProgress,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light,
        SkullBlockEntityModel model,
        RenderLayer renderLayer
    ) {
        matrices.push();
        if (direction == null) {
            matrices.translate(0.5F, 0.0F, 0.5F);
        } else {
            float f = 0.25F;
            matrices.translate(0.5F - direction.getOffsetX() * 0.25F, 0.25F, 0.5F - direction.getOffsetZ() * 0.25F);
        }

        matrices.scale(-1.0F, -1.0F, 1.0F);
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);
        model.setHeadRotation(animationProgress, yaw, 0.0F);
        model.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
        matrices.pop();
    }

    public static RenderLayer getRenderLayer(SkullBlock.SkullType type, @Nullable ProfileComponent profile) {
        return getRenderLayer(type, profile, null);
    }

    public static RenderLayer getRenderLayer(SkullBlock.SkullType type, @Nullable ProfileComponent profile, @Nullable Identifier texture) {
        return type == SkullBlock.Type.PLAYER && profile != null
            ? RenderLayer.getEntityTranslucent(
                texture != null ? texture : MinecraftClient.getInstance().getSkinProvider().getSkinTextures(profile.gameProfile()).texture()
            )
            : RenderLayer.getEntityCutoutNoCullZOffset(texture != null ? texture : TEXTURES.get(type));
    }
}

