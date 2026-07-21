package com.abelian.mixin;

import com.abelian.regionTick.RegionBlockEventProcessor;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public abstract class ServerWorldBlockEventMixin {
    @Inject(method = "processSyncedBlockEvents", at = @At("HEAD"), cancellable = true)
    private void processUncontrolledBlockEvents(CallbackInfo ci) {
        RegionBlockEventProcessor.process((ServerWorld) (Object) this, owner -> owner == null || !owner.isControlled());
        ci.cancel();
    }
}
