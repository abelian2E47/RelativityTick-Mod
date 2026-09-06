package com.abelian;

import com.abelian.mixin.ServerChunkManagerAccessor;
import com.abelian.mixin.ServerWorldAccessor;
import com.abelian.mixin.WorldAccessor;
import com.abelian.regionTick.RegionTickManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.EntityList;
import net.minecraft.world.SpawnDensityCapper;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.BlockEntityTickInvoker;
import net.minecraft.world.chunk.WorldChunk;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
    private static final ThreadLocal<Map<ServerWorld, SpawnHelper.Info>> SPAWN_INFOS =
            ThreadLocal.withInitial(IdentityHashMap::new);
    private static final ThreadLocal<IdentityHashMap<EntityList, EntityListState>> ENTITY_LIST_STATES =
            ThreadLocal.withInitial(IdentityHashMap::new);
    private static final ThreadLocal<IdentityHashMap<ServerWorld, BlockEntityTickerState>> BLOCK_ENTITY_TICKER_STATES =
            ThreadLocal.withInitial(IdentityHashMap::new);

    public static boolean isCustomTickInProgress() {
        return CUSTOM_TICK_IN_PROGRESS.get();
    }

    public static void setCustomTickInProgress(boolean value) {
        CUSTOM_TICK_IN_PROGRESS.set(value);
    }

    public static void beginRegionTickBatch() {
        REGION_TICK_OWNERS.get().clear();
        SPAWN_INFOS.get().clear();
        ENTITY_LIST_STATES.get().clear();
        BLOCK_ENTITY_TICKER_STATES.get().clear();
    }

    public static void markEntityListMutated(EntityList entityList) {
        EntityListState state = ENTITY_LIST_STATES.get().get(entityList);
        if (state != null) {
            state.revision++;
        }
    }
    public static void markBlockEntityTickersDirty(World world) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        BlockEntityTickerState state = BLOCK_ENTITY_TICKER_STATES.get().get(serverWorld);
        if (state != null) {
            state.dirty = true;
        }
    }

    // ===== Lithium "block entity sleeping" compatibility =====
    // Lithium (mixin.world.block_entity_ticking.sleeping) swaps the ticker of idle block entities (hoppers,
    // furnaces, brewing stands, ...) to a no-op "sleeping" ticker that reports a null position, and only wakes
    // them via events (container content changes, entity/item movement, comparator/block updates).
    // RelativityTick advances controlled regions by ticking, which never produces those events by itself: a
    // block entity that falls asleep inside a controlled region would never be ticked again (e.g. hoppers stop
    // transferring items). Wake all sleeping block entities of the region's chunks before every region tick so
    // they get re-indexed and ticked exactly like vanilla. Sleeping stays active outside controlled regions.
    // No compile/runtime dependency on lithium: the interface is looked up reflectively and only used when the
    // mod is actually loaded.
    private static boolean lithiumSleepingChecked = false;
    private static boolean lithiumSleepingPresent = false;
    private static Class<?> lithiumSleepingBlockEntityClass;
    private static Method lithiumSleepingBlockEntityIsSleeping;
    private static Method lithiumSleepingBlockEntityWakeUp;

    public static void wakeSleepingBlockEntities(ServerWorld world, Set<Long> chunkPositions) {
        if (!lithiumSleepingAvailable() || chunkPositions.isEmpty()) {
            return;
        }

        Class<?> sleepingClass = lithiumSleepingBlockEntityClass;
        Method isSleeping = lithiumSleepingBlockEntityIsSleeping;
        Method wakeUp = lithiumSleepingBlockEntityWakeUp;
        for (long chunkPosLong : chunkPositions) {
            ChunkPos chunkPos = new ChunkPos(chunkPosLong);
            WorldChunk chunk = world.getChunkManager().getWorldChunk(chunkPos.x, chunkPos.z);
            if (chunk == null) continue;

            for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                if (blockEntity.isRemoved() || !sleepingClass.isInstance(blockEntity)) continue;
                try {
                    if ((Boolean) isSleeping.invoke(blockEntity)) {
                        wakeUp.invoke(blockEntity);
                    }
                } catch (ReflectiveOperationException e) {
                    //接口异常时静默放弃唤醒,回退为未装锂时的行为
                    lithiumSleepingPresent = false;
                    return;
                }
            }
        }
    }

    private static boolean lithiumSleepingAvailable() {
        if (lithiumSleepingChecked) {
            return lithiumSleepingPresent;
        }
        lithiumSleepingChecked = true;
        try {
            if (!FabricLoader.getInstance().isModLoaded("lithium")) {
                return false;
            }
            Class<?> sleepingClass = Class.forName(
                    "net.caffeinemc.mods.lithium.common.block.entity.SleepingBlockEntity", false,
                    ServerTickBridge.class.getClassLoader());
            lithiumSleepingBlockEntityIsSleeping = sleepingClass.getMethod("isSleeping");
            lithiumSleepingBlockEntityWakeUp = sleepingClass.getMethod("wakeUpNow");
            lithiumSleepingBlockEntityClass = sleepingClass;
            lithiumSleepingPresent = true;
        } catch (Throwable ignored) {
            //未装锂或版本接口不一致时保持原行为
        }
        return lithiumSleepingPresent;
    }

    public static List<Entity> getOrderedEntitySnapshot(ServerWorld world) {
        EntityList entityList = ((ServerWorldAccessor) world).getEntityList();
        IdentityHashMap<EntityList, EntityListState> states = ENTITY_LIST_STATES.get();
        EntityListState state = states.get(entityList);
        if (state == null) {
            state = new EntityListState();
            states.put(entityList, state);
        }

        if (state.snapshot != null && state.snapshotRevision == state.revision) {
            return state.snapshot;
        }

        List<Entity> snapshot = new ArrayList<>();
        entityList.forEach(snapshot::add);
        state.snapshot = snapshot;
        state.snapshotRevision = state.revision;
        return snapshot;
    }

    private static final class EntityListState {
        private long revision;
        private long snapshotRevision = Long.MIN_VALUE;
        private List<Entity> snapshot;
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
        private final Map<Long, List<IndexedBlockEntityTicker>> byChunk = new HashMap<>();
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
                    // Lithium keeps sleeping tickers in the world's list. They become indexable again when woken.
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
            List<IndexedBlockEntityTicker> tickers = byChunk.computeIfAbsent(chunkPos, ignored -> new ArrayList<>());
            int insertion = tickers.size();
            while (insertion > 0 && tickers.get(insertion - 1).order() > ticker.order()) {
                insertion--;
            }
            tickers.add(insertion, ticker);
        }

        private void forEach(Set<Long> chunkPositions, Consumer<BlockEntityTickInvoker> action) {
            refreshUnindexedTickers();
            cursors.clear();
            try {
                for (long chunkPos : chunkPositions) {
                    List<IndexedBlockEntityTicker> tickers = byChunk.get(chunkPos);
                    if (tickers != null && !tickers.isEmpty()) {
                        cursors.add(new BlockEntityTickerCursor(tickers));
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
                cursors.clear();
            }
        }
    }

    private record IndexedBlockEntityTicker(BlockEntityTickInvoker invoker, int order) {
    }

    private static final class BlockEntityTickerCursor {
        private final List<IndexedBlockEntityTicker> tickers;
        private int index;

        private BlockEntityTickerCursor(List<IndexedBlockEntityTicker> tickers) {
            this.tickers = tickers;
        }

        private IndexedBlockEntityTicker current() {
            return tickers.get(index);
        }

        private boolean advance() {
            return ++index < tickers.size();
        }
    }

    public static boolean claimEntity(Entity entity, RegionTickManager region) {
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
