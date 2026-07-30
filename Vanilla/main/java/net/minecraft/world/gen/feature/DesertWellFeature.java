package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.loot.LootTables;
import net.minecraft.predicate.block.BlockStatePredicate;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class DesertWellFeature extends Feature<DefaultFeatureConfig> {
    private static final BlockStatePredicate CAN_GENERATE = BlockStatePredicate.forBlock(Blocks.SAND);
    private final BlockState sand = Blocks.SAND.getDefaultState();
    private final BlockState slab = Blocks.SANDSTONE_SLAB.getDefaultState();
    private final BlockState wall = Blocks.SANDSTONE.getDefaultState();
    private final BlockState fluidInside = Blocks.WATER.getDefaultState();

    public DesertWellFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess structureWorldAccess = context.getWorld();
        BlockPos blockPos = context.getOrigin();
        blockPos = blockPos.up();

        while (structureWorldAccess.isAir(blockPos) && blockPos.getY() > structureWorldAccess.getBottomY() + 2) {
            blockPos = blockPos.down();
        }

        if (!CAN_GENERATE.test(structureWorldAccess.getBlockState(blockPos))) {
            return false;
        }

        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (structureWorldAccess.isAir(blockPos.add(i, -1, j)) && structureWorldAccess.isAir(blockPos.add(i, -2, j))) {
                    return false;
                }
            }
        }

        for (int k = -2; k <= 0; k++) {
            for (int l = -2; l <= 2; l++) {
                for (int m = -2; m <= 2; m++) {
                    structureWorldAccess.setBlockState(blockPos.add(l, k, m), this.wall, Block.NOTIFY_LISTENERS);
                }
            }
        }

        structureWorldAccess.setBlockState(blockPos, this.fluidInside, Block.NOTIFY_LISTENERS);

        for (Direction direction : Direction.Type.HORIZONTAL) {
            structureWorldAccess.setBlockState(blockPos.offset(direction), this.fluidInside, Block.NOTIFY_LISTENERS);
        }

        BlockPos blockPos2 = blockPos.down();
        structureWorldAccess.setBlockState(blockPos2, this.sand, Block.NOTIFY_LISTENERS);

        for (Direction direction2 : Direction.Type.HORIZONTAL) {
            structureWorldAccess.setBlockState(blockPos2.offset(direction2), this.sand, Block.NOTIFY_LISTENERS);
        }

        for (int n = -2; n <= 2; n++) {
            for (int o = -2; o <= 2; o++) {
                if (n == -2 || n == 2 || o == -2 || o == 2) {
                    structureWorldAccess.setBlockState(blockPos.add(n, 1, o), this.wall, Block.NOTIFY_LISTENERS);
                }
            }
        }

        structureWorldAccess.setBlockState(blockPos.add(2, 1, 0), this.slab, Block.NOTIFY_LISTENERS);
        structureWorldAccess.setBlockState(blockPos.add(-2, 1, 0), this.slab, Block.NOTIFY_LISTENERS);
        structureWorldAccess.setBlockState(blockPos.add(0, 1, 2), this.slab, Block.NOTIFY_LISTENERS);
        structureWorldAccess.setBlockState(blockPos.add(0, 1, -2), this.slab, Block.NOTIFY_LISTENERS);

        for (int p = -1; p <= 1; p++) {
            for (int q = -1; q <= 1; q++) {
                if (p == 0 && q == 0) {
                    structureWorldAccess.setBlockState(blockPos.add(p, 4, q), this.wall, Block.NOTIFY_LISTENERS);
                } else {
                    structureWorldAccess.setBlockState(blockPos.add(p, 4, q), this.slab, Block.NOTIFY_LISTENERS);
                }
            }
        }

        for (int r = 1; r <= 3; r++) {
            structureWorldAccess.setBlockState(blockPos.add(-1, r, -1), this.wall, Block.NOTIFY_LISTENERS);
            structureWorldAccess.setBlockState(blockPos.add(-1, r, 1), this.wall, Block.NOTIFY_LISTENERS);
            structureWorldAccess.setBlockState(blockPos.add(1, r, -1), this.wall, Block.NOTIFY_LISTENERS);
            structureWorldAccess.setBlockState(blockPos.add(1, r, 1), this.wall, Block.NOTIFY_LISTENERS);
        }

        BlockPos blockPos3 = blockPos;
        List<BlockPos> list = List.of(blockPos3, blockPos3.east(), blockPos3.south(), blockPos3.west(), blockPos3.north());
        Random random = context.getRandom();
        generateSuspiciousSand(structureWorldAccess, Util.getRandom(list, random).down(1));
        generateSuspiciousSand(structureWorldAccess, Util.getRandom(list, random).down(2));
        return true;
    }

    private static void generateSuspiciousSand(StructureWorldAccess world, BlockPos pos) {
        world.setBlockState(pos, Blocks.SUSPICIOUS_SAND.getDefaultState(), Block.NOTIFY_ALL);
        world.getBlockEntity(pos, BlockEntityType.BRUSHABLE_BLOCK)
            .ifPresent(blockEntity -> blockEntity.setLootTable(LootTables.DESERT_WELL_ARCHAEOLOGY, pos.asLong()));
    }
}

