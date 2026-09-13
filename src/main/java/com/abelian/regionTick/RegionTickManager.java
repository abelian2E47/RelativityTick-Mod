package com.abelian.regionTick;

import com.abelian.RegionPersistentState;
import com.abelian.config.RelativityTickConfig;
import com.abelian.RegionTimeContext;
import com.abelian.ServerTickBridge;
import com.abelian.network.ScheduledTickDataPayload;
import com.abelian.network.ScheduledTickRecord;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import com.abelian.RelativityTickUtils;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;

import java.util.ArrayList;
import java.util.HashSet;
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


public class    RegionTickManager {
    private static final int MAX_SCHEDULED_TICK_RECORDS = 1024;
    private static final int MAX_TICKS_EXECUTED_PER_STEP = 65536;

    public enum RegionState {
        RELEASED,
        FROZEN,
        RUNNING
    }

    private final RegistryKey<World> dimension;
    private final String id;
    private final Set<Long> chunkPositions;
    private final  ArrayList<ChunkTickManager> region = new ArrayList<>();
    private long startTime = 0;
    private long currentWorldTime = 0;
    private int stepped = 0;
    private int pendingSteps = 0;
    private double rate = 20;
    private double accumulator = 0.0;
    private double tickDurationLimit = 10.0;
    private boolean disableHopperTick = false;
    private boolean disableEntityTick = false;
    private boolean disableObserverTick = false;
    private RegionState state = RegionState.RELEASED;
    private boolean scheduledTicksDirty = true;
    private boolean anchorInRealTime = false;

    private static final int TPS_AVERAGE_WINDOW_GT = 100;
    private final int[] recentStepCounts = new int[TPS_AVERAGE_WINDOW_GT];
    private int recentStepCursor = 0;
    private int recentStepSamples = 0;
    private int recentStepTotal = 0;
    private double regionTPS = 0;
    private float regionTickDuration = 0;

    private boolean reachMsptLimit = false;
    private boolean reachTickDurationLimit = false;


    public RegionTickManager(String id, RegistryKey<World> dimension, Set<Long> chunkPositions){
        this.id = id;
        this.dimension = dimension;
        for (long chunkPos : chunkPositions){
            region.add(new ChunkTickManager(chunkPos));
        }
        this.chunkPositions = new HashSet<>(chunkPositions);

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
        region.add(chunk);
        return true;
    }

    public boolean removeChunk(long chunkPos, ServerWorld world) {
        if (!this.chunkPositions.remove(chunkPos)) return false;

        ChunkTickManager target = null;
        for (ChunkTickManager chunk : region) {
            if (chunk.getChunkPosLong() == chunkPos) {
                target = chunk;
                break;
            }
        }

        if (target != null) {
            if (isControlled()) {
                long currentWorldTime = world.getTime();
                target.releaseChunk(world.getBlockTickScheduler(), this, currentWorldTime, startTime, stepped);
                target.releaseChunk(world.getFluidTickScheduler(), this, currentWorldTime, startTime, stepped);
            }
            region.remove(target);
            markScheduledTicksDirty();
        }

        return true;
    }

    public <T> void takeOverRegion(WorldTickScheduler<T> worldScheduler) {
        for (ChunkTickManager chunk : region){
            chunk.takeOverChunk(worldScheduler, this);
        }
    }

    public <T> void releaseRegion(WorldTickScheduler<T> worldScheduler, long currentWorldTime) {
        for (ChunkTickManager chunk : region) {
            chunk.releaseChunk(worldScheduler, this, currentWorldTime, startTime, stepped);
        }
        this.anchorInRealTime = true;
    }

    public void takeOverChunk(long chunkPos, ServerWorld world) {
        for (ChunkTickManager chunk : region) {
            if (chunk.getChunkPosLong() != chunkPos) continue;
            long offset = getVirtualTime() - world.getTime();
            chunk.retakeOverChunk(world.getBlockTickScheduler(), this, offset);
            chunk.retakeOverChunk(world.getFluidTickScheduler(), this, offset);
            markScheduledTicksDirty();
            return;
        }
    }

    //区块卸载时将计划刻换算回真实时间线
    public void detachChunk(long chunkPos, ServerWorld world) {
        for (ChunkTickManager chunk : region) {
            if (chunk.getChunkPosLong() != chunkPos) continue;
            chunk.releaseChunk(world.getBlockTickScheduler(), this, world.getTime(), startTime, stepped);
            chunk.releaseChunk(world.getFluidTickScheduler(), this, world.getTime(), startTime, stepped);
            WorldChunk worldChunk = world.getChunkManager().getWorldChunk(ChunkPos.getPackedX(chunkPos), ChunkPos.getPackedZ(chunkPos));
            if (worldChunk != null) {
                worldChunk.setNeedsSaving(true);
            }
            return;
        }
    }

    public void releaseChunkToWorld(long chunkPos, ServerWorld world) {
        if (anchorInRealTime || (getStartTime() == 0 && getStepped() == 0)) return;
        this.anchorInRealTime = true;
        for (ChunkTickManager chunk : region) {
            if (chunk.getChunkPosLong() != chunkPos) continue;
            chunk.releaseChunkToWorld(world.getBlockTickScheduler(), this, world.getTime());
            chunk.releaseChunkToWorld(world.getFluidTickScheduler(), this, world.getTime());
            return;
        }
    }

    public void setStartTime(long time) {
        this.startTime = time;
        this.stepped = 0;
        this.currentWorldTime = time;
        this.anchorInRealTime = false;
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
                RegionBlockEventProcessor.process(world, this);
            } finally {
                worldAccessor.setInBlockTick(previousInBlockTick);
            }
            this.tickEntities(world);
            this.tickBlockEntities(world);
        } finally {
            RegionTimeContext.end();
        }
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
        int blockExecuted = tickScheduledTicks(blockScheduler, filterBlockTicker, virtualTime);
        int fluidExecuted = tickScheduledTicks(fluidScheduler, fluidTicker, virtualTime);
        //若计划刻队列有变动，向客户端发送渲染快照
        if (blockExecuted > 0 || fluidExecuted > 0 || scheduledTicksDirty) {
            sendScheduledTickSnapshot(world);
            scheduledTicksDirty = false;
        }
    }

    private void tickBlockEntities(ServerWorld world) {
        //防止漏斗休眠，lithium兼容
        ServerTickBridge.rebindBlockEntityTickers(world, chunkPositions);
        boolean shouldTick = world.getTickManager().shouldTick();
        ServerTickBridge.forEachBlockEntityTicker(world, chunkPositions, invoker -> {
            if (!shouldTick || invoker.isRemoved()) return;

            BlockPos pos = invoker.getPos();
            if (pos == null || !chunkPositions.contains(ChunkPos.toLong(pos))) return;

            if (disableHopperTick && world.getBlockState(pos).isOf(Blocks.HOPPER)) return;

            if (world.shouldTickBlockPos(pos)) {
                invoker.tick();
            }
        });
    }

    private void tickEntities(ServerWorld world) {
        if (disableEntityTick) return;

        for (Entity entity : ServerTickBridge.getOrderedEntitySnapshot(world)) {
            if (entity instanceof PlayerEntity || entity.isRemoved()) continue;
            if (!chunkPositions.contains(entity.getChunkPos().toLong())) continue;
            if (!world.shouldTickEntity(entity.getBlockPos())) continue;
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
        List<EntityStateRecord> entityStates = new ArrayList<>();
        if (!isControlled()) return entityStates;

        for (Entity entity : ServerTickBridge.getOrderedEntitySnapshot(world)) {
            if (entity instanceof PlayerEntity || entity.isRemoved()) continue;
            if (!chunkPositions.contains(entity.getChunkPos().toLong())) continue;
            if (!world.shouldTickEntity(entity.getBlockPos())) continue;
            entityStates.add(new EntityStateRecord(
                    entity.getId(), entity.getX(), entity.getY(), entity.getZ(),
                    entity.getYaw(), entity.getPitch(),
                    entity.getVelocity().x, entity.getVelocity().y, entity.getVelocity().z
            ));
        }
        return entityStates;
    }


    private <T> int tickScheduledTicks(WorldTickScheduler<T> worldScheduler, BiConsumer<BlockPos, T> ticker, long virtualTrigger) {
        WorldTickSchedulerAccessor<T> worldAccess = (WorldTickSchedulerAccessor<T>) worldScheduler;
        Queue<ChunkTickScheduler<T>> tickableSchedulers = new PriorityQueue<>(
                (first, second) -> OrderedTick.TRIGGER_TICK_COMPARATOR
                        .compare(first.peekNextTick(), second.peekNextTick()));

        for (ChunkTickManager chunk : region) {
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
        Queue<OrderedTick<T>> dueTicks = worldAccess.getTickableTicks();
        List<OrderedTick<T>> tickedTicks = worldAccess.getTickedTicks();
        Set<OrderedTick<?>> inFlightTicks = worldAccess.getCopiedTickableTicksList();
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
        for (ChunkTickManager chunk : region) {
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
        sendScheduledTicks(collectScheduledTicks(world.getBlockTickScheduler()), collectScheduledTicks(world.getFluidTickScheduler()));
    }

    private void sendScheduledTicks(List<ScheduledTickRecord> blockRecords, List<ScheduledTickRecord> fluidRecords) {
        if (!RelativityTickConfig.isScheduledTickSendEnabled()) return;

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
        //MC 1.21 的 SpawnHelper.spawn 内部按组做 spawnAnimals/spawnMonsters/rare/isBelowCap 过滤，
        //等价于 1.21.4 的 collectSpawnableGroups；此处补上原版 tickChunks 的外层 (spawnMonsters || spawnAnimals) 守卫，
        //使其与 1.21.4 的 !spawnGroups.isEmpty() 守卫行为一致
        boolean canSpawn = doMobSpawning
                && (managerAccessor.getSpawnMonsters() || managerAccessor.getSpawnAnimals());

        ServerChunkManager chunkManager = world.getChunkManager();

        for (long chunkPosLong : chunkPositions) {
            ChunkPos chunkPos = new ChunkPos(chunkPosLong);

            WorldChunk chunk = chunkManager.getWorldChunk(chunkPos.x, chunkPos.z);
            if (chunk == null) continue;
            if (!world.shouldTick(chunkPos)) continue;
            //区块时间
            chunk.increaseInhabitedTime(1L);
            //生物生成
            if (canSpawn && world.getWorldBorder().contains(chunkPos)) {
                SpawnHelper.spawn(world, chunk, spawnInfo,
                        managerAccessor.getSpawnAnimals(),
                        managerAccessor.getSpawnMonsters(),
                        world.getTime() % 400L == 0L);
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

    public void setState(RegionState state) { this.state = state; }

    public boolean isControlled(){ return state != RegionState.RELEASED; }

    public boolean isRunning(){ return state == RegionState.RUNNING; }

    public boolean isStepping(){ return pendingSteps > 0;}

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

    public void recordTickDuration(long totalNanoInTick) {this.regionTickDuration = (float)(totalNanoInTick / 1_000_000.0);}

    public void recordGlobalTickSteps(int stepsTaken) {
        this.recentStepTotal -= this.recentStepCounts[this.recentStepCursor];
        this.recentStepCounts[this.recentStepCursor] = stepsTaken;
        this.recentStepTotal += stepsTaken;
        this.recentStepCursor = (this.recentStepCursor + 1) % TPS_AVERAGE_WINDOW_GT;
        if (this.recentStepSamples < TPS_AVERAGE_WINDOW_GT) {
            this.recentStepSamples++;
        }

        this.regionTPS = this.recentStepTotal * 20.0 / this.recentStepSamples;
    }

    public boolean hasFullTpsSampleWindow() {
        return this.recentStepSamples >= TPS_AVERAGE_WINDOW_GT;
    }

    public void resetRecentStepCount(double targetTPS){
        int syntheticTotal = (int) Math.round(targetTPS * TPS_AVERAGE_WINDOW_GT / 20.0);
        int baseSteps = syntheticTotal / TPS_AVERAGE_WINDOW_GT;
        int extraSteps = syntheticTotal % TPS_AVERAGE_WINDOW_GT;

        this.recentStepTotal = 0;
        for (int i = 0; i < TPS_AVERAGE_WINDOW_GT; i++) {
            int steps = baseSteps + (i < extraSteps ? 1 : 0);
            this.recentStepCounts[i] = steps;
            this.recentStepTotal += steps;
        }

        this.recentStepCursor = 0;
        this.recentStepSamples = TPS_AVERAGE_WINDOW_GT;
        this.regionTPS = targetTPS;
    }


    public double getTPS() { return regionTPS; }
    public float getRegionTickDuration() { return regionTickDuration; }

}
