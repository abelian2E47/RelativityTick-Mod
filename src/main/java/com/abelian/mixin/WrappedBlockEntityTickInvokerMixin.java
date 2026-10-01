package com.abelian.mixin;

import com.abelian.RebindableBlockEntityTickInvoker;
import net.minecraft.world.chunk.BlockEntityTickInvoker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.world.chunk.WorldChunk$WrappedBlockEntityTickInvoker")
public abstract class WrappedBlockEntityTickInvokerMixin implements RebindableBlockEntityTickInvoker {
    @Unique
    private BlockEntityTickInvoker relativityTick$lastReboundInner;

    @Accessor("wrapped")
    public abstract BlockEntityTickInvoker relativityTick$getWrappedInner();

    @Override
    public boolean relativityTick$rebindNeeded() {
        return this.relativityTick$lastReboundInner != this.relativityTick$getWrappedInner();
    }

    @Override
    public void relativityTick$markRebound() {
        this.relativityTick$lastReboundInner = this.relativityTick$getWrappedInner();
    }

    @Override
    public boolean relativityTick$isSleeping() {
        return this.relativityTick$getWrappedInner().getPos() == null;
    }
}
