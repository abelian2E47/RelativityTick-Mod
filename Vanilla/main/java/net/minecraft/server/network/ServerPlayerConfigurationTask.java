package net.minecraft.server.network;

import java.util.function.Consumer;
import net.minecraft.network.packet.Packet;

public interface ServerPlayerConfigurationTask {
    void sendPacket(Consumer<Packet<?>> sender);

    ServerPlayerConfigurationTask.Key getKey();

    record Key(String id) {
        @Override
        public String toString() {
            return this.id;
        }
    }
}

