package com.abelian.client.mixin;

import com.abelian.client.clientRegionTick.ClientRegion;
import com.abelian.client.clientRegionTick.ClientRegionManager;
import com.abelian.client.render.EntityInterpolationManager;
import com.abelian.client.render.RegionTickDeltaManager;
import net.minecraft.client.render.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Shadow public Camera camera;

    @Unique
    private static final ThreadLocal<Entity> RELATIVITYTICK_CURRENT_ENTITY = new ThreadLocal<>();

    private static final String RENDER_METHOD = "render(Lnet/minecraft/entity/Entity;DDDFFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V";

    @Inject(method = RENDER_METHOD, at = @At("HEAD"))
    private void captureEntity(Entity entity, double x, double y, double z, float yaw, float tickDelta,
                                MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                CallbackInfo ci) {
        RELATIVITYTICK_CURRENT_ENTITY.set(entity);
    }

    @Inject(method = RENDER_METHOD, at = @At("RETURN"))
    private void releaseEntity(Entity entity, double x, double y, double z, float yaw, float tickDelta,
                               MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                               CallbackInfo ci) {
        RELATIVITYTICK_CURRENT_ENTITY.remove();
    }

    @Redirect(
            method = RENDER_METHOD,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(DDD)V", ordinal = 0)
    )
    private void adjustPosition(MatrixStack matrices, double x, double y, double z) {
        Entity entity = RELATIVITYTICK_CURRENT_ENTITY.get();
        RegionRenderData data = getRegionRenderData(entity);
        if (data == null) {
            matrices.translate(x, y, z);
            return;
        }

        Vec3d relativePos = data.position().subtract(this.camera.getPos());
        matrices.translate(relativePos.x, relativePos.y, relativePos.z);
    }

    @ModifyArgs(
            method = RENDER_METHOD,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EntityRenderer;render(Lnet/minecraft/entity/Entity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V")
    )
    private void adjustTickDelta(Args args) {
        Entity entity = RELATIVITYTICK_CURRENT_ENTITY.get();
        RegionRenderData data = getRegionRenderData(entity);
        if (data != null) {
            args.set(2, data.tickDelta());
        }
    }

    @Unique
    private RegionRenderData getRegionRenderData(Entity entity) {
        if (entity == null || entity instanceof PlayerEntity) return null;

        Entity vehicle = entity.getVehicle();
        boolean isPassenger = vehicle != null && !vehicle.isRemoved() && vehicle.hasPassenger(entity);
        Entity regionAnchor = isPassenger ? vehicle : entity;
        ChunkPos entityChunkPos = regionAnchor.getChunkPos();
        ClientRegion region = regionAnchor.getWorld() instanceof ClientWorld world
                ? ClientRegionManager.getRegion(world, entityChunkPos)
                : null;
        if (region == null || !region.isControlled()) return null;

        float tickDelta = RegionTickDeltaManager.getTickDelta(region.getId());
        Vec3d position = !region.isRunning() && !RegionTickDeltaManager.hasActiveInterpolation(region.getId())
                ? entity.getPos()
                : isPassenger
                        ? getPassengerRenderPos(entity, vehicle, region.getId(), tickDelta)
                        : EntityInterpolationManager.getInterpolatedEntityPos(entity, region.getId(), tickDelta);
        return new RegionRenderData(position, !region.isRunning() && !RegionTickDeltaManager.hasActiveInterpolation(region.getId()) ? 1.0f : tickDelta);
    }

    @Unique
    private static Vec3d getPassengerRenderPos(Entity passenger, Entity vehicle, String regionID, float tickDelta) {
        Vec3d vehicleRenderPos = EntityInterpolationManager.getInterpolatedEntityPos(vehicle, regionID, tickDelta);
        Vec3d passengerOffset = vehicle.getPassengerRidingPos(passenger)
                .subtract(vehicle.getPos())
                .subtract(passenger.getVehicleAttachmentPos(vehicle));
        return vehicleRenderPos.add(passengerOffset);
    }

    @Unique
    private record RegionRenderData(Vec3d position, float tickDelta) {}
}
