package com.abelian.network;

import com.abelian.RelativityTick;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record RegionTimePayload(String regionId, long virtualTime) implements CustomPayload {
    public static final Id<RegionTimePayload> ID = new CustomPayload.Id<>(RelativityTick.REGION_TIME_PACKET_ID);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static final PacketCodec<RegistryByteBuf, RegionTimePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            RegionTimePayload::regionId,
            PacketCodecs.LONG,
            RegionTimePayload::virtualTime,
            RegionTimePayload::new
    );
}
