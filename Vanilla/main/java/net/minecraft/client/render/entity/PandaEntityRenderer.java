package net.minecraft.client.render.entity;

import com.google.common.collect.Maps;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.feature.PandaHeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PandaEntityModel;
import net.minecraft.client.render.entity.state.ItemHolderEntityRenderState;
import net.minecraft.client.render.entity.state.PandaEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.PandaEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public class PandaEntityRenderer extends AgeableMobEntityRenderer<PandaEntity, PandaEntityRenderState, PandaEntityModel> {
    private static final Map<PandaEntity.Gene, Identifier> TEXTURES = Util.make(Maps.newEnumMap(PandaEntity.Gene.class), map -> {
        map.put(PandaEntity.Gene.NORMAL, Identifier.ofVanilla("textures/entity/panda/panda.png"));
        map.put(PandaEntity.Gene.LAZY, Identifier.ofVanilla("textures/entity/panda/lazy_panda.png"));
        map.put(PandaEntity.Gene.WORRIED, Identifier.ofVanilla("textures/entity/panda/worried_panda.png"));
        map.put(PandaEntity.Gene.PLAYFUL, Identifier.ofVanilla("textures/entity/panda/playful_panda.png"));
        map.put(PandaEntity.Gene.BROWN, Identifier.ofVanilla("textures/entity/panda/brown_panda.png"));
        map.put(PandaEntity.Gene.WEAK, Identifier.ofVanilla("textures/entity/panda/weak_panda.png"));
        map.put(PandaEntity.Gene.AGGRESSIVE, Identifier.ofVanilla("textures/entity/panda/aggressive_panda.png"));
    });

    public PandaEntityRenderer(EntityRendererFactory.Context context) {
        super(
            context, new PandaEntityModel(context.getPart(EntityModelLayers.PANDA)), new PandaEntityModel(context.getPart(EntityModelLayers.PANDA_BABY)), 0.9F
        );
        this.addFeature(new PandaHeldItemFeatureRenderer(this));
    }

    public Identifier getTexture(PandaEntityRenderState pandaEntityRenderState) {
        return TEXTURES.getOrDefault(pandaEntityRenderState.gene, TEXTURES.get(PandaEntity.Gene.NORMAL));
    }

    public PandaEntityRenderState createRenderState() {
        return new PandaEntityRenderState();
    }

    public void updateRenderState(PandaEntity pandaEntity, PandaEntityRenderState pandaEntityRenderState, float f) {
        super.updateRenderState(pandaEntity, pandaEntityRenderState, f);
        ItemHolderEntityRenderState.update(pandaEntity, pandaEntityRenderState, this.itemModelResolver);
        pandaEntityRenderState.gene = pandaEntity.getProductGene();
        pandaEntityRenderState.askingForBamboo = pandaEntity.getAskForBambooTicks() > 0;
        pandaEntityRenderState.sneezing = pandaEntity.isSneezing();
        pandaEntityRenderState.sneezeProgress = pandaEntity.getSneezeProgress();
        pandaEntityRenderState.eating = pandaEntity.isEating();
        pandaEntityRenderState.scaredByThunderstorm = pandaEntity.isScaredByThunderstorm();
        pandaEntityRenderState.sitting = pandaEntity.isSitting();
        pandaEntityRenderState.sittingAnimationProgress = pandaEntity.getSittingAnimationProgress(f);
        pandaEntityRenderState.lieOnBackAnimationProgress = pandaEntity.getLieOnBackAnimationProgress(f);
        pandaEntityRenderState.rollOverAnimationProgress = pandaEntity.isBaby() ? 0.0F : pandaEntity.getRollOverAnimationProgress(f);
        pandaEntityRenderState.playingTicks = pandaEntity.playingTicks > 0 ? pandaEntity.playingTicks + f : 0.0F;
    }

    protected void setupTransforms(PandaEntityRenderState pandaEntityRenderState, MatrixStack matrixStack, float f, float g) {
        super.setupTransforms(pandaEntityRenderState, matrixStack, f, g);
        if (pandaEntityRenderState.playingTicks > 0.0F) {
            float h = MathHelper.fractionalPart(pandaEntityRenderState.playingTicks);
            int i = MathHelper.floor(pandaEntityRenderState.playingTicks);
            int j = i + 1;
            float k = 7.0F;
            float l = pandaEntityRenderState.baby ? 0.3F : 0.8F;
            if (i < 8.0F) {
                float m = 90.0F * i / 7.0F;
                float n = 90.0F * j / 7.0F;
                float o = this.getAngle(m, n, j, h, 8.0F);
                matrixStack.translate(0.0F, (l + 0.2F) * (o / 90.0F), 0.0F);
                matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-o));
            } else if (i < 16.0F) {
                float p = (i - 8.0F) / 7.0F;
                float q = 90.0F + 90.0F * p;
                float r = 90.0F + 90.0F * (j - 8.0F) / 7.0F;
                float s = this.getAngle(q, r, j, h, 16.0F);
                matrixStack.translate(0.0F, l + 0.2F + (l - 0.2F) * (s - 90.0F) / 90.0F, 0.0F);
                matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-s));
            } else if (i < 24.0F) {
                float t = (i - 16.0F) / 7.0F;
                float u = 180.0F + 90.0F * t;
                float v = 180.0F + 90.0F * (j - 16.0F) / 7.0F;
                float w = this.getAngle(u, v, j, h, 24.0F);
                matrixStack.translate(0.0F, l + l * (270.0F - w) / 90.0F, 0.0F);
                matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-w));
            } else if (i < 32) {
                float x = (i - 24.0F) / 7.0F;
                float y = 270.0F + 90.0F * x;
                float z = 270.0F + 90.0F * (j - 24.0F) / 7.0F;
                float ab = this.getAngle(y, z, j, h, 32.0F);
                matrixStack.translate(0.0F, l * ((360.0F - ab) / 90.0F), 0.0F);
                matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-ab));
            }
        }

        float bb = pandaEntityRenderState.sittingAnimationProgress;
        if (bb > 0.0F) {
            matrixStack.translate(0.0F, 0.8F * bb, 0.0F);
            matrixStack.multiply(
                RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.lerp(bb, pandaEntityRenderState.pitch, pandaEntityRenderState.pitch + 90.0F))
            );
            matrixStack.translate(0.0F, -1.0F * bb, 0.0F);
            if (pandaEntityRenderState.scaredByThunderstorm) {
                float cb = (float)(Math.cos(pandaEntityRenderState.age * 1.25F) * Math.PI * 0.05F);
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(cb));
                if (pandaEntityRenderState.baby) {
                    matrixStack.translate(0.0F, 0.8F, 0.55F);
                }
            }
        }

        float db = pandaEntityRenderState.lieOnBackAnimationProgress;
        if (db > 0.0F) {
            float eb = pandaEntityRenderState.baby ? 0.5F : 1.3F;
            matrixStack.translate(0.0F, eb * db, 0.0F);
            matrixStack.multiply(
                RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.lerp(db, pandaEntityRenderState.pitch, pandaEntityRenderState.pitch + 180.0F))
            );
        }
    }

    private float getAngle(float f, float g, int i, float h, float j) {
        return i < j ? MathHelper.lerp(h, f, g) : f;
    }
}

