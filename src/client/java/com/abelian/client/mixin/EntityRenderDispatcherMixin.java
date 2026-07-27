package com.abelian.client.mixin;

import com.abelian.client.clientRegionTick.ClientRegion;
import com.abelian.client.clientRegionTick.ClientRegionManager;
import com.abelian.client.render.EntityInterpolationManager;
import com.abelian.client.render.RegionTickDeltaManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Shadow public Camera camera;

    @ModifyArgs(method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V"))
    private void adjustPosition(Args args) {
        Entity entity = args.get(0);
        if (entity instanceof PlayerEntity) return;
        Entity vehicle = entity.getVehicle();
        boolean isPassenger = vehicle != null && !vehicle.isRemoved() && vehicle.hasPassenger(entity);
        Entity regionAnchor = isPassenger ? vehicle : entity;

        ChunkPos entityChunkPos = regionAnchor.getChunkPos();
        ClientRegion region = regionAnchor.getWorld() instanceof ClientWorld world ? ClientRegionManager.getRegion(world, entityChunkPos) : null;
        if (region != null && region.isControlled()) {
            String regionID = region.getId();
            float tickDelta = RegionTickDeltaManager.getTickDelta(regionID);
            if (!region.isRunning() && !RegionTickDeltaManager.hasActiveInterpolation(regionID)) {
                args.set(4, 1.0f);
                return;
            }
            args.set(4, tickDelta);
            Vec3d camPos = this.camera.getPos();
            Vec3d worldRenderPos = isPassenger
                    ? getPassengerRenderPos(entity, vehicle, regionID, tickDelta)
                    : EntityInterpolationManager.getInterpolatedEntityPos(entity, regionID, tickDelta);
            Vec3d relativePos = worldRenderPos.subtract(camPos);
            args.set(1, relativePos.x);
            args.set(2, relativePos.y);
            args.set(3, relativePos.z);
        }
    }

    //乘客特殊处理
    @Unique
    private static Vec3d getPassengerRenderPos(Entity passenger, Entity vehicle, String regionID, float tickDelta) {
        Vec3d vehicleRenderPos = EntityInterpolationManager.getInterpolatedEntityPos(vehicle, regionID, tickDelta);
        Vec3d passengerOffset = vehicle.getPassengerRidingPos(passenger)
                .subtract(vehicle.getPos())
                .subtract(passenger.getVehicleAttachmentPos(vehicle));
        return vehicleRenderPos.add(passengerOffset);
    }
}
