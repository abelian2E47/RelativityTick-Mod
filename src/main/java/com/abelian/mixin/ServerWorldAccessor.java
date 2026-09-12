package com.abelian.mixin;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.server.world.BlockEvent;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerWorld.class)
public interface ServerWorldAccessor {
    @Invoker("tickEntity")
    void invokeTickEntity(Entity entity);

    @Accessor("syncedBlockEventQueue")
    ObjectLinkedOpenHashSet<BlockEvent> getSyncedBlockEventQueue();

    @Invoker("processBlockEvent")
    boolean invokeProcessBlockEvent(BlockEvent event);

    @Accessor("entityList")
    net.minecraft.world.EntityList getEntityList();

    // [修改点1] 暴露 ServerWorld.inBlockTick 的写入口：区域步进需要在 step 内按原版 ServerWorld.tick
    // 的开窗方式（L352 置 true → L403 在 processSyncedBlockEvents 之后置 false）设置该标志。
    @Accessor("inBlockTick")
    void setInBlockTick(boolean inBlockTick);
}
