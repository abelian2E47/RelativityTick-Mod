package com.abelian.client.mixin;

import com.abelian.client.ClientRegionManager;
import com.abelian.client.RegionTickDeltaManager;
import com.abelian.network.RegionSyncPayload;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBlockEntity.class)
public class PistonBlockEntityMixin {
    @Shadow
    private float progress;

    @Shadow
    private float lastProgress;

    @Inject(method = "getProgress", at = @At("HEAD"), cancellable = true)
    private void onGetProgress(float partialTick, CallbackInfoReturnable<Float> cir) {
        ChunkPos chunkPos = new ChunkPos(((PistonBlockEntity)(Object)this).getPos());
        RegionSyncPayload region = ((PistonBlockEntity)(Object)this).getWorld() instanceof ClientWorld world ? ClientRegionManager.getRegion(world, chunkPos) : null;
        if (region == null || !region.isControlled()) return;

        float tickDelta = ClientRegionManager.isRegionFrozenAndIdle(region) ? 1.0F : RegionTickDeltaManager.getTickDelta(region.id());
        cir.setReturnValue(MathHelper.lerp(tickDelta, this.lastProgress, this.progress));
    }
}
