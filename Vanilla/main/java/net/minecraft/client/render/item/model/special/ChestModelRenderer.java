package net.minecraft.client.render.item.model.special;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.model.ChestBlockModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class ChestModelRenderer implements SimpleSpecialModelRenderer {
    public static final Identifier CHRISTMAS_ID = Identifier.ofVanilla("christmas");
    public static final Identifier NORMAL_ID = Identifier.ofVanilla("normal");
    public static final Identifier TRAPPED_ID = Identifier.ofVanilla("trapped");
    public static final Identifier ENDER_ID = Identifier.ofVanilla("ender");
    private final ChestBlockModel model;
    private final SpriteIdentifier textureId;
    private final float openness;

    public ChestModelRenderer(ChestBlockModel model, SpriteIdentifier textureId, float openness) {
        this.model = model;
        this.textureId = textureId;
        this.openness = openness;
    }

    @Override
    public void render(
        ModelTransformationMode modelTransformationMode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, boolean glint
    ) {
        VertexConsumer vertexConsumer = this.textureId.getVertexConsumer(vertexConsumers, RenderLayer::getEntitySolid);
        this.model.setLockAndLidPitch(this.openness);
        this.model.render(matrices, vertexConsumer, light, overlay);
    }

    @Environment(EnvType.CLIENT)
    public record Unbaked(Identifier texture, float openness) implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<ChestModelRenderer.Unbaked> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Identifier.CODEC.fieldOf("texture").forGetter(ChestModelRenderer.Unbaked::texture),
                    Codec.FLOAT.optionalFieldOf("openness", 0.0F).forGetter(ChestModelRenderer.Unbaked::openness)
                )
                .apply(instance, ChestModelRenderer.Unbaked::new)
        );

        public Unbaked(Identifier texture) {
            this(texture, 0.0F);
        }

        @Override
        public MapCodec<ChestModelRenderer.Unbaked> getCodec() {
            return CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(LoadedEntityModels entityModels) {
            ChestBlockModel chestBlockModel = new ChestBlockModel(entityModels.getModelPart(EntityModelLayers.CHEST));
            SpriteIdentifier spriteIdentifier = TexturedRenderLayers.createChestTextureId(this.texture);
            return new ChestModelRenderer(chestBlockModel, spriteIdentifier, this.openness);
        }
    }
}

