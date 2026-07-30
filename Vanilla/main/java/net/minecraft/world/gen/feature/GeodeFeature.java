package net.minecraft.world.gen.feature;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BuddingAmethystBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class GeodeFeature extends Feature<GeodeFeatureConfig> {
    private static final Direction[] DIRECTIONS = Direction.values();

    public GeodeFeature(Codec<GeodeFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<GeodeFeatureConfig> context) {
        GeodeFeatureConfig geodeFeatureConfig = context.getConfig();
        Random random = context.getRandom();
        BlockPos blockPos = context.getOrigin();
        StructureWorldAccess structureWorldAccess = context.getWorld();
        int i = geodeFeatureConfig.minGenOffset;
        int j = geodeFeatureConfig.maxGenOffset;
        List<Pair<BlockPos, Integer>> list = Lists.newLinkedList();
        int k = geodeFeatureConfig.distributionPoints.get(random);
        ChunkRandom chunkRandom = new ChunkRandom(new CheckedRandom(structureWorldAccess.getSeed()));
        DoublePerlinNoiseSampler doublePerlinNoiseSampler = DoublePerlinNoiseSampler.create(chunkRandom, -4, 1.0);
        List<BlockPos> list2 = Lists.newLinkedList();
        double d = (double)k / geodeFeatureConfig.outerWallDistance.getMax();
        GeodeLayerThicknessConfig geodeLayerThicknessConfig = geodeFeatureConfig.layerThicknessConfig;
        GeodeLayerConfig geodeLayerConfig = geodeFeatureConfig.layerConfig;
        GeodeCrackConfig geodeCrackConfig = geodeFeatureConfig.crackConfig;
        double e = 1.0 / Math.sqrt(geodeLayerThicknessConfig.filling);
        double f = 1.0 / Math.sqrt(geodeLayerThicknessConfig.innerLayer + d);
        double g = 1.0 / Math.sqrt(geodeLayerThicknessConfig.middleLayer + d);
        double h = 1.0 / Math.sqrt(geodeLayerThicknessConfig.outerLayer + d);
        double l = 1.0 / Math.sqrt(geodeCrackConfig.baseCrackSize + random.nextDouble() / 2.0 + (k > 3 ? d : 0.0));
        boolean bl = random.nextFloat() < geodeCrackConfig.generateCrackChance;
        int m = 0;

        for (int n = 0; n < k; n++) {
            int o = geodeFeatureConfig.outerWallDistance.get(random);
            int p = geodeFeatureConfig.outerWallDistance.get(random);
            int q = geodeFeatureConfig.outerWallDistance.get(random);
            BlockPos blockPos2 = blockPos.add(o, p, q);
            BlockState blockState = structureWorldAccess.getBlockState(blockPos2);
            if ((blockState.isAir() || blockState.isIn(geodeLayerConfig.invalidBlocks)) && ++m > geodeFeatureConfig.invalidBlocksThreshold) {
                return false;
            }

            list.add(Pair.of(blockPos2, geodeFeatureConfig.pointOffset.get(random)));
        }

        if (bl) {
            int r = random.nextInt(4);
            int s = k * 2 + 1;
            if (r == 0) {
                list2.add(blockPos.add(s, 7, 0));
                list2.add(blockPos.add(s, 5, 0));
                list2.add(blockPos.add(s, 1, 0));
            } else if (r == 1) {
                list2.add(blockPos.add(0, 7, s));
                list2.add(blockPos.add(0, 5, s));
                list2.add(blockPos.add(0, 1, s));
            } else if (r == 2) {
                list2.add(blockPos.add(s, 7, s));
                list2.add(blockPos.add(s, 5, s));
                list2.add(blockPos.add(s, 1, s));
            } else {
                list2.add(blockPos.add(0, 7, 0));
                list2.add(blockPos.add(0, 5, 0));
                list2.add(blockPos.add(0, 1, 0));
            }
        }

        List<BlockPos> list3 = Lists.newArrayList();
        Predicate<BlockState> predicate = notInBlockTagPredicate(geodeFeatureConfig.layerConfig.cannotReplace);

        for (BlockPos blockPos3 : BlockPos.iterate(blockPos.add(i, i, i), blockPos.add(j, j, j))) {
            double t = doublePerlinNoiseSampler.sample(blockPos3.getX(), blockPos3.getY(), blockPos3.getZ()) * geodeFeatureConfig.noiseMultiplier;
            double u = 0.0;
            double v = 0.0;

            for (Pair<BlockPos, Integer> pair : list) {
                u += MathHelper.inverseSqrt(blockPos3.getSquaredDistance(pair.getFirst()) + pair.getSecond().intValue()) + t;
            }

            for (BlockPos blockPos4 : list2) {
                v += MathHelper.inverseSqrt(blockPos3.getSquaredDistance(blockPos4) + geodeCrackConfig.crackPointOffset) + t;
            }

            if (!(u < h)) {
                if (bl && v >= l && u < e) {
                    this.setBlockStateIf(structureWorldAccess, blockPos3, Blocks.AIR.getDefaultState(), predicate);

                    for (Direction direction : DIRECTIONS) {
                        BlockPos blockPos5 = blockPos3.offset(direction);
                        FluidState fluidState = structureWorldAccess.getFluidState(blockPos5);
                        if (!fluidState.isEmpty()) {
                            structureWorldAccess.scheduleFluidTick(blockPos5, fluidState.getFluid(), 0);
                        }
                    }
                } else if (u >= e) {
                    this.setBlockStateIf(structureWorldAccess, blockPos3, geodeLayerConfig.fillingProvider.get(random, blockPos3), predicate);
                } else if (u >= f) {
                    boolean bl2 = random.nextFloat() < geodeFeatureConfig.useAlternateLayer0Chance;
                    if (bl2) {
                        this.setBlockStateIf(structureWorldAccess, blockPos3, geodeLayerConfig.alternateInnerLayerProvider.get(random, blockPos3), predicate);
                    } else {
                        this.setBlockStateIf(structureWorldAccess, blockPos3, geodeLayerConfig.innerLayerProvider.get(random, blockPos3), predicate);
                    }

                    if ((!geodeFeatureConfig.placementsRequireLayer0Alternate || bl2) && random.nextFloat() < geodeFeatureConfig.usePotentialPlacementsChance) {
                        list3.add(blockPos3.toImmutable());
                    }
                } else if (u >= g) {
                    this.setBlockStateIf(structureWorldAccess, blockPos3, geodeLayerConfig.middleLayerProvider.get(random, blockPos3), predicate);
                } else if (u >= h) {
                    this.setBlockStateIf(structureWorldAccess, blockPos3, geodeLayerConfig.outerLayerProvider.get(random, blockPos3), predicate);
                }
            }
        }

        List<BlockState> list4 = geodeLayerConfig.innerBlocks;

        for (BlockPos blockPos6 : list3) {
            BlockState blockState2 = Util.getRandom(list4, random);

            for (Direction direction2 : DIRECTIONS) {
                if (blockState2.contains(Properties.FACING)) {
                    blockState2 = blockState2.with(Properties.FACING, direction2);
                }

                BlockPos blockPos7 = blockPos6.offset(direction2);
                BlockState blockState3 = structureWorldAccess.getBlockState(blockPos7);
                if (blockState2.contains(Properties.WATERLOGGED)) {
                    blockState2 = blockState2.with(Properties.WATERLOGGED, blockState3.getFluidState().isStill());
                }

                if (BuddingAmethystBlock.canGrowIn(blockState3)) {
                    this.setBlockStateIf(structureWorldAccess, blockPos7, blockState2, predicate);
                    break;
                }
            }
        }

        return true;
    }
}

