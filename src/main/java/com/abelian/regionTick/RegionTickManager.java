package com.abelian.regionTick;

import com.abelian.RegionPersistentState;
import com.abelian.config.RelativityTickConfig;
import com.abelian.RegionTimeContext;
import com.abelian.ServerTickBridge;
import com.abelian.network.ScheduledTickDataPayload;
import com.abelian.network.ScheduledTickRecord;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import com.abelian.RelativityTickUtils;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.PriorityQueue;
import java.util.Queue;

import net.minecraft.util.math.ChunkPos;
import com.abelian.mixin.ServerChunkManagerAccessor;
import com.abelian.mixin.ServerWorldAccessor;
import com.abelian.network.EntityStateRecord;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.tick.WorldTickScheduler;
import com.abelian.mixin.WorldTickSchedulerAccessor;
import net.minecraft.world.tick.ChunkTickScheduler;
import net.minecraft.world.tick.OrderedTick;


public class RegionTickManager {
    private static final int MAX_SCHEDULED_TICK_RECORDS = 1024;
    private static final int MAX_TICKS_EXECUTED_PER_STEP = 65536;

    public enum RegionState {
        RELEASED,
        FROZEN,
        RUNNING,
        //每 gt 都用 dash 原语跑满区域/全局预算。约束见 RegionDashRunner。
        SPRINTING
    }

    private final RegistryKey<World> dimension;
    private final String id;
    private final LongSet chunkPositions;
    private final Long2ObjectMap<ChunkTickManager> region = new Long2ObjectOpenHashMap<>();
    private long startTime = 0;
    private long currentWorldTime = 0;

    private int stepped = 0;
    private int pendingSteps = 0;
    private int pendingDashSteps = 0;

    private double rate = 20;
    private double accumulator = 0.0;
    private double tickDurationLimit = 10.0;
    private boolean disableHopperTick = false;
    private boolean disableEntityTick = false;
    private boolean disableObserverTick = false;
    private RegionState state = RegionState.RELEASED;
    private boolean sprintContinuous = false;
    private int sprintRemainingGt = 0;
    private RegionState stateBeforeSprint = RegionState.FROZEN;
    private boolean scheduledTicksDirty = true;
    private boolean anchorInRealTime = false;
    private boolean deferScheduledTickSnapshot = false;

    //TPS 统计：以真实墙钟时间窗口计算“实际推进的区域 GT / 真实秒数”，再做 EMA 平滑。
    //窗口而不是单个服务端 GT，避免低 TPS、长帧或 dash 批次造成瞬时值过度抖动。
    private static final double TPS_AVERAGE_TIME_CONSTANT_SECONDS = 2.0;
    private static final long TPS_SAMPLE_WINDOW_NANOS = 1_000_000_000L;
    //采样时长不足这个值就读数还不可信：status 显示“采样中”，不拿目标速率冒充实测。
    private static final double TPS_MIN_SAMPLE_SECONDS = 1.0;
    //区域刻耗时用同一个思路平滑：单个 gt 的抖动不该直接变成读数。
    private static final double TICK_DURATION_TIME_CONSTANT_SECONDS = 2.0;
    private double regionTPS = 0;
    private long tpsWindowStartNano = 0;
    private int tpsWindowStartStepped = 0;
    private long tpsSampledNano = 0;
    private boolean tpsSampling = false;
    private float regionTickDuration = 0;
    private long tickDurationLastNano = 0;
    private boolean tickDurationSampling = false;

    private boolean reachMsptLimit = false;
    private boolean reachTickDurationLimit = false;


    public RegionTickManager(String id, RegistryKey<World> dimension, Set<Long> chunkPositions){
        this.id = id;
        this.dimension = dimension;
        for (long chunkPos : chunkPositions){
            region.put(chunkPos, new ChunkTickManager(chunkPos));
        }
        this.chunkPositions = new LongOpenHashSet(chunkPositions);

    }

    public boolean addChunk(long chunkPos, ServerWorld world) {
        if (!this.chunkPositions.add(chunkPos)) return false;

        ChunkTickManager chunk = new ChunkTickManager(chunkPos);
        if (isControlled()) {
            //接管进区域需换算到虚拟时间线
            long reanchorOffset = getVirtualTime() - world.getTime();
            chunk.retakeOverChunk(world.getBlockTickScheduler(), this, reanchorOffset);
            chunk.retakeOverChunk(world.getFluidTickScheduler(), this, reanchorOffset);
            markScheduledTicksDirty();
        }
        region.put(chunkPos, chunk);
        return true;
    }

    public boolean removeChunk(long chunkPos, ServerWorld world) {
        if (!this.chunkPositions.remove(chunkPos)) return false;

        ChunkTickManager target = region.remove(chunkPos);
        if (target != null) {
            if (isControlled()) {
                long currentWorldTime = world.getTime();
                target.releaseChunk(world.getBlockTickScheduler(), this, currentWorldTime, startTime, stepped);
                target.releaseChunk(world.getFluidTickScheduler(), this, currentWorldTime, startTime, stepped);
            }
            markScheduledTicksDirty();
        }

        return true;
    }

    public <T> void takeOverRegion(WorldTickScheduler<T> worldScheduler) {
        //原实现在遍历 ArrayList 时直接接管,而接管过程会改写调度器队列;
        //这里改为对区块快照迭代,避免依赖 fastutil 映射在迭代期修改的未定义行为。
        for (ChunkTickManager chunk : new ArrayList<>(region.values())){
            chunk.takeOverChunk(worldScheduler, this);
        }
    }

    public <T> void releaseRegion(WorldTickScheduler<T> worldScheduler, long currentWorldTime) {
        for (ChunkTickManager chunk : region.values()) {
            chunk.releaseChunk(worldScheduler, this, currentWorldTime, startTime, stepped);
        }
        this.anchorInRealTime = true;
    }

    public void takeOverChunk(long chunkPos, ServerWorld world) {
        ChunkTickManager chunk = region.get(chunkPos);
        if (chunk == null) return;
        long offset = getVirtualTime() - world.getTime();
        chunk.retakeOverChunk(world.getBlockTickScheduler(), this, offset);
        chunk.retakeOverChunk(world.getFluidTickScheduler(), this, offset);
        markScheduledTicksDirty();
    }

    //区块卸载时将计划刻换算回真实时间线
    public void detachChunk(long chunkPos, ServerWorld world) {
        ChunkTickManager chunk = region.get(chunkPos);
        if (chunk == null) return;
        chunk.releaseChunk(world.getBlockTickScheduler(), this, world.getTime(), startTime, stepped);
        chunk.releaseChunk(world.getFluidTickScheduler(), this, world.getTime(), startTime, stepped);
        WorldChunk worldChunk = world.getChunkManager().getWorldChunk(ChunkPos.getPackedX(chunkPos), ChunkPos.getPackedZ(chunkPos));
        if (worldChunk != null) {
            worldChunk.markNeedsSaving();
        }
    }

    public void releaseChunkToWorld(long chunkPos, ServerWorld world) {
        if (anchorInRealTime || (getStartTime() == 0 && getStepped() == 0)) return;
        this.anchorInRealTime = true;
        ChunkTickManager chunk = region.get(chunkPos);
        if (chunk == null) return;
        chunk.releaseChunkToWorld(world.getBlockTickScheduler(), this, world.getTime());
        chunk.releaseChunkToWorld(world.getFluidTickScheduler(), this, world.getTime());
    }

    public void setStartTime(long time) {
        this.startTime = time;
        this.stepped = 0;
        this.currentWorldTime = time;
        this.anchorInRealTime = false;
        resetStepRateSampling();
    }

    public void setCurrentWorldTime(long time) {
        this.currentWorldTime = time;
    }

    public long getSchedulingTime() {
        MinecraftServer server = RelativityTickUtils.getServer();
        ServerWorld world = server == null ? null : server.getWorld(dimension);
        return world == null ? currentWorldTime : world.getLevelProperties().getTime();
    }

    public long getSchedulingVirtualTime() {
        MinecraftServer server = RelativityTickUtils.getServer();
        ServerWorld world = server == null ? null : server.getWorld(dimension);
        Long virtualTime = world == null ? null : RegionTimeContext.getTime(world);
        return virtualTime != null ? virtualTime : getVirtualTime();
    }


    public long getVirtualTime() {
        return startTime + stepped;
    }

    public void tickRegion(ServerWorld world, WorldTickScheduler<Block> blockScheduler, BiConsumer<BlockPos, Block> blockTicker, WorldTickScheduler<Fluid> fluidScheduler, BiConsumer<BlockPos, Fluid> fluidTicker) {
        this.tickRegion(world, blockScheduler, blockTicker, fluidScheduler, fluidTicker, new ServerTickBridge.LocalTickState());
    }

    public void tickRegion(ServerWorld world, WorldTickScheduler<Block> blockScheduler, BiConsumer<BlockPos, Block> blockTicker, WorldTickScheduler<Fluid> fluidScheduler, BiConsumer<BlockPos, Fluid> fluidTicker, ServerTickBridge.LocalTickState tickState) {
        if (!isControlled()) return;

        setCurrentWorldTime(world.getTime());
        stepped++;
        long virtualTime = startTime + stepped;
        RegionTimeContext.begin(world, virtualTime);
        ServerWorldAccessor worldAccessor = (ServerWorldAccessor) world;
        try {
            boolean previousInBlockTick = world.isInBlockTick();
            worldAccessor.setInBlockTick(true);
            try {
                tickRegionScheduledTicks(world, blockScheduler, blockTicker, fluidScheduler, fluidTicker, virtualTime);
                tickChunkWorld(world);
                RegionBlockEventProcessor.process(world, this, chunkPos -> tickState.chunkGate(world, chunkPos).blockTicks());
            } finally {
                worldAccessor.setInBlockTick(previousInBlockTick);
            }
            this.tickEntities(world, tickState);
            this.tickBlockEntities(world, tickState);
        } finally {
            RegionTimeContext.end();
        }
    }

    //某一侧"区域内最早到期时间"的取法：只读 peekNextTick，不动任何调度器状态。
    private static <T> long earliestTriggerOf(ChunkTickScheduler<T> chunkScheduler, long current) {
        if (chunkScheduler == null) return current;
        OrderedTick<T> nextTick = chunkScheduler.peekNextTick();
        return nextTick != null && nextTick.triggerTick() < current ? nextTick.triggerTick() : current;
    }

    //执行本步到期的计划刻
    private void tickRegionScheduledTicks(ServerWorld world, WorldTickScheduler<Block> blockScheduler, BiConsumer<BlockPos, Block> blockTicker,
                                          WorldTickScheduler<Fluid> fluidScheduler, BiConsumer<BlockPos, Fluid> fluidTicker, long virtualTime) {
        BiConsumer<BlockPos, Block> filterBlockTicker = disableObserverTick
                ? (pos, block) -> {
                    if (block != Blocks.OBSERVER) {
                        blockTicker.accept(pos, block);
                    }
                }
                : blockTicker;
        //空转短路所需的"区域最早就绪时间"在这里一趟取完两侧（原来方块/流体各扫一遍 region.values()，
        //每步 2×O(区块数)）。只读 peekNextTick，不动任何调度器状态。
        WorldTickSchedulerAccessor<Block> blockAccess = (WorldTickSchedulerAccessor<Block>) blockScheduler;
        WorldTickSchedulerAccessor<Fluid> fluidAccess = (WorldTickSchedulerAccessor<Fluid>) fluidScheduler;
        long earliestBlockTrigger = Long.MAX_VALUE;
        long earliestFluidTrigger = Long.MAX_VALUE;
        for (ChunkTickManager chunk : region.values()) {
            long chunkPos = chunk.getChunkPosLong();
            earliestBlockTrigger = earliestTriggerOf(blockAccess.getChunkTickSchedulers().get(chunkPos), earliestBlockTrigger);
            earliestFluidTrigger = earliestTriggerOf(fluidAccess.getChunkTickSchedulers().get(chunkPos), earliestFluidTrigger);
        }

        int blockExecuted = tickScheduledTicks(blockScheduler, filterBlockTicker, virtualTime, earliestBlockTrigger);
        if (blockExecuted > 0) {
            //方块阶段确实执行了计划刻 → 期间可能新排出流体刻。原实现的流体侧扫描发生在方块执行之后，
            //这里重取一次流体侧，保证"当步就该执行的流体刻"不会被推迟到下一步。
            //（方块侧无需重取：它的扫描在原实现里同样在方块执行之前。）
            earliestFluidTrigger = Long.MAX_VALUE;
            for (ChunkTickManager chunk : region.values()) {
                earliestFluidTrigger = earliestTriggerOf(fluidAccess.getChunkTickSchedulers().get(chunk.getChunkPosLong()), earliestFluidTrigger);
            }
        }
        int fluidExecuted = tickScheduledTicks(fluidScheduler, fluidTicker, virtualTime, earliestFluidTrigger);
        //若计划刻队列有变动，向客户端发送渲染快照
        if (blockExecuted > 0 || fluidExecuted > 0 || scheduledTicksDirty) {
            if (!deferScheduledTickSnapshot) {
                sendScheduledTickSnapshot(world);
            }
            //dash 抑制期间也清脏标记：收尾的补发是无条件发一次，不会漏掉这次变动
            scheduledTicksDirty = false;
        }
    }

    private void tickBlockEntities(ServerWorld world, ServerTickBridge.LocalTickState tickState) {
        //lithium兼容: 只把"已经醒着但内部 ticker 被换过"的方块实体重绑回活 ticker;
        //正在休眠的保持休眠(见 ServerTickBridge.rebindBlockEntityTickers 的注释),这样漏斗等
        //在区域内的休眠收益与正常游戏一致。
        //但"区域没在跑刻时"发生过的活动必须补一次唤醒: 那种事件在锂那边打的是真实时刻,会被判成
        //"早于上次检查 → 没有变化",于是睡着的漏斗再也不会醒(物品一直躺在上面)。
        ServerTickBridge.wakeChunksWithOutsideActivity(world, chunkPositions, tickState);
        ServerTickBridge.rebindBlockEntityTickers(world, chunkPositions);
        boolean shouldTick = world.getTickManager().shouldTick();
        ServerTickBridge.forEachBlockEntityTicker(world, chunkPositions, invoker -> {
            if (!shouldTick || invoker.isRemoved()) return;

            BlockPos pos = invoker.getPos();
            if (pos == null) return;
            long chunkPos = ChunkPos.toLong(pos);
            if (!chunkPositions.contains(chunkPos)) return;

            if (disableHopperTick && world.getBlockState(pos).isOf(Blocks.HOPPER)) return;

            //区块门控与坐标无关（World.shouldTickBlockPos 内部就是按区块查），整步摊销成每区块一次
            if (tickState.chunkGate(world, chunkPos).blockTicks()) {
                invoker.tick();
            }
        });
    }

    private void tickEntities(ServerWorld world, ServerTickBridge.LocalTickState tickState) {
        if (disableEntityTick) return;

        //按区域预过滤的批级子表（P1）：顺序与原"每步过滤世界快照"完全一致（同一源表、同一谓词），
        //只是整批算一次；下面的逐项复查一步都不能省，它们负责批内移动/消失的实体。
        List<Entity> candidates = ServerTickBridge.getRegionEntitySnapshot(world, this, chunkPositions, tickState.entitySnapshot(world));
        for (Entity entity : candidates) {
            if (entity instanceof PlayerEntity || entity.isRemoved()) continue;
            long chunkPos = entity.getChunkPos().toLong();
            if (!chunkPositions.contains(chunkPos)) continue;
            if (!tickState.chunkGate(world, chunkPos).entityTicks()) continue;
            if (!ServerTickBridge.claimEntity(entity, this)) continue;
            if (!isPassenger(entity)) {
                tickEntity(world, entity);
            }
        }
    }

    private static boolean isPassenger(Entity entity) {
        Entity vehicle = entity.getVehicle();
        return vehicle != null && !vehicle.isRemoved() && vehicle.hasPassenger(entity);
    }

    private void tickEntity(ServerWorld world, Entity entity) {
        Entity vehicle = entity.getVehicle();
        if (vehicle != null) {
            if (!vehicle.isRemoved() && vehicle.hasPassenger(entity)) return;
            entity.stopRiding();
        }

        entity.checkDespawn();
        if (entity.isRemoved()) return;

        ServerTickBridge.setCustomTickInProgress(true);
        try {
            ((ServerWorldAccessor) world).invokeTickEntity(entity);
        } finally {
            ServerTickBridge.setCustomTickInProgress(false);
        }
    }


    public List<EntityStateRecord> collectEntityStates(ServerWorld world) {
        return collectEntityStates(world, new ServerTickBridge.LocalTickState());
    }

    public List<EntityStateRecord> collectEntityStates(ServerWorld world, ServerTickBridge.LocalTickState tickState) {
        List<EntityStateRecord> entityStates = new ArrayList<>();
        if (!isControlled()) return entityStates;

        for (Entity entity : tickState.entitySnapshot(world)) {
            if (entity instanceof PlayerEntity || entity.isRemoved()) continue;
            long chunkPos = entity.getChunkPos().toLong();
            if (!chunkPositions.contains(chunkPos)) continue;
            if (!tickState.chunkGate(world, chunkPos).entityTicks()) continue;
            entityStates.add(new EntityStateRecord(
                    entity.getId(), entity.getX(), entity.getY(), entity.getZ(),
                    entity.getYaw(), entity.getPitch(),
                    entity.getVelocity().x, entity.getVelocity().y, entity.getVelocity().z
            ));
        }
        return entityStates;
    }


    private <T> int tickScheduledTicks(WorldTickScheduler<T> worldScheduler, BiConsumer<BlockPos, T> ticker, long virtualTrigger, long earliestTrigger) {
        WorldTickSchedulerAccessor<T> worldAccess = (WorldTickSchedulerAccessor<T>) worldScheduler;
        //空转短路：优先队列只为把"本步到期的区块队列"按 triggerTick 归并。若本区域最早就绪的
        //计划刻都还没到期，整段收集都不必发生（低倍率/空载区域大量步进属于这种情况）。
        //earliestTrigger 由调用方在本侧该扫的时刻扫出（方块侧在执行之前、流体侧在方块执行之后，
        //与原来的扫描时机一致），此处只读比较，不动任何调度器状态；真正执行前才对三个容器做清理，
        //与原实现一致。
        Queue<OrderedTick<T>> dueTicks = worldAccess.getTickableTicks();
        List<OrderedTick<T>> tickedTicks = worldAccess.getTickedTicks();
        Set<OrderedTick<?>> inFlightTicks = worldAccess.getCopiedTickableTicksList();

        if (earliestTrigger > virtualTrigger) {
            dueTicks.clear();
            tickedTicks.clear();
            inFlightTicks.clear();
            return 0;
        }

        Queue<ChunkTickScheduler<T>> tickableSchedulers = new PriorityQueue<>(
                (first, second) -> OrderedTick.TRIGGER_TICK_COMPARATOR
                        .compare(first.peekNextTick(), second.peekNextTick()));

        for (ChunkTickManager chunk : region.values()) {
            ChunkTickScheduler<T> scheduler = worldAccess.getChunkTickSchedulers().get(chunk.getChunkPosLong());
            if (scheduler == null) continue;

            OrderedTick<T> nextTick = scheduler.peekNextTick();
            if (nextTick == null) continue;

            if (nextTick.triggerTick() <= virtualTrigger) {
                tickableSchedulers.add(scheduler);
            }
        }

        //[修改点3] 收集阶段：按原版 WorldTickScheduler 的批语义，先把本步到期的计划刻全部 poll 出队列，再统一执行。
        //原版 tick() 是 collectTickableTicks 先把整批到期 tick 移出队列、之后才逐条执行，所以执行期间这些
        //tick 的 isQueued=false；ObserverBlock 的 getStateForNeighborUpdate/onBlockAdded/onStateReplaced 都用
        //isQueued 决定是否补排 +2 计划刻。就地 poll 立刻执行会让 isQueued 仍为 true 而漏排，
        //实测：活塞推脸对脸侦测器时钟在区域内退化成 6gt（原版/非受控为 4gt）。
        //[修改点4] 这一批必须落在原版 WorldTickScheduler.tickableTicks 上（而不是本地 List）。
        //isTicking(pos,type) 只查 tickableTicks / copiedTickableTicksList，红石元件
        //（AbstractRedstoneGateBlock.updatePowered、ComparatorBlock、RedstoneTorchBlock）用它判断
        //"该位置的计划刻已经在本批里了，别再排一个"。用本地 List 收集会让 isTicking 恒为 false，
        //导致同批内被邻居更新的元件多排一个 +2 计划刻，元件提前 2gt 动作。
        //实测：四格一档中继器链撤源后末端两个同时熄灭（原版是依次熄灭）。
        dueTicks.clear();
        tickedTicks.clear();
        inFlightTicks.clear();

        int executedTicks = 0;
        try {
            while (!tickableSchedulers.isEmpty() && dueTicks.size() < MAX_TICKS_EXECUTED_PER_STEP) {
                ChunkTickScheduler<T> scheduler = tickableSchedulers.poll();
                OrderedTick<T> tick = scheduler.pollNextTick();
                if (tick == null) continue;

                dueTicks.add(tick);

                OrderedTick<T> competingTick = peekNextTick(tickableSchedulers);
                while (dueTicks.size() < MAX_TICKS_EXECUTED_PER_STEP) {
                    OrderedTick<T> nextTick = scheduler.peekNextTick();
                    if (nextTick == null || nextTick.triggerTick() > virtualTrigger
                            || competingTick != null
                            && OrderedTick.TRIGGER_TICK_COMPARATOR.compare(nextTick, competingTick) > 0) {
                        break;
                    }

                    OrderedTick<T> collected = scheduler.pollNextTick();
                    if (collected == null) break;
                    dueTicks.add(collected);
                }

                OrderedTick<T> nextTick = scheduler.peekNextTick();
                if (nextTick != null && nextTick.triggerTick() <= virtualTrigger) {
                    tickableSchedulers.add(scheduler);
                }
            }

            //执行阶段：此时本步到期的 tick 已全部离开区块队列，isQueued 语义与原版一致；
            //逐条 poll 出 tickableTicks 并同步剔除 copiedTickableTicksList，isTicking 语义也与原版一致；
            //执行期间新排的计划刻不在本批内，留到下一步，与原版"新 tick 不在本刻批里"一致。
            while (!dueTicks.isEmpty()) {
                OrderedTick<T> executed = dueTicks.poll();
                if (!inFlightTicks.isEmpty()) {
                    inFlightTicks.remove(executed);
                }
                tickedTicks.add(executed);
                ticker.accept(executed.pos(), executed.type());
                executedTicks++;
            }
        } finally {
            dueTicks.clear();
            tickedTicks.clear();
            inFlightTicks.clear();
        }

        return executedTicks;
    }

    private <T> List<ScheduledTickRecord> collectScheduledTicks(WorldTickScheduler<T> worldScheduler) {
        WorldTickSchedulerAccessor<T> worldAccess = (WorldTickSchedulerAccessor<T>) worldScheduler;
        List<ScheduledTickRecord> scheduledTicks = new ArrayList<>();
        for (ChunkTickManager chunk : region.values()) {
            ChunkTickScheduler<T> scheduler = worldAccess.getChunkTickSchedulers().get(chunk.getChunkPosLong());
            if (scheduler == null || scheduler.peekNextTick() == null) continue;

            //收集计划刻
            Iterator<OrderedTick<T>> tickIterator = scheduler.getQueueAsStream().iterator();
            while (tickIterator.hasNext() && scheduledTicks.size() < MAX_SCHEDULED_TICK_RECORDS) {
                OrderedTick<T> tick = tickIterator.next();
                scheduledTicks.add(new ScheduledTickRecord(
                        tick.pos(), tick.triggerTick(), tick.subTickOrder(), tick.priority().getIndex()));
            }
        }
        return scheduledTicks;
    }

    public void markScheduledTicksDirty() {
        this.scheduledTicksDirty = true;
    }

    //区域计划刻快照发包
    public void sendScheduledTickSnapshot(ServerWorld world) {
        if (!isControlled()) return;
        //配置关闭时连收集都不做：collectScheduledTicks 要遍历本区域全部区块、逐条 new ScheduledTickRecord
        if (!RelativityTickConfig.isScheduledTickSendEnabled()) return;
        sendScheduledTicks(collectScheduledTicks(world.getBlockTickScheduler()), collectScheduledTicks(world.getFluidTickScheduler()));
    }

    //dash 抑制开关，见字段注释；调用方务必成对使用（try/finally）。
    public void setDeferScheduledTickSnapshot(boolean defer) {
        this.deferScheduledTickSnapshot = defer;
    }

    private void sendScheduledTicks(List<ScheduledTickRecord> blockRecords, List<ScheduledTickRecord> fluidRecords) {
        if (!RelativityTickConfig.isScheduledTickSendEnabled()) return;

        //必须合并成一包发送：客户端 ClientScheduledTickManager 是按 regionId 覆盖存放（put），
        //拆成方块/流体两包会让后到的流体包把方块计划刻从可视化数据里顶掉。
        List<ScheduledTickRecord> allRecords = new ArrayList<>(blockRecords.size() + fluidRecords.size());
        allRecords.addAll(blockRecords);
        allRecords.addAll(fluidRecords);

        ScheduledTickDataPayload payload = new ScheduledTickDataPayload(id, allRecords);
        for (ServerPlayerEntity player : RelativityTickUtils.getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private static <T> OrderedTick<T> peekNextTick(Queue<ChunkTickScheduler<T>> schedulers) {
        ChunkTickScheduler<T> scheduler = schedulers.peek();
        return scheduler == null ? null : scheduler.peekNextTick();
    }

    private void tickChunkWorld(ServerWorld world) {
        if (!RelativityTickConfig.isChunkTickEnabled()) return;

        ServerChunkManagerAccessor managerAccessor = (ServerChunkManagerAccessor) world.getChunkManager();

        SpawnHelper.Info spawnInfo = ServerTickBridge.getSpawnInfo(world);
        boolean doMobSpawning = world.getGameRules().getBoolean(net.minecraft.world.GameRules.DO_MOB_SPAWNING);
        int randomTickSpeed = world.getGameRules().getInt(net.minecraft.world.GameRules.RANDOM_TICK_SPEED);
        List<SpawnGroup> spawnGroups = doMobSpawning
                ? SpawnHelper.collectSpawnableGroups(
                        spawnInfo,
                        managerAccessor.getSpawnAnimals(),
                        managerAccessor.getSpawnMonsters(),
                        world.getTime() % 400L == 0L
                )
                : List.of();

        ServerChunkManager chunkManager = world.getChunkManager();

        for (long chunkPosLong : chunkPositions) {
            ChunkPos chunkPos = new ChunkPos(chunkPosLong);

            WorldChunk chunk = chunkManager.getWorldChunk(chunkPos.x, chunkPos.z);
            if (chunk == null) continue;
            if (!world.shouldTick(chunkPos)) continue;
            //区块时间
            chunk.increaseInhabitedTime(1L);
            //生物生成
            if (!spawnGroups.isEmpty()
                    && world.getWorldBorder().contains(chunkPos)) {
                SpawnHelper.spawn(world, chunk, spawnInfo, spawnGroups);
            }
            //random tick
            if (world.shouldTickBlocksInChunk(chunkPosLong)) {
                world.tickChunk(chunk, randomTickSpeed);
            }
        }
    }

    public void setPendingSteps(int steps){
        this.pendingSteps = steps;
    }

    public int getPendingSteps(){return this.pendingSteps;}

    public void setPendingDashSteps(int steps){this.pendingDashSteps = Math.max(0, steps);}

    public int getPendingDashSteps(){return this.pendingDashSteps;}

    //两种待执行步数的合计：status 的"等待步进"行按合计展示
    public int getPendingWorkSteps(){return this.pendingSteps + this.pendingDashSteps;}

    public int cancelPendingDashSteps(){
        int canceled = this.pendingDashSteps;
        this.pendingDashSteps = 0;
        return canceled;
    }

    public int cancelAllPendingSteps(){
        int canceled = getPendingWorkSteps();
        this.pendingSteps = 0;
        this.pendingDashSteps = 0;
        return canceled;
    }

    public void setRate(double rate){this.rate = rate;}

    public double getRate(){return rate;}

    public void setAccumulator(double accumulator) {this.accumulator = accumulator;}

    public double getAccumulator() {return accumulator;}

    public double getTickDurationLimit() {return tickDurationLimit;}

    public void setMaxRegionCostMs(double maxRegionCostMs) {this.tickDurationLimit = Math.max(1.0, maxRegionCostMs);}


    public boolean isDisableHopperTick() { return disableHopperTick; }

    public void setDisableHopperTick(boolean disableHopperTick) { this.disableHopperTick = disableHopperTick; }

    public boolean isDisableEntityTick() { return disableEntityTick; }

    public void setDisableEntityTick(boolean disableEntityTick) { this.disableEntityTick = disableEntityTick; }

    public boolean isDisableObserverTick() { return disableObserverTick; }

    public void setDisableObserverTick(boolean disableObserverTick) { this.disableObserverTick = disableObserverTick; }

    public RegionState getState() { return state; }

    public void setState(RegionState state) {
        if (state != RegionState.SPRINTING) {
            this.sprintContinuous = false;
            this.sprintRemainingGt = 0;
        }
        this.state = state;
    }

    public boolean isControlled(){ return state != RegionState.RELEASED; }

    public boolean isRunning(){ return state == RegionState.RUNNING; }

    public boolean isSprinting(){ return state == RegionState.SPRINTING; }

    public boolean isStepping(){ return pendingSteps > 0;}

    //开始冲刺：previousState 为冲刺结束时要恢复的状态（未接管时由调用方传 FROZEN，冲刺不会自动释放区域）
    public void startSprint(RegionState previousState, int sprintTicks, boolean continuous) {
        this.stateBeforeSprint = previousState == RegionState.SPRINTING ? RegionState.FROZEN : previousState;
        this.sprintContinuous = continuous;
        this.sprintRemainingGt = continuous ? 0 : Math.max(1, sprintTicks);
        setState(RegionState.SPRINTING);
    }

    //每个冲刺 gt 扣一次；返回 true 表示有界冲刺的 gt 数已经跑完
    public boolean consumeSprintGt() {
        if (sprintContinuous) return false;
        if (sprintRemainingGt > 0) sprintRemainingGt--;
        return sprintRemainingGt <= 0;
    }

    public void stopSprint() {
        setState(stateBeforeSprint == RegionState.SPRINTING ? RegionState.FROZEN : stateBeforeSprint);
    }

    public boolean isSprintContinuous() { return sprintContinuous; }

    public int getSprintRemainingGt() { return sprintRemainingGt; }

    public RegionState getStateBeforeSprint() { return stateBeforeSprint; }

    public long getStartTime(){
        return startTime;
    }

    public int getStepped(){
        return stepped;
    }

    public String getID() {
        return id;
    }

    public RegistryKey<World> getDimension() {
        return dimension;
    }

    public String getDimensionId() {
        return dimension.getValue().toString();
    }

    public boolean isInWorld(ServerWorld world) {
        return world.getRegistryKey().equals(dimension);
    }

    public RegionPersistentState.RegionData toPersistentData() {
        return new RegionPersistentState.RegionData(dimension, chunkPositions);
    }

    public Set<Long> getChunkPositions() {
        return Set.copyOf(this.chunkPositions);
    }

    public boolean hasReachedMsptLimit() { return reachMsptLimit; }

    public void setReachedMsptLimit(boolean reachMsptLimit) { this.reachMsptLimit = reachMsptLimit; }

    public boolean hasReachedTickDurationLimit() { return reachTickDurationLimit; }

    public void  setReachTickDurationLimit(boolean reachTickDurationLimit) { this.reachTickDurationLimit = reachTickDurationLimit; }

    /**
     * 区域刻耗时同样按真实经过时间加权平均，避免单个 gt 的抖动（大 gt/空 gt）直接变成读数。
     * totalNanoInTick <= 0 表示这个 gt 没推进，不参与平均。
     */
    public void recordTickDuration(long totalNanoInTick) {
        if (totalNanoInTick <= 0) return;

        double durationMs = totalNanoInTick / 1_000_000.0;
        long nowNano = System.nanoTime();

        if (!this.tickDurationSampling) {
            this.tickDurationSampling = true;
            this.tickDurationLastNano = nowNano;
            this.regionTickDuration = (float) durationMs;
            return;
        }

        long elapsedNano = nowNano - this.tickDurationLastNano;
        this.tickDurationLastNano = nowNano;
        if (elapsedNano <= 0) return;

        double alpha = 1.0 - Math.exp(-(elapsedNano / 1_000_000_000.0) / TICK_DURATION_TIME_CONSTANT_SECONDS);
        this.regionTickDuration += (float) ((durationMs - this.regionTickDuration) * alpha);
    }

    /**
     * 按完整墙钟时间窗口统计区域真实推进速度，再对窗口结果做时间加权 EMA。
     * 分子是 stepped 的实际增量，分母是 System.nanoTime() 的实际经过时间，
     * 因此不会把“服务端每秒 20 次回调”误当成区域每秒 20 GT。
     */
    public void sampleStepRate(int currentStepped, long nowNano) {
        if (!this.tpsSampling) {
            this.tpsSampling = true;
            this.tpsWindowStartNano = nowNano;
            this.tpsWindowStartStepped = currentStepped;
            return;
        }

        long elapsedNano = nowNano - this.tpsWindowStartNano;
        if (elapsedNano <= 0) return;

        //虚拟时间线重置或 int 回绕时，丢弃跨越断点的窗口，避免制造巨大假速率。
        if (currentStepped < this.tpsWindowStartStepped) {
            resetStepRateSampling();
            return;
        }
        if (elapsedNano < TPS_SAMPLE_WINDOW_NANOS) return;

        int deltaSteps = currentStepped - this.tpsWindowStartStepped;
        double elapsedSeconds = elapsedNano / 1_000_000_000.0;
        double measuredTPS = deltaSteps / elapsedSeconds;
        if (this.tpsSampledNano == 0) {
            this.regionTPS = measuredTPS;
        } else {
            double alpha = 1.0 - Math.exp(-elapsedSeconds / TPS_AVERAGE_TIME_CONSTANT_SECONDS);
            this.regionTPS += (measuredTPS - this.regionTPS) * alpha;
        }

        this.tpsSampledNano += elapsedNano;
        this.tpsWindowStartNano = nowNano;
        this.tpsWindowStartStepped = currentStepped;
    }

    /** 采样时长是否已够长：够长才把读数拿去显示、发包或与旧值比较。 */
    public boolean isStepRateSampleReady() {
        return this.tpsSampledNano >= (long) (TPS_MIN_SAMPLE_SECONDS * 1_000_000_000.0);
    }

    /** 速率改变、接管、载入等换掉虚拟时间线时调用：丢掉旧样本，读数回到“采样中”。 */
    public void resetStepRateSampling() {
        this.tpsSampling = false;
        this.tpsSampledNano = 0;
        this.tpsWindowStartNano = 0;
        this.tpsWindowStartStepped = this.stepped;
        this.regionTPS = 0;
    }


    public double getTPS() { return regionTPS; }
    public float getRegionTickDuration() { return regionTickDuration; }

}


