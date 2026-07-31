package net.minecraft.client.render.item.model.special;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BedBlockEntityRenderer;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class BedModelRenderer implements SimpleSpecialModelRenderer {
    private final BedBlockEntityRenderer blockEntityRenderer;
    private final SpriteIdentifier textureId;

    public BedModelRenderer(BedBlockEntityRenderer blockEntityRenderer, SpriteIdentifier textureId) {
        this.blockEntityRenderer = blockEntityRenderer;
        this.textureId = textureId;
    }

    @Override
    public void render(
        ModelTransformationMode modelTransformationMode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, boolean glint
    ) {
        this.blockEntityRenderer.renderAsItem(matrices, vertexConsumers, light, overlay, this.textureId);
    }

    @Environment(EnvType.CLIENT)
    public record Unbaked(Identifier texture) implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<BedModelRenderer.Unbaked> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(Identifier.CODEC.fieldOf("texture").forGetter(BedModelRenderer.Unbaked::texture))
                .apply(instance, BedModelRenderer.Unbaked::new)
        );

        public Unbaked(DyeColor color) {
            this(TexturedRenderLayers.createColorId(color));
        }

        @Override
        public MapCodec<BedModelRenderer.Unbaked> getCodec() {
            return CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(LoadedEntityModels entityModels) {
            return new BedModelRenderer(new BedBlockEntityRenderer(entityModels), TexturedRenderLayers.createBedTextureId(this.texture));
        }
    }
}

