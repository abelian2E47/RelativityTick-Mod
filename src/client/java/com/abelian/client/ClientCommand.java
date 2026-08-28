package com.abelian.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.abelian.client.config.RelativityTickClientConfig;
import com.abelian.regionTick.RegionsManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


public class ClientCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("regionManager")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("create")
                        .then(CommandManager.argument("id", StringArgumentType.string())
                                .executes(context -> create(context.getSource(), StringArgumentType.getString(context, "id")))
                        )
                )
                .then(CommandManager.literal("chunk")
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("id", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            RegionsManager.getRegionIds().forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> addChunk(context.getSource(), StringArgumentType.getString(context, "id")))
                                )
                        )
                        .then(CommandManager.literal("remove")
                                .executes(context -> removeChunk(context.getSource()))
                        )
                .then(CommandManager.literal("select")
                        .executes(context -> select(context.getSource()))
                        )
                )
        );
    }

    //客户端本地渲染配置命令：通过 fabric 客户端命令 API 注册，专用服务器客户端同样可见
    public static void registerClientConfigCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(ClientCommandManager.literal("relativityTickClient")
                .then(ClientCommandManager.literal("scheduledTickRender")
                        .executes(context -> showScheduledTickRender(context.getSource()))
                        .then(ClientCommandManager.literal("enabled")
                                .then(ClientCommandManager.argument("value", BoolArgumentType.bool())
                                        .executes(context -> setScheduledTickRenderEnabled(
                                                context.getSource(), BoolArgumentType.getBool(context, "value")))))
                        .then(ClientCommandManager.literal("textScale")
                                .then(ClientCommandManager.argument("value", DoubleArgumentType.doubleArg(0.005, 1.0))
                                        .executes(context -> setScheduledTickTextScale(
                                                context.getSource(), DoubleArgumentType.getDouble(context, "value"))))))
                .then(ClientCommandManager.literal("regionLineWidth")
                        .executes(context -> showRegionLineWidth(context.getSource()))
                        .then(ClientCommandManager.argument("value", DoubleArgumentType.doubleArg(0.5, 16.0))
                                .executes(context -> setRegionLineWidth(
                                        context.getSource(), DoubleArgumentType.getDouble(context, "value"))))));
    }

    private static int showScheduledTickRender(FabricClientCommandSource source) {
        source.sendFeedback(Text.translatable("relativitytick.command.client.config.scheduled_tick_render",
                RelativityTickClientConfig.isRenderScheduledTicksEnabled()));
        return 1;
    }

    private static int setScheduledTickRenderEnabled(FabricClientCommandSource source, boolean value) {
        try {
            RelativityTickClientConfig.setRenderScheduledTicksEnabled(value);
        } catch (IOException e) {
            source.sendError(Text.translatable("relativitytick.command.error.config_save_failed").formatted(Formatting.RED));
            return 0;
        }
        return showScheduledTickRender(source);
    }

    private static int setScheduledTickTextScale(FabricClientCommandSource source, double value) {
        try {
            RelativityTickClientConfig.setScheduledTickTextScale(value);
        } catch (IOException e) {
            source.sendError(Text.translatable("relativitytick.command.error.config_save_failed").formatted(Formatting.RED));
            return 0;
        }
        source.sendFeedback(Text.translatable("relativitytick.command.client.config.text_scale",
                formatValue(RelativityTickClientConfig.getScheduledTickTextScale())));
        return 1;
    }

    private static int showRegionLineWidth(FabricClientCommandSource source) {
        source.sendFeedback(Text.translatable("relativitytick.command.client.config.region_line_width",
                formatValue(RelativityTickClientConfig.getRegionLineWidth())));
        return 1;
    }

    private static int setRegionLineWidth(FabricClientCommandSource source, double value) {
        try {
            RelativityTickClientConfig.setRegionLineWidth(value);
        } catch (IOException e) {
            source.sendError(Text.translatable("relativitytick.command.error.config_save_failed").formatted(Formatting.RED));
            return 0;
        }
        return showRegionLineWidth(source);
    }

    private static String formatValue(double value) {
        java.math.BigDecimal decimal = java.math.BigDecimal.valueOf(value);
        return decimal.setScale(4, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static int select(ServerCommandSource source) {
        RelativityTickClient.selectChunksMode();
        return 1;
    }


    private static int create(ServerCommandSource source, String id) {
        if (RegionsManager.getRegion(id) != null) {
            source.sendError(Text.translatable("relativitytick.command.client.error.region_exists", id));
            return 0;
        }

        if (!RelativityTickClient.selectChunks.isEmpty()) {
            Set<Long> chunksToSend = new HashSet<>(RelativityTickClient.selectChunks);
            ClientPlayNetworking.send(new SelectionOperationPayload(chunksToSend, id));

            RelativityTickClient.selectChunks.clear();
            RelativityTickClient.currentState = RelativityTickClient.SelectionState.OFF;
            source.sendFeedback(() -> Text.translatable("relativitytick.command.client.region_submitted",
                    Text.literal(id).formatted(Formatting.AQUA)), false);
            return 1;
        }

        source.sendError(Text.translatable("relativitytick.command.client.error.no_region_selection"));
        return 0;
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