package net.minecraft.client.render.item.model.special;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.entity.model.TridentEntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ModelTransformationMode;

@Environment(EnvType.CLIENT)
public class TridentModelRenderer implements SimpleSpecialModelRenderer {
    private final TridentEntityModel model;

    public TridentModelRenderer(TridentEntityModel model) {
        this.model = model;
    }

    @Override
    public void render(
        ModelTransformationMode modelTransformationMode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, boolean glint
    ) {
        matrices.push();
        matrices.scale(1.0F, -1.0F, -1.0F);
        VertexConsumer vertexConsumer = ItemRenderer.getItemGlintConsumer(vertexConsumers, this.model.getLayer(TridentEntityModel.TEXTURE), false, glint);
        this.model.render(matrices, vertexConsumer, light, overlay);
        matrices.pop();
    }

    @Environment(EnvType.CLIENT)
    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<TridentModelRenderer.Unbaked> CODEC = MapCodec.unit(new TridentModelRenderer.Unbaked());

        @Override
        public MapCodec<TridentModelRenderer.Unbaked> getCodec() {
            return CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(LoadedEntityModels entityModels) {
            return new TridentModelRenderer(new TridentEntityModel(entityModels.getModelPart(EntityModelLayers.TRIDENT)));
        }
    }
}

