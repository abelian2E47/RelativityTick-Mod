package com.abelian.mixin;

import com.abelian.regionTick.RegionTickManager;
import com.abelian.regionTick.RegionsManager;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerChunkManager.class)
public abstract class ServerChunkManagerInhabitedTimeMixin {
    //MC 1.21 的 tickChunks 无区块列表形参，改为拦下每区块的居住时间累加；受控区块的生成与随机刻分别由 SpawnHelperMixin 与 ServerWorldMixin 拦下
    @Redirect(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/WorldChunk;increaseInhabitedTime(J)V"))
    private void skipControlledInhabitedTime(WorldChunk chunk, long delta) {
        if (chunk.getWorld() instanceof ServerWorld world) {
            RegionTickManager region = RegionsManager.getRegionByChunk(world, chunk.getPos().toLong());
            if (region != null && region.isControlled()) return;
        }
        chunk.increaseInhabitedTime(delta);
    }
}
