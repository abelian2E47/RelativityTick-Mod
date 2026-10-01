package com.abelian.mixin;

import com.abelian.regionTick.RegionTickManager;
import com.abelian.regionTick.RegionsManager;
import com.abelian.RegionTimeContext;
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
public abstract class   WorldChunkDirectBlockEntityTickInvokerMixin {
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

        //区域刻内部不取消自己的 tick。把它提到查询之前：区域刻的方块实体 tick 走的就是这个原版
        //invoker，这一步能整段省掉"每方块实体每次 tick 一次区块→区域查询"的开销。
        if (RegionTimeContext.getTime(world) != null) {
            return;
        }

        long chunkPos = ChunkPos.toLong(this.blockEntity.getPos());
        RegionTickManager region = RegionsManager.getRegionByChunk(world, chunkPos);
        if (region != null && region.isControlled()) {
            ci.cancel();
        }
    }
}
