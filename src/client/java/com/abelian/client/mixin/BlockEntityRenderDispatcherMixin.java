package com.abelian.client.mixin;

import com.abelian.client.clientRegionTick.ClientRegion;
import com.abelian.client.clientRegionTick.ClientRegionManager;
import com.abelian.client.render.RegionTickDeltaManager;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    //受控区域内的方块实体改用区域自己的渲染插值进度（1.21 的外层 render 把调用包进 lambda，只能改私有 render 的参数）
    @ModifyVariable(
            method = "render(Lnet/minecraft/client/render/block/entity/BlockEntityRenderer;Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 2
    )
    private static float useRegionTickDelta(float tickDelta, net.minecraft.client.render.block.entity.BlockEntityRenderer<?> renderer, BlockEntity blockEntity) {
        if (!(blockEntity.getWorld() instanceof ClientWorld world)) return tickDelta;

        ClientRegion region = ClientRegionManager.getRegion(world, new ChunkPos(blockEntity.getPos()));
        if (region == null || !region.isControlled()) return tickDelta;

        return RegionTickDeltaManager.getTickDelta(region.getId());
    }
}
