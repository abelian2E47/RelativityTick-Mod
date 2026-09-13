package com.abelian.client.mixin;

import com.abelian.client.clientRegionTick.ClientRegion;
import com.abelian.client.clientRegionTick.ClientRegionManager;
import com.abelian.client.config.RelativityTickClientConfig;
import com.abelian.client.render.EntityInterpolationManager;
import com.abelian.client.render.RegionTickDeltaManager;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//1.21.11 的实体渲染改为先收集 EntityRenderState（世界坐标 x/y/z 由 EntityRenderer.updateRenderState 用 tickProgress 插值），故插值钩子挂在 EntityRenderManager.getAndUpdateRenderState 上
@Mixin(EntityRenderManager.class)
public class EntityRenderManagerMixin {

    //先把该实体所在受控区域的 tickDelta 换进 tickProgress，使原版按区域节奏插值（肢体动作等同样受影响）
    @ModifyVariable(method = "getAndUpdateRenderState", at = @At("HEAD"), argsOnly = true, index = 2)
    private float replaceTickProgress(float tickProgress, Entity entity) {
        if (!RelativityTickClientConfig.isEntityRenderInterpolationEnabled()) {
            return isControlled(entity) == null ? tickProgress : 1.0f;
        }
        ClientRegion region = isControlled(entity);
        if (region == null) return tickProgress;
        return RegionTickDeltaManager.getTickDelta(region.getId());
    }

    //再用本模组维护的区间覆盖渲染坐标（此处为世界坐标，相机偏移由 WorldRenderer 在提交渲染命令时统一减去）
    @Inject(method = "getAndUpdateRenderState", at = @At("RETURN"))
    private void adjustRenderStatePosition(Entity entity, float tickProgress, CallbackInfoReturnable<EntityRenderState> cir) {
        ClientRegion region = isControlled(entity);
        if (region == null) return;

        EntityRenderState state = cir.getReturnValue();
        if (state == null) return;

        Entity vehicle = entity.getVehicle();
        boolean isPassenger = vehicle != null && !vehicle.isRemoved() && vehicle.hasPassenger(entity);
        Entity regionAnchor = isPassenger ? vehicle : entity;

        //关闭插值时渲染实体在当前tick的服务端坐标
        if (!RelativityTickClientConfig.isEntityRenderInterpolationEnabled()) {
            Vec3d exactPos = entity.getEntityPos();
            state.x = exactPos.x;
            state.y = exactPos.y;
            state.z = exactPos.z;
            return;
        }

        String regionID = region.getId();
        EntityInterpolationManager.EntityRenderInterpolation anchorInterpolation = EntityInterpolationManager.getInterpolation(regionAnchor, regionID);
        if (anchorInterpolation == null) return;

        float tickDelta = RegionTickDeltaManager.getTickDelta(regionID);
        Vec3d worldRenderPos = isPassenger
                ? getPassengerRenderPos(entity, vehicle, anchorInterpolation, tickDelta)
                : EntityInterpolationManager.interpolate(anchorInterpolation, tickDelta);
        state.x = worldRenderPos.x;
        state.y = worldRenderPos.y;
        state.z = worldRenderPos.z;
    }

    //取该实体（乘客则取载具）所在的受控区域，非受控或玩家返回 null
    @Unique
    private static ClientRegion isControlled(Entity entity) {
        if (entity instanceof PlayerEntity) return null;
        Entity vehicle = entity.getVehicle();
        boolean isPassenger = vehicle != null && !vehicle.isRemoved() && vehicle.hasPassenger(entity);
        Entity regionAnchor = isPassenger ? vehicle : entity;

        ChunkPos entityChunkPos = regionAnchor.getChunkPos();
        ClientRegion region = regionAnchor.getEntityWorld() instanceof ClientWorld world
                ? ClientRegionManager.getRegion(world, entityChunkPos)
                : null;
        return region != null && region.isControlled() ? region : null;
    }

    //乘客特殊处理
    @Unique
    private static Vec3d getPassengerRenderPos(Entity passenger, Entity vehicle,
                                               EntityInterpolationManager.EntityRenderInterpolation vehicleInterpolation,
                                               float tickDelta) {
        Vec3d vehicleRenderPos = EntityInterpolationManager.interpolate(vehicleInterpolation, tickDelta);
        Vec3d passengerOffset = vehicle.getPassengerRidingPos(passenger)
                .subtract(vehicle.getEntityPos())
                .subtract(passenger.getVehicleAttachmentPos(vehicle));
        return vehicleRenderPos.add(passengerOffset);
    }
}
