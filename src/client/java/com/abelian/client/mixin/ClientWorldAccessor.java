package com.abelian.client.mixin;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.world.entity.ClientEntityManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientWorld.class)
public interface ClientWorldAccessor {
    @Accessor("entityManager")
    ClientEntityManager<Entity> getEntityManager();

    @Invoker("tickEntity")
    void invokeTickEntity(Entity entity);
}
