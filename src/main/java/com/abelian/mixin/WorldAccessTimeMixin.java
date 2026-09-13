package com.abelian.mixin;

import com.abelian.RegionTimeContext;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//1.21.11 的 World 不再覆写 getTime，该方法成了 WorldAccess 的 default 方法，故区域虚拟时间改在接口默认实现上拦截
@Mixin(WorldAccess.class)
public interface WorldAccessTimeMixin {
    @Inject(method = "getTime", at = @At("HEAD"), cancellable = true)
    private void useRegionTickTime(CallbackInfoReturnable<Long> cir) {
        if (!(this instanceof World world)) return;
        Long tickTime = RegionTimeContext.getTime(world);
        if (tickTime != null) {
            cir.setReturnValue(tickTime);
        }
    }
}
