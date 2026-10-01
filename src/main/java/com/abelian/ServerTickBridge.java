package com.abelian;

import com.abelian.mixin.ServerChunkManagerAccessor;
import com.abelian.mixin.ServerEntityManagerAccessor;
import com.abelian.mixin.ServerWorldAccessor;
import com.abelian.mixin.ServerWorldEntityAccessor;
import com.abelian.mixin.WorldAccessor;
import com.abelian.mixin.WorldChunkAccessor;
import com.abelian.regionTick.RegionTickManager;
import com.abelian.regionTick.RegionsManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.EntityList;
import net.minecraft.world.SpawnDensityCapper;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.BlockEntityTickInvoker;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.entity.SectionedEntityCache;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Consumer;
public class ServerTickBridge {
    private static final ThreadLocal<Boolean> CUSTOM_TICK_IN_PROGRESS = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Map<Entity, RegionTickManager>> REGION_TICK_OWNERS =
            ThreadLocal.withInitial(IdentityHashMap::new);
    //本批被声明为"只 tick 一个区域"时记下那个区域（dash 用）；null 表示照旧按归属表记账。
    private static final ThreadLocal<RegionTickManager> BATCH_SINGLE_REGION = new ThreadLocal<>();
    private static final ThreadLocal<Map<ServerWorld, SpawnHelper.Info>> SPAWN_INFOS =
            ThreadLocal.withInitial(IdentityHashMap::new);
    private static final ThreadLocal<IdentityHashMap<ServerWorld, BlockEntityTickerState>> BLOCK_ENTITY_TICKER_STATES =
            ThreadLocal.withInitial(IdentityHashMap::new);
    //批序号：每次 beginRegionTickBatch（每个服务端刻 / 每次 dash 命令前）推进一次。
    //实体快照按批复用：同一批内实体列表没被增删就一直复用同一份，跨批必然重建。
    private static final ThreadLocal<EntitySnapshotBatches> ENTITY_SNAPSHOT_BATCHES =
            ThreadLocal.withInitial(EntitySnapshotBatches::new);

    public static boolean isCustomTickInProgress() {
        return CUSTOM_TICK_IN_PROGRESS.get();
    }

    public static void setCustomTickInProgress(boolean value) {
        CUSTOM_TICK_IN_PROGRESS.set(value);
    }

    public static void beginRegionTickBatch() {
        REGION_TICK_OWNERS.get().clear();
        SPAWN_INFOS.get().clear();
        BLOCK_ENTITY_TICKER_STATES.get().clear();
        ENTITY_SNAPSHOT_BATCHES.get().batchIndex++;
        BATCH_SINGLE_REGION.remove();
    }

    //声明"本批只会 tick 这一个区域"（dash 命令在开批后立刻调用）。
    //兑现条件：该区域是本批第一个、也是唯一一个 tick 的区域——区域刻的唯一入口是每个服务端刻
    //END_SERVER_TICK 里那次 beginRegionTickBatch + dash 命令自己开的那次，所以这个前提当前成立。
    //万一将来有第二个区域混进同一批，claimEntity 会关掉这个判定（见那里的注释）。
    public static void markSingleRegionTickBatch(RegionTickManager region) {
        BATCH_SINGLE_REGION.set(region);
    }

    //一步区域刻内的共享状态：把"区块级门控判定"按步摊销（每区块一次，而不是每方块实体/每实体/每方块事件一次）。
    //实体快照不放在这里——它按"批"复用（见 EntitySnapshotBatches），dash 的 N 步共用同一份。
    public static final class LocalTickState {
        //免装箱：long 主键直接寻址，避免每次 get/put 装箱一个 Long
        private final Long2ObjectMap<LocalChunkGate> chunkGates = new Long2ObjectOpenHashMap<>();
        //门控查询复用的可变坐标：原实现每个区块、每一侧各 new 一个 BlockPos
        private final BlockPos.Mutable gatePos = new BlockPos.Mutable();
        public void clear() {
            for (LocalChunkGate gate : chunkGates.values()) {
                gate.reset();
            }
        }

        //区块门控懒计算,每区块每步只算一次
        public LocalChunkGate chunkGate(ServerWorld world, long chunkPos) {
            LocalChunkGate gate = chunkGates.get(chunkPos);
            if (gate == null) {
                gate = new LocalChunkGate(world, gatePos, chunkPos);
                chunkGates.put(chunkPos, gate);
            }
            return gate;
        }

        public List<Entity> entitySnapshot(ServerWorld world) {
            return getOrderedEntitySnapshot(world);
        }
    }

    //区块门控结果。两侧判定分开懒计算：方块实体路径只读 blockTicks、实体路径只读 entityTicks，
    //按需查一个即可（原实现创建 gate 时无脑查两次）。
    public static final class LocalChunkGate {
        private final ServerWorld world;
        private final BlockPos.Mutable pos;
        private final long chunkPos;
        private boolean blockTicks;
        private boolean entityTicks;
        private boolean blockTicksKnown;
        private boolean entityTicksKnown;

        private LocalChunkGate(ServerWorld world, BlockPos.Mutable pos, long chunkPos) {
            this.world = world;
            this.pos = pos;
            this.chunkPos = chunkPos;
        }

        private void reset() {
            blockTicksKnown = false;
            entityTicksKnown = false;
        }

        public boolean blockTicks() {
            if (!blockTicksKnown) {
                //World.shouldTickBlockPos(pos) 内部就是 shouldTickBlocksInChunk(ChunkPos.toLong(pos))，
                //直接按区块查，省掉 BlockPos 分配与 ChunkPos.toLong 往返（1.21.4 World:583）。
                blockTicks = world.shouldTickBlocksInChunk(chunkPos);
                blockTicksKnown = true;
            }
            return blockTicks;
        }

        public boolean entityTicks() {
            if (!entityTicksKnown) {
                //实体侧仍需坐标入参（ServerWorld.shouldTickEntity(BlockPos)），复用同一个可变坐标。
                entityTicks = world.shouldTickEntity(pos.set(
                        ChunkPos.getPackedX(chunkPos) << 4, 0, ChunkPos.getPackedZ(chunkPos) << 4));
                entityTicksKnown = true;
            }
            return entityTicks;
        }
    }

    //实体列表增删的失效信号。只增不减：多重建一次不会出错，漏重建才会（会漏掉新生成的实体）。
    public static void markEntityListMutated(EntityList entityList) {
        ENTITY_SNAPSHOT_BATCHES.get().markMutated(entityList);
    }
    public static void markBlockEntityTickersDirty(World world) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        BlockEntityTickerState state = BLOCK_ENTITY_TICKER_STATES.get().get(serverWorld);
        if (state != null) {
            state.dirty = true;
        }
    }

    //区域"外部活动"记录。锂的休眠判定是时间戳比较(SectionedEntityMovementTracker.isUnchangedSince),
    //读的是 world.getTime();而本模组只在区域刻内把它换成虚拟时间(startTime + stepped)。
    //于是"区域没在跑刻时"发生的事件(玩家往冻结区域里丢物品、summon 掉落物、方块变更)打的是真实时刻,
    //永远早于区内记录下来的检查时刻,锂会判定"期间没有任何变化"而入睡——之后没有任何通知能再叫醒它,
    //物品就一直躺在漏斗上。这里把这类外部活动按区块攒起来,在下一个区域步开始时补一次重绑
    //(重绑 = 锂语义下的完整唤醒: 会清掉休眠状态,闲下来还能再正常入睡),其余区块照旧跳过休眠项。
    private static final Map<ServerWorld, LongSet> PENDING_WAKE_CHUNKS = new IdentityHashMap<>();

    public static void markChunkActivity(World world, long chunkPos) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        //区域刻内: 锂自己的事件通路(容器内容变化/实体移动通知)与区内时间基准一致,不需要补。
        if (RegionTimeContext.getTime(serverWorld) != null) return;
        RegionTickManager region = RegionsManager.getRegionByChunk(serverWorld, chunkPos);
        if (region == null || !region.isControlled()) return;

        LongSet pending = PENDING_WAKE_CHUNKS.get(serverWorld);
        if (pending == null) {
            pending = new LongOpenHashSet(4);
            PENDING_WAKE_CHUNKS.put(serverWorld, pending);
        }
        pending.add(chunkPos);
    }

    //每个区域步开始时(方块实体阶段之前)调用,做两件事:
    //1) 重绑该区块里休眠的方块实体 = 锂语义下的完整唤醒(清掉休眠状态,闲下来还能再睡);
    //2) 让该区块的实体重新入档(stopTracking + startTracking): 锂在 Listener 的构造处就会发一次移动
    //   通知,时间戳取"当前时刻"——此刻正处在区域刻内,拿到的是虚拟时间,锂的
    //   SectionedEntityMovementTracker 才会认为"期间有变化"。
    //   缺了第 2 步,醒着的漏斗依然会在 HopperBlockEntityMixin 对 getItemsAtAndAbove 的短路处直接返回
    //   上一次的空结果(collectItemEntityTracker.isUnchangedSince(collectItemEntityAttemptTime)),不去
    //   找脚边静止的物品——这正是"物品一直在、漏斗就是不捡"的原因: 区外事件打的是真实时刻,永远早于
    //   区内记录下来的尝试时刻,而静止物品不会再产生任何新通知。
    //不属于本区域的记录原样留着,交给它所属的区域处理。
    public static void wakeChunksWithOutsideActivity(ServerWorld world, Set<Long> chunkPositions, LocalTickState tickState) {
        LongSet pending = PENDING_WAKE_CHUNKS.get(world);
        if (pending == null || pending.isEmpty()) return;
        if (!isLithiumLoaded()) {
            //非锂环境既没有休眠也没有这些时间型捷径,记录直接丢掉(避免长时间攒着)
            pending.clear();
            return;
        }

        LongSet consumed = REBIND_CHUNKS.get();
        consumed.clear();

        LongIterator iterator = pending.longIterator();
        while (iterator.hasNext()) {
            long chunkPos = iterator.nextLong();
            if (!chunkPositions.contains(chunkPos)) continue;
            iterator.remove();
            consumed.add(chunkPos);

            WorldChunk chunk = world.getChunkManager().getWorldChunk(
                    ChunkPos.getPackedX(chunkPos), ChunkPos.getPackedZ(chunkPos));
            if (chunk == null) continue;

            WorldChunkAccessor chunkAccessor = (WorldChunkAccessor) chunk;
            Map<BlockPos, ?> tickers = chunkAccessor.relativityTick$getBlockEntityTickers();
            if (tickers.isEmpty()) continue;

            List<BlockPos> pendingRebind = REBIND_PENDING.get();
            pendingRebind.clear();
            for (Map.Entry<BlockPos, ?> entry : tickers.entrySet()) {
                BlockEntity blockEntity = chunk.getBlockEntity(entry.getKey());
                if (blockEntity != null && !blockEntity.isRemoved()) {
                    pendingRebind.add(entry.getKey());
                }
            }

            //先收集再动作:updateTicker 在"该位置当前方块不再提供 ticker"时会移除表项,边遍历边改会炸遍历器
            for (int i = 0; i < pendingRebind.size(); i++) {
                BlockPos pos = pendingRebind.get(i);
                BlockEntity blockEntity = chunk.getBlockEntity(pos);
                if (blockEntity == null || blockEntity.isRemoved()) continue;

                chunkAccessor.relativityTick$updateTicker(blockEntity);
                if (tickers.get(pos) instanceof RebindableBlockEntityTickInvoker rebound) {
                    rebound.relativityTick$markRebound();
                }
            }
            pendingRebind.clear();
        }

        if (consumed.isEmpty()) return;

        //实体侧的唤醒(快照是副本,遍历期间改实体管理器自己的区段表是安全的)
        for (Entity entity : tickState.entitySnapshot(world)) {
            if (entity.isRemoved() || entity instanceof PlayerEntity) continue;
            if (!consumed.contains(ChunkPos.toLong(entity.getBlockPos()))) continue;

            notifyLithiumEntityMovement(world, entity);
        }
        consumed.clear();
    }

    //实体侧的唤醒。这里以前是"重新入档"(stopTracking + startTracking)重建实体的网络追踪器,
    //但那条路径不会走到锂的移动通知上: 锂只在实体自己的 ServerEntityManager.Listener 构造、
    //跨区段移动、被移除这三处发通知(锂 0.15.3 的 ServerEntityManagerListenerMixin),
    //重建网络追踪器跟这三件事都无关;代价却是先发销毁包再发一次生成包,客户端 ClientWorld.addEntity
    //会把同 id 的旧实体直接丢弃再建新实例——掉落物的 itemAge/uniqueOffset 是实例级状态,
    //于是动画播放状态被重置(表现为玩家一操作,掉落物的浮动/自转就从头开始)。
    //现在直接替锂执行它自己那次通知: 取实体所在区段,用锂自己的 getNotificationMask 算掩码,
    //调它自己的 lithium$trackEntityMovement(mask, world.getTime())。此刻正在区域刻内,
    //world.getTime() 就是虚拟时间,正是锂的时间戳比较所需要的。
    //安全性: 该通知只写区段上的时间戳、叫醒一次性监听者,不改任何实体/追踪器状态,
    //多发或多余地发一次最多让漏斗多扫一遍(它本来就是"宁可多扫"的语义)。
    private static void notifyLithiumEntityMovement(ServerWorld world, Entity entity) {
        if (!LithiumMovementNotify.isResolved()) {
            //这个锂版本没有这套入口: 退回旧做法(代价是客户端实体会被销毁重建)
            ServerEntityManagerAccessor<Entity> manager =
                    (ServerEntityManagerAccessor<Entity>) ((ServerWorldEntityAccessor) world).getEntityManager();
            manager.relativityTick$stopTracking(entity);
            manager.relativityTick$startTracking(entity);
            return;
        }

        //掩码为 0 表示锂对这些实体本来就不做通知(它只跟踪掉落物与容器类实体)
        int notificationMask = LithiumMovementNotify.notificationMask(entity);
        if (notificationMask == 0) return;

        SectionedEntityCache<Entity> cache = ((ServerEntityManagerAccessor<Entity>)
                ((ServerWorldEntityAccessor) world).getEntityManager()).getCache();
        Object section = cache.findTrackingSection(ChunkSectionPos.toLong(entity.getBlockPos()));
        if (section == null) return;

        LithiumMovementNotify.trackEntityMovement(section, notificationMask, world.getTime());
    }

    //锂的实体移动通知入口(反射解析一次,解析不到就视为该锂版本不兼容,由调用方走旧做法)
    private static final class LithiumMovementNotify {
        private static final String SECTION_CLASS_NAME =
                "net.caffeinemc.mods.lithium.common.tracking.entity.EntityMovementTrackerSection";
        private static final String HELPER_CLASS_NAME =
                "net.caffeinemc.mods.lithium.common.tracking.entity.MovementTrackerHelper";
        private static final Class<?> SECTION_CLASS;
        private static final Method NOTIFICATION_MASK;
        private static final Method TRACK_ENTITY_MOVEMENT;

        static {
            Class<?> sectionClass = null;
            Method notificationMask = null;
            Method trackEntityMovement = null;
            try {
                sectionClass = Class.forName(SECTION_CLASS_NAME);
                trackEntityMovement = sectionClass.getMethod("lithium$trackEntityMovement", int.class, long.class);
                notificationMask = Class.forName(HELPER_CLASS_NAME).getMethod("getNotificationMask", Entity.class);
            } catch (Throwable ignored) {
                sectionClass = null;
                notificationMask = null;
                trackEntityMovement = null;
            }
            SECTION_CLASS = sectionClass;
            NOTIFICATION_MASK = notificationMask;
            TRACK_ENTITY_MOVEMENT = trackEntityMovement;
        }

        private LithiumMovementNotify() {
        }

        static boolean isResolved() {
            return SECTION_CLASS != null && NOTIFICATION_MASK != null && TRACK_ENTITY_MOVEMENT != null;
        }

        static int notificationMask(Entity entity) {
            try {
                return (Integer) NOTIFICATION_MASK.invoke(null, entity);
            } catch (Throwable ignored) {
                return 0;
            }
        }

        static void trackEntityMovement(Object section, int notificationMask, long time) {
            if (!SECTION_CLASS.isInstance(section)) return;
            try {
                TRACK_ENTITY_MOVEMENT.invoke(section, notificationMask, time);
            } catch (Throwable ignored) {
            }
        }
    }

    //lithium兼容: 锂会让"没事干"的方块实体(漏斗/熔炉/酿造台/营火/潜滋盒等)入睡——把区块 ticker
    //包装器内部持有的 ticker 换成一个空转且**没有坐标**的 SLEEPING_BLOCK_ENTITY_TICKER。
    //原实现在每步无条件把这些方块实体按原版方式重绑回可工作的 ticker(WorldChunk.updateTicker),
    //而锂自己的 LevelChunkMixin 在 rebind/addBlockEntityTicker 处会清掉休眠状态,于是每步都把
    //它们叫醒: 每个区域步都要跑一次完整 tick(漏斗就是这样,比正常游戏卡很多),还要重建 ticker 对象。
    //现在反过来: 正在休眠的不动它,让锂的休眠在区域内照常生效。
    //
    //为什么安全: 锂的休眠是事件驱动的wake——容器内容变化、监听中的容器被移除、比较器接入、
    //掉落物/实体移动进追踪区段、setCooldown(7),以及最关键的"方块状态变化时原版 updateTicker"
    //(解锁/锁住漏斗就走这条),这些事件在区域内都由原版路径照常触发(本模组不绕过 setBlockState);
    //锂的时间型休眠(SleepUntilTimeBlockEntityTickInvoker、"只睡当前刻")读的是 Level.getGameTime(),
    //而区域刻里 World.getTime() 已被本模组换成虚拟时间,所以唤醒时刻也是自洽的。
    //代价: 一个正在休眠的方块实体每步只剩一次身份比较(rebindNeeded);它也不会再被本模组的
    //坐标索引步进(SLEEPING ticker 的 getPos() 为 null,本来就会被归到 unindexed),这正是休眠该有的样子。
    //加速: 原实现遍历区块里全部方块实体(箱子/告示牌等不 tick 的占多数),每个都要走一遍
    //getBlockEntityTicker + Map.compute + 两次分配;现在只遍历区块自己的 ticker 表,
    //并跳过"自上次绑定后内部 ticker 没被动过"的那些——入睡一定会换掉内部对象,所以身份没变
    //即等价于"这期间无人改动,再绑一次只是换成等价的新对象",跳过是等价的。
    private static final ThreadLocal<List<BlockPos>> REBIND_PENDING = ThreadLocal.withInitial(ArrayList::new);
    //外部活动消费时复用的一组区块号
    private static final ThreadLocal<LongSet> REBIND_CHUNKS = ThreadLocal.withInitial(() -> new LongOpenHashSet(4));

    private static boolean lithiumLoaded = false;
    private static boolean lithiumLoadedChecked = false;

    public static void rebindBlockEntityTickers(ServerWorld world, Set<Long> chunkPositions) {
        if (!isLithiumLoaded() || chunkPositions.isEmpty()) {
            return;
        }

        for (long chunkPosLong : chunkPositions) {
            WorldChunk chunk = world.getChunkManager().getWorldChunk(
                    ChunkPos.getPackedX(chunkPosLong), ChunkPos.getPackedZ(chunkPosLong));
            if (chunk == null) continue;

            WorldChunkAccessor chunkAccessor = (WorldChunkAccessor) chunk;
            Map<BlockPos, ?> tickers = chunkAccessor.relativityTick$getBlockEntityTickers();
            if (tickers.isEmpty()) continue;

            List<BlockPos> pending = REBIND_PENDING.get();
            pending.clear();
            for (Map.Entry<BlockPos, ?> entry : tickers.entrySet()) {
                //不是本模组包装器的(别的模组换过类型)按原行为一律重绑
                if (entry.getValue() instanceof RebindableBlockEntityTickInvoker rebindable) {
                    if (rebindable.relativityTick$isSleeping()) {
                        //正在休眠: 绝不重绑(重绑会经锂的 LevelChunkMixin 唤醒它),记为"已看过",
                        //这样每步只剩一次身份比较的开销;它自己醒过来(内部换成活 ticker)后
                        //rebindNeeded() 会重新为真,下一轮会正常处理。
                        rebindable.relativityTick$markRebound();
                        continue;
                    }
                    if (!rebindable.relativityTick$rebindNeeded()) {
                        continue;
                    }
                }
                pending.add(entry.getKey());
            }

            //先收集再动作:updateTicker 在"该位置当前方块不再提供 ticker"时会移除表项,边遍历边改会炸遍历器
            for (int i = 0; i < pending.size(); i++) {
                BlockPos pos = pending.get(i);
                BlockEntity blockEntity = chunk.getBlockEntity(pos);
                if (blockEntity == null || blockEntity.isRemoved()) continue;

                chunkAccessor.relativityTick$updateTicker(blockEntity);
                if (tickers.get(pos) instanceof RebindableBlockEntityTickInvoker rebound) {
                    rebound.relativityTick$markRebound();
                }
            }
            pending.clear();
        }
    }

    private static boolean isLithiumLoaded() {
        if (!lithiumLoadedChecked) {
            lithiumLoadedChecked = true;
            lithiumLoaded = FabricLoader.getInstance().isModLoaded("lithium");
        }
        return lithiumLoaded;
    }

    public static List<Entity> getOrderedEntitySnapshot(ServerWorld world) {
        return ENTITY_SNAPSHOT_BATCHES.get().get(world);
    }

    /**
     * 批内按区域过滤好的实体表。与原"每步从世界快照里过滤"得到的列表逐元素同序（同一源表、同一谓词），
     * 只是整批只算一次。调用方仍需每步复查 isRemoved/是否还在区域区块内/门控，因为批内实体可能移动或消失。
     */
    public static List<Entity> getRegionEntitySnapshot(
            ServerWorld world, RegionTickManager region, LongSet chunkPositions, List<Entity> worldSnapshot) {
        EntitySnapshotBatches batches = ENTITY_SNAPSHOT_BATCHES.get();
        EntityListState state = batches.states.get(world);
        if (state == null) {
            //还没取过该世界的快照（调用方应先调 entitySnapshot）：退回未过滤的世界快照，行为等价于原实现
            return worldSnapshot;
        }
        return state.regionView(region, chunkPositions, worldSnapshot);
    }

    private static final class EntitySnapshotBatches {
        private final IdentityHashMap<ServerWorld, EntityListState> states = new IdentityHashMap<>();
        private long batchIndex;

        private List<Entity> get(ServerWorld world) {
            EntityList entityList = ((ServerWorldAccessor) world).getEntityList();
            EntityListState state = states.get(world);
            if (state == null) {
                state = new EntityListState();
                states.put(world, state);
            }
            return state.get(entityList, batchIndex);
        }

        //实体列表增删只让"持有这份列表快照"的 state 失效。state 数很少（取过快照的世界数，个位数），
        //扫描一遍就是几次引用比较；这样别的维度/世界的增删不会连累本批的快照缓存。
        private void markMutated(EntityList entityList) {
            for (EntityListState state : states.values()) {
                state.markMutated(entityList);
            }
        }
    }

    //同一批内实体列表没被增删时复用同一份快照：一次区域刻里 tickEntities 与 collectEntityStates
    //都要遍历实体,各自都要一份完整列表；dash 的 N 步更是要 N 份。批序号在 beginRegionTickBatch
    //时推进,所以跨批必然重建；批内一旦增删（EntityListMixin → markEntityListMutated）也立即失效。
    //世界实体列表按网络 id 插入序存储,成员不变则顺序不变,所以复用与重建得到的遍历内容一致。
    //重建时始终新建列表对象（不复用旧实例）：调用方可能正持有旧列表迭代,原地清空会炸遍历器。
    private static final class EntityListState {
        private EntityList entityList;
        private long revision;
        private long snapshotBatch = Long.MIN_VALUE;
        private long snapshotRevision = Long.MIN_VALUE;
        private List<Entity> snapshot;
        //批内按区域过滤好的实体子表：内容与顺序等于"世界快照按该区域区块过滤"的子序列——同一份源表、
        //同一谓词、同一顺序，只是把过滤从"每步"提到"每批"。快照重建（跨批、或实体列表增删让 revision
        //前进）时整表作废，所以批内新增的实体会在下一步重新参与过滤；移出区域/被移除的实体由每步的复查
        //兜住。区域区块集合大小也参与校验，区域中途加/减区块时不会用旧表。
        private IdentityHashMap<RegionTickManager, List<Entity>> regionViews;
        private IdentityHashMap<RegionTickManager, Integer> regionViewSizes;

        private List<Entity> get(EntityList current, long batchIndex) {
            if (snapshot != null && entityList == current
                    && snapshotBatch == batchIndex && snapshotRevision == revision) {
                return snapshot;
            }

            List<Entity> updated = new ArrayList<>();
            current.forEach(updated::add);
            snapshot = updated;
            entityList = current;
            snapshotBatch = batchIndex;
            snapshotRevision = revision;
            //快照换了，批内的区域子表一律作废
            regionViews = null;
            regionViewSizes = null;
            return updated;
        }

        private List<Entity> regionView(RegionTickManager region, LongSet chunkPositions, List<Entity> snapshot) {
            if (regionViews != null) {
                List<Entity> cached = regionViews.get(region);
                if (cached != null) {
                    Integer size = regionViewSizes.get(region);
                    if (size != null && size == chunkPositions.size()) {
                        return cached;
                    }
                }
            }

            List<Entity> filtered = new ArrayList<>();
            for (int i = 0; i < snapshot.size(); i++) {
                Entity entity = snapshot.get(i);
                if (entity instanceof PlayerEntity || entity.isRemoved()) continue;
                if (!chunkPositions.contains(entity.getChunkPos().toLong())) continue;
                filtered.add(entity);
            }

            if (regionViews == null) {
                regionViews = new IdentityHashMap<>();
                regionViewSizes = new IdentityHashMap<>();
            }
            regionViews.put(region, filtered);
            regionViewSizes.put(region, chunkPositions.size());
            return filtered;
        }

        private void markMutated(EntityList mutated) {
            if (entityList == mutated) {
                revision++;
            }
        }
    }
    public static void forEachBlockEntityTicker(
            ServerWorld world,
            Set<Long> chunkPositions,
            Consumer<BlockEntityTickInvoker> action
    ) {
        WorldAccessor accessor = (WorldAccessor) world;
        List<BlockEntityTickInvoker> tickers = accessor.getBlockEntityTickers();
        List<BlockEntityTickInvoker> pending = accessor.getPendingBlockEntityTickers();
        IdentityHashMap<ServerWorld, BlockEntityTickerState> states = BLOCK_ENTITY_TICKER_STATES.get();
        BlockEntityTickerState state = states.get(world);
        if (state == null) {
            state = new BlockEntityTickerState();
            states.put(world, state);
        }

        accessor.setIteratingTickingBlockEntities(true);
        try {
            if (!pending.isEmpty()) {
                tickers.addAll(pending);
                pending.clear();
                state.dirty = true;
            }

            if (state.dirty) {
                state.rebuild(tickers);
            }
            state.forEach(chunkPositions, action);
        } finally {
            accessor.setIteratingTickingBlockEntities(false);
        }
    }

    private static final class BlockEntityTickerState {
        private static final Comparator<BlockEntityTickerCursor> CURSOR_COMPARATOR =
                Comparator.comparingInt(cursor -> cursor.current().order());
        private final Long2ObjectMap<List<IndexedBlockEntityTicker>> byChunk = new Long2ObjectOpenHashMap<>();
        private final List<IndexedBlockEntityTicker> unindexedTickers = new ArrayList<>();
        private final PriorityQueue<BlockEntityTickerCursor> cursors = new PriorityQueue<>(CURSOR_COMPARATOR);
        private boolean dirty = true;

        private void rebuild(List<BlockEntityTickInvoker> tickers) {
            byChunk.clear();
            unindexedTickers.clear();
            Iterator<BlockEntityTickInvoker> iterator = tickers.iterator();
            int order = 0;
            while (iterator.hasNext()) {
                BlockEntityTickInvoker invoker = iterator.next();
                if (invoker == null || invoker.isRemoved()) {
                    iterator.remove();
                    continue;
                }

                BlockPos pos = invoker.getPos();
                IndexedBlockEntityTicker indexedTicker = new IndexedBlockEntityTicker(invoker, order++);
                if (pos == null) {
                    unindexedTickers.add(indexedTicker);
                    continue;
                }

                addTicker(pos, indexedTicker);
            }
            dirty = false;
        }

        private void refreshUnindexedTickers() {
            Iterator<IndexedBlockEntityTicker> iterator = unindexedTickers.iterator();
            while (iterator.hasNext()) {
                IndexedBlockEntityTicker indexedTicker = iterator.next();
                BlockEntityTickInvoker invoker = indexedTicker.invoker();
                if (invoker.isRemoved()) {
                    iterator.remove();
                    continue;
                }

                BlockPos pos = invoker.getPos();
                if (pos == null) {
                    continue;
                }

                addTicker(pos, indexedTicker);
                iterator.remove();
            }
        }

        private void addTicker(BlockPos pos, IndexedBlockEntityTicker ticker) {
            long chunkPos = ChunkPos.toLong(pos);
            List<IndexedBlockEntityTicker> tickers = byChunk.get(chunkPos);
            if (tickers == null) {
                tickers = new ArrayList<>();
                byChunk.put(chunkPos, tickers);
            }
            int insertion = tickers.size();
            while (insertion > 0 && tickers.get(insertion - 1).order() > ticker.order()) {
                insertion--;
            }
            tickers.add(insertion, ticker);
        }

        private void forEach(Set<Long> chunkPositions, Consumer<BlockEntityTickInvoker> action) {
            refreshUnindexedTickers();
            try {
                for (long chunkPos : chunkPositions) {
                    List<IndexedBlockEntityTicker> tickers = byChunk.get(chunkPos);
                    if (tickers != null && !tickers.isEmpty()) {
                        cursors.add(obtainCursor(tickers));
                    }
                }

                while (!cursors.isEmpty()) {
                    BlockEntityTickerCursor cursor = cursors.poll();
                    IndexedBlockEntityTicker ticker = cursor.current();
                    if (!ticker.invoker().isRemoved()) {
                        action.accept(ticker.invoker());
                    }
                    if (cursor.advance()) {
                        cursors.add(cursor);
                    }
                }
            } finally {
                recycleAll();
            }
        }

        //游标改为池化复用：原实现每步都为每个有 ticker 的区块新建一个游标对象并丢弃。
        private final List<BlockEntityTickerCursor> cursorPool = new ArrayList<>();

        private BlockEntityTickerCursor obtainCursor(List<IndexedBlockEntityTicker> tickers) {
            BlockEntityTickerCursor cursor = cursorPool.isEmpty()
                    ? new BlockEntityTickerCursor()
                    : cursorPool.remove(cursorPool.size() - 1);
            cursor.reset(tickers);
            return cursor;
        }

        private void recycleAll() {
            for (BlockEntityTickerCursor cursor : cursors) {
                cursorPool.add(cursor);
            }
            cursors.clear();
        }
    }

    private record IndexedBlockEntityTicker(BlockEntityTickInvoker invoker, int order) {
    }

    private static final class BlockEntityTickerCursor {
        private List<IndexedBlockEntityTicker> tickers;
        private int index;

        private void reset(List<IndexedBlockEntityTicker> tickers) {
            this.tickers = tickers;
            this.index = 0;
        }

        private IndexedBlockEntityTicker current() {
            return tickers.get(index);
        }

        private boolean advance() {
            return ++index < tickers.size();
        }
    }

    public static boolean claimEntity(Entity entity, RegionTickManager region) {
        RegionTickManager declared = BATCH_SINGLE_REGION.get();
        if (declared != null) {
            if (declared == region) {
                return true;
            }
            return false;
        }

        RegionTickManager owner = REGION_TICK_OWNERS.get().putIfAbsent(entity, region);
        return owner == null || owner == region;
    }

    public static SpawnHelper.Info getSpawnInfo(ServerWorld world) {
        return SPAWN_INFOS.get().computeIfAbsent(world, ServerTickBridge::createSpawnInfo);
    }

    private static SpawnHelper.Info createSpawnInfo(ServerWorld world) {
        ServerChunkManagerAccessor managerAccessor = (ServerChunkManagerAccessor) world.getChunkManager();
        return SpawnHelper.setupSpawn(
                managerAccessor.getTicketManager().getTickedChunkCount(),
                world.iterateEntities(),
                managerAccessor::invokeIfChunkLoaded,
                new SpawnDensityCapper(managerAccessor.getChunkLoadingManager())
        );
    }
}




