package net.minecraft.entity.mob;

import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CreakingHeartBlock;
import net.minecraft.block.entity.CreakingHeartBlockEntity;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.control.BodyControl;
import net.minecraft.entity.ai.control.JumpControl;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.PathContext;
import net.minecraft.entity.ai.pathing.PathNodeNavigator;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;

public class CreakingEntity extends HostileEntity {
    private static final TrackedData<Boolean> UNROOTED = DataTracker.registerData(CreakingEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> ACTIVE = DataTracker.registerData(CreakingEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> CRUMBLING = DataTracker.registerData(CreakingEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Optional<BlockPos>> HOME_POS = DataTracker.registerData(
        CreakingEntity.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_POS
    );
    private static final int field_54573 = 15;
    private static final int field_54574 = 1;
    private static final float ATTACK_DAMAGE = 3.0F;
    private static final float field_54576 = 32.0F;
    private static final float field_54577 = 144.0F;
    public static final int field_54566 = 40;
    private static final float field_54578 = 0.4F;
    public static final float field_54567 = 0.3F;
    public static final int field_54569 = 16545810;
    public static final int field_54580 = 6250335;
    public static final int field_55485 = 8;
    public static final int field_55486 = 45;
    private static final int field_55488 = 4;
    private int attackAnimationTimer;
    public final AnimationState attackAnimationState = new AnimationState();
    public final AnimationState invulnerableAnimationState = new AnimationState();
    public final AnimationState crumblingAnimationState = new AnimationState();
    private int invulnerableAnimationTimer;
    private boolean glowingEyesWhileCrumbling;
    private int nextEyeFlickerTime;
    private int playerIntersectionTimer;

    public CreakingEntity(EntityType<? extends CreakingEntity> entityType, World world) {
        super(entityType, world);
        this.lookControl = new CreakingEntity.CreakingLookControl(this);
        this.moveControl = new CreakingEntity.CreakingMoveControl(this);
        this.jumpControl = new CreakingEntity.CreakingJumpControl(this);
        MobNavigation mobNavigation = (MobNavigation)this.getNavigation();
        mobNavigation.setCanSwim(true);
        this.experiencePoints = 0;
    }

    public void initHomePos(BlockPos homePos) {
        this.setHomePos(homePos);
        this.setPathfindingPenalty(PathNodeType.DAMAGE_OTHER, 8.0F);
        this.setPathfindingPenalty(PathNodeType.POWDER_SNOW, 8.0F);
        this.setPathfindingPenalty(PathNodeType.LAVA, 8.0F);
        this.setPathfindingPenalty(PathNodeType.DAMAGE_FIRE, 0.0F);
        this.setPathfindingPenalty(PathNodeType.DANGER_FIRE, 0.0F);
    }

    public boolean isTransient() {
        return this.getHomePos() != null;
    }

    @Override
    protected BodyControl createBodyControl() {
        return new CreakingEntity.CreakingBodyControl(this);
    }

    @Override
    protected Brain.Profile<CreakingEntity> createBrainProfile() {
        return CreakingBrain.createBrainProfile();
    }

    @Override
    protected Brain<?> deserializeBrain(Dynamic<?> dynamic) {
        return CreakingBrain.create(this.createBrainProfile().deserialize(dynamic));
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(UNROOTED, true);
        builder.add(ACTIVE, false);
        builder.add(CRUMBLING, false);
        builder.add(HOME_POS, Optional.empty());
    }

    public static DefaultAttributeContainer.Builder createCreakingAttributes() {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.MAX_HEALTH, 1.0)
            .add(EntityAttributes.MOVEMENT_SPEED, 0.4F)
            .add(EntityAttributes.ATTACK_DAMAGE, 3.0)
            .add(EntityAttributes.FOLLOW_RANGE, 32.0)
            .add(EntityAttributes.STEP_HEIGHT, 1.0625);
    }

    public boolean isUnrooted() {
        return this.dataTracker.get(UNROOTED);
    }

    @Override
    public boolean tryAttack(ServerWorld world, Entity target) {
        if (!(target instanceof LivingEntity)) {
            return false;
        }

        this.attackAnimationTimer = 15;
        this.getWorld().sendEntityStatus(this, EntityStatuses.PLAY_ATTACK_SOUND);
        return super.tryAttack(world, target);
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        BlockPos blockPos = this.getHomePos();
        if (blockPos == null || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.damage(world, source, amount);
        }

        if (!this.isInvulnerableTo(world, source) && this.invulnerableAnimationTimer <= 0 && !this.isDead()) {
            PlayerEntity playerEntity = this.becomeAngryAndGetPlayer(source);
            Entity entity = source.getSource();
            if (!(entity instanceof LivingEntity) && !(entity instanceof ProjectileEntity) && playerEntity == null) {
                return false;
            }

            this.invulnerableAnimationTimer = 8;
            this.getWorld().sendEntityStatus(this, EntityStatuses.INVULNERABLE_CREAKING_HIT);
            if (this.getWorld().getBlockEntity(blockPos) instanceof CreakingHeartBlockEntity creakingHeartBlockEntity
                && creakingHeartBlockEntity.isPuppet(this)) {
                if (playerEntity != null) {
                    creakingHeartBlockEntity.onPuppetDamage();
                }

                this.playHurtSound(source);
            }

            return true;
        } else {
            return false;
        }
    }

    public PlayerEntity becomeAngryAndGetPlayer(DamageSource damageSource) {
        this.becomeAngry(damageSource);
        return this.setAttackingPlayer(damageSource);
    }

    @Override
    public boolean isPushable() {
        return super.isPushable() && this.isUnrooted();
    }

    @Override
    public void addVelocity(double deltaX, double deltaY, double deltaZ) {
        if (this.isUnrooted()) {
            super.addVelocity(deltaX, deltaY, deltaZ);
        }
    }

    @Override
    public Brain<CreakingEntity> getBrain() {
        return (Brain<CreakingEntity>)super.getBrain();
    }

    @Override
    protected void mobTick(ServerWorld world) {
        Profiler profiler = Profilers.get();
        profiler.push("creakingBrain");
        this.getBrain().tick((ServerWorld)this.getWorld(), this);
        profiler.pop();
        CreakingBrain.updateActivities(this);
    }

    @Override
    public void tickMovement() {
        if (this.invulnerableAnimationTimer > 0) {
            this.invulnerableAnimationTimer--;
        }

        if (this.attackAnimationTimer > 0) {
            this.attackAnimationTimer--;
        }

        if (!this.getWorld().isClient) {
            boolean bl = this.dataTracker.get(UNROOTED);
            boolean bl2 = this.shouldBeUnrooted();
            if (bl2 != bl) {
                this.emitGameEvent(GameEvent.ENTITY_ACTION);
                if (bl2) {
                    this.playSound(SoundEvents.ENTITY_CREAKING_UNFREEZE);
                } else {
                    this.stopMovement();
                    this.playSound(SoundEvents.ENTITY_CREAKING_FREEZE);
                }
            }

            this.dataTracker.set(UNROOTED, bl2);
        }

        super.tickMovement();
    }

    @Override
    public void tick() {
        if (!this.getWorld().isClient) {
            BlockPos blockPos = this.getHomePos();
            if (blockPos != null) {
                boolean bl = this.getWorld().getBlockEntity(blockPos) instanceof CreakingHeartBlockEntity creakingHeartBlockEntity
                    && creakingHeartBlockEntity.isPuppet(this);
                if (!bl) {
                    this.setHealth(0.0F);
                }
            }
        }

        super.tick();
        if (this.getWorld().isClient) {
            this.tickAttackAnimation();
            this.updateCrumblingEyeFlicker();
        }
    }

    @Override
    protected void updatePostDeath() {
        if (this.isTransient() && this.isCrumbling()) {
            this.deathTime++;
            if (!this.getWorld().isClient() && this.deathTime > 45 && !this.isRemoved()) {
                this.finishCrumbling();
            }
        } else {
            super.updatePostDeath();
        }
    }

    @Override
    protected void updateLimbs(float posDelta) {
        float f = Math.min(posDelta * 25.0F, 3.0F);
        this.limbAnimator.updateLimbs(f, 0.4F, 1.0F);
    }

    private void tickAttackAnimation() {
        this.attackAnimationState.setRunning(this.attackAnimationTimer > 0, this.age);
        this.invulnerableAnimationState.setRunning(this.invulnerableAnimationTimer > 0, this.age);
        this.crumblingAnimationState.setRunning(this.isCrumbling(), this.age);
    }

    public void finishCrumbling() {
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            Box box = this.getBoundingBox();
            Vec3d vec3d = box.getCenter();
            double d = box.getLengthX() * 0.3;
            double e = box.getLengthY() * 0.3;
            double f = box.getLengthZ() * 0.3;
            serverWorld.spawnParticles(
                new BlockStateParticleEffect(ParticleTypes.BLOCK_CRUMBLE, Blocks.PALE_OAK_WOOD.getDefaultState()), vec3d.x, vec3d.y, vec3d.z, 100, d, e, f, 0.0
            );
            serverWorld.spawnParticles(
                new BlockStateParticleEffect(ParticleTypes.BLOCK_CRUMBLE, Blocks.CREAKING_HEART.getDefaultState().with(CreakingHeartBlock.ACTIVE, true)),
                vec3d.x,
                vec3d.y,
                vec3d.z,
                10,
                d,
                e,
                f,
                0.0
            );
        }

        this.playSound(this.getDeathSound());
        this.remove(Entity.RemovalReason.DISCARDED);
    }

    public void killFromHeart(DamageSource damageSource) {
        this.becomeAngryAndGetPlayer(damageSource);
        this.onDeath(damageSource);
        this.playSound(SoundEvents.ENTITY_CREAKING_TWITCH);
    }

    @Override
    public void handleStatus(byte status) {
        if (status == EntityStatuses.INVULNERABLE_CREAKING_HIT) {
            this.invulnerableAnimationTimer = 8;
            this.playHurtSound(this.getDamageSources().generic());
        } else if (status == EntityStatuses.PLAY_ATTACK_SOUND) {
            this.attackAnimationTimer = 15;
            this.playAttackSound();
        } else {
            super.handleStatus(status);
        }
    }

    @Override
    public boolean isFireImmune() {
        return this.isTransient() || super.isFireImmune();
    }

    @Override
    public boolean canBeNameTagged() {
        return !this.isTransient() && super.canBeNameTagged();
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return !this.isTransient() && super.canAddPassenger(passenger);
    }

    @Override
    protected boolean couldAcceptPassenger() {
        return !this.isTransient() && super.couldAcceptPassenger();
    }

    @Override
    protected void addPassenger(Entity passenger) {
        if (this.isTransient()) {
            throw new IllegalStateException("Should never addPassenger without checking couldAcceptPassenger()");
        }
    }

    @Override
    public boolean canUsePortals(boolean allowVehicles) {
        return !this.isTransient() && super.canUsePortals(allowVehicles);
    }

    @Override
    protected EntityNavigation createNavigation(World world) {
        return new CreakingEntity.CreakingNavigation(this, world);
    }

    public boolean isStuckWithPlayer() {
        List<PlayerEntity> list = this.brain.getOptionalRegisteredMemory(MemoryModuleType.NEAREST_PLAYERS).orElse(List.of());
        if (list.isEmpty()) {
            this.playerIntersectionTimer = 0;
            return false;
        }

        Box box = this.getBoundingBox();

        for (PlayerEntity playerEntity : list) {
            if (box.contains(playerEntity.getEyePos())) {
                this.playerIntersectionTimer++;
                return this.playerIntersectionTimer > 4;
            }
        }

        this.playerIntersectionTimer = 0;
        return false;
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("home_pos")) {
            this.initHomePos(NbtHelper.toBlockPos(nbt, "home_pos").orElseThrow());
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        BlockPos blockPos = this.getHomePos();
        if (blockPos != null) {
            nbt.put("home_pos", NbtHelper.fromBlockPos(blockPos));
        }
    }

    public void setHomePos(BlockPos pos) {
        this.dataTracker.set(HOME_POS, Optional.of(pos));
    }

    @Nullable
    public BlockPos getHomePos() {
        return this.dataTracker.get(HOME_POS).orElse(null);
    }

    public void setCrumbling() {
        this.dataTracker.set(CRUMBLING, true);
    }

    public boolean isCrumbling() {
        return this.dataTracker.get(CRUMBLING);
    }

    public boolean hasGlowingEyesWhileCrumbling() {
        return this.glowingEyesWhileCrumbling;
    }

    public void updateCrumblingEyeFlicker() {
        if (this.deathTime > this.nextEyeFlickerTime) {
            this.nextEyeFlickerTime = this.deathTime
                + this.getRandom()
                    .nextBetween(this.glowingEyesWhileCrumbling ? 2 : this.deathTime / 4, this.glowingEyesWhileCrumbling ? 8 : this.deathTime / 2);
            this.glowingEyesWhileCrumbling = !this.glowingEyesWhileCrumbling;
        }
    }

    @Override
    public void playAttackSound() {
        this.playSound(SoundEvents.ENTITY_CREAKING_ATTACK);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isActive() ? null : SoundEvents.ENTITY_CREAKING_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.isTransient() ? SoundEvents.ENTITY_CREAKING_SWAY : super.getHurtSound(source);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_CREAKING_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.ENTITY_CREAKING_STEP, 0.15F, 1.0F);
    }

    @Nullable
    @Override
    public LivingEntity getTarget() {
        return this.getTargetInBrain();
    }

    @Override
    protected void sendAiDebugData() {
        super.sendAiDebugData();
        DebugInfoSender.sendBrainDebugData(this);
    }

    @Override
    public void takeKnockback(double strength, double x, double z) {
        if (this.isUnrooted()) {
            super.takeKnockback(strength, x, z);
        }
    }

    public boolean shouldBeUnrooted() {
        List<PlayerEntity> list = this.brain.getOptionalRegisteredMemory(MemoryModuleType.NEAREST_PLAYERS).orElse(List.of());
        boolean bl = this.isActive();
        if (list.isEmpty()) {
            if (bl) {
                this.deactivate();
            }

            return true;
        } else {
            boolean bl2 = false;

            for (PlayerEntity playerEntity : list) {
                if (this.canTarget(playerEntity) && !this.isTeammate(playerEntity)) {
                    bl2 = true;
                    if ((!bl || LivingEntity.NOT_WEARING_GAZE_DISGUISE_PREDICATE.test(playerEntity))
                        && this.isEntityLookingAtMe(
                            playerEntity, 0.5, false, true, this.getEyeY(), this.getY() + 0.5 * this.getScale(), (this.getEyeY() + this.getY()) / 2.0
                        )) {
                        if (bl) {
                            return false;
                        }

                        if (playerEntity.squaredDistanceTo(this) < 144.0) {
                            this.activate(playerEntity);
                            return false;
                        }
                    }
                }
            }

            if (!bl2 && bl) {
                this.deactivate();
            }

            return true;
        }
    }

    public void activate(PlayerEntity player) {
        this.getBrain().remember(MemoryModuleType.ATTACK_TARGET, player);
        this.emitGameEvent(GameEvent.ENTITY_ACTION);
        this.playSound(SoundEvents.ENTITY_CREAKING_ACTIVATE);
        this.setActive(true);
    }

    public void deactivate() {
        this.getBrain().forget(MemoryModuleType.ATTACK_TARGET);
        this.emitGameEvent(GameEvent.ENTITY_ACTION);
        this.playSound(SoundEvents.ENTITY_CREAKING_DEACTIVATE);
        this.setActive(false);
    }

    public void setActive(boolean active) {
        this.dataTracker.set(ACTIVE, active);
    }

    public boolean isActive() {
        return this.dataTracker.get(ACTIVE);
    }

    @Override
    public float getPathfindingFavor(BlockPos pos, WorldView world) {
        return 0.0F;
    }

    class CreakingBodyControl extends BodyControl {
        public CreakingBodyControl(final CreakingEntity creaking) {
            super(creaking);
        }

        @Override
        public void tick() {
            if (CreakingEntity.this.isUnrooted()) {
                super.tick();
            }
        }
    }

    class CreakingJumpControl extends JumpControl {
        public CreakingJumpControl(final CreakingEntity creaking) {
            super(creaking);
        }

        @Override
        public void tick() {
            if (CreakingEntity.this.isUnrooted()) {
                super.tick();
            } else {
                CreakingEntity.this.setJumping(false);
            }
        }
    }

    class CreakingLandPathNodeMaker extends LandPathNodeMaker {
        private static final int field_54896 = 1024;

        @Override
        public PathNodeType getDefaultNodeType(PathContext context, int x, int y, int z) {
            BlockPos blockPos = CreakingEntity.this.getHomePos();
            if (blockPos == null) {
                return super.getDefaultNodeType(context, x, y, z);
            }

            double d = blockPos.getSquaredDistance(new Vec3i(x, y, z));
            return d > 1024.0 && d >= blockPos.getSquaredDistance(context.getEntityPos()) ? PathNodeType.BLOCKED : super.getDefaultNodeType(context, x, y, z);
        }
    }

    class CreakingLookControl extends LookControl {
        public CreakingLookControl(final CreakingEntity creaking) {
            super(creaking);
        }

        @Override
        public void tick() {
            if (CreakingEntity.this.isUnrooted()) {
                super.tick();
            }
        }
    }

    class CreakingMoveControl extends MoveControl {
        public CreakingMoveControl(final CreakingEntity creaking) {
            super(creaking);
        }

        @Override
        public void tick() {
            if (CreakingEntity.this.isUnrooted()) {
                super.tick();
            }
        }
    }

    class CreakingNavigation extends MobNavigation {
        CreakingNavigation(final CreakingEntity creaking, final World world) {
            super(creaking, world);
        }

        @Override
        public void tick() {
            if (CreakingEntity.this.isUnrooted()) {
                super.tick();
            }
        }

        @Override
        protected PathNodeNavigator createPathNodeNavigator(int range) {
            this.nodeMaker = CreakingEntity.this.new CreakingLandPathNodeMaker();
            this.nodeMaker.setCanEnterOpenDoors(true);
            return new PathNodeNavigator(this.nodeMaker, range);
        }
    }
}

