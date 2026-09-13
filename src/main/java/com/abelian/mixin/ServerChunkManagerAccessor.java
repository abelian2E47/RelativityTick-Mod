package com.abelian.mixin;

import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Consumer;

//MC 1.21.11 移除了 spawnAnimals 字段（原版固定传 true），但 spawnMonsters 仍为逐世界生效的开关
@Mixin(ServerChunkManager.class)
public interface ServerChunkManagerAccessor {
    @Accessor("spawnMonsters")
    boolean getSpawnMonsters();

    @Accessor("chunkLoadingManager")
    ServerChunkLoadingManager getChunkLoadingManager();

    @Invoker("ifChunkLoaded")
    void invokeIfChunkLoaded(long chunkPos, Consumer<WorldChunk> consumer);
}
