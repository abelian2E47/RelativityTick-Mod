package com.abelian.mixin;

import net.minecraft.world.World;
import net.minecraft.world.chunk.BlockEntityTickInvoker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(World.class)
public interface WorldAccessor {
    @Accessor("blockEntityTickers")
    List<BlockEntityTickInvoker> getBlockEntityTickers();

    @Accessor("pendingBlockEntityTickers")
    List<BlockEntityTickInvoker> getPendingBlockEntityTickers();

    @Accessor("iteratingTickingBlockEntities")
    boolean isIteratingTickingBlockEntities();

    @Accessor("iteratingTickingBlockEntities")
    void setIteratingTickingBlockEntities(boolean value);
}
