package com.abelian.mixin;

import com.abelian.ServerTickBridge;
import net.minecraft.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin {
    @Inject(method = "markRemoved", at = @At("HEAD"))
    private void relativityTick$invalidateTickerCache(CallbackInfo ci) {
        BlockEntity blockEntity = (BlockEntity) (Object) this;
        if (blockEntity.hasWorld()) {
            ServerTickBridge.markBlockEntityTickersDirty(blockEntity.getWorld());
        }
    }
}
