package com.abelian.client.mixin;

import com.abelian.client.clientRegionTick.ClientRegionManager;
import com.abelian.client.clientRegionTick.ClientTickBridge;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net/minecraft/world/chunk/WorldChunk$DirectBlockEntityTickInvoker")
public abstract class ClientWorldChunkDirectBlockEntityTickInvokerMixin {
    @Shadow
    @Final
    private BlockEntity blockEntity;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void skipControlledRegionBlockEntityTick(CallbackInfo ci) {
        if (ClientTickBridge.isCustomTickInProgress()) return;
        if (!(this.blockEntity.getWorld() instanceof ClientWorld world)) return;

        ChunkPos chunkPos = new ChunkPos(this.blockEntity.getPos());
        if (ClientRegionManager.isRegionControlled(world, chunkPos)) {
            ci.cancel();
        }
    }
}
