package com.abelian.network;


import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;

import static com.abelian.RelativityTick.SCHEDULED_TICK_DATA_PAYLOAD;

public record ScheduledTickDataPayload(List<ScheduledTickRecord> scheduledTicks) implements CustomPayload  {
    public static final Id<ScheduledTickDataPayload> ID = new CustomPayload.Id<>(SCHEDULED_TICK_DATA_PAYLOAD);
    @Override
    public Id<? extends CustomPayload> getId() {return ID;}

    public static final PacketCodec<RegistryByteBuf, ScheduledTickDataPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.collection(ArrayList::new, ScheduledTickRecord.CODEC),
            ScheduledTickDataPayload::scheduledTicks,

            ScheduledTickDataPayload::new
    );

}
