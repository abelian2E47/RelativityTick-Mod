package net.minecraft.client.render.item.model;

import com.mojang.serialization.MapCodec;
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
public class EmptyItemModel implements ItemModel {
    public static final ItemModel INSTANCE = new EmptyItemModel();

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
    }

    @Environment(EnvType.CLIENT)
    public record Unbaked() implements ItemModel.Unbaked {
        public static final MapCodec<EmptyItemModel.Unbaked> CODEC = MapCodec.unit(EmptyItemModel.Unbaked::new);

        @Override
        public void resolve(ResolvableModel.Resolver resolver) {
        }

        @Override
        public ItemModel bake(ItemModel.BakeContext context) {
            return EmptyItemModel.INSTANCE;
        }

        @Override
        public MapCodec<EmptyItemModel.Unbaked> getCodec() {
            return CODEC;
        }
    }
}

