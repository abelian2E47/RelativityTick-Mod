package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import java.util.Collection;
import net.minecraft.block.entity.SculkSpreadManager;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;

public class SculkVeinBlock extends MultifaceGrowthBlock implements SculkSpreadable {
    public static final MapCodec<SculkVeinBlock> CODEC = createCodec(SculkVeinBlock::new);
    private final MultifaceGrower allGrowTypeGrower = new MultifaceGrower(new SculkVeinBlock.SculkVeinGrowChecker(MultifaceGrower.GROW_TYPES));
    private final MultifaceGrower samePositionOnlyGrower = new MultifaceGrower(new SculkVeinBlock.SculkVeinGrowChecker(MultifaceGrower.GrowType.SAME_POSITION));

    @Override
    public MapCodec<SculkVeinBlock> getCodec() {
        return CODEC;
    }

    public SculkVeinBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public MultifaceGrower getGrower() {
        return this.allGrowTypeGrower;
    }

    public MultifaceGrower getSamePositionOnlyGrower() {
        return this.samePositionOnlyGrower;
    }

    public static boolean place(WorldAccess world, BlockPos pos, BlockState state, Collection<Direction> directions) {
        boolean bl = false;
        BlockState blockState = Blocks.SCULK_VEIN.getDefaultState();

        for (Direction direction : directions) {
            if (canGrowOn(world, pos, direction)) {
                blockState = blockState.with(getProperty(direction), true);
                bl = true;
            }
        }

        if (!bl) {
            return false;
        }

        if (!state.getFluidState().isEmpty()) {
            blockState = blockState.with(MultifaceBlock.WATERLOGGED, true);
        }

        world.setBlockState(pos, blockState, Block.NOTIFY_ALL);
        return true;
    }

    @Override
    public void spreadAtSamePosition(WorldAccess world, BlockState state, BlockPos pos, Random random) {
        if (state.isOf(this)) {
            for (Direction direction : DIRECTIONS) {
                BooleanProperty booleanProperty = getProperty(direction);
                if (state.get(booleanProperty) && world.getBlockState(pos.offset(direction)).isOf(Blocks.SCULK)) {
                    state = state.with(booleanProperty, false);
                }
            }

            if (!hasAnyDirection(state)) {
                FluidState fluidState = world.getFluidState(pos);
                state = (fluidState.isEmpty() ? Blocks.AIR : Blocks.WATER).getDefaultState();
            }

            world.setBlockState(pos, state, Block.NOTIFY_ALL);
            SculkSpreadable.super.spreadAtSamePosition(world, state, pos, random);
        }
    }

    @Override
    public int spread(
        SculkSpreadManager.Cursor cursor,
        WorldAccess world,
        BlockPos catalystPos,
        Random random,
        SculkSpreadManager spreadManager,
        boolean shouldConvertToBlock
    ) {
        if (shouldConvertToBlock && this.convertToBlock(spreadManager, world, cursor.getPos(), random)) {
            return cursor.getCharge() - 1;
        } else {
            return random.nextInt(spreadManager.getSpreadChance()) == 0 ? MathHelper.floor(cursor.getCharge() * 0.5F) : cursor.getCharge();
        }
    }

    private boolean convertToBlock(SculkSpreadManager spreadManager, WorldAccess world, BlockPos pos, Random random) {
        BlockState blockState = world.getBlockState(pos);
        TagKey<Block> tagKey = spreadManager.getReplaceableTag();

        for (Direction direction : Direction.shuffle(random)) {
            if (hasDirection(blockState, direction)) {
                BlockPos blockPos = pos.offset(direction);
                BlockState blockState2 = world.getBlockState(blockPos);
                if (blockState2.isIn(tagKey)) {
                    BlockState blockState3 = Blocks.SCULK.getDefaultState();
                    world.setBlockState(blockPos, blockState3, Block.NOTIFY_ALL);
                    Block.pushEntitiesUpBeforeBlockChange(blockState2, blockState3, world, blockPos);
                    world.playSound(null, blockPos, SoundEvents.BLOCK_SCULK_SPREAD, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    this.allGrowTypeGrower.grow(blockState3, world, blockPos, spreadManager.isWorldGen());
                    Direction direction2 = direction.getOpposite();

                    for (Direction direction3 : DIRECTIONS) {
                        if (direction3 != direction2) {
                            BlockPos blockPos2 = blockPos.offset(direction3);
                            BlockState blockState4 = world.getBlockState(blockPos2);
                            if (blockState4.isOf(this)) {
                                this.spreadAtSamePosition(world, blockState4, blockPos2, random);
                            }
                        }
                    }

                    return true;
                }
            }
        }

        return false;
    }

    public static boolean veinCoversSculkReplaceable(WorldAccess world, BlockState state, BlockPos pos) {
        if (!state.isOf(Blocks.SCULK_VEIN)) {
            return false;
        }

        for (Direction direction : DIRECTIONS) {
            if (hasDirection(state, direction) && world.getBlockState(pos.offset(direction)).isIn(BlockTags.SCULK_REPLACEABLE)) {
                return true;
            }
        }

        return false;
    }

    class SculkVeinGrowChecker extends MultifaceGrower.LichenGrowChecker {
        private final MultifaceGrower.GrowType[] growTypes;

        public SculkVeinGrowChecker(final MultifaceGrower.GrowType... growTypes) {
            super(SculkVeinBlock.this);
            this.growTypes = growTypes;
        }

        @Override
        public boolean canGrow(BlockView world, BlockPos pos, BlockPos growPos, Direction direction, BlockState state) {
            BlockState blockState = world.getBlockState(growPos.offset(direction));
            if (!blockState.isOf(Blocks.SCULK) && !blockState.isOf(Blocks.SCULK_CATALYST) && !blockState.isOf(Blocks.MOVING_PISTON)) {
                if (pos.getManhattanDistance(growPos) == 2) {
                    BlockPos blockPos = pos.offset(direction.getOpposite());
                    if (world.getBlockState(blockPos).isSideSolidFullSquare(world, blockPos, direction)) {
                        return false;
                    }
                }

                FluidState fluidState = state.getFluidState();
                if (!fluidState.isEmpty() && !fluidState.isOf(Fluids.WATER)) {
                    return false;
                } else {
                    return state.isIn(BlockTags.FIRE) ? false : state.isReplaceable() || super.canGrow(world, pos, growPos, direction, state);
                }
            } else {
                return false;
            }
        }

        @Override
        public MultifaceGrower.GrowType[] getGrowTypes() {
            return this.growTypes;
        }

        @Override
        public boolean canGrow(BlockState state) {
            return !state.isOf(Blocks.SCULK_VEIN);
        }
    }
}

