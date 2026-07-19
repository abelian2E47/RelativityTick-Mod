package com.abelian.client.mixin;

import com.abelian.client.ClientRegionManager;
import com.abelian.client.ClientRegionTicker;
import com.abelian.client.RegionTickDeltaManager;
import com.abelian.network.RegionSyncPayload;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
        ChunkPos entityChunkPos = entity.getChunkPos();
        RegionSyncPayload region = entity.getWorld() instanceof ClientWorld world ? ClientRegionManager.getRegion(world, entityChunkPos) : null;
        if (region != null && region.isControlled()) {
            String regionID = region.id();
            float tickDelta = RegionTickDeltaManager.getTickDelta(regionID);
            if (ClientRegionManager.isRegionFrozenAndIdle(region) && !RegionTickDeltaManager.hasActiveInterpolation(regionID)) {
                args.set(4, 1.0f);
                return;
            }
            args.set(4, tickDelta);
            Vec3d camPos = this.camera.getPos();
            Vec3d worldRenderPos = ClientRegionTicker.getInterpolatedEntityPos(entity, regionID, tickDelta);
            Vec3d relativePos = worldRenderPos.subtract(camPos);
            args.set(1, relativePos.x);
            args.set(2, relativePos.y);
            args.set(3, relativePos.z);
        }
    }
}
