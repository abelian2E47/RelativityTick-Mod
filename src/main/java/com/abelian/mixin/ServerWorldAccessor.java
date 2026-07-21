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
}
