package com.abelian.mixin;

import com.abelian.regionTick.RegionTickManager;
import com.abelian.regionTick.RegionsManager;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerChunkManager.class)
public abstract class ServerChunkManagerInhabitedTimeMixin {
    //受控区块的 inhabitedTime 由 RegionTickManager.tickChunkWorld 按区域步推进,
    //这里从原版 tickChunks 的待处理列表中剔除,避免同一服务器 tick 双计
    @Inject(method = "tickChunks(Lnet/minecraft/util/profiler/Profiler;JLjava/util/List;)V", at = @At("HEAD"))
    private void excludeControlledChunks(net.minecraft.util.profiler.Profiler profiler, long timeDelta, List<WorldChunk> chunks, CallbackInfo ci) {
        chunks.removeIf(chunk -> {
            if (!(chunk.getWorld() instanceof ServerWorld world)) return false;
            RegionTickManager region = RegionsManager.getRegionByChunk(world, chunk.getPos().toLong());
            return region != null && region.isControlled();
        });
    }
}
