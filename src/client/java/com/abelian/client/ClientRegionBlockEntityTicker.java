package com.abelian.client;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ClientRegionBlockEntityTicker {
    private static final List<BlockEntity> BLOCK_ENTITY_BUFFER = new ArrayList<>(64);

    @SuppressWarnings("unchecked")
    public static void tickBlockEntities(ClientWorld world, Set<Long> chunkPositions) {
        for (long chunkPosLong : chunkPositions) {
            ChunkPos chunkPos = new ChunkPos(chunkPosLong);
            WorldChunk chunk = world.getChunk(chunkPos.x, chunkPos.z);
            if (chunk == null) continue;

            BLOCK_ENTITY_BUFFER.clear();
            for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                if (!blockEntity.isRemoved() && blockEntity.hasWorld()) {
                    BLOCK_ENTITY_BUFFER.add(blockEntity);
                }
            }

            for (BlockEntity blockEntity : BLOCK_ENTITY_BUFFER) {
                BlockState state = blockEntity.getCachedState();
                BlockEntityTicker<BlockEntity> ticker = (BlockEntityTicker<BlockEntity>) state.getBlockEntityTicker(world, blockEntity.getType());
                if (ticker != null) {
                    ticker.tick(world, blockEntity.getPos(), state, blockEntity);
                }
            }
        }

        BLOCK_ENTITY_BUFFER.clear();
    }
}
