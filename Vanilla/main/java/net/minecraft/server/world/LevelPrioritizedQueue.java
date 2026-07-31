package net.minecraft.server.world;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.util.List;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.util.math.ChunkPos;

public class LevelPrioritizedQueue {
    public static final int LEVEL_COUNT = ChunkLevels.INACCESSIBLE + 2;
    private final List<Long2ObjectLinkedOpenHashMap<List<Runnable>>> values = IntStream.range(0, LEVEL_COUNT)
        .mapToObj(level -> new Long2ObjectLinkedOpenHashMap<>())
        .toList();
    private volatile int topPriority = LEVEL_COUNT;
    private final String name;

    public LevelPrioritizedQueue(String name) {
        this.name = name;
    }

    protected void updateLevel(int fromLevel, ChunkPos pos, int toLevel) {
        if (fromLevel < LEVEL_COUNT) {
            Long2ObjectLinkedOpenHashMap<List<Runnable>> long2ObjectLinkedOpenHashMap = this.values.get(fromLevel);
            List<Runnable> list = long2ObjectLinkedOpenHashMap.remove(pos.toLong());
            if (fromLevel == this.topPriority) {
                while (this.hasQueuedElement() && this.values.get(this.topPriority).isEmpty()) {
                    this.topPriority++;
                }
            }

            if (list != null && !list.isEmpty()) {
                this.values.get(toLevel).computeIfAbsent(pos.toLong(), chunkPos -> Lists.newArrayList()).addAll(list);
                this.topPriority = Math.min(this.topPriority, toLevel);
            }
        }
    }

    protected void add(Runnable task, long pos, int level) {
        this.values.get(level).computeIfAbsent(pos, chunkPos -> Lists.newArrayList()).add(task);
        this.topPriority = Math.min(this.topPriority, level);
    }

    protected void remove(long pos, boolean removeElement) {
        for (Long2ObjectLinkedOpenHashMap<List<Runnable>> long2ObjectLinkedOpenHashMap : this.values) {
            List<Runnable> list = long2ObjectLinkedOpenHashMap.get(pos);
            if (list != null) {
                if (removeElement) {
                    list.clear();
                }

                if (list.isEmpty()) {
                    long2ObjectLinkedOpenHashMap.remove(pos);
                }
            }
        }

        while (this.hasQueuedElement() && this.values.get(this.topPriority).isEmpty()) {
            this.topPriority++;
        }
    }

    @Nullable
    public LevelPrioritizedQueue.Entry poll() {
        if (!this.hasQueuedElement()) {
            return null;
        }

        int i = this.topPriority;
        Long2ObjectLinkedOpenHashMap<List<Runnable>> long2ObjectLinkedOpenHashMap = this.values.get(i);
        long l = long2ObjectLinkedOpenHashMap.firstLongKey();
        List<Runnable> list = long2ObjectLinkedOpenHashMap.removeFirst();

        while (this.hasQueuedElement() && this.values.get(this.topPriority).isEmpty()) {
            this.topPriority++;
        }

        return new LevelPrioritizedQueue.Entry(l, list);
    }

    public boolean hasQueuedElement() {
        return this.topPriority < LEVEL_COUNT;
    }

    @Override
    public String toString() {
        return this.name + " " + this.topPriority + "...";
    }

    public record Entry(long chunkPos, List<Runnable> tasks) {
    }
}

