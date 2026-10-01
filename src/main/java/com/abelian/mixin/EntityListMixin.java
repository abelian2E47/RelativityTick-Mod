package com.abelian.mixin;

import com.abelian.ServerTickBridge;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.EntityList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityList.class)
public abstract class EntityListMixin {
    @Inject(method = "add", at = @At("HEAD"))
    private void markAdd(Entity entity, CallbackInfo ci) {
        ServerTickBridge.markEntityListMutated((EntityList) (Object) this);
        ServerTickBridge.markChunkActivity(entity.getWorld(), ChunkPos.toLong(entity.getBlockPos()));
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void markRemove(Entity entity, CallbackInfo ci) {
        ServerTickBridge.markEntityListMutated((EntityList) (Object) this);
        ServerTickBridge.markChunkActivity(entity.getWorld(), ChunkPos.toLong(entity.getBlockPos()));
    }
}
