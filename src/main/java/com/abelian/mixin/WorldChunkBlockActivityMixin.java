package com.abelian.mixin;

import com.abelian.ServerTickBridge;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldChunk.class)
public class WorldChunkBlockActivityMixin {
    @Shadow
    @Final
    World world;

    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void relativityTick$recordBlockActivity(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> cir) {
        ServerTickBridge.markChunkActivity(this.world, ChunkPos.toLong(pos));
    }
}
