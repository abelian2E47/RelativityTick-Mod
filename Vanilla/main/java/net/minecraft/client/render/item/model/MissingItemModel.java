package net.minecraft.client.render.item.model;

import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;

@Environment(EnvType.CLIENT)
public class MissingItemModel implements ItemModel {
    private final BakedModel missingModel;

    public MissingItemModel(BakedModel missingModel) {
        this.missingModel = missingModel;
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
        state.newLayer().setModel(this.missingModel, TexturedRenderLayers.getEntityCutout());
    }
}

