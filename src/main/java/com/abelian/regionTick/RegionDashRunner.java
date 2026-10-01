package com.abelian.regionTick;

import com.abelian.RelativityTickUtils;
import com.abelian.ServerTickBridge;
import com.abelian.config.RelativityTickConfig;
import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.tick.WorldTickScheduler;

import java.util.function.BiConsumer;

//dash 原语：步数不用 accumulator 配额，一直跑到区域预算或服务端 MSPT 上限把它截断为止。
//截断判定与 RelativityTick.runRegionTicks（rate/step 走的路径）完全一致：先看全局 MSPT，再看区域预算，
//并且都发生在"至少已经执行了一步"之后 —— 所以一次 dash 永远至少推进 1 gt。
//dash 命令、dash 剩余步数的后续 gt、以及 sprint 的每个 gt 都走这里。
public final class RegionDashRunner {

    public record DashResult(int stepsTaken, int remainingSteps, long durationNano) {}

    private RegionDashRunner() {}

    public static DashResult runDash(MinecraftServer server, RegionTickManager region, ServerWorld world,
                                     int maxSteps, boolean exclusiveBatch) {
        long regionStartNano = System.nanoTime();
        long regionBudgetNano = (long) (region.getTickDurationLimit() * 1_000_000L);
        WorldTickScheduler<Block> blockScheduler = world.getBlockTickScheduler();
        WorldTickScheduler<Fluid> fluidScheduler = world.getFluidTickScheduler();
        BiConsumer<BlockPos, Block> blockTicker = (pos, block) -> RelativityTickUtils.tickBlock(world, pos, block);
        BiConsumer<BlockPos, Fluid> fluidTicker = (pos, fluid) -> RelativityTickUtils.tickFluid(world, pos, fluid);

        region.setReachedMsptLimit(false);
        region.setReachTickDurationLimit(false);

        if (exclusiveBatch) {
            ServerTickBridge.markSingleRegionTickBatch(region);
        }

        int stepsTaken = 0;
        long durationNano = 0L;
        ServerTickBridge.LocalTickState tickState = new ServerTickBridge.LocalTickState();
        region.setDeferScheduledTickSnapshot(true);
        try {
            while (stepsTaken < maxSteps && regionBudgetNano > 0) {
                long tickStartNano = System.nanoTime();
                tickState.clear();
                region.tickRegion(world, blockScheduler, blockTicker, fluidScheduler, fluidTicker, tickState);
                durationNano += System.nanoTime() - tickStartNano;
                stepsTaken++;

                if (RelativityTickUtils.getServerMspt(server) >= RelativityTickConfig.getMaxMspt()) {
                    region.setReachedMsptLimit(true);
                    break;
                }

                if (System.nanoTime() - regionStartNano >= regionBudgetNano) {
                    region.setReachTickDurationLimit(true);
                    break;
                }
            }
        } finally {
            region.setDeferScheduledTickSnapshot(false);
        }

        if (stepsTaken > 0) {
            region.sendScheduledTickSnapshot(world);
        }

        return new DashResult(stepsTaken, Math.max(0, maxSteps - stepsTaken), durationNano);
    }
}
