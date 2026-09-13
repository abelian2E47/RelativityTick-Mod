package com.abelian.mixin;

import net.minecraft.server.world.ChunkLevelManager;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Consumer;

@Mixin(ServerChunkManager.class)
public interface ServerChunkManagerAccessor {
    //MC 1.21.8 把 getTickedChunkCount 从 ChunkTicketManager 移到了 ChunkLevelManager
    @Accessor("levelManager")
    ChunkLevelManager getLevelManager();

    @Accessor("chunkLoadingManager")
    ServerChunkLoadingManager getChunkLoadingManager();

    @Accessor("spawnMonsters")
    boolean getSpawnMonsters();

    @Accessor("spawnAnimals")
    boolean getSpawnAnimals();

    @Invoker("ifChunkLoaded")
    void invokeIfChunkLoaded(long chunkPos, Consumer<net.minecraft.world.chunk.WorldChunk> consumer);
}
