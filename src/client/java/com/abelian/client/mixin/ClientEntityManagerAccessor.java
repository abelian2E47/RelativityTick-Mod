package com.abelian.client.mixin;

import net.minecraft.world.entity.ClientEntityManager;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.SectionedEntityCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientEntityManager.class)
public interface ClientEntityManagerAccessor<T extends EntityLike> {
    @Accessor("cache")
    SectionedEntityCache<T> getCache();
}
