package com.abelian.mixin;

import com.abelian.regionTick.Region;
import com.abelian.regionTick.RegionsManager;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net/minecraft/world/chunk/WorldChunk$DirectBlockEntityTickInvoker")
public abstract class WorldChunkDirectBlockEntityTickInvokerMixin {
    @Shadow
    @Final
    private BlockEntity blockEntity;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void skipControlledRegionBlockEntityTick(CallbackInfo ci) {
        if (this.blockEntity.isRemoved() || !this.blockEntity.hasWorld()) {
            return;
        }

        if (!(this.blockEntity.getWorld() instanceof ServerWorld world)) {
            return;
        }

        long chunkPos = ChunkPos.toLong(this.blockEntity.getPos());
        Region region = RegionsManager.getRegionByChunk(world, chunkPos);
        if (region != null && region.isControlled()) {
            ci.cancel();
        }
    }
}
