package com.abelian.network;

import com.abelian.regionTick.RegionTickManager;
import com.abelian.regionTick.RegionsManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public final class RegionStatusSync {
    private static final long MIN_REQUEST_INTERVAL_NANOS = 40_000_000L;
    private static final Map<UUID, Long> LAST_REQUEST_NANOS = new ConcurrentHashMap<>();

    private RegionStatusSync() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(RegionStatusRequestPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (!allowRequest(player)) return;
            context.server().execute(() -> sendStatus(player, payload.regionId()));
        });
    }

    public static void clear() {
        LAST_REQUEST_NANOS.clear();
    }

    private static boolean allowRequest(ServerPlayerEntity player) {
        long now = System.nanoTime();
        Long previous = LAST_REQUEST_NANOS.put(player.getUuid(), now);
        return previous == null || now - previous >= MIN_REQUEST_INTERVAL_NANOS;
    }

    private static void sendStatus(ServerPlayerEntity player, String regionId) {
        RegionTickManager region = RegionsManager.getRegion(regionId);
        if (region == null) return;

        ServerWorld world = player.getServerWorld();
        if (world == null || !region.isInWorld(world)) return;

        ServerPlayNetworking.send(player, RegionStatusPayload.of(region));
    }
}
