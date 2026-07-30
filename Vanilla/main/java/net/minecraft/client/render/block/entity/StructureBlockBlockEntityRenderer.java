package net.minecraft.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.StructureBlockBlockEntity;
import net.minecraft.block.enums.StructureBlockMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.test.StructureTestUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.shape.BitSetVoxelSet;
import net.minecraft.util.shape.VoxelSet;
import net.minecraft.world.BlockView;

@Environment(EnvType.CLIENT)
public class StructureBlockBlockEntityRenderer implements BlockEntityRenderer<StructureBlockBlockEntity> {
    public StructureBlockBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    public void render(
        StructureBlockBlockEntity structureBlockBlockEntity, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, int j
    ) {
        if (MinecraftClient.getInstance().player.isCreativeLevelTwoOp() || MinecraftClient.getInstance().player.isSpectator()) {
            BlockPos blockPos = structureBlockBlockEntity.getOffset();
            Vec3i vec3i = structureBlockBlockEntity.getSize();
            if (vec3i.getX() >= 1 && vec3i.getY() >= 1 && vec3i.getZ() >= 1) {
                if (structureBlockBlockEntity.getMode() == StructureBlockMode.SAVE || structureBlockBlockEntity.getMode() == StructureBlockMode.LOAD) {
                    double d = blockPos.getX();
                    double e = blockPos.getZ();
                    double g = blockPos.getY();
                    double h = g + vec3i.getY();
                    double k;
                    double l;
                    switch (structureBlockBlockEntity.getMirror()) {
                        case LEFT_RIGHT:
                            k = vec3i.getX();
                            l = -vec3i.getZ();
                            break;
                        case FRONT_BACK:
                            k = -vec3i.getX();
                            l = vec3i.getZ();
                            break;
                        default:
                            k = vec3i.getX();
                            l = vec3i.getZ();
                    }

                    double cb;
                    double db;
                    double eb;
                    double fb;
                    switch (structureBlockBlockEntity.getRotation()) {
                        case CLOCKWISE_90:
                            cb = l < 0.0 ? d : d + 1.0;
                            db = k < 0.0 ? e + 1.0 : e;
                            eb = cb - l;
                            fb = db + k;
                            break;
                        case CLOCKWISE_180:
                            cb = k < 0.0 ? d : d + 1.0;
                            db = l < 0.0 ? e : e + 1.0;
                            eb = cb - k;
                            fb = db - l;
                            break;
                        case COUNTERCLOCKWISE_90:
                            cb = l < 0.0 ? d + 1.0 : d;
                            db = k < 0.0 ? e : e + 1.0;
                            eb = cb + l;
                            fb = db - k;
                            break;
                        default:
                            cb = k < 0.0 ? d + 1.0 : d;
                            db = l < 0.0 ? e + 1.0 : e;
                            eb = cb + k;
                            fb = db + l;
                    }

                    float gb = 1.0F;
                    float hb = 0.9F;
                    float ib = 0.5F;
                    if (structureBlockBlockEntity.getMode() == StructureBlockMode.SAVE || structureBlockBlockEntity.shouldShowBoundingBox()) {
                        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayer.getLines());
                        VertexRendering.drawBox(matrixStack, vertexConsumer, cb, g, db, eb, h, fb, 0.9F, 0.9F, 0.9F, 1.0F, 0.5F, 0.5F, 0.5F);
                    }

                    if (structureBlockBlockEntity.getMode() == StructureBlockMode.SAVE && structureBlockBlockEntity.shouldShowAir()) {
                        this.renderInvisibleBlocks(structureBlockBlockEntity, vertexConsumerProvider, matrixStack);
                    }
                }
            }
        }
    }

    private void renderInvisibleBlocks(StructureBlockBlockEntity entity, VertexConsumerProvider vertexConsumers, MatrixStack matrices) {
        BlockView blockView = entity.getWorld();
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());
        BlockPos blockPos = entity.getPos();
        BlockPos blockPos2 = StructureTestUtil.getOrigin(entity);

        for (BlockPos blockPos3 : BlockPos.iterate(blockPos2, blockPos2.add(entity.getSize()).add(-1, -1, -1))) {
            BlockState blockState = blockView.getBlockState(blockPos3);
            boolean bl = blockState.isAir();
            boolean bl2 = blockState.isOf(Blocks.STRUCTURE_VOID);
            boolean bl3 = blockState.isOf(Blocks.BARRIER);
            boolean bl4 = blockState.isOf(Blocks.LIGHT);
            boolean bl5 = bl2 || bl3 || bl4;
            if (bl || bl5) {
                float f = bl ? 0.05F : 0.0F;
                double d = blockPos3.getX() - blockPos.getX() + 0.45F - f;
                double e = blockPos3.getY() - blockPos.getY() + 0.45F - f;
                double g = blockPos3.getZ() - blockPos.getZ() + 0.45F - f;
                double h = blockPos3.getX() - blockPos.getX() + 0.55F + f;
                double i = blockPos3.getY() - blockPos.getY() + 0.55F + f;
                double j = blockPos3.getZ() - blockPos.getZ() + 0.55F + f;
                if (bl) {
                    VertexRendering.drawBox(matrices, vertexConsumer, d, e, g, h, i, j, 0.5F, 0.5F, 1.0F, 1.0F, 0.5F, 0.5F, 1.0F);
                } else if (bl2) {
                    VertexRendering.drawBox(matrices, vertexConsumer, d, e, g, h, i, j, 1.0F, 0.75F, 0.75F, 1.0F, 1.0F, 0.75F, 0.75F);
                } else if (bl3) {
                    VertexRendering.drawBox(matrices, vertexConsumer, d, e, g, h, i, j, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F);
                } else if (bl4) {
                    VertexRendering.drawBox(matrices, vertexConsumer, d, e, g, h, i, j, 1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 0.0F);
                }
            }
        }
    }

    private void method_61047(StructureBlockBlockEntity entity, VertexConsumer vertexConsumer, MatrixStack matrices) {
        BlockView blockView = entity.getWorld();
        if (blockView != null) {
            BlockPos blockPos = entity.getPos();
            BlockPos blockPos2 = StructureTestUtil.getOrigin(entity);
            Vec3i vec3i = entity.getSize();
            VoxelSet voxelSet = new BitSetVoxelSet(vec3i.getX(), vec3i.getY(), vec3i.getZ());

            for (BlockPos blockPos3 : BlockPos.iterate(blockPos2, blockPos2.add(vec3i).add(-1, -1, -1))) {
                if (blockView.getBlockState(blockPos3).isOf(Blocks.STRUCTURE_VOID)) {
                    voxelSet.set(blockPos3.getX() - blockPos2.getX(), blockPos3.getY() - blockPos2.getY(), blockPos3.getZ() - blockPos2.getZ());
                }
            }

            voxelSet.forEachDirection((direction, x, y, z) -> {
                float f = 0.48F;
                float g = x + blockPos2.getX() - blockPos.getX() + 0.5F - 0.48F;
                float h = y + blockPos2.getY() - blockPos.getY() + 0.5F - 0.48F;
                float i = z + blockPos2.getZ() - blockPos.getZ() + 0.5F - 0.48F;
                float j = x + blockPos2.getX() - blockPos.getX() + 0.5F + 0.48F;
                float k = y + blockPos2.getY() - blockPos.getY() + 0.5F + 0.48F;
                float l = z + blockPos2.getZ() - blockPos.getZ() + 0.5F + 0.48F;
                VertexRendering.drawSide(matrices, vertexConsumer, direction, g, h, i, j, k, l, 0.75F, 0.75F, 1.0F, 0.2F);
            });
        }
    }

    public boolean rendersOutsideBoundingBox(StructureBlockBlockEntity structureBlockBlockEntity) {
        return true;
    }

    @Override
    public int getRenderDistance() {
        return 96;
    }
}

