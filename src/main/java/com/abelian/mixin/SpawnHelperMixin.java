package com.abelian.mixin;

import com.abelian.RegionTickContext;
import com.abelian.regionTick.RegionsManager;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SpawnHelper.class)
public abstract class SpawnHelperMixin {
    @Inject(method = "spawn", at = @At("HEAD"), cancellable = true)
    private static void skipSpawning(ServerWorld world, WorldChunk chunk,
                                                     SpawnHelper.Info info, List<SpawnGroup> groups,
                                                     CallbackInfo ci) {
        if (RegionTickContext.getTime(world) != null) return;
        com.abelian.regionTick.Region region = RegionsManager.getRegionByChunk(world, chunk.getPos().toLong());
        if (region != null && region.isControlled()) {
            ci.cancel();
        }
    }
}
