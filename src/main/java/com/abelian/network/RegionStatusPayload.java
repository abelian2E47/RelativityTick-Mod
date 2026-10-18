package com.abelian.network;

import com.abelian.RelativityTick;
import com.abelian.regionTick.RegionTickManager;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record RegionStatusPayload(
        String regionId,
        RegionTickManager.RegionState state,
        double rate,
        double tps,
        boolean tpsSampleReady,
        float tickWindowMs,
        float msprtMs,
        double tickWindowLimitMs,
        long virtualTime,
        int chunkCount,
        int pendingSteps,
        int pendingDashSteps,
        int sprintRemainingGt,
        boolean sprintContinuous,
        boolean reachedMsptLimit,
        boolean reachedTickDurationLimit,
        byte disableFlags
) implements CustomPayload {
    public static final Id<RegionStatusPayload> ID =
            new CustomPayload.Id<>(RelativityTick.REGION_STATUS_PACKET_ID);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static RegionStatusPayload of(RegionTickManager region) {
        return new RegionStatusPayload(
                region.getID(),
                region.getState(),
                region.getRate(),
                region.getTPS(),
                region.isStepRateSampleReady(),
                region.getTickWindowMs(),
                region.getMsprtMs(),
                region.getTickWindowLimitMs(),
                region.getVirtualTime(),
                region.getChunkPositions().size(),
                region.getPendingSteps(),
                region.getPendingDashSteps(),
                region.getSprintRemainingGt(),
                region.isSprintContinuous(),
                region.hasReachedMsptLimit(),
                region.hasReachedTickDurationLimit(),
                encodeDisableFlags(region));
    }

    private static byte encodeDisableFlags(RegionTickManager region) {
        return (byte) ((region.isDisableHopperTick() ? 1 : 0)
                | (region.isDisableEntityTick() ? 2 : 0)
                | (region.isDisableObserverTick() ? 4 : 0));
    }

    public boolean disableHopperTick() {
        return (disableFlags & 1) != 0;
    }

    public boolean disableEntityTick() {
        return (disableFlags & 2) != 0;
    }

    public boolean disableObserverTick() {
        return (disableFlags & 4) != 0;
    }

    public int pendingWorkSteps() {
        return pendingSteps + pendingDashSteps;
    }

    private void write(RegistryByteBuf buf) {
        buf.writeString(regionId);
        RegionStateCodec.CODEC.encode(buf, state);
        buf.writeDouble(rate);
        buf.writeDouble(tps);
        buf.writeBoolean(tpsSampleReady);
        buf.writeFloat(tickWindowMs);
        buf.writeFloat(msprtMs);
        buf.writeDouble(tickWindowLimitMs);
        buf.writeLong(virtualTime);
        buf.writeVarInt(chunkCount);
        buf.writeVarInt(pendingSteps);
        buf.writeVarInt(pendingDashSteps);
        buf.writeVarInt(sprintRemainingGt);
        buf.writeBoolean(sprintContinuous);
        buf.writeBoolean(reachedMsptLimit);
        buf.writeBoolean(reachedTickDurationLimit);
        buf.writeByte(disableFlags);
    }

    private static RegionStatusPayload read(RegistryByteBuf buf) {
        return new RegionStatusPayload(
                buf.readString(),
                RegionStateCodec.CODEC.decode(buf),
                buf.readDouble(),
                buf.readDouble(),
                buf.readBoolean(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readDouble(),
                buf.readLong(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readByte());
    }

    public static final PacketCodec<RegistryByteBuf, RegionStatusPayload> CODEC =
            PacketCodec.of((value, buf) -> value.write(buf), RegionStatusPayload::read);
}
