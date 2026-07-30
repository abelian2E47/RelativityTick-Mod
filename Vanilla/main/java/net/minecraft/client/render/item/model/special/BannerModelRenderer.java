package net.minecraft.client.render.item.model.special;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BannerBlockEntityRenderer;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.DyeColor;

@Environment(EnvType.CLIENT)
public class BannerModelRenderer implements SpecialModelRenderer<BannerPatternsComponent> {
    private final BannerBlockEntityRenderer blockEntityRenderer;
    private final DyeColor baseColor;

    public BannerModelRenderer(DyeColor baseColor, BannerBlockEntityRenderer blockEntityRenderer) {
        this.blockEntityRenderer = blockEntityRenderer;
        this.baseColor = baseColor;
    }

    @Nullable
    public BannerPatternsComponent getData(ItemStack itemStack) {
        return itemStack.get(DataComponentTypes.BANNER_PATTERNS);
    }

    public void render(
        @Nullable BannerPatternsComponent bannerPatternsComponent,
        ModelTransformationMode modelTransformationMode,
        MatrixStack matrixStack,
        VertexConsumerProvider vertexConsumerProvider,
        int i,
        int j,
        boolean bl
    ) {
        this.blockEntityRenderer
            .renderAsItem(
                matrixStack, vertexConsumerProvider, i, j, this.baseColor, Objects.requireNonNullElse(bannerPatternsComponent, BannerPatternsComponent.DEFAULT)
            );
    }

    @Environment(EnvType.CLIENT)
    public record Unbaked(DyeColor baseColor) implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<BannerModelRenderer.Unbaked> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(DyeColor.CODEC.fieldOf("color").forGetter(BannerModelRenderer.Unbaked::baseColor))
                .apply(instance, BannerModelRenderer.Unbaked::new)
        );

        @Override
        public MapCodec<BannerModelRenderer.Unbaked> getCodec() {
            return CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(LoadedEntityModels entityModels) {
            return new BannerModelRenderer(this.baseColor, new BannerBlockEntityRenderer(entityModels));
        }
    }
}

