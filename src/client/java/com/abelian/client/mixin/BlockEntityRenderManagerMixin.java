package com.abelian.client.mixin;

import com.abelian.client.clientRegionTick.ClientRegion;
import com.abelian.client.clientRegionTick.ClientRegionManager;
import com.abelian.client.render.RegionTickDeltaManager;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

//1.21.11 的方块实体渲染改为先取 BlockEntityRenderState，tickProgress 在 getRenderState 处传入
@Mixin(BlockEntityRenderManager.class)
public abstract class BlockEntityRenderManagerMixin {
    @ModifyVariable(method = "getRenderState", at = @At("HEAD"), argsOnly = true, index = 2)
    private float useRegionTickDelta(float tickProgress, BlockEntity blockEntity) {
        if (!(blockEntity.getWorld() instanceof ClientWorld world)) return tickProgress;

        ClientRegion region = ClientRegionManager.getRegion(world, new ChunkPos(blockEntity.getPos()));
        if (region == null || !region.isControlled()) return tickProgress;

        return RegionTickDeltaManager.getTickDelta(region.getId());
    }
}
