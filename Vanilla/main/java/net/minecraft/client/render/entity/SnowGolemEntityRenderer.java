package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.feature.SnowGolemPumpkinFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.SnowGolemEntityModel;
import net.minecraft.client.render.entity.state.SnowGolemEntityRenderState;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class SnowGolemEntityRenderer extends MobEntityRenderer<SnowGolemEntity, SnowGolemEntityRenderState, SnowGolemEntityModel> {
    private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/snow_golem.png");

    public SnowGolemEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new SnowGolemEntityModel(context.getPart(EntityModelLayers.SNOW_GOLEM)), 0.5F);
        this.addFeature(new SnowGolemPumpkinFeatureRenderer(this, context.getBlockRenderManager()));
    }

    public Identifier getTexture(SnowGolemEntityRenderState snowGolemEntityRenderState) {
        return TEXTURE;
    }

    public SnowGolemEntityRenderState createRenderState() {
        return new SnowGolemEntityRenderState();
    }

    public void updateRenderState(SnowGolemEntity snowGolemEntity, SnowGolemEntityRenderState snowGolemEntityRenderState, float f) {
        super.updateRenderState(snowGolemEntity, snowGolemEntityRenderState, f);
        snowGolemEntityRenderState.hasPumpkin = snowGolemEntity.hasPumpkin();
    }
}

