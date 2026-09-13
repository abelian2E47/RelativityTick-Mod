package com.abelian.mixin;
import com.abelian.ServerTickBridge;

import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(World.class)
public class WorldMixin {
    //1.21.11 的 getTime 拦截移到 WorldAccessTimeMixin（World 已不覆写该方法）

    @Inject(method = "addBlockEntityTicker", at = @At("HEAD"))
    private void invalidateBlockEntityTickerCache(net.minecraft.world.chunk.BlockEntityTickInvoker ticker, CallbackInfo ci) {
        ServerTickBridge.markBlockEntityTickersDirty((World) (Object) this);
    }
}
