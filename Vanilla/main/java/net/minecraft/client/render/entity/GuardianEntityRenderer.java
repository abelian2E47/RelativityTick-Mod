package net.minecraft.client.render.entity;

import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.GuardianEntityModel;
import net.minecraft.client.render.entity.state.GuardianEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public class GuardianEntityRenderer extends MobEntityRenderer<GuardianEntity, GuardianEntityRenderState, GuardianEntityModel> {
    private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/guardian.png");
    private static final Identifier EXPLOSION_BEAM_TEXTURE = Identifier.ofVanilla("textures/entity/guardian_beam.png");
    private static final RenderLayer LAYER = RenderLayer.getEntityCutoutNoCull(EXPLOSION_BEAM_TEXTURE);

    public GuardianEntityRenderer(EntityRendererFactory.Context context) {
        this(context, 0.5F, EntityModelLayers.GUARDIAN);
    }

    protected GuardianEntityRenderer(EntityRendererFactory.Context ctx, float shadowRadius, EntityModelLayer layer) {
        super(ctx, new GuardianEntityModel(ctx.getPart(layer)), shadowRadius);
    }

    public boolean shouldRender(GuardianEntity guardianEntity, Frustum frustum, double d, double e, double f) {
        if (super.shouldRender(guardianEntity, frustum, d, e, f)) {
            return true;
        }

        if (guardianEntity.hasBeamTarget()) {
            LivingEntity livingEntity = guardianEntity.getBeamTarget();
            if (livingEntity != null) {
                Vec3d vec3d = this.fromLerpedPosition(livingEntity, livingEntity.getHeight() * 0.5, 1.0F);
                Vec3d vec3d2 = this.fromLerpedPosition(guardianEntity, guardianEntity.getStandingEyeHeight(), 1.0F);
                return frustum.isVisible(new Box(vec3d2.x, vec3d2.y, vec3d2.z, vec3d.x, vec3d.y, vec3d.z));
            }
        }

        return false;
    }

    private Vec3d fromLerpedPosition(LivingEntity entity, double yOffset, float delta) {
        double d = MathHelper.lerp(delta, entity.lastRenderX, entity.getX());
        double e = MathHelper.lerp(delta, entity.lastRenderY, entity.getY()) + yOffset;
        double f = MathHelper.lerp(delta, entity.lastRenderZ, entity.getZ());
        return new Vec3d(d, e, f);
    }

    public void render(GuardianEntityRenderState guardianEntityRenderState, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        super.render(guardianEntityRenderState, matrixStack, vertexConsumerProvider, i);
        Vec3d vec3d = guardianEntityRenderState.beamTargetPos;
        if (vec3d != null) {
            float f = guardianEntityRenderState.beamTicks * 0.5F % 1.0F;
            matrixStack.push();
            matrixStack.translate(0.0F, guardianEntityRenderState.standingEyeHeight, 0.0F);
            renderBeam(
                matrixStack,
                vertexConsumerProvider.getBuffer(LAYER),
                vec3d.subtract(guardianEntityRenderState.cameraPosVec),
                guardianEntityRenderState.beamTicks,
                guardianEntityRenderState.beamProgress,
                f
            );
            matrixStack.pop();
        }
    }

    private static void renderBeam(MatrixStack matrices, VertexConsumer vertexConsumer, Vec3d vec3d, float beamTicks, float f, float g) {
        float h = (float)(vec3d.length() + 1.0);
        vec3d = vec3d.normalize();
        float i = (float)Math.acos(vec3d.y);
        float j = (float) (Math.PI / 2) - (float)Math.atan2(vec3d.z, vec3d.x);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j * (180.0F / (float)Math.PI)));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(i * (180.0F / (float)Math.PI)));
        float k = beamTicks * 0.05F * -1.5F;
        float l = f * f;
        int m = 64 + (int)(l * 191.0F);
        int n = 32 + (int)(l * 191.0F);
        int o = 128 - (int)(l * 64.0F);
        float p = 0.2F;
        float q = 0.282F;
        float r = MathHelper.cos(k + (float) (Math.PI * 3.0 / 4.0)) * 0.282F;
        float s = MathHelper.sin(k + (float) (Math.PI * 3.0 / 4.0)) * 0.282F;
        float t = MathHelper.cos(k + (float) (Math.PI / 4)) * 0.282F;
        float u = MathHelper.sin(k + (float) (Math.PI / 4)) * 0.282F;
        float v = MathHelper.cos(k + ((float) Math.PI * 5.0F / 4.0F)) * 0.282F;
        float w = MathHelper.sin(k + ((float) Math.PI * 5.0F / 4.0F)) * 0.282F;
        float x = MathHelper.cos(k + ((float) Math.PI * 7.0F / 4.0F)) * 0.282F;
        float y = MathHelper.sin(k + ((float) Math.PI * 7.0F / 4.0F)) * 0.282F;
        float z = MathHelper.cos(k + (float) Math.PI) * 0.2F;
        float ab = MathHelper.sin(k + (float) Math.PI) * 0.2F;
        float bb = MathHelper.cos(k + 0.0F) * 0.2F;
        float cb = MathHelper.sin(k + 0.0F) * 0.2F;
        float db = MathHelper.cos(k + (float) (Math.PI / 2)) * 0.2F;
        float eb = MathHelper.sin(k + (float) (Math.PI / 2)) * 0.2F;
        float fb = MathHelper.cos(k + (float) (Math.PI * 3.0 / 2.0)) * 0.2F;
        float gb = MathHelper.sin(k + (float) (Math.PI * 3.0 / 2.0)) * 0.2F;
        float hb = h;
        float ib = 0.0F;
        float jb = 0.4999F;
        float kb = -1.0F + g;
        float lb = kb + h * 2.5F;
        MatrixStack.Entry entry = matrices.peek();
        vertex(vertexConsumer, entry, z, hb, ab, m, n, o, 0.4999F, lb);
        vertex(vertexConsumer, entry, z, 0.0F, ab, m, n, o, 0.4999F, kb);
        vertex(vertexConsumer, entry, bb, 0.0F, cb, m, n, o, 0.0F, kb);
        vertex(vertexConsumer, entry, bb, hb, cb, m, n, o, 0.0F, lb);
        vertex(vertexConsumer, entry, db, hb, eb, m, n, o, 0.4999F, lb);
        vertex(vertexConsumer, entry, db, 0.0F, eb, m, n, o, 0.4999F, kb);
        vertex(vertexConsumer, entry, fb, 0.0F, gb, m, n, o, 0.0F, kb);
        vertex(vertexConsumer, entry, fb, hb, gb, m, n, o, 0.0F, lb);
        float mb = MathHelper.floor(beamTicks) % 2 == 0 ? 0.5F : 0.0F;
        vertex(vertexConsumer, entry, r, hb, s, m, n, o, 0.5F, mb + 0.5F);
        vertex(vertexConsumer, entry, t, hb, u, m, n, o, 1.0F, mb + 0.5F);
        vertex(vertexConsumer, entry, x, hb, y, m, n, o, 1.0F, mb);
        vertex(vertexConsumer, entry, v, hb, w, m, n, o, 0.5F, mb);
    }

    private static void vertex(
        VertexConsumer vertexConsumer, MatrixStack.Entry matrix, float x, float y, float z, int red, int green, int blue, float u, float v
    ) {
        vertexConsumer.vertex(matrix, x, y, z)
            .color(red, green, blue, 255)
            .texture(u, v)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
            .normal(matrix, 0.0F, 1.0F, 0.0F);
    }

    public Identifier getTexture(GuardianEntityRenderState guardianEntityRenderState) {
        return TEXTURE;
    }

    public GuardianEntityRenderState createRenderState() {
        return new GuardianEntityRenderState();
    }

    public void updateRenderState(GuardianEntity guardianEntity, GuardianEntityRenderState guardianEntityRenderState, float f) {
        super.updateRenderState(guardianEntity, guardianEntityRenderState, f);
        guardianEntityRenderState.spikesExtension = guardianEntity.getSpikesExtension(f);
        guardianEntityRenderState.tailAngle = guardianEntity.getTailAngle(f);
        guardianEntityRenderState.cameraPosVec = guardianEntity.getCameraPosVec(f);
        Entity entity = getBeamTarget(guardianEntity);
        if (entity != null) {
            guardianEntityRenderState.rotationVec = guardianEntity.getRotationVec(f);
            guardianEntityRenderState.lookAtPos = entity.getCameraPosVec(f);
        } else {
            guardianEntityRenderState.rotationVec = null;
            guardianEntityRenderState.lookAtPos = null;
        }

        LivingEntity livingEntity = guardianEntity.getBeamTarget();
        if (livingEntity != null) {
            guardianEntityRenderState.beamProgress = guardianEntity.getBeamProgress(f);
            guardianEntityRenderState.beamTicks = guardianEntity.getBeamTicks() + f;
            guardianEntityRenderState.beamTargetPos = this.fromLerpedPosition(livingEntity, livingEntity.getHeight() * 0.5, f);
        } else {
            guardianEntityRenderState.beamTargetPos = null;
        }
    }

    @Nullable
    private static Entity getBeamTarget(GuardianEntity guardian) {
        Entity entity = MinecraftClient.getInstance().getCameraEntity();
        return guardian.hasBeamTarget() ? guardian.getBeamTarget() : entity;
    }
}

