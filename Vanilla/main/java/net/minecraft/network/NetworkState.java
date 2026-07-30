package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.handler.PacketBundleHandler;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.util.annotation.Debug;

public interface NetworkState<T extends PacketListener> {
    NetworkPhase id();

    NetworkSide side();

    PacketCodec<ByteBuf, Packet<? super T>> codec();

    @Nullable
    PacketBundleHandler bundleHandler();

    interface Factory<T extends PacketListener, B extends ByteBuf> {
        NetworkState<T> bind(Function<ByteBuf, B> registryBinder);

        NetworkPhase phase();

        NetworkSide side();

        @Debug
        void forEachPacketType(NetworkState.Factory.PacketTypeConsumer callback);

        @FunctionalInterface
        interface PacketTypeConsumer {
            void accept(PacketType<?> type, int protocolId);
        }
    }
}

