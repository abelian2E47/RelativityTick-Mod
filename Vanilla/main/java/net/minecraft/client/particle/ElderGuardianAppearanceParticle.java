package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ElderGuardianEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.GuardianEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public class ElderGuardianAppearanceParticle extends Particle {
    private final Model model;
    private final RenderLayer layer = RenderLayer.getEntityTranslucent(ElderGuardianEntityRenderer.TEXTURE);

    ElderGuardianAppearanceParticle(ClientWorld clientWorld, double d, double e, double f) {
        super(clientWorld, d, e, f);
        this.model = new GuardianEntityModel(MinecraftClient.getInstance().getLoadedEntityModels().getModelPart(EntityModelLayers.ELDER_GUARDIAN));
        this.gravityStrength = 0.0F;
        this.maxAge = 30;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.CUSTOM;
    }

    @Override
    public void renderCustom(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Camera camera, float tickDelta) {
        float f = (this.age + tickDelta) / this.maxAge;
        float g = 0.05F + 0.5F * MathHelper.sin(f * (float) Math.PI);
        int i = ColorHelper.fromFloats(g, 1.0F, 1.0F, 1.0F);
        matrices.push();
        matrices.multiply(camera.getRotation());
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(60.0F - 150.0F * f));
        float h = 0.42553192F;
        matrices.scale(0.42553192F, -0.42553192F, -0.42553192F);
        matrices.translate(0.0F, -0.56F, 3.5F);
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(this.layer);
        this.model.render(matrices, vertexConsumer, 15728880, OverlayTexture.DEFAULT_UV, i);
        matrices.pop();
    }

    @Override
    public void render(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<SimpleParticleType> {
        public Particle createParticle(
            SimpleParticleType simpleParticleType, ClientWorld clientWorld, double d, double e, double f, double g, double h, double i
        ) {
            return new ElderGuardianAppearanceParticle(clientWorld, d, e, f);
        }
    }
}

