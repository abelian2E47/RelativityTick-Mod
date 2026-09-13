package com.abelian.mixin;

import com.abelian.regionTick.RegionTickManager;
import com.abelian.regionTick.RegionsManager;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerChunkManager.class)
public abstract class ServerChunkManagerInhabitedTimeMixin {
    //1.21.11 的 tickChunks 无区块列表形参，等价点是每区块的 tickSpawningChunk（居住时间/雷暴/生成）；受控区块整段跳过，随机刻由 ServerWorldMixin 拦下
    @Inject(method = "tickSpawningChunk", at = @At("HEAD"), cancellable = true)
    private void skipControlledChunks(WorldChunk chunk, long timeDelta, List<SpawnGroup> spawnableGroups, SpawnHelper.Info info, CallbackInfo ci) {
        if (!(chunk.getWorld() instanceof ServerWorld world)) return;
        RegionTickManager region = RegionsManager.getRegionByChunk(world, chunk.getPos().toLong());
        if (region != null && region.isControlled()) {
            ci.cancel();
        }
    }
}
