package net.minecraft.network.packet.c2s.play;

import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.packet.PlayPackets;
import net.minecraft.util.math.Vec3d;

public record VehicleMoveC2SPacket(Vec3d position, float yaw, float pitch, boolean onGround) implements Packet<ServerPlayPacketListener> {
    public static final PacketCodec<PacketByteBuf, VehicleMoveC2SPacket> CODEC = PacketCodec.tuple(
        Vec3d.PACKET_CODEC,
        VehicleMoveC2SPacket::position,
        PacketCodecs.FLOAT,
        VehicleMoveC2SPacket::yaw,
        PacketCodecs.FLOAT,
        VehicleMoveC2SPacket::pitch,
        PacketCodecs.BOOLEAN,
        VehicleMoveC2SPacket::onGround,
        VehicleMoveC2SPacket::new
    );

    public static VehicleMoveC2SPacket fromVehicle(Entity vehicle) {
        return new VehicleMoveC2SPacket(
            new Vec3d(vehicle.getLerpTargetX(), vehicle.getLerpTargetY(), vehicle.getLerpTargetZ()), vehicle.getYaw(), vehicle.getPitch(), vehicle.isOnGround()
        );
    }

    @Override
    public PacketType<VehicleMoveC2SPacket> getPacketType() {
        return PlayPackets.MOVE_VEHICLE_C2S;
    }

    public void apply(ServerPlayPacketListener serverPlayPacketListener) {
        serverPlayPacketListener.onVehicleMove(this);
    }
}

