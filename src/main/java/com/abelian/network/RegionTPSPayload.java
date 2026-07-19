package com.abelian.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.codec.PacketCodecs;


import static com.abelian.RelativityTick.REGION_TPS_SYNC_PACKET_ID;

public record RegionTPSPayload(String regionID, float regionCostMs, double TPS) implements CustomPayload {
    public static final Id<RegionTPSPayload> ID = new CustomPayload.Id<>(REGION_TPS_SYNC_PACKET_ID);
    @Override
    public Id<? extends CustomPayload> getId() {return ID;}

    public static final PacketCodec<RegistryByteBuf, RegionTPSPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            RegionTPSPayload::regionID,

            PacketCodecs.FLOAT,
            RegionTPSPayload::regionCostMs,

            PacketCodecs.DOUBLE,
            RegionTPSPayload::TPS,

            RegionTPSPayload::new
    );


}
