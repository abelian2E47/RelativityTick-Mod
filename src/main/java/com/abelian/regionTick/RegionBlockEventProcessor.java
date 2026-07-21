package com.abelian.regionTick;

import com.abelian.mixin.ServerWorldAccessor;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.network.packet.s2c.play.BlockEventS2CPacket;
import net.minecraft.server.world.BlockEvent;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class RegionBlockEventProcessor {
    private RegionBlockEventProcessor() {
    }

    public static void process(ServerWorld world, Predicate<RegionTickManager> shouldProcess) {
        ServerWorldAccessor accessor = (ServerWorldAccessor) world;
        ObjectLinkedOpenHashSet<BlockEvent> queue = accessor.getSyncedBlockEventQueue();
        if (queue.isEmpty()) return;

        List<BlockEvent> deferred = new ArrayList<>(queue.size());
        while (!queue.isEmpty()) {
            BlockEvent event = queue.removeFirst();
            RegionTickManager owner = RegionsManager.getRegionByChunk(world, ChunkPos.toLong(event.pos()));
            if (!shouldProcess.test(owner) || !world.shouldTickBlockPos(event.pos())) {
                deferred.add(event);
                continue;
            }

            if (accessor.invokeProcessBlockEvent(event)) {
                BlockPos pos = event.pos();
                world.getServer().getPlayerManager().sendToAround(null, pos.getX(), pos.getY(), pos.getZ(), 64.0,
                        world.getRegistryKey(), new BlockEventS2CPacket(pos, event.block(), event.type(), event.data()));
            }
        }
        queue.addAll(deferred);
    }
}
