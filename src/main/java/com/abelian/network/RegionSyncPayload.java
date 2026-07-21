package com.abelian.network;

import com.abelian.RelativityTick;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.HashSet;
import java.util.Set;

public record RegionSyncPayload(String id, String dimension, Set<Long> chunkPositions, boolean isControlled, boolean isRunning, boolean stepping, double rate, long virtualTime) implements CustomPayload {
    public static final Id<RegionSyncPayload> ID = new CustomPayload.Id<>(RelativityTick.REGION_SYNC_PACKET_ID);
    @Override
    public Id<? extends CustomPayload> getId() {return ID;}
    public static final PacketCodec<RegistryByteBuf, RegionSyncPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            RegionSyncPayload::id,

            PacketCodecs.STRING,
            RegionSyncPayload::dimension,

            PacketCodecs.collection(HashSet::new, PacketCodecs.LONG),
            RegionSyncPayload::chunkPositions,

            PacketCodecs.BOOLEAN,
            RegionSyncPayload::isControlled,

            PacketCodecs.BOOLEAN,
            RegionSyncPayload::isRunning,

            PacketCodecs.BOOLEAN,
            RegionSyncPayload::stepping,

            PacketCodecs.DOUBLE,
            RegionSyncPayload::rate,
            PacketCodecs.LONG,
            RegionSyncPayload::virtualTime,


            RegionSyncPayload::new
    );
}
