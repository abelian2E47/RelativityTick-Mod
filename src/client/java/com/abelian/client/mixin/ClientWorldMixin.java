package com.abelian.client.mixin;

import com.abelian.client.clientRegionTick.ClientRegionManager;
import com.abelian.client.clientRegionTick.ClientTickBridge;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Inject(method = "tickEntity", at = @At("HEAD"), cancellable = true)
    private <T extends Entity> void onTickEntityClient(Entity entity, CallbackInfo ci) {
        if (ClientTickBridge.isCustomTickInProgress()) return;

        if (entity instanceof PlayerEntity) return;
        if (!entity.getWorld().isClient()) return;

        ChunkPos chunkPos = entity.getChunkPos();
        if (!ClientRegionManager.isRegionControlled((ClientWorld) entity.getWorld(), chunkPos)) return;

        ci.cancel();
    }
}
