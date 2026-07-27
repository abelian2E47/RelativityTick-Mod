package com.abelian.mixin;

import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Consumer;

@Mixin(ServerChunkManager.class)
public interface ServerChunkManagerAccessor {
    @Accessor("ticketManager")
    ChunkTicketManager getTicketManager();

    @Accessor("chunkLoadingManager")
    ServerChunkLoadingManager getChunkLoadingManager();

    @Accessor("spawnMonsters")
    boolean getSpawnMonsters();

    @Accessor("spawnAnimals")
    boolean getSpawnAnimals();

    @Invoker("ifChunkLoaded")
    void invokeIfChunkLoaded(long chunkPos, Consumer<net.minecraft.world.chunk.WorldChunk> consumer);
}
