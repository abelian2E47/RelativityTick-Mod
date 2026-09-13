package com.abelian.network;

import com.abelian.RelativityTick;
import com.abelian.regionTick.RegionTickManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public record RegionSyncPayload(String id, String dimension, Set<Long> chunkPositions, RegionTickManager.RegionState state, double rate, long virtualTime, byte disableFlags) implements CustomPayload {
    public static final Id<RegionSyncPayload> ID = new CustomPayload.Id<>(RelativityTick.REGION_SYNC_PACKET_ID);
    @Override
    public Id<? extends CustomPayload> getId() {return ID;}
    private static final PacketCodec<ByteBuf, RegionTickManager.RegionState> STATE_CODEC =
            PacketCodecs.indexed(i -> RegionTickManager.RegionState.values()[i], RegionTickManager.RegionState::ordinal);
    private static final PacketCodec<ByteBuf, Collection<Long>> CHUNK_POSITIONS_CODEC =
            PacketCodecs.collection(HashSet::new, PacketCodecs.VAR_LONG);

    public RegionSyncPayload(String id, String dimension, Set<Long> chunkPositions, RegionTickManager.RegionState state, double rate, long virtualTime,
                             boolean disableHopperTick, boolean disableEntityTick, boolean disableObserverTick) {
        this(id, dimension, chunkPositions, state, rate, virtualTime,
                (byte) ((disableHopperTick ? 1 : 0) | (disableEntityTick ? 2 : 0) | (disableObserverTick ? 4 : 0)));
    }

    public boolean disableHopperTick() {
        return (disableFlags & 1) != 0;
    }

    public boolean disableEntityTick() {
        return (disableFlags & 2) != 0;
    }

    public boolean disableObserverTick() {
        return (disableFlags & 4) != 0;
    }

    //MC 1.21 的 PacketCodec.tuple 最多 6 元，本包有 7 个字段，改为显式编解码
    public static final PacketCodec<ByteBuf, RegionSyncPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                PacketCodecs.STRING.encode(buf, payload.id());
                PacketCodecs.STRING.encode(buf, payload.dimension());
                CHUNK_POSITIONS_CODEC.encode(buf, payload.chunkPositions());
                STATE_CODEC.encode(buf, payload.state());
                PacketCodecs.DOUBLE.encode(buf, payload.rate());
                PacketCodecs.VAR_LONG.encode(buf, payload.virtualTime());
                PacketCodecs.BYTE.encode(buf, payload.disableFlags());
            },
            buf -> new RegionSyncPayload(
                    PacketCodecs.STRING.decode(buf),
                    PacketCodecs.STRING.decode(buf),
                    new HashSet<>(CHUNK_POSITIONS_CODEC.decode(buf)),
                    STATE_CODEC.decode(buf),
                    PacketCodecs.DOUBLE.decode(buf),
                    PacketCodecs.VAR_LONG.decode(buf),
                    PacketCodecs.BYTE.decode(buf))
    );
}
