package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.dimension.NetherPortal;

public abstract class AbstractFireBlock extends Block {
    private static final int SET_ON_FIRE_SECONDS = 8;
    private static final int MIN_FIRE_TICK_INCREMENT = 1;
    private static final int MAX_FIRE_TICK_INCREMENT = 3;
    private final float damage;
    protected static final float BASE_SOUND_VOLUME = 1.0F;
    protected static final VoxelShape BASE_SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);

    public AbstractFireBlock(AbstractBlock.Settings settings, float damage) {
        super(settings);
        this.damage = damage;
    }

    @Override
    protected abstract MapCodec<? extends AbstractFireBlock> getCodec();

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getState(ctx.getWorld(), ctx.getBlockPos());
    }

    public static BlockState getState(BlockView world, BlockPos pos) {
        BlockPos blockPos = pos.down();
        BlockState blockState = world.getBlockState(blockPos);
        return SoulFireBlock.isSoulBase(blockState) ? Blocks.SOUL_FIRE.getDefaultState() : ((FireBlock)Blocks.FIRE).getStateForPosition(world, pos);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return BASE_SHAPE;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (random.nextInt(24) == 0) {
            world.playSound(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                SoundEvents.BLOCK_FIRE_AMBIENT,
                SoundCategory.BLOCKS,
                1.0F + random.nextFloat(),
                random.nextFloat() * 0.7F + 0.3F,
                false
            );
        }

        BlockPos blockPos = pos.down();
        BlockState blockState = world.getBlockState(blockPos);
        if (!this.isFlammable(blockState) && !blockState.isSideSolidFullSquare(world, blockPos, Direction.UP)) {
            if (this.isFlammable(world.getBlockState(pos.west()))) {
                for (int j = 0; j < 2; j++) {
                    double g = pos.getX() + random.nextDouble() * 0.1F;
                    double h = pos.getY() + random.nextDouble();
                    double k = pos.getZ() + random.nextDouble();
                    world.addParticle(ParticleTypes.LARGE_SMOKE, g, h, k, 0.0, 0.0, 0.0);
                }
            }

            if (this.isFlammable(world.getBlockState(pos.east()))) {
                for (int l = 0; l < 2; l++) {
                    double m = pos.getX() + 1 - random.nextDouble() * 0.1F;
                    double n = pos.getY() + random.nextDouble();
                    double o = pos.getZ() + random.nextDouble();
                    world.addParticle(ParticleTypes.LARGE_SMOKE, m, n, o, 0.0, 0.0, 0.0);
                }
            }

            if (this.isFlammable(world.getBlockState(pos.north()))) {
                for (int p = 0; p < 2; p++) {
                    double q = pos.getX() + random.nextDouble();
                    double r = pos.getY() + random.nextDouble();
                    double s = pos.getZ() + random.nextDouble() * 0.1F;
                    world.addParticle(ParticleTypes.LARGE_SMOKE, q, r, s, 0.0, 0.0, 0.0);
                }
            }

            if (this.isFlammable(world.getBlockState(pos.south()))) {
                for (int t = 0; t < 2; t++) {
                    double u = pos.getX() + random.nextDouble();
                    double v = pos.getY() + random.nextDouble();
                    double w = pos.getZ() + 1 - random.nextDouble() * 0.1F;
                    world.addParticle(ParticleTypes.LARGE_SMOKE, u, v, w, 0.0, 0.0, 0.0);
                }
            }

            if (this.isFlammable(world.getBlockState(pos.up()))) {
                for (int x = 0; x < 2; x++) {
                    double y = pos.getX() + random.nextDouble();
                    double z = pos.getY() + 1 - random.nextDouble() * 0.1F;
                    double ab = pos.getZ() + random.nextDouble();
                    world.addParticle(ParticleTypes.LARGE_SMOKE, y, z, ab, 0.0, 0.0, 0.0);
                }
            }
        } else {
            for (int i = 0; i < 3; i++) {
                double d = pos.getX() + random.nextDouble();
                double e = pos.getY() + random.nextDouble() * 0.5 + 0.5;
                double f = pos.getZ() + random.nextDouble();
                world.addParticle(ParticleTypes.LARGE_SMOKE, d, e, f, 0.0, 0.0, 0.0);
            }
        }
    }

    protected abstract boolean isFlammable(BlockState state);

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!entity.isFireImmune()) {
            if (entity.getFireTicks() < 0) {
                entity.setFireTicks(entity.getFireTicks() + 1);
            } else if (entity instanceof ServerPlayerEntity) {
                int i = world.getRandom().nextBetweenExclusive(1, 3);
                entity.setFireTicks(entity.getFireTicks() + i);
            }

            if (entity.getFireTicks() >= 0) {
                entity.setOnFireFor(8.0F);
            }
        }

        entity.serverDamage(world.getDamageSources().inFire(), this.damage);
        super.onEntityCollision(state, world, pos, entity);
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.isOf(state.getBlock())) {
            if (isOverworldOrNether(world)) {
                Optional<NetherPortal> optional = NetherPortal.getNewPortal(world, pos, Direction.Axis.X);
                if (optional.isPresent()) {
                    optional.get().createPortal(world);
                    return;
                }
            }

            if (!state.canPlaceAt(world, pos)) {
                world.removeBlock(pos, false);
            }
        }
    }

    private static boolean isOverworldOrNether(World world) {
        return world.getRegistryKey() == World.OVERWORLD || world.getRegistryKey() == World.NETHER;
    }

    @Override
    protected void spawnBreakParticles(World world, PlayerEntity player, BlockPos pos, BlockState state) {
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient()) {
            world.syncWorldEvent(null, WorldEvents.FIRE_EXTINGUISHED, pos, 0);
        }

        return super.onBreak(world, pos, state, player);
    }

    public static boolean canPlaceAt(World world, BlockPos pos, Direction direction) {
        BlockState blockState = world.getBlockState(pos);
        return !blockState.isAir() ? false : getState(world, pos).canPlaceAt(world, pos) || shouldLightPortalAt(world, pos, direction);
    }

    private static boolean shouldLightPortalAt(World world, BlockPos pos, Direction direction) {
        if (!isOverworldOrNether(world)) {
            return false;
        }

        BlockPos.Mutable mutable = pos.mutableCopy();
        boolean bl = false;

        for (Direction direction2 : Direction.values()) {
            if (world.getBlockState(mutable.set(pos).move(direction2)).isOf(Blocks.OBSIDIAN)) {
                bl = true;
                break;
            }
        }

        if (!bl) {
            return false;
        }

        Direction.Axis axis = direction.getAxis().isHorizontal()
            ? direction.rotateYCounterclockwise().getAxis()
            : Direction.Type.HORIZONTAL.randomAxis(world.random);
        return NetherPortal.getNewPortal(world, pos, axis).isPresent();
    }
}

