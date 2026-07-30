package net.minecraft.world.gen.treedecorator;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.VineBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public class TrunkVineTreeDecorator extends TreeDecorator {
    public static final MapCodec<TrunkVineTreeDecorator> CODEC = MapCodec.unit(() -> TrunkVineTreeDecorator.INSTANCE);
    public static final TrunkVineTreeDecorator INSTANCE = new TrunkVineTreeDecorator();

    @Override
    protected TreeDecoratorType<?> getType() {
        return TreeDecoratorType.TRUNK_VINE;
    }

    @Override
    public void generate(TreeDecorator.Generator generator) {
        Random random = generator.getRandom();
        generator.getLogPositions().forEach(pos -> {
            if (random.nextInt(3) > 0) {
                BlockPos blockPos = pos.west();
                if (generator.isAir(blockPos)) {
                    generator.replaceWithVine(blockPos, VineBlock.EAST);
                }
            }

            if (random.nextInt(3) > 0) {
                BlockPos blockPos2 = pos.east();
                if (generator.isAir(blockPos2)) {
                    generator.replaceWithVine(blockPos2, VineBlock.WEST);
                }
            }

            if (random.nextInt(3) > 0) {
                BlockPos blockPos3 = pos.north();
                if (generator.isAir(blockPos3)) {
                    generator.replaceWithVine(blockPos3, VineBlock.SOUTH);
                }
            }

            if (random.nextInt(3) > 0) {
                BlockPos blockPos4 = pos.south();
                if (generator.isAir(blockPos4)) {
                    generator.replaceWithVine(blockPos4, VineBlock.NORTH);
                }
            }
        });
    }
}

