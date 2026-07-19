package com.abelian.network;

import com.abelian.RelativityTick;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;

public record RegionEntitySyncPayload(String regionId, List<EntityStateRecord> entities) implements CustomPayload {
    public static final Id<RegionEntitySyncPayload> ID = new CustomPayload.Id<>(RelativityTick.REGION_ENTITY_SYNC_PACKET_ID);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static final PacketCodec<RegistryByteBuf, RegionEntitySyncPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            RegionEntitySyncPayload::regionId,

            PacketCodecs.collection(ArrayList::new, EntityStateRecord.CODEC),
            RegionEntitySyncPayload::entities,

            RegionEntitySyncPayload::new
    );
}
