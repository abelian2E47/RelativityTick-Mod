package com.abelian.network;

import com.abelian.RelativityTick;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

    public record RegionStepPayload(String regionID, int steps, double accumulatorPhase) implements CustomPayload {
    public static final Id<RegionStepPayload> ID = new CustomPayload.Id<>(RelativityTick.REGION_STEP_PACKET_ID);
    @Override
    public Id<? extends CustomPayload> getId() {return ID;}

    public static final PacketCodec<RegistryByteBuf, RegionStepPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            RegionStepPayload::regionID,

            PacketCodecs.INTEGER,
            RegionStepPayload::steps,

            PacketCodecs.DOUBLE,
            RegionStepPayload::accumulatorPhase,


            RegionStepPayload::new
    );

}
