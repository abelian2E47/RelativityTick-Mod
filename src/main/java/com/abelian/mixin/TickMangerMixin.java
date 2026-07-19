package com.abelian.mixin;

import com.abelian.regionFreeze.RegionsManager;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.tick.TickManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TickManager.class)
public class TickMangerMixin {
    @Inject(method = "shouldSkipTick", at = @At("RETURN"), cancellable = true)
    public void shouldSkipTick(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        cir.setReturnValue(shouldFreeze(entity));
    }

    @Unique
    private boolean shouldFreeze(Entity entity) {
        if (entity instanceof net.minecraft.entity.player.PlayerEntity) return false;
        if (entity.getWorld().isClient()) return false;

        long posLong = ChunkPos.toLong(entity.getBlockPos());
        var region = RegionsManager.getRegionByChunk((ServerWorld) entity.getWorld(), posLong);

        return region != null && region.isControlled();
    }

}
