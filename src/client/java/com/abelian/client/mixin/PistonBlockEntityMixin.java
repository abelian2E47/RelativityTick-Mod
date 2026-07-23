package com.abelian.client.mixin;

import com.abelian.client.clientRegionTick.ClientRegion;
import com.abelian.client.clientRegionTick.ClientRegionManager;
import com.abelian.client.render.RegionTickDeltaManager;
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
        ClientRegion region = ((PistonBlockEntity)(Object)this).getWorld() instanceof ClientWorld world ? ClientRegionManager.getRegion(world, chunkPos) : null;
        if (region == null || !region.isControlled()) return;

        float tickDelta = (!region.isRunning() ? 1.0F : RegionTickDeltaManager.getTickDelta(region.getId()));
        cir.setReturnValue(MathHelper.lerp(tickDelta, this.lastProgress, this.progress));
    }
}
