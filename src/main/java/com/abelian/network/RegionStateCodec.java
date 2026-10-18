package com.abelian.network;

import com.abelian.regionTick.RegionTickManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public final class RegionStateCodec {
    public static final PacketCodec<ByteBuf, RegionTickManager.RegionState> CODEC =
            PacketCodecs.indexed(i -> RegionTickManager.RegionState.values()[i], RegionTickManager.RegionState::ordinal);

    private RegionStateCodec() {
    }
}
