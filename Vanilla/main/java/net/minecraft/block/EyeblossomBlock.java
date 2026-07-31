package net.minecraft.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.TrailParticleEffect;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public class EyeblossomBlock extends FlowerBlock {
    public static final MapCodec<EyeblossomBlock> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(Codec.BOOL.fieldOf("open").forGetter(block -> block.state.open), createSettingsCodec())
            .apply(instance, EyeblossomBlock::new)
    );
    private static final int NOTIFY_RANGE_XZ = 3;
    private static final int NOTIFY_RANGE_Y = 2;
    private final EyeblossomBlock.EyeblossomState state;

    @Override
    public MapCodec<? extends EyeblossomBlock> getCodec() {
        return CODEC;
    }

    public EyeblossomBlock(EyeblossomBlock.EyeblossomState state, AbstractBlock.Settings settings) {
        super(state.stewEffect, state.effectLengthInSeconds, settings);
        this.state = state;
    }

    public EyeblossomBlock(boolean open, AbstractBlock.Settings settings) {
        super(EyeblossomBlock.EyeblossomState.of(open).stewEffect, EyeblossomBlock.EyeblossomState.of(open).effectLengthInSeconds, settings);
        this.state = EyeblossomBlock.EyeblossomState.of(open);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (this.state.isOpen() && random.nextInt(700) == 0) {
            BlockState blockState = world.getBlockState(pos.down());
            if (blockState.isOf(Blocks.PALE_MOSS_BLOCK)) {
                world.playSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_EYEBLOSSOM_IDLE, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
            }
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (this.updateStateAndNotifyOthers(state, world, pos, random)) {
            world.playSound((PlayerEntity)null, pos, this.state.getOpposite().longSound, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }

        super.randomTick(state, world, pos, random);
    }

    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (this.updateStateAndNotifyOthers(state, world, pos, random)) {
            world.playSound((PlayerEntity)null, pos, this.state.getOpposite().sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }

        super.scheduledTick(state, world, pos, random);
    }

    private boolean updateStateAndNotifyOthers(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!world.getDimension().natural()) {
            return false;
        }

        if (world.isDay() != this.state.open) {
            return false;
        }

        EyeblossomBlock.EyeblossomState eyeblossomState = this.state.getOpposite();
        world.setBlockState(pos, eyeblossomState.getBlockState(), Block.NOTIFY_ALL);
        world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(state));
        eyeblossomState.spawnTrailParticle(world, pos, random);
        BlockPos.iterate(pos.add(-3, -2, -3), pos.add(3, 2, 3)).forEach(otherPos -> {
            BlockState blockState2 = world.getBlockState(otherPos);
            if (blockState2 == state) {
                double d = Math.sqrt(pos.getSquaredDistance(otherPos));
                int i = random.nextBetween((int)(d * 5.0), (int)(d * 10.0));
                world.scheduleBlockTick(otherPos, state.getBlock(), i);
            }
        });
        return true;
    }

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient()
            && world.getDifficulty() != Difficulty.PEACEFUL
            && entity instanceof BeeEntity beeEntity
            && BeeEntity.isAttractive(state)
            && !beeEntity.hasStatusEffect(StatusEffects.POISON)) {
            beeEntity.addStatusEffect(this.getContactEffect());
        }
    }

    @Override
    public StatusEffectInstance getContactEffect() {
        return new StatusEffectInstance(StatusEffects.POISON, 25);
    }

    public enum EyeblossomState {
        OPEN(true, StatusEffects.BLINDNESS, 11.0F, SoundEvents.BLOCK_EYEBLOSSOM_OPEN_LONG, SoundEvents.BLOCK_EYEBLOSSOM_OPEN, 16545810),
        CLOSED(false, StatusEffects.NAUSEA, 7.0F, SoundEvents.BLOCK_EYEBLOSSOM_CLOSE_LONG, SoundEvents.BLOCK_EYEBLOSSOM_CLOSE, 6250335);

        final boolean open;
        final RegistryEntry<StatusEffect> stewEffect;
        final float effectLengthInSeconds;
        final SoundEvent longSound;
        final SoundEvent sound;
        private final int particleColor;

        EyeblossomState(
            final boolean open,
            final RegistryEntry<StatusEffect> stewEffect,
            final float effectLengthInSeconds,
            final SoundEvent longSound,
            final SoundEvent sound,
            final int particleColor
        ) {
            this.open = open;
            this.stewEffect = stewEffect;
            this.effectLengthInSeconds = effectLengthInSeconds;
            this.longSound = longSound;
            this.sound = sound;
            this.particleColor = particleColor;
        }

        public Block getBlock() {
            return this.open ? Blocks.OPEN_EYEBLOSSOM : Blocks.CLOSED_EYEBLOSSOM;
        }

        public BlockState getBlockState() {
            return this.getBlock().getDefaultState();
        }

        public EyeblossomBlock.EyeblossomState getOpposite() {
            return of(!this.open);
        }

        public boolean isOpen() {
            return this.open;
        }

        public static EyeblossomBlock.EyeblossomState of(boolean open) {
            return open ? OPEN : CLOSED;
        }

        public void spawnTrailParticle(ServerWorld world, BlockPos pos, Random random) {
            Vec3d vec3d = pos.toCenterPos();
            double d = 0.5 + random.nextDouble();
            Vec3d vec3d2 = new Vec3d(random.nextDouble() - 0.5, random.nextDouble() + 1.0, random.nextDouble() - 0.5);
            Vec3d vec3d3 = vec3d.add(vec3d2.multiply(d));
            TrailParticleEffect trailParticleEffect = new TrailParticleEffect(vec3d3, this.particleColor, (int)(20.0 * d));
            world.spawnParticles(trailParticleEffect, vec3d.x, vec3d.y, vec3d.z, 1, 0.0, 0.0, 0.0, 0.0);
        }

        public SoundEvent getLongSound() {
            return this.longSound;
        }
    }
}

