package com.abelian.client.mixin;

import com.abelian.client.clientRegionTick.ClientRegion;
import com.abelian.client.clientRegionTick.ClientRegionManager;
import com.abelian.client.config.RelativityTickClientConfig;
import com.abelian.client.render.EntityInterpolationManager;
import com.abelian.client.render.RegionTickDeltaManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @ModifyArgs(method = "renderEntity(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;render(Lnet/minecraft/entity/Entity;DDDFFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"))
    private void adjustPosition(Args args) {
        Entity entity = args.get(0);
        if (entity instanceof PlayerEntity) return;
        Entity vehicle = entity.getVehicle();
        boolean isPassenger = vehicle != null && !vehicle.isRemoved() && vehicle.hasPassenger(entity);
        Entity regionAnchor = isPassenger ? vehicle : entity;

        ChunkPos entityChunkPos = regionAnchor.getChunkPos();
        ClientRegion region = regionAnchor.getWorld() instanceof ClientWorld world ? ClientRegionManager.getRegion(world, entityChunkPos) : null;
        if (region == null || !region.isControlled()) return;

        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        //关闭插值时渲染实体在当前tick的服务端坐标
        if (!RelativityTickClientConfig.isEntityRenderInterpolationEnabled()) {
            Vec3d exactPos = entity.getPos();
            args.set(1, exactPos.x - camPos.x);
            args.set(2, exactPos.y - camPos.y);
            args.set(3, exactPos.z - camPos.z);
            args.set(5, 1.0f);
            return;
        }

        String regionID = region.getId();
        EntityInterpolationManager.EntityRenderInterpolation anchorInterpolation = EntityInterpolationManager.getInterpolation(regionAnchor, regionID);
        if (anchorInterpolation == null) return;

        float tickDelta = RegionTickDeltaManager.getTickDelta(regionID);
        args.set(5, tickDelta);

        Vec3d worldRenderPos = isPassenger
                ? getPassengerRenderPos(entity, vehicle, anchorInterpolation, tickDelta)
                : EntityInterpolationManager.interpolate(anchorInterpolation, tickDelta);
        Vec3d relativePos = worldRenderPos.subtract(camPos);
        args.set(1, relativePos.x);
        args.set(2, relativePos.y);
        args.set(3, relativePos.z);
    }

    //乘客特殊处理
    @Unique
    private static Vec3d getPassengerRenderPos(Entity passenger, Entity vehicle,
                                               EntityInterpolationManager.EntityRenderInterpolation vehicleInterpolation,
                                               float tickDelta) {
        Vec3d vehicleRenderPos = EntityInterpolationManager.interpolate(vehicleInterpolation, tickDelta);
        Vec3d passengerOffset = vehicle.getPassengerRidingPos(passenger)
                .subtract(vehicle.getPos())
                .subtract(passenger.getVehicleAttachmentPos(vehicle));
        return vehicleRenderPos.add(passengerOffset);
    }
}
