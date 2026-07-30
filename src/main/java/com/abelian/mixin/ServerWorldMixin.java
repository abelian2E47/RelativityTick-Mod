package com.abelian.mixin;

import com.abelian.RelativityTickUtils;
import com.abelian.ServerTickBridge;

import com.abelian.regionTick.Region;
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
public abstract class ServerWorldMixin  {
    @Inject(method = "tickEntity", at = @At("HEAD"), cancellable = true)
    private <T extends Entity> void skipEntityTick(Entity entity, CallbackInfo ci) {
        if (ServerTickBridge.isCustomTickInProgress()) return;
        if (entity instanceof PlayerEntity) return;
        if (entity.getWorld().isClient()) return;

        ServerWorld world = (ServerWorld) entity.getWorld();
        long chunkPosLong = ChunkPos.toLong(entity.getBlockPos());
        Region region = RegionsManager.getRegionByChunk(world, chunkPosLong);
        if (region != null && region.isControlled()) {
            ci.cancel();
        }
    }

    @Inject(method = "tickChunk", at = @At("HEAD"), cancellable = true)
    private void skipChunkTick(net.minecraft.world.chunk.WorldChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        ServerWorld serverWorld = RelativityTickUtils.getServer().getWorld(chunk.getWorld().getRegistryKey());
        if (com.abelian.RegionTickContext.getTime(serverWorld) != null) return;
        Region region = RegionsManager.getRegionByChunk(serverWorld, chunk.getPos().toLong());
        if (region != null && region.isControlled()) {
            ci.cancel();
        }
    }
}
