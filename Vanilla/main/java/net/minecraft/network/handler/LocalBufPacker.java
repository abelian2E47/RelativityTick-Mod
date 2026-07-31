package net.minecraft.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.OpaqueByteBufHolder;

public class LocalBufPacker extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext context, Object buf, ChannelPromise channelPromise) {
        context.write(OpaqueByteBufHolder.pack(buf), channelPromise);
    }
}

