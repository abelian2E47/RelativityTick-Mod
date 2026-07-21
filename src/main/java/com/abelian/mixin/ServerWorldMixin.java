package com.abelian.mixin;

import com.abelian.ServerTickBridge;

import com.abelian.regionTick.RegionTickManager;
import com.abelian.regionTick.RegionsManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin {
    @Inject(method = "tickEntity", at = @At("HEAD"), cancellable = true)
    private <T extends Entity> void onTickEntity(Entity entity, CallbackInfo ci) {
        if (ServerTickBridge.isCustomTickInProgress()) return;
        if (entity instanceof PlayerEntity) return;
        if (entity.getWorld().isClient()) return;
        long chunkPosLong = ChunkPos.toLong(entity.getBlockPos());
        RegionTickManager region = RegionsManager.getRegionByChunk((ServerWorld) entity.getWorld(), chunkPosLong);

        if (region != null && region.isControlled()) {
            if (!region.isRunning()) {
                ci.cancel();
            }
        }
    }
}
