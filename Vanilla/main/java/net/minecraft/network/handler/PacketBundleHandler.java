package net.minecraft.network.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.BundlePacket;
import net.minecraft.network.packet.BundleSplitterPacket;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;

public interface PacketBundleHandler {
    int MAX_PACKETS = 4096;

    static <T extends PacketListener, P extends BundlePacket<? super T>> PacketBundleHandler create(
        final PacketType<P> id, final Function<Iterable<Packet<? super T>>, P> bundleFunction, final BundleSplitterPacket<? super T> splitter
    ) {
        return new PacketBundleHandler() {
            @Override
            public void forEachPacket(Packet<?> packet, Consumer<Packet<?>> consumer) {
                if (packet.getPacketType() == id) {
                    P bundlePacket = (P)packet;
                    consumer.accept(splitter);
                    bundlePacket.getPackets().forEach(consumer);
                    consumer.accept(splitter);
                } else {
                    consumer.accept(packet);
                }
            }

            @Nullable
            @Override
            public PacketBundleHandler.Bundler createBundler(Packet<?> splitterx) {
                return splitter == splitter ? new PacketBundleHandler.Bundler() {
                    private final List<Packet<? super T>> packets = new ArrayList<>();

                    @Nullable
                    @Override
                    public Packet<?> add(Packet<?> packet) {
                        if (packet == splitter) {
                            return bundleFunction.apply(this.packets);
                        }

                        Packet<T> packet2 = (Packet<T>)packet;
                        if (this.packets.size() >= 4096) {
                            throw new IllegalStateException("Too many packets in a bundle");
                        }

                        this.packets.add(packet2);
                        return null;
                    }
                } : null;
            }
        };
    }

    void forEachPacket(Packet<?> packet, Consumer<Packet<?>> consumer);

    @Nullable
    PacketBundleHandler.Bundler createBundler(Packet<?> splitter);

    interface Bundler {
        @Nullable
        Packet<?> add(Packet<?> packet);
    }
}

