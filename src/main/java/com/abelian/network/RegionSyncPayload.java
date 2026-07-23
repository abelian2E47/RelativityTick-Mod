package com.abelian.network;

import com.abelian.RelativityTick;
import com.abelian.regionTick.Region;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.HashSet;
import java.util.Set;

public record RegionSyncPayload(String id, String dimension, Set<Long> chunkPositions, Region.RegionState state, double rate) implements CustomPayload {
    public static final Id<RegionSyncPayload> ID = new CustomPayload.Id<>(RelativityTick.REGION_SYNC_PACKET_ID);
    @Override
    public Id<? extends CustomPayload> getId() {return ID;}
    private static final PacketCodec<ByteBuf, Region.RegionState> STATE_CODEC =
            PacketCodecs.indexed(i -> Region.RegionState.values()[i], Region.RegionState::ordinal);

    public static final PacketCodec<ByteBuf, RegionSyncPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            RegionSyncPayload::id,

            PacketCodecs.STRING,
            RegionSyncPayload::dimension,

            PacketCodecs.collection(HashSet::new, PacketCodecs.LONG),
            RegionSyncPayload::chunkPositions,

            STATE_CODEC,
            RegionSyncPayload::state,

            PacketCodecs.DOUBLE,
            RegionSyncPayload::rate,

            RegionSyncPayload::new
    );
}
