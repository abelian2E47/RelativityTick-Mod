package com.abelian.mixin;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(WorldChunk.class)
public interface WorldChunkAccessor {
    @Invoker("updateTicker")
    void relativityTick$updateTicker(BlockEntity blockEntity);

    //区块自己的"坐标 → ticker 包装器"表：真正在 tick 的方块实体才在里面。
    //每步的 lithium 唤醒 rebind 只遍历这张表，而不是区块里全部方块实体（箱子/告示牌等不 tick 的占多数）。
    @Accessor("blockEntityTickers")
    Map<BlockPos, ?> relativityTick$getBlockEntityTickers();
}
