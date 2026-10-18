package com.abelian.network;

import com.abelian.RelativityTick;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record RegionStatusRequestPayload(String regionId) implements CustomPayload {
    public static final Id<RegionStatusRequestPayload> ID =
            new CustomPayload.Id<>(RelativityTick.REGION_STATUS_REQUEST_PACKET_ID);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static final PacketCodec<RegistryByteBuf, RegionStatusRequestPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            RegionStatusRequestPayload::regionId,

            RegionStatusRequestPayload::new
    );
}
