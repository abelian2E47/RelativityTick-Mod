package com.abelian.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

public record EntityStateRecord(
        int entityId,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        double velocityX,
        double velocityY,
        double velocityZ
) {
    public static final PacketCodec<RegistryByteBuf, EntityStateRecord> CODEC = PacketCodec.of(EntityStateRecord::write, EntityStateRecord::new);

    private EntityStateRecord(RegistryByteBuf buf) {
        this(
                buf.readVarInt(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble()
        );
    }

    private void write(RegistryByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
        buf.writeDouble(velocityX);
        buf.writeDouble(velocityY);
        buf.writeDouble(velocityZ);
    }
}
