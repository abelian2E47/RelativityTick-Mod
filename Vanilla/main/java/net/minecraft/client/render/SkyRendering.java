package net.minecraft.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class SkyRendering implements AutoCloseable {
    private static final Identifier SUN_TEXTURE = Identifier.ofVanilla("textures/environment/sun.png");
    private static final Identifier MOON_PHASES_TEXTURE = Identifier.ofVanilla("textures/environment/moon_phases.png");
    public static final Identifier END_SKY_TEXTURE = Identifier.ofVanilla("textures/environment/end_sky.png");
    private static final float field_53144 = 512.0F;
    private final VertexBuffer starBuffer = VertexBuffer.createAndUpload(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION, this::tessellateStar);
    private final VertexBuffer skyBuffer = VertexBuffer.createAndUpload(
        VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION, vertexConsumer -> this.tessellateSky(vertexConsumer, 16.0F)
    );
    private final VertexBuffer darkSkyBuffer = VertexBuffer.createAndUpload(
        VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION, vertexConsumer -> this.tessellateSky(vertexConsumer, -16.0F)
    );
    private final VertexBuffer endSkyBuffer = VertexBuffer.createAndUpload(
        VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR, this::tessellateEndSky
    );

    private void tessellateStar(VertexConsumer vertexConsumer) {
        Random random = Random.create(10842L);
        int i = 1500;
        float f = 100.0F;

        for (int j = 0; j < 1500; j++) {
            float g = random.nextFloat() * 2.0F - 1.0F;
            float h = random.nextFloat() * 2.0F - 1.0F;
            float k = random.nextFloat() * 2.0F - 1.0F;
            float l = 0.15F + random.nextFloat() * 0.1F;
            float m = MathHelper.magnitude(g, h, k);
            if (!(m <= 0.010000001F) && !(m >= 1.0F)) {
                Vector3f vector3f = new Vector3f(g, h, k).normalize(100.0F);
                float n = (float)(random.nextDouble() * (float) Math.PI * 2.0);
                Matrix3f matrix3f = new Matrix3f().rotateTowards(new Vector3f(vector3f).negate(), new Vector3f(0.0F, 1.0F, 0.0F)).rotateZ(-n);
                vertexConsumer.vertex(new Vector3f(l, -l, 0.0F).mul(matrix3f).add(vector3f));
                vertexConsumer.vertex(new Vector3f(l, l, 0.0F).mul(matrix3f).add(vector3f));
                vertexConsumer.vertex(new Vector3f(-l, l, 0.0F).mul(matrix3f).add(vector3f));
                vertexConsumer.vertex(new Vector3f(-l, -l, 0.0F).mul(matrix3f).add(vector3f));
            }
        }
    }

    private void tessellateSky(VertexConsumer vertexConsumer, float height) {
        float f = Math.signum(height) * 512.0F;
        vertexConsumer.vertex(0.0F, height, 0.0F);

        for (int i = -180; i <= 180; i += 45) {
            vertexConsumer.vertex(f * MathHelper.cos(i * (float) (Math.PI / 180.0)), height, 512.0F * MathHelper.sin(i * (float) (Math.PI / 180.0)));
        }
    }

    public void renderSky(float red, float green, float blue) {
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        this.skyBuffer.draw(RenderLayer.getSky());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void renderSkyDark(MatrixStack matrices) {
        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
        matrices.push();
        matrices.translate(0.0F, 12.0F, 0.0F);
        this.darkSkyBuffer.draw(RenderLayer.getSky());
        matrices.pop();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void renderCelestialBodies(
        MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers, float rot, int phase, float alpha, float starBrightness, Fog fog
    ) {
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rot * 360.0F));
        this.renderSun(alpha, vertexConsumers, matrices);
        this.renderMoon(phase, alpha, vertexConsumers, matrices);
        vertexConsumers.draw();
        if (starBrightness > 0.0F) {
            this.renderStars(fog, starBrightness, matrices);
        }

        matrices.pop();
    }

    private void renderSun(float alpha, VertexConsumerProvider vertexConsumers, MatrixStack matrices) {
        float f = 30.0F;
        float g = 100.0F;
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getCelestial(SUN_TEXTURE));
        int i = ColorHelper.getWhite(alpha);
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        vertexConsumer.vertex(matrix4f, -30.0F, 100.0F, -30.0F).texture(0.0F, 0.0F).color(i);
        vertexConsumer.vertex(matrix4f, 30.0F, 100.0F, -30.0F).texture(1.0F, 0.0F).color(i);
        vertexConsumer.vertex(matrix4f, 30.0F, 100.0F, 30.0F).texture(1.0F, 1.0F).color(i);
        vertexConsumer.vertex(matrix4f, -30.0F, 100.0F, 30.0F).texture(0.0F, 1.0F).color(i);
    }

    private void renderMoon(int phase, float alpha, VertexConsumerProvider vertexConsumers, MatrixStack matrices) {
        float f = 20.0F;
        int i = phase % 4;
        int j = phase / 4 % 2;
        float g = (i + 0) / 4.0F;
        float h = (j + 0) / 2.0F;
        float k = (i + 1) / 4.0F;
        float l = (j + 1) / 2.0F;
        float m = 100.0F;
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getCelestial(MOON_PHASES_TEXTURE));
        int n = ColorHelper.getWhite(alpha);
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        vertexConsumer.vertex(matrix4f, -20.0F, -100.0F, 20.0F).texture(k, l).color(n);
        vertexConsumer.vertex(matrix4f, 20.0F, -100.0F, 20.0F).texture(g, l).color(n);
        vertexConsumer.vertex(matrix4f, 20.0F, -100.0F, -20.0F).texture(g, h).color(n);
        vertexConsumer.vertex(matrix4f, -20.0F, -100.0F, -20.0F).texture(k, h).color(n);
    }

    private void renderStars(Fog fog, float color, MatrixStack matrices) {
        Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
        matrix4fStack.pushMatrix();
        matrix4fStack.mul(matrices.peek().getPositionMatrix());
        RenderSystem.setShaderColor(color, color, color, color);
        RenderSystem.setShaderFog(Fog.DUMMY);
        this.starBuffer.draw(RenderLayer.getStars());
        RenderSystem.setShaderFog(fog);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        matrix4fStack.popMatrix();
    }

    public void renderGlowingSky(MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers, float angleRadians, int color) {
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
        float f = MathHelper.sin(angleRadians) < 0.0F ? 180.0F : 0.0F;
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90.0F));
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getSunriseSunset());
        float g = ColorHelper.getAlphaFloat(color);
        vertexConsumer.vertex(matrix4f, 0.0F, 100.0F, 0.0F).color(color);
        int i = ColorHelper.zeroAlpha(color);
        int j = 16;

        for (int k = 0; k <= 16; k++) {
            float h = k * (float) (Math.PI * 2) / 16.0F;
            float l = MathHelper.sin(h);
            float m = MathHelper.cos(h);
            vertexConsumer.vertex(matrix4f, l * 120.0F, m * 120.0F, -m * 40.0F * g).color(i);
        }

        matrices.pop();
    }

    private void tessellateEndSky(VertexConsumer vertexConsumer) {
        for (int i = 0; i < 6; i++) {
            Matrix4f matrix4f = new Matrix4f();
            switch (i) {
                case 1:
                    matrix4f.rotationX((float) (Math.PI / 2));
                    break;
                case 2:
                    matrix4f.rotationX((float) (-Math.PI / 2));
                    break;
                case 3:
                    matrix4f.rotationX((float) Math.PI);
                    break;
                case 4:
                    matrix4f.rotationZ((float) (Math.PI / 2));
                    break;
                case 5:
                    matrix4f.rotationZ((float) (-Math.PI / 2));
            }

            vertexConsumer.vertex(matrix4f, -100.0F, -100.0F, -100.0F).texture(0.0F, 0.0F).color(-14145496);
            vertexConsumer.vertex(matrix4f, -100.0F, -100.0F, 100.0F).texture(0.0F, 16.0F).color(-14145496);
            vertexConsumer.vertex(matrix4f, 100.0F, -100.0F, 100.0F).texture(16.0F, 16.0F).color(-14145496);
            vertexConsumer.vertex(matrix4f, 100.0F, -100.0F, -100.0F).texture(16.0F, 0.0F).color(-14145496);
        }
    }

    public void renderEndSky() {
        this.endSkyBuffer.draw(RenderLayer.getEndSky());
    }

    @Override
    public void close() {
        this.starBuffer.close();
        this.skyBuffer.close();
        this.darkSkyBuffer.close();
        this.endSkyBuffer.close();
    }
}

