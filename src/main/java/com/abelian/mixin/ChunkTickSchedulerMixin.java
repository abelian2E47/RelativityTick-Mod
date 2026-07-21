package com.abelian.mixin;

import com.abelian.regionTick.RegionTickManager;
import com.abelian.regionTick.RegionsManager;
import net.minecraft.world.tick.ChunkTickScheduler;
import net.minecraft.world.tick.OrderedTick;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ChunkTickScheduler.class)
public abstract class ChunkTickSchedulerMixin<T> {
    @Unique
    private static final ThreadLocal<Boolean> RELATIVITYTICK_RESCHEDULING = ThreadLocal.withInitial(() -> false);

    @Inject(method = "scheduleTick", at = @At("HEAD"), cancellable = true)
    private void adjustScheduledTick(OrderedTick<T> orderedTick, CallbackInfo ci) {
        if (RELATIVITYTICK_RESCHEDULING.get()) {
            return;
        }

        ChunkTickScheduler<T> scheduler = (ChunkTickScheduler<T>) (Object) this;
        RegionTickManager region = RegionsManager.getControlledRegionByScheduler(scheduler);

        if (region == null) {
            return;
        }

        //换算到区域虚拟时间轴
        long startTime = region.getFreezeStartTime();
        int stepped = region.getStepped();
        long currentTime = region.getCurrentWorldTime();
        long delay = orderedTick.triggerTick() - currentTime;
        long correctedTriggerTick = startTime + stepped + delay;

        OrderedTick<T> correctedTick = new OrderedTick<>(
                orderedTick.type(),
                orderedTick.pos(),
                correctedTriggerTick,
                orderedTick.priority(),
                orderedTick.subTickOrder()
        );

        RELATIVITYTICK_RESCHEDULING.set(true);
        try {
            scheduler.scheduleTick(correctedTick);
        } finally {
            RELATIVITYTICK_RESCHEDULING.set(false);
        }
        //跳过原始未修正 tick。
        ci.cancel();
    }
}
