package net.minecraft.client.render.item.property.numeric;

import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public abstract class NeedleAngleState {
    private final boolean wobble;

    protected NeedleAngleState(boolean wobble) {
        this.wobble = wobble;
    }

    public float getValue(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity user, int seed) {
        Entity entity = user != null ? user : stack.getHolder();
        if (entity == null) {
            return 0.0F;
        }

        if (world == null && entity.getWorld() instanceof ClientWorld clientWorld) {
            world = clientWorld;
        }

        return world == null ? 0.0F : this.getAngle(stack, world, seed, entity);
    }

    protected abstract float getAngle(ItemStack stack, ClientWorld world, int seed, Entity user);

    protected boolean hasWobble() {
        return this.wobble;
    }

    protected NeedleAngleState.Angler createAngler(float speedMultiplier) {
        return this.wobble ? createWobblyAngler(speedMultiplier) : createInstantAngler();
    }

    public static NeedleAngleState.Angler createWobblyAngler(final float speedMultiplier) {
        return new NeedleAngleState.Angler() {
            private float angle;
            private float speed;
            private long lastUpdateTime;

            @Override
            public float getAngle() {
                return this.angle;
            }

            @Override
            public boolean shouldUpdate(long time) {
                return this.lastUpdateTime != time;
            }

            @Override
            public void update(long time, float target) {
                this.lastUpdateTime = time;
                float f = MathHelper.floorMod(target - this.angle + 0.5F, 1.0F) - 0.5F;
                this.speed += f * 0.1F;
                this.speed = this.speed * speedMultiplier;
                this.angle = MathHelper.floorMod(this.angle + this.speed, 1.0F);
            }
        };
    }

    public static NeedleAngleState.Angler createInstantAngler() {
        return new NeedleAngleState.Angler() {
            private float angle;

            @Override
            public float getAngle() {
                return this.angle;
            }

            @Override
            public boolean shouldUpdate(long time) {
                return true;
            }

            @Override
            public void update(long time, float target) {
                this.angle = target;
            }
        };
    }

    @Environment(EnvType.CLIENT)
    public interface Angler {
        float getAngle();

        boolean shouldUpdate(long time);

        void update(long time, float target);
    }
}

