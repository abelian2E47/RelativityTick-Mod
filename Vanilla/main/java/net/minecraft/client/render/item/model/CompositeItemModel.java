package net.minecraft.client.render.item.model;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.ResolvableModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;

@Environment(EnvType.CLIENT)
public class CompositeItemModel implements ItemModel {
    private final List<ItemModel> models;

    public CompositeItemModel(List<ItemModel> models) {
        this.models = models;
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
        state.addLayers(this.models.size());

        for (ItemModel itemModel : this.models) {
            itemModel.update(state, stack, resolver, transformationMode, world, user, seed);
        }
    }

    @Environment(EnvType.CLIENT)
    public record Unbaked(List<ItemModel.Unbaked> models) implements ItemModel.Unbaked {
        public static final MapCodec<CompositeItemModel.Unbaked> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(ItemModelTypes.CODEC.listOf().fieldOf("models").forGetter(CompositeItemModel.Unbaked::models))
                .apply(instance, CompositeItemModel.Unbaked::new)
        );

        @Override
        public MapCodec<CompositeItemModel.Unbaked> getCodec() {
            return CODEC;
        }

        @Override
        public void resolve(ResolvableModel.Resolver resolver) {
            for (ItemModel.Unbaked unbaked : this.models) {
                unbaked.resolve(resolver);
            }
        }

        @Override
        public ItemModel bake(ItemModel.BakeContext context) {
            return new CompositeItemModel(this.models.stream().map(model -> model.bake(context)).toList());
        }
    }
}

