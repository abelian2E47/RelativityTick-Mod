package com.abelian.network;

import com.abelian.RelativityTick;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.HashSet;
import java.util.Set;

public record SelectionOperationPayload(Set<Long> chunkPositions,String id) implements CustomPayload {
    public static final Id<SelectionOperationPayload> ID = new CustomPayload.Id<>(RelativityTick.SELECTION_OPERATION_PACKET_ID);
    @Override
    public Id<? extends CustomPayload> getId() {return ID;}

    public static final PacketCodec<PacketByteBuf, SelectionOperationPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.collection(HashSet::new, PacketCodecs.LONG),
                    SelectionOperationPayload::chunkPositions,

                    PacketCodecs.STRING,
                    SelectionOperationPayload::id,

                    SelectionOperationPayload::new
            );
}
