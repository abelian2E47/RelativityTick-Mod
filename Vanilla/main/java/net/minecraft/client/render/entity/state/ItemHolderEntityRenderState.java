package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ModelTransformationMode;

@Environment(EnvType.CLIENT)
public class ItemHolderEntityRenderState extends LivingEntityRenderState {
    public final ItemRenderState itemRenderState = new ItemRenderState();

    public static void update(LivingEntity entity, ItemHolderEntityRenderState state, ItemModelManager itemModelManager) {
        itemModelManager.updateForLivingEntity(state.itemRenderState, entity.getMainHandStack(), ModelTransformationMode.GROUND, false, entity);
    }
}

