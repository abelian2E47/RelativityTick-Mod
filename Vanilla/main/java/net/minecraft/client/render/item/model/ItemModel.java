package net.minecraft.client.render.item.model;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.Baker;
import net.minecraft.client.render.model.ModelRotation;
import net.minecraft.client.render.model.ResolvableModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public interface ItemModel {
    void update(
        ItemRenderState state,
        ItemStack stack,
        ItemModelManager resolver,
        ModelTransformationMode transformationMode,
        @Nullable ClientWorld world,
        @Nullable LivingEntity user,
        int seed
    );

    @Environment(EnvType.CLIENT)
    record BakeContext(Baker blockModelBaker, LoadedEntityModels entityModelSet, ItemModel missingItemModel) {
        public BakedModel bake(Identifier id) {
            return this.blockModelBaker().bake(id, ModelRotation.X0_Y0);
        }
    }

    @Environment(EnvType.CLIENT)
    interface Unbaked extends ResolvableModel {
        MapCodec<? extends ItemModel.Unbaked> getCodec();

        ItemModel bake(ItemModel.BakeContext context);
    }
}

