package net.minecraft.client.render.debug;

import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.s2c.custom.DebugStructuresCustomPayload;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public class StructureDebugRenderer implements DebugRenderer.Renderer {
    private final MinecraftClient client;
    private final Map<RegistryKey<World>, Map<String, BlockBox>> structureBoundingBoxes = Maps.newIdentityHashMap();
    private final Map<RegistryKey<World>, Map<String, DebugStructuresCustomPayload.Piece>> structurePiecesBoundingBoxes = Maps.newIdentityHashMap();
    private static final int RANGE = 500;

    public StructureDebugRenderer(MinecraftClient client) {
        this.client = client;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, double cameraX, double cameraY, double cameraZ) {
        Camera camera = this.client.gameRenderer.getCamera();
        RegistryKey<World> registryKey = this.client.world.getRegistryKey();
        BlockPos blockPos = BlockPos.ofFloored(camera.getPos().x, 0.0, camera.getPos().z);
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());
        if (this.structureBoundingBoxes.containsKey(registryKey)) {
            for (BlockBox blockBox : this.structureBoundingBoxes.get(registryKey).values()) {
                if (blockPos.isWithinDistance(blockBox.getCenter(), 500.0)) {
                    VertexRendering.drawBox(
                        matrices,
                        vertexConsumer,
                        blockBox.getMinX() - cameraX,
                        blockBox.getMinY() - cameraY,
                        blockBox.getMinZ() - cameraZ,
                        blockBox.getMaxX() + 1 - cameraX,
                        blockBox.getMaxY() + 1 - cameraY,
                        blockBox.getMaxZ() + 1 - cameraZ,
                        1.0F,
                        1.0F,
                        1.0F,
                        1.0F,
                        1.0F,
                        1.0F,
                        1.0F
                    );
                }
            }
        }

        Map<String, DebugStructuresCustomPayload.Piece> map = this.structurePiecesBoundingBoxes.get(registryKey);
        if (map != null) {
            for (DebugStructuresCustomPayload.Piece piece : map.values()) {
                BlockBox blockBox2 = piece.boundingBox();
                if (blockPos.isWithinDistance(blockBox2.getCenter(), 500.0)) {
                    if (piece.isStart()) {
                        VertexRendering.drawBox(
                            matrices,
                            vertexConsumer,
                            blockBox2.getMinX() - cameraX,
                            blockBox2.getMinY() - cameraY,
                            blockBox2.getMinZ() - cameraZ,
                            blockBox2.getMaxX() + 1 - cameraX,
                            blockBox2.getMaxY() + 1 - cameraY,
                            blockBox2.getMaxZ() + 1 - cameraZ,
                            0.0F,
                            1.0F,
                            0.0F,
                            1.0F,
                            0.0F,
                            1.0F,
                            0.0F
                        );
                    } else {
                        VertexRendering.drawBox(
                            matrices,
                            vertexConsumer,
                            blockBox2.getMinX() - cameraX,
                            blockBox2.getMinY() - cameraY,
                            blockBox2.getMinZ() - cameraZ,
                            blockBox2.getMaxX() + 1 - cameraX,
                            blockBox2.getMaxY() + 1 - cameraY,
                            blockBox2.getMaxZ() + 1 - cameraZ,
                            0.0F,
                            0.0F,
                            1.0F,
                            1.0F,
                            0.0F,
                            0.0F,
                            1.0F
                        );
                    }
                }
            }
        }
    }

    public void addStructure(BlockBox boundingBox, List<DebugStructuresCustomPayload.Piece> pieces, RegistryKey<World> dimensionKey) {
        this.structureBoundingBoxes.computeIfAbsent(dimensionKey, dimension -> new HashMap<>()).put(boundingBox.toString(), boundingBox);
        Map<String, DebugStructuresCustomPayload.Piece> map = this.structurePiecesBoundingBoxes.computeIfAbsent(dimensionKey, dimension -> new HashMap<>());

        for (DebugStructuresCustomPayload.Piece piece : pieces) {
            map.put(piece.boundingBox().toString(), piece);
        }
    }

    @Override
    public void clear() {
        this.structureBoundingBoxes.clear();
        this.structurePiecesBoundingBoxes.clear();
    }
}

