package net.minecraft.client.render.item.model;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.render.item.model.special.SpecialModelTypes;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.ResolvableModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class SpecialItemModel<T> implements ItemModel {
    private final SpecialModelRenderer<T> specialModelType;
    private final BakedModel base;

    public SpecialItemModel(SpecialModelRenderer<T> specialModelType, BakedModel base) {
        this.specialModelType = specialModelType;
        this.base = base;
    }

    @Override
    public void update(
        ItemRenderState state,
        ItemStack stack,
        ItemModelManager resolver,
        ModelTransformationMode transformationMode,
        @Nullable ClientWorld world,
        @Nullable LivingEntity user,
        int seed
    ) {
        ItemRenderState.LayerRenderState layerRenderState = state.newLayer();
        if (stack.hasGlint()) {
            layerRenderState.setGlint(ItemRenderState.Glint.STANDARD);
        }

        layerRenderState.setSpecialModel(this.specialModelType, this.specialModelType.getData(stack), this.base);
    }

    @Environment(EnvType.CLIENT)
    public record Unbaked(Identifier base, SpecialModelRenderer.Unbaked specialModel) implements ItemModel.Unbaked {
        public static final MapCodec<SpecialItemModel.Unbaked> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Identifier.CODEC.fieldOf("base").forGetter(SpecialItemModel.Unbaked::base),
                    SpecialModelTypes.CODEC.fieldOf("model").forGetter(SpecialItemModel.Unbaked::specialModel)
                )
                .apply(instance, SpecialItemModel.Unbaked::new)
        );

        @Override
        public void resolve(ResolvableModel.Resolver resolver) {
            resolver.resolve(this.base);
        }

        @Override
        public ItemModel bake(ItemModel.BakeContext context) {
            BakedModel bakedModel = context.bake(this.base);
            SpecialModelRenderer<?> specialModelRenderer = this.specialModel.bake(context.entityModelSet());
            return specialModelRenderer == null ? context.missingItemModel() : new SpecialItemModel<>(specialModelRenderer, bakedModel);
        }

        @Override
        public MapCodec<SpecialItemModel.Unbaked> getCodec() {
            return CODEC;
        }
    }
}

