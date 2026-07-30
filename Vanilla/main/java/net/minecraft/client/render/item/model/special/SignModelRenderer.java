package net.minecraft.client.render.item.model.special;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.WoodType;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.SignBlockEntityRenderer;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class SignModelRenderer implements SimpleSpecialModelRenderer {
    private final Model model;
    private final SpriteIdentifier texture;

    public SignModelRenderer(Model model, SpriteIdentifier texture) {
        this.model = model;
        this.texture = texture;
    }

    @Override
    public void render(
        ModelTransformationMode modelTransformationMode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, boolean glint
    ) {
        SignBlockEntityRenderer.renderAsItem(matrices, vertexConsumers, light, overlay, this.model, this.texture);
    }

    @Environment(EnvType.CLIENT)
    public record Unbaked(WoodType woodType, Optional<Identifier> texture) implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<SignModelRenderer.Unbaked> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    WoodType.CODEC.fieldOf("wood_type").forGetter(SignModelRenderer.Unbaked::woodType),
                    Identifier.CODEC.optionalFieldOf("texture").forGetter(SignModelRenderer.Unbaked::texture)
                )
                .apply(instance, SignModelRenderer.Unbaked::new)
        );

        public Unbaked(WoodType woodType) {
            this(woodType, Optional.empty());
        }

        @Override
        public MapCodec<SignModelRenderer.Unbaked> getCodec() {
            return CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(LoadedEntityModels entityModels) {
            Model model = SignBlockEntityRenderer.createSignModel(entityModels, this.woodType, true);
            SpriteIdentifier spriteIdentifier = this.texture
                .<SpriteIdentifier>map(TexturedRenderLayers::createSignTextureId)
                .orElseGet(() -> TexturedRenderLayers.getSignTextureId(this.woodType));
            return new SignModelRenderer(model, spriteIdentifier);
        }
    }
}

