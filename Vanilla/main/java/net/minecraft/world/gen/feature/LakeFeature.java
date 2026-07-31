package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

@Deprecated
public class LakeFeature extends Feature<LakeFeature.Config> {
    private static final BlockState CAVE_AIR = Blocks.CAVE_AIR.getDefaultState();

    public LakeFeature(Codec<LakeFeature.Config> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<LakeFeature.Config> context) {
        BlockPos blockPos = context.getOrigin();
        StructureWorldAccess structureWorldAccess = context.getWorld();
        Random random = context.getRandom();
        LakeFeature.Config config = context.getConfig();
        if (blockPos.getY() <= structureWorldAccess.getBottomY() + 4) {
            return false;
        }

        blockPos = blockPos.down(4);
        boolean[] bls = new boolean[2048];
        int i = random.nextInt(4) + 4;

        for (int j = 0; j < i; j++) {
            double d = random.nextDouble() * 6.0 + 3.0;
            double e = random.nextDouble() * 4.0 + 2.0;
            double f = random.nextDouble() * 6.0 + 3.0;
            double g = random.nextDouble() * (16.0 - d - 2.0) + 1.0 + d / 2.0;
            double h = random.nextDouble() * (8.0 - e - 4.0) + 2.0 + e / 2.0;
            double k = random.nextDouble() * (16.0 - f - 2.0) + 1.0 + f / 2.0;

            for (int l = 1; l < 15; l++) {
                for (int m = 1; m < 15; m++) {
                    for (int n = 1; n < 7; n++) {
                        double o = (l - g) / (d / 2.0);
                        double p = (n - h) / (e / 2.0);
                        double q = (m - k) / (f / 2.0);
                        double r = o * o + p * p + q * q;
                        if (r < 1.0) {
                            bls[(l * 16 + m) * 8 + n] = true;
                        }
                    }
                }
            }
        }

        BlockState blockState = config.fluid().get(random, blockPos);

        for (int s = 0; s < 16; s++) {
            for (int t = 0; t < 16; t++) {
                for (int u = 0; u < 8; u++) {
                    boolean bl = !bls[(s * 16 + t) * 8 + u]
                        && (
                            s < 15 && bls[((s + 1) * 16 + t) * 8 + u]
                                || s > 0 && bls[((s - 1) * 16 + t) * 8 + u]
                                || t < 15 && bls[(s * 16 + t + 1) * 8 + u]
                                || t > 0 && bls[(s * 16 + (t - 1)) * 8 + u]
                                || u < 7 && bls[(s * 16 + t) * 8 + u + 1]
                                || u > 0 && bls[(s * 16 + t) * 8 + (u - 1)]
                        );
                    if (bl) {
                        BlockState blockState2 = structureWorldAccess.getBlockState(blockPos.add(s, u, t));
                        if (u >= 4 && blockState2.isLiquid()) {
                            return false;
                        }

                        if (u < 4 && !blockState2.isSolid() && structureWorldAccess.getBlockState(blockPos.add(s, u, t)) != blockState) {
                            return false;
                        }
                    }
                }
            }
        }

        for (int v = 0; v < 16; v++) {
            for (int w = 0; w < 16; w++) {
                for (int x = 0; x < 8; x++) {
                    if (bls[(v * 16 + w) * 8 + x]) {
                        BlockPos blockPos2 = blockPos.add(v, x, w);
                        if (this.canReplace(structureWorldAccess.getBlockState(blockPos2))) {
                            boolean bl2 = x >= 4;
                            structureWorldAccess.setBlockState(blockPos2, bl2 ? CAVE_AIR : blockState, Block.NOTIFY_LISTENERS);
                            if (bl2) {
                                structureWorldAccess.scheduleBlockTick(blockPos2, CAVE_AIR.getBlock(), 0);
                                this.markBlocksAboveForPostProcessing(structureWorldAccess, blockPos2);
                            }
                        }
                    }
                }
            }
        }

        BlockState blockState3 = config.barrier().get(random, blockPos);
        if (!blockState3.isAir()) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int ab = 0; ab < 8; ab++) {
                        boolean bl3 = !bls[(y * 16 + z) * 8 + ab]
                            && (
                                y < 15 && bls[((y + 1) * 16 + z) * 8 + ab]
                                    || y > 0 && bls[((y - 1) * 16 + z) * 8 + ab]
                                    || z < 15 && bls[(y * 16 + z + 1) * 8 + ab]
                                    || z > 0 && bls[(y * 16 + (z - 1)) * 8 + ab]
                                    || ab < 7 && bls[(y * 16 + z) * 8 + ab + 1]
                                    || ab > 0 && bls[(y * 16 + z) * 8 + (ab - 1)]
                            );
                        if (bl3 && (ab < 4 || random.nextInt(2) != 0)) {
                            BlockState blockState4 = structureWorldAccess.getBlockState(blockPos.add(y, ab, z));
                            if (blockState4.isSolid() && !blockState4.isIn(BlockTags.LAVA_POOL_STONE_CANNOT_REPLACE)) {
                                BlockPos blockPos3 = blockPos.add(y, ab, z);
                                structureWorldAccess.setBlockState(blockPos3, blockState3, Block.NOTIFY_LISTENERS);
                                this.markBlocksAboveForPostProcessing(structureWorldAccess, blockPos3);
                            }
                        }
                    }
                }
            }
        }

        if (blockState.getFluidState().isIn(FluidTags.WATER)) {
            for (int bb = 0; bb < 16; bb++) {
                for (int cb = 0; cb < 16; cb++) {
                    int db = 4;
                    BlockPos blockPos4 = blockPos.add(bb, 4, cb);
                    if (structureWorldAccess.getBiome(blockPos4).value().canSetIce(structureWorldAccess, blockPos4, false)
                        && this.canReplace(structureWorldAccess.getBlockState(blockPos4))) {
                        structureWorldAccess.setBlockState(blockPos4, Blocks.ICE.getDefaultState(), Block.NOTIFY_LISTENERS);
                    }
                }
            }
        }

        return true;
    }

    private boolean canReplace(BlockState state) {
        return !state.isIn(BlockTags.FEATURES_CANNOT_REPLACE);
    }

    public record Config(BlockStateProvider fluid, BlockStateProvider barrier) implements FeatureConfig {
        public static final Codec<LakeFeature.Config> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    BlockStateProvider.TYPE_CODEC.fieldOf("fluid").forGetter(LakeFeature.Config::fluid),
                    BlockStateProvider.TYPE_CODEC.fieldOf("barrier").forGetter(LakeFeature.Config::barrier)
                )
                .apply(instance, LakeFeature.Config::new)
        );
    }
}

