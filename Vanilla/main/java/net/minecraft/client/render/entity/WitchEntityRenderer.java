package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.feature.WitchHeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.WitchEntityModel;
import net.minecraft.client.render.entity.state.ItemHolderEntityRenderState;
import net.minecraft.client.render.entity.state.WitchEntityRenderState;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class WitchEntityRenderer extends MobEntityRenderer<WitchEntity, WitchEntityRenderState, WitchEntityModel> {
    private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/witch.png");

    public WitchEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new WitchEntityModel(context.getPart(EntityModelLayers.WITCH)), 0.5F);
        this.addFeature(new WitchHeldItemFeatureRenderer(this));
    }

    public Identifier getTexture(WitchEntityRenderState witchEntityRenderState) {
        return TEXTURE;
    }

    public WitchEntityRenderState createRenderState() {
        return new WitchEntityRenderState();
    }

    public void updateRenderState(WitchEntity witchEntity, WitchEntityRenderState witchEntityRenderState, float f) {
        super.updateRenderState(witchEntity, witchEntityRenderState, f);
        ItemHolderEntityRenderState.update(witchEntity, witchEntityRenderState, this.itemModelResolver);
        witchEntityRenderState.id = witchEntity.getId();
        ItemStack itemStack = witchEntity.getMainHandStack();
        witchEntityRenderState.holdingItem = !itemStack.isEmpty();
        witchEntityRenderState.holdingPotion = itemStack.isOf(Items.POTION);
    }
}

