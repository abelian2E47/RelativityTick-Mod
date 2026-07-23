package com.abelian.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.abelian.regionTick.RegionsManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import com.abelian.network.SelectionOperationPayload;

import java.util.HashSet;
import java.util.Set;

public class ClientCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("regionManager")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("create")
                        .then(CommandManager.argument("id", StringArgumentType.string())
                                .executes(context -> create(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "id")
                                ))
                        )
                )
                .then(CommandManager.literal("chunk")
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("id", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            RegionsManager.getRegionIds().forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> addChunk(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "id")
                                        ))
                                )
                        )
                        .then(CommandManager.literal("remove")
                                .executes(context -> removeChunk(context.getSource()))
                        )
                )
        );
    }

    private static int create(ServerCommandSource source, String id) {
        if (RegionsManager.getRegion(id) != null) {
            source.sendError(Text.translatable("relativitytick.command.client.error.region_exists", id));
            return 0;
        }

        if (RelativityTickClient.currentState == RelativityTickClient.SelectionState.AWAITING_CONFIRM && !RelativityTickClient.selectChunks.isEmpty()) {
            Set<Long> chunksToSend = new HashSet<>(RelativityTickClient.selectChunks);

            ClientPlayNetworking.send(new SelectionOperationPayload(chunksToSend, id));

            RelativityTickClient.selectChunks.clear();
            RelativityTickClient.currentState = RelativityTickClient.SelectionState.OFF;

            source.sendFeedback(() -> Text.translatable("relativitytick.command.client.region_submitted",
                    Text.literal(id).formatted(Formatting.AQUA)), false);

            return 1;
        } else {
            source.sendError(Text.translatable("relativitytick.command.client.error.no_region_selection"));
            return 0;
        }
    }

    private static int addChunk(ServerCommandSource source, String id) {
        ServerWorld world = source.getWorld();
        if (!RelativityTickClient.selectChunks.isEmpty()) {
            Set<Long> chunksToAdd = new HashSet<>(RelativityTickClient.selectChunks);
            int added = RegionsManager.addChunksToRegion(id, chunksToAdd, world);
            RelativityTickClient.selectChunks.clear();
            RelativityTickClient.currentState = RelativityTickClient.SelectionState.OFF;

            source.sendFeedback(() -> Text.translatable("relativitytick.command.client.selected_chunks_added",
                    Text.literal(String.valueOf(added)).formatted(Formatting.GOLD),
                    Text.literal(id).formatted(Formatting.AQUA)), false);
            return 1;
        }

        ChunkPos chunkPos = getSourceChunkPos(source);
        RegionsManager.addChunkToRegion(id, chunkPos.toLong(), world);

        source.sendFeedback(() -> Text.translatable("relativitytick.command.client.chunk_added",
                Text.literal(chunkPos.x + " " + chunkPos.z).formatted(Formatting.GOLD),
                Text.literal(id).formatted(Formatting.AQUA)), false);
        return 1;
    }

    private static int removeChunk(ServerCommandSource source) {
        ServerWorld world = source.getWorld();
        ChunkPos chunkPos = getSourceChunkPos(source);
        String id = RegionsManager.getRegionIdByChunk(world, chunkPos.toLong());
        if (id == null) {
            source.sendError(Text.translatable("relativitytick.command.client.error.chunk_not_in_region", chunkPos.x, chunkPos.z));
            return 0;
        }

        RegionsManager.removeChunkFromRegion(id, chunkPos.toLong(), world);
        source.sendFeedback(() -> Text.translatable("relativitytick.command.client.chunk_removed",
                Text.literal(chunkPos.x + " " + chunkPos.z).formatted(Formatting.GOLD),
                Text.literal(id).formatted(Formatting.AQUA)), false);
        return 1;
    }

    private static ChunkPos getSourceChunkPos(ServerCommandSource source) {
        BlockPos blockPos = BlockPos.ofFloored(source.getPosition());
        return new ChunkPos(blockPos);
    }
}