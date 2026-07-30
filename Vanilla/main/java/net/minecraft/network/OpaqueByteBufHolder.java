package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.ReferenceCounted;

/**
 * A holder of {@link ByteBuf}. This is used in local connection to skip
 * unnecessary operations that check {@code instanceof ByteBuf}.
 */
public record OpaqueByteBufHolder(ByteBuf contents) implements ReferenceCounted {
    public OpaqueByteBufHolder(final ByteBuf contents) {
        this.contents = ByteBufUtil.ensureAccessible(contents);
    }

    public static Object pack(Object buf) {
        return buf instanceof ByteBuf byteBuf ? new OpaqueByteBufHolder(byteBuf) : buf;
    }

    public static Object unpack(Object holder) {
        return holder instanceof OpaqueByteBufHolder opaqueByteBufHolder ? ByteBufUtil.ensureAccessible(opaqueByteBufHolder.contents) : holder;
    }

    @Override
    public int refCnt() {
        return this.contents.refCnt();
    }

    public OpaqueByteBufHolder retain() {
        this.contents.retain();
        return this;
    }

    public OpaqueByteBufHolder retain(int i) {
        this.contents.retain(i);
        return this;
    }

    public OpaqueByteBufHolder touch() {
        this.contents.touch();
        return this;
    }

    public OpaqueByteBufHolder touch(Object object) {
        this.contents.touch(object);
        return this;
    }

    @Override
    public boolean release() {
        return this.contents.release();
    }

    @Override
    public boolean release(int count) {
        return this.contents.release(count);
    }
}

