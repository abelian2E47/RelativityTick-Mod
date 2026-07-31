package net.minecraft.entity.ai.pathing;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class BirdNavigation extends EntityNavigation {
    public BirdNavigation(MobEntity mobEntity, World world) {
        super(mobEntity, world);
    }

    @Override
    protected PathNodeNavigator createPathNodeNavigator(int range) {
        this.nodeMaker = new BirdPathNodeMaker();
        return new PathNodeNavigator(this.nodeMaker, range);
    }

    @Override
    protected boolean canPathDirectlyThrough(Vec3d origin, Vec3d target) {
        return doesNotCollide(this.entity, origin, target, true);
    }

    @Override
    protected boolean isAtValidPosition() {
        return this.canSwim() && this.entity.isInFluid() || !this.entity.hasVehicle();
    }

    @Override
    protected Vec3d getPos() {
        return this.entity.getPos();
    }

    @Override
    public Path findPathTo(Entity entity, int distance) {
        return this.findPathTo(entity.getBlockPos(), distance);
    }

    @Override
    public void tick() {
        this.tickCount++;
        if (this.inRecalculationCooldown) {
            this.recalculatePath();
        }

        if (!this.isIdle()) {
            if (this.isAtValidPosition()) {
                this.continueFollowingPath();
            } else if (this.currentPath != null && !this.currentPath.isFinished()) {
                Vec3d vec3d = this.currentPath.getNodePosition(this.entity);
                if (this.entity.getBlockX() == MathHelper.floor(vec3d.x)
                    && this.entity.getBlockY() == MathHelper.floor(vec3d.y)
                    && this.entity.getBlockZ() == MathHelper.floor(vec3d.z)) {
                    this.currentPath.next();
                }
            }

            DebugInfoSender.sendPathfindingData(this.world, this.entity, this.currentPath, this.nodeReachProximity);
            if (!this.isIdle()) {
                Vec3d vec3d2 = this.currentPath.getNodePosition(this.entity);
                this.entity.getMoveControl().moveTo(vec3d2.x, vec3d2.y, vec3d2.z, this.speed);
            }
        }
    }

    public void setCanPathThroughDoors(boolean canPathThroughDoors) {
        this.nodeMaker.setCanOpenDoors(canPathThroughDoors);
    }

    @Override
    public boolean isValidPosition(BlockPos pos) {
        return this.world.getBlockState(pos).hasSolidTopSurface(this.world, pos, this.entity);
    }
}

