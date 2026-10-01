package com.abelian.mixin;

import net.minecraft.server.world.ServerEntityManager;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.SectionedEntityCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerEntityManager.class)
public interface ServerEntityManagerAccessor<T extends EntityLike> {
    @Accessor("cache")
    SectionedEntityCache<T> getCache();

    @Invoker("stopTracking")
    void relativityTick$stopTracking(T entity);

    @Invoker("startTracking")
    void relativityTick$startTracking(T entity);
}