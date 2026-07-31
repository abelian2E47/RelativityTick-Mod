package net.minecraft.block.entity;

import com.mojang.datafixers.util.Either;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CreakingHeartBlock;
import net.minecraft.block.MultifaceBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LargeEntitySpawnHelper;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.CreakingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.TrailParticleEffect;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;

public class CreakingHeartBlockEntity extends BlockEntity {
    private static final int field_54776 = 32;
    public static final int field_54775 = 32;
    private static final int field_54777 = 34;
    private static final int field_54778 = 16;
    private static final int field_54779 = 8;
    private static final int field_54780 = 5;
    private static final int field_54781 = 20;
    private static final int field_55498 = 5;
    private static final int field_54782 = 100;
    private static final int field_54783 = 10;
    private static final int field_54784 = 10;
    private static final int field_54785 = 50;
    private static final int field_55085 = 2;
    private static final int field_55086 = 64;
    private static final int field_55499 = 30;
    private static final Optional<CreakingEntity> DEFAULT_CREAKING_PUPPET = Optional.empty();
    @Nullable
    private Either<CreakingEntity, UUID> creakingPuppet;
    private long ticks;
    private int creakingUpdateTimer;
    private int trailParticlesSpawnTimer;
    @Nullable
    private Vec3d lastCreakingPuppetPos;
    private int comparatorOutput;

    public CreakingHeartBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityType.CREAKING_HEART, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, CreakingHeartBlockEntity blockEntity) {
        blockEntity.ticks++;
        if (world instanceof ServerWorld serverWorld) {
            int i = blockEntity.calcComparatorOutput();
            if (blockEntity.comparatorOutput != i) {
                blockEntity.comparatorOutput = i;
                world.updateComparators(pos, Blocks.CREAKING_HEART);
            }

            if (blockEntity.trailParticlesSpawnTimer > 0) {
                if (blockEntity.trailParticlesSpawnTimer > 50) {
                    blockEntity.spawnTrailParticles(serverWorld, 1, true);
                    blockEntity.spawnTrailParticles(serverWorld, 1, false);
                }

                if (blockEntity.trailParticlesSpawnTimer % 10 == 0 && blockEntity.lastCreakingPuppetPos != null) {
                    blockEntity.getCreakingPuppet().ifPresent(creaking -> blockEntity.lastCreakingPuppetPos = creaking.getBoundingBox().getCenter());
                    Vec3d vec3d = Vec3d.ofCenter(pos);
                    float f = 0.2F + 0.8F * (100 - blockEntity.trailParticlesSpawnTimer) / 100.0F;
                    Vec3d vec3d2 = vec3d.subtract(blockEntity.lastCreakingPuppetPos).multiply(f).add(blockEntity.lastCreakingPuppetPos);
                    BlockPos blockPos = BlockPos.ofFloored(vec3d2);
                    float g = blockEntity.trailParticlesSpawnTimer / 2.0F / 100.0F + 0.5F;
                    serverWorld.playSound((PlayerEntity)null, blockPos, SoundEvents.BLOCK_CREAKING_HEART_HURT, SoundCategory.BLOCKS, g, 1.0F);
                }

                blockEntity.trailParticlesSpawnTimer--;
            }

            if (blockEntity.creakingUpdateTimer-- < 0) {
                blockEntity.creakingUpdateTimer = blockEntity.world == null ? 20 : blockEntity.world.random.nextInt(5) + 20;
                if (blockEntity.creakingPuppet == null) {
                    if (!CreakingHeartBlock.shouldBeEnabled(state, world, pos)) {
                        world.setBlockState(pos, state.with(CreakingHeartBlock.ACTIVE, false), Block.NOTIFY_ALL);
                    } else if (state.get(CreakingHeartBlock.ACTIVE)) {
                        if (CreakingHeartBlock.isNightAndNatural(world)) {
                            if (world.getDifficulty() != Difficulty.PEACEFUL) {
                                if (serverWorld.getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING)) {
                                    PlayerEntity playerEntity = world.getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), 32.0, false);
                                    if (playerEntity != null) {
                                        CreakingEntity creakingEntity2 = spawnCreakingPuppet(serverWorld, blockEntity);
                                        if (creakingEntity2 != null) {
                                            blockEntity.setCreakingPuppet(creakingEntity2);
                                            creakingEntity2.playSound(SoundEvents.ENTITY_CREAKING_SPAWN);
                                            world.playSound(
                                                (PlayerEntity)null,
                                                blockEntity.getPos(),
                                                SoundEvents.BLOCK_CREAKING_HEART_SPAWN,
                                                SoundCategory.BLOCKS,
                                                1.0F,
                                                1.0F
                                            );
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Optional<CreakingEntity> optional = blockEntity.getCreakingPuppet();
                    if (optional.isPresent()) {
                        CreakingEntity creakingEntity = optional.get();
                        if (!CreakingHeartBlock.isNightAndNatural(world) || blockEntity.getDistanceToPuppet() > 34.0 || creakingEntity.isStuckWithPlayer()) {
                            blockEntity.killPuppet(null);
                            return;
                        }

                        if (!CreakingHeartBlock.shouldBeEnabled(state, world, pos) && blockEntity.creakingPuppet == null) {
                            world.setBlockState(pos, state.with(CreakingHeartBlock.ACTIVE, false), Block.NOTIFY_ALL);
                        }
                    }
                }
            }
        }
    }

    private double getDistanceToPuppet() {
        return this.getCreakingPuppet().map(creaking -> Math.sqrt(creaking.squaredDistanceTo(Vec3d.ofBottomCenter(this.getPos())))).orElse(0.0);
    }

    private void clearCreakingPuppet() {
        this.creakingPuppet = null;
        this.markDirty();
    }

    public void setCreakingPuppet(CreakingEntity creakingPuppet) {
        this.creakingPuppet = Either.left(creakingPuppet);
        this.markDirty();
    }

    public void setCreakingPuppetFromUuid(UUID creakingPuppetUuid) {
        this.creakingPuppet = Either.right(creakingPuppetUuid);
        this.ticks = 0L;
        this.markDirty();
    }

    private Optional<CreakingEntity> getCreakingPuppet() {
        if (this.creakingPuppet == null) {
            return DEFAULT_CREAKING_PUPPET;
        }

        if (this.creakingPuppet.left().isPresent()) {
            CreakingEntity creakingEntity = this.creakingPuppet.left().get();
            if (!creakingEntity.isRemoved()) {
                return Optional.of(creakingEntity);
            }

            this.setCreakingPuppetFromUuid(creakingEntity.getUuid());
        }

        if (this.world instanceof ServerWorld serverWorld && this.creakingPuppet.right().isPresent()) {
            UUID uUID = this.creakingPuppet.right().get();
            if (serverWorld.getEntity(uUID) instanceof CreakingEntity creakingEntity2) {
                this.setCreakingPuppet(creakingEntity2);
                return Optional.of(creakingEntity2);
            } else {
                if (this.ticks >= 30L) {
                    this.clearCreakingPuppet();
                }

                return DEFAULT_CREAKING_PUPPET;
            }
        } else {
            return DEFAULT_CREAKING_PUPPET;
        }
    }

    @Nullable
    private static CreakingEntity spawnCreakingPuppet(ServerWorld world, CreakingHeartBlockEntity blockEntity) {
        BlockPos blockPos = blockEntity.getPos();
        Optional<CreakingEntity> optional = LargeEntitySpawnHelper.trySpawnAt(
            EntityType.CREAKING, SpawnReason.SPAWNER, world, blockPos, 5, 16, 8, LargeEntitySpawnHelper.Requirements.CREAKING, true
        );
        if (optional.isEmpty()) {
            return null;
        }

        CreakingEntity creakingEntity = optional.get();
        world.emitGameEvent(creakingEntity, GameEvent.ENTITY_PLACE, creakingEntity.getPos());
        world.sendEntityStatus(creakingEntity, EntityStatuses.ADD_DEATH_PARTICLES);
        creakingEntity.initHomePos(blockPos);
        return creakingEntity;
    }

    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        return this.createComponentlessNbt(registries);
    }

    public void onPuppetDamage() {
        if (this.getCreakingPuppet().orElse(null) instanceof CreakingEntity creakingEntity) {
            if (this.world instanceof ServerWorld serverWorld) {
                if (this.trailParticlesSpawnTimer <= 0) {
                    this.spawnTrailParticles(serverWorld, 20, false);
                    int i = this.world.getRandom().nextBetween(2, 3);

                    for (int j = 0; j < i; j++) {
                        this.findResinGenerationPos().ifPresent(pos -> {
                            this.world.playSound((PlayerEntity)null, pos, SoundEvents.BLOCK_RESIN_PLACE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                            this.world.emitGameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Emitter.of(this.world.getBlockState(pos)));
                        });
                    }

                    this.trailParticlesSpawnTimer = 100;
                    this.lastCreakingPuppetPos = creakingEntity.getBoundingBox().getCenter();
                }
            }
        }
    }

    private Optional<BlockPos> findResinGenerationPos() {
        Mutable<BlockPos> mutable = new MutableObject<>(null);
        BlockPos.iterateRecursively(this.pos, 2, 64, (pos, consumer) -> {
            for (Direction direction : Util.copyShuffled(Direction.values(), this.world.random)) {
                BlockPos blockPos = pos.offset(direction);
                if (this.world.getBlockState(blockPos).isIn(BlockTags.PALE_OAK_LOGS)) {
                    consumer.accept(blockPos);
                }
            }
        }, pos -> {
            if (!this.world.getBlockState(pos).isIn(BlockTags.PALE_OAK_LOGS)) {
                return BlockPos.IterationState.ACCEPT;
            }

            for (Direction direction : Util.copyShuffled(Direction.values(), this.world.random)) {
                BlockPos blockPos = pos.offset(direction);
                BlockState blockState = this.world.getBlockState(blockPos);
                Direction direction2 = direction.getOpposite();
                if (blockState.isAir()) {
                    blockState = Blocks.RESIN_CLUMP.getDefaultState();
                } else if (blockState.isOf(Blocks.WATER) && blockState.getFluidState().isStill()) {
                    blockState = Blocks.RESIN_CLUMP.getDefaultState().with(MultifaceBlock.WATERLOGGED, true);
                }

                if (blockState.isOf(Blocks.RESIN_CLUMP) && !MultifaceBlock.hasDirection(blockState, direction2)) {
                    this.world.setBlockState(blockPos, blockState.with(MultifaceBlock.getProperty(direction2), true), Block.NOTIFY_ALL);
                    mutable.setValue(blockPos);
                    return BlockPos.IterationState.STOP;
                }
            }

            return BlockPos.IterationState.ACCEPT;
        });
        return Optional.ofNullable(mutable.getValue());
    }

    private void spawnTrailParticles(ServerWorld world, int count, boolean towardsPuppet) {
        if (this.getCreakingPuppet().orElse(null) instanceof CreakingEntity creakingEntity) {
            int i = towardsPuppet ? 16545810 : 6250335;
            Random random = world.random;

            for (double d = 0.0; d < count; d++) {
                Box box = creakingEntity.getBoundingBox();
                Vec3d vec3d = box.getMinPos()
                    .add(random.nextDouble() * box.getLengthX(), random.nextDouble() * box.getLengthY(), random.nextDouble() * box.getLengthZ());
                Vec3d vec3d2 = Vec3d.of(this.getPos()).add(random.nextDouble(), random.nextDouble(), random.nextDouble());
                if (towardsPuppet) {
                    Vec3d vec3d3 = vec3d;
                    vec3d = vec3d2;
                    vec3d2 = vec3d3;
                }

                TrailParticleEffect trailParticleEffect = new TrailParticleEffect(vec3d2, i, random.nextInt(40) + 10);
                world.spawnParticles(trailParticleEffect, true, true, vec3d.x, vec3d.y, vec3d.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    public void killPuppet(@Nullable DamageSource damageSource) {
        if (this.getCreakingPuppet().orElse(null) instanceof CreakingEntity creakingEntity) {
            if (damageSource == null) {
                creakingEntity.finishCrumbling();
            } else {
                creakingEntity.killFromHeart(damageSource);
                creakingEntity.setCrumbling();
                creakingEntity.setHealth(0.0F);
            }

            this.clearCreakingPuppet();
        }
    }

    public boolean isPuppet(CreakingEntity creaking) {
        return this.getCreakingPuppet().map(puppet -> puppet == creaking).orElse(false);
    }

    public int getComparatorOutput() {
        return this.comparatorOutput;
    }

    public int calcComparatorOutput() {
        if (this.creakingPuppet != null && !this.getCreakingPuppet().isEmpty()) {
            double d = this.getDistanceToPuppet();
            double e = Math.clamp(d, 0.0, 32.0) / 32.0;
            return 15 - (int)Math.floor(e * 15.0);
        } else {
            return 0;
        }
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        if (nbt.contains("creaking")) {
            this.setCreakingPuppetFromUuid(nbt.getUuid("creaking"));
        } else {
            this.clearCreakingPuppet();
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        if (this.creakingPuppet != null) {
            nbt.putUuid("creaking", this.creakingPuppet.map(Entity::getUuid, uuid -> (UUID)uuid));
        }
    }
}

