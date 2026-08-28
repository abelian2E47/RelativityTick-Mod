package com.abelian.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.math.BlockPos;

public record ScheduledTickRecord(BlockPos pos, long trigger, long subTickOrder, int priority) {
    public static final PacketCodec<RegistryByteBuf, ScheduledTickRecord> CODEC = PacketCodec.of(ScheduledTickRecord::write, ScheduledTickRecord::new);

    private ScheduledTickRecord(RegistryByteBuf buf) {
        this(
                buf.readBlockPos(),
                buf.readLong(),
                buf.readLong(),
                buf.readInt()
        );
    }

    private void write(RegistryByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeLong(trigger);
        buf.writeLong(subTickOrder);
        buf.writeInt(priority);
    }
}
