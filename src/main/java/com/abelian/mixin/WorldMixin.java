package com.abelian.mixin;

import com.abelian.RegionTickContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class WorldMixin {
    @Inject(method = "getTime", at = @At("HEAD"), cancellable = true)
    private void useRegionTickTime(CallbackInfoReturnable<Long> cir) {
        Long tickTime = RegionTickContext.getTime((World) (Object) this);
        if (tickTime != null) {
            cir.setReturnValue(tickTime);
        }
    }
}
