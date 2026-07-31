package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingHorseEntityRenderState;
import net.minecraft.entity.passive.AbstractHorseEntity;

@Environment(EnvType.CLIENT)
public abstract class AbstractHorseEntityRenderer<T extends AbstractHorseEntity, S extends LivingHorseEntityRenderState, M extends EntityModel<? super S>>
    extends AgeableMobEntityRenderer<T, S, M> {
    public AbstractHorseEntityRenderer(EntityRendererFactory.Context context, M model, M babyModel) {
        super(context, model, babyModel, 0.75F);
    }

    public void updateRenderState(T abstractHorseEntity, S livingHorseEntityRenderState, float f) {
        super.updateRenderState(abstractHorseEntity, livingHorseEntityRenderState, f);
        livingHorseEntityRenderState.saddled = abstractHorseEntity.isSaddled();
        livingHorseEntityRenderState.hasPassengers = abstractHorseEntity.hasPassengers();
        livingHorseEntityRenderState.eatingGrassAnimationProgress = abstractHorseEntity.getEatingGrassAnimationProgress(f);
        livingHorseEntityRenderState.angryAnimationProgress = abstractHorseEntity.getAngryAnimationProgress(f);
        livingHorseEntityRenderState.eatingAnimationProgress = abstractHorseEntity.getEatingAnimationProgress(f);
        livingHorseEntityRenderState.waggingTail = abstractHorseEntity.tailWagTicks > 0;
    }
}

