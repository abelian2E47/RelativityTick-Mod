package net.minecraft.client.render.item.model.special;

import com.mojang.serialization.MapCodec;
import java.util.Objects;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BannerBlockEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.entity.model.ShieldEntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.DyeColor;

@Environment(EnvType.CLIENT)
public class ShieldModelRenderer implements SpecialModelRenderer<ComponentMap> {
    private final ShieldEntityModel model;

    public ShieldModelRenderer(ShieldEntityModel model) {
        this.model = model;
    }

    @Nullable
    public ComponentMap getData(ItemStack itemStack) {
        return itemStack.getImmutableComponents();
    }

    public void render(
        @Nullable ComponentMap componentMap,
        ModelTransformationMode modelTransformationMode,
        MatrixStack matrixStack,
        VertexConsumerProvider vertexConsumerProvider,
        int i,
        int j,
        boolean bl
    ) {
        BannerPatternsComponent bannerPatternsComponent = componentMap != null
            ? componentMap.getOrDefault(DataComponentTypes.BANNER_PATTERNS, BannerPatternsComponent.DEFAULT)
            : BannerPatternsComponent.DEFAULT;
        DyeColor dyeColor = componentMap != null ? componentMap.get(DataComponentTypes.BASE_COLOR) : null;
        boolean bl2 = !bannerPatternsComponent.layers().isEmpty() || dyeColor != null;
        matrixStack.push();
        matrixStack.scale(1.0F, -1.0F, -1.0F);
        SpriteIdentifier spriteIdentifier = bl2 ? ModelBaker.SHIELD_BASE : ModelBaker.SHIELD_BASE_NO_PATTERN;
        VertexConsumer vertexConsumer = spriteIdentifier.getSprite()
            .getTextureSpecificVertexConsumer(
                ItemRenderer.getItemGlintConsumer(
                    vertexConsumerProvider, this.model.getLayer(spriteIdentifier.getAtlasId()), modelTransformationMode == ModelTransformationMode.GUI, bl
                )
            );
        this.model.getHandle().render(matrixStack, vertexConsumer, i, j);
        if (bl2) {
            BannerBlockEntityRenderer.renderCanvas(
                matrixStack,
                vertexConsumerProvider,
                i,
                j,
                this.model.getPlate(),
                spriteIdentifier,
                false,
                Objects.requireNonNullElse(dyeColor, DyeColor.WHITE),
                bannerPatternsComponent,
                bl,
                false
            );
        } else {
            this.model.getPlate().render(matrixStack, vertexConsumer, i, j);
        }

        matrixStack.pop();
    }

    @Environment(EnvType.CLIENT)
    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final ShieldModelRenderer.Unbaked INSTANCE = new ShieldModelRenderer.Unbaked();
        public static final MapCodec<ShieldModelRenderer.Unbaked> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public MapCodec<ShieldModelRenderer.Unbaked> getCodec() {
            return CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(LoadedEntityModels entityModels) {
            return new ShieldModelRenderer(new ShieldEntityModel(entityModels.getModelPart(EntityModelLayers.SHIELD)));
        }
    }
}

