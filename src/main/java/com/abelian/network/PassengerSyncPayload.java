package com.abelian.network;

import com.abelian.RelativityTick;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;


public record PassengerSyncPayload(int passengerId, int vehicleID) implements CustomPayload{
    public static final Id<PassengerSyncPayload> ID = new CustomPayload.Id<>(RelativityTick.PASSENGER_SYNC_PACKET_ID);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static final PacketCodec<RegistryByteBuf, PassengerSyncPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER,
            PassengerSyncPayload::passengerId,

            PacketCodecs.INTEGER,
            PassengerSyncPayload::vehicleID,

            PassengerSyncPayload::new
    );
}
