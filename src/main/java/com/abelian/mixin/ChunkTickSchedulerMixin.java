package com.abelian.mixin;

import com.abelian.regionTick.ChunkTickManager;
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

        if (region == null) return;

        long currentTime = region.getSchedulingTime();
        long delay = orderedTick.triggerTick() - currentTime;
        long correctedTriggerTick = region.getSchedulingVirtualTime() + delay;

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
        region.markScheduledTicksDirty();
        //跳过原始未修正 tick。
        ci.cancel();
    }

    //区块读档时 disable(真实时间) 把存档延迟 tick 物化进队列;受控区块需把真实锚点换算回虚拟时间线,
    //否则计划刻按真实时间触发(冻结区域失效/倍速区域时序错乱)。
    //此处 time 与物化使用同一时间基准,换算精确;卸载侧 detachChunk 已保证落盘 delay 是虚拟相对延迟
    @Inject(method = "disable", at = @At("TAIL"))
    private void reanchorMaterializedTicks(long time, CallbackInfo ci) {
        ChunkTickScheduler<T> scheduler = (ChunkTickScheduler<T>) (Object) this;
        RegionTickManager region = RegionsManager.getControlledRegionByScheduler(scheduler);
        if (region == null) return;
        if (scheduler.peekNextTick() == null) return;

        long offset = region.getVirtualTime() - time;
        if (offset == 0) return;

        //平移内部会逐条 scheduleTick,必须置位重调度标记防止 adjustScheduledTick 二次修正
        RELATIVITYTICK_RESCHEDULING.set(true);
        try {
            ChunkTickManager.shiftScheduledTicks(scheduler, offset);
        } finally {
            RELATIVITYTICK_RESCHEDULING.set(false);
        }
        region.markScheduledTicksDirty();
    }
}
