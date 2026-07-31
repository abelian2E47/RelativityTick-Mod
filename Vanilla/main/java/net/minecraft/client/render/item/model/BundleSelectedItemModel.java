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
import net.minecraft.item.BundleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;

@Environment(EnvType.CLIENT)
public class BundleSelectedItemModel implements ItemModel {
    static final ItemModel INSTANCE = new BundleSelectedItemModel();

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
        ItemStack itemStack = BundleItem.getSelectedStack(stack);
        if (!itemStack.isEmpty()) {
            resolver.update(state, itemStack, transformationMode, world, user, seed);
        }
    }

    @Environment(EnvType.CLIENT)
    public record Unbaked() implements ItemModel.Unbaked {
        public static final MapCodec<BundleSelectedItemModel.Unbaked> CODEC = MapCodec.unit(new BundleSelectedItemModel.Unbaked());

        @Override
        public MapCodec<BundleSelectedItemModel.Unbaked> getCodec() {
            return CODEC;
        }

        @Override
        public ItemModel bake(ItemModel.BakeContext context) {
            return BundleSelectedItemModel.INSTANCE;
        }

        @Override
        public void resolve(ResolvableModel.Resolver resolver) {
        }
    }
}

