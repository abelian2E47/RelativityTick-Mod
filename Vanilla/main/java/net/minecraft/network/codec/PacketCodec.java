package net.minecraft.network.codec;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import com.mojang.datafixers.util.Function6;
import com.mojang.datafixers.util.Function7;
import com.mojang.datafixers.util.Function8;
import io.netty.buffer.ByteBuf;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * A codec that is used for serializing a packet.
 * 
 * <p>Packet codecs serialize to, and deserialize from, {@link net.minecraft.network.PacketByteBuf},
 * which is a stream of data. To integrate the classic {@link net.minecraft.network.PacketByteBuf}-based
 * code, use {@link #of(ValueFirstEncoder, PacketDecoder)}
 * like this:
 * 
 * <pre>{@code
 * public static final PacketCodec<PacketByteBuf, MyPacket> CODEC = PacketCodec.of(MyPacket::write, MyPacket::new);
 * 
 * private MyPacket(PacketByteBuf buf) {
 * 	this.text = buf.readString();
 * }
 * 
 * private void write(PacketByteBuf buf) {
 * 	buf.writeString(this.text);
 * }
 * }</pre>
 * 
 * <p>While this serves similar functions as codecs in the DataFixerUpper library,
 * the two are wholly separate and DataFixerUpper methods cannot be used with this.
 * However, a packet codec may reference a regular codec by using {@link
 * PacketCodecs#codec}, which serializes the data to NBT.
 * 
 * <p>See {@link PacketCodecs} for codecs to serialize various objects.
 * 
 * @param <B> the type of the buffer; {@link net.minecraft.network.RegistryByteBuf}
 * for play-phase packets, {@link net.minecraft.network.PacketByteBuf} for other
 * phases (like configuration)
 * @param <V> the type of the value to be encoded/decoded
 */
public interface PacketCodec<B, V> extends PacketDecoder<B, V>, PacketEncoder<B, V> {
    /**
     * {@return a packet codec from the {@code encoder} and {@code decoder}}
     * 
     * @apiNote This is useful for integrating with code that uses static methods for
     * packet writing, where the buffer is the first argument, like
     * {@code static void write(PacketByteBuf buf, Data data)}.
     * For code that uses instance methods like {@code void write(PacketByteBuf buf)},
     * use {@link #of(ValueFirstEncoder, PacketDecoder)}.
     */
    static <B, V> PacketCodec<B, V> ofStatic(final PacketEncoder<B, V> encoder, final PacketDecoder<B, V> decoder) {
        return new PacketCodec<B, V>() {
            @Override
            public V decode(B buf) {
                return decoder.decode(buf);
            }

            @Override
            public void encode(B buf, V value) {
                encoder.encode(buf, value);
            }
        };
    }

    /**
     * {@return a packet codec from the {@code encoder} and {@code decoder}}
     * 
     * @apiNote This is useful for integrating with code that uses instance methods for
     * packet writing, like {@code void write(PacketByteBuf buf)}.
     * For code that uses static methods like {@code static void write(PacketByteBuf buf, Data data)},
     * where the buffer is the first argument, use {@link #ofStatic(PacketEncoder, PacketDecoder)}.
     */
    static <B, V> PacketCodec<B, V> of(final ValueFirstEncoder<B, V> encoder, final PacketDecoder<B, V> decoder) {
        return new PacketCodec<B, V>() {
            @Override
            public V decode(B buf) {
                return decoder.decode(buf);
            }

            @Override
            public void encode(B buf, V value) {
                encoder.encode(value, buf);
            }
        };
    }

    /**
     * {@return a codec that always returns {@code value}}
     * 
     * <p>This does not encode anything. Instead, it throws {@link
     * IllegalStateException} when the value does not
     * equal {@code value}. This comparison is made with {@code equals()}, not
     * reference equality ({@code ==}).
     */
    static <B, V> PacketCodec<B, V> unit(final V value) {
        return new PacketCodec<B, V>() {
            @Override
            public V decode(B buf) {
                return value;
            }

            @Override
            public void encode(B buf, V valuex) {
                if (!value.equals(value)) {
                    throw new IllegalStateException("Can't encode '" + value + "', expected '" + value + "'");
                }
            }
        };
    }

    /**
     * {@return the result mapped with {@code function}}
     * 
     * <p>For example, passing {@code PacketCodecs::optional} makes the value
     * optional. Additionally, this method can be used like Stream {@link
     * java.util.stream.Collectors} - hence its name. For example, to make a codec
     * for a list of something, write {@code parentCodec.collect(PacketCodecs.toList())}.
     * 
     * @see PacketCodecs#optional
     * @see PacketCodecs#toCollection
     * @see PacketCodecs#toList
     */
    default <O> PacketCodec<B, O> collect(PacketCodec.ResultFunction<B, V, O> function) {
        return function.apply(this);
    }

    /**
     * {@return a codec that maps its encode input and decode output with {@code from}
     * and {@code to}, respectively}
     * 
     * <p>This can be used to transform a codec for a simple value (like a string)
     * into a corresponding, more complex value (like an identifier). An example:
     * 
     * <pre>{@code
     * public static final PacketCodec<ByteBuf, Identifier> PACKET_CODEC = PacketCodecs.STRING.xmap(Identifier::new, Identifier::toString);
     * }</pre>
     */
    default <O> PacketCodec<B, O> xmap(final Function<? super V, ? extends O> to, final Function<? super O, ? extends V> from) {
        return new PacketCodec<B, O>() {
            @Override
            public O decode(B buf) {
                return (O)to.apply(PacketCodec.this.decode(buf));
            }

            @Override
            public void encode(B buf, O value) {
                PacketCodec.this.encode(buf, (V)from.apply(value));
            }
        };
    }

    default <O extends ByteBuf> PacketCodec<O, V> mapBuf(final Function<O, ? extends B> function) {
        return new PacketCodec<O, V>() {
            public V decode(O byteBuf) {
                B object = (B)function.apply(byteBuf);
                return PacketCodec.this.decode(object);
            }

            public void encode(O byteBuf, V object) {
                B object2 = (B)function.apply(byteBuf);
                PacketCodec.this.encode(object2, object);
            }
        };
    }

    /**
     * {@return a codec that dispatches one of the sub-codecs based on the type}
     * 
     * <p>For example, subtypes of {@link net.minecraft.stat.Stat} requires different values
     * to be serialized, yet it makes sense to use the same codec for all stats.
     * This method should be called on the codec for the "type" - like {@link
     * net.minecraft.stat.StatType}. An example:
     * 
     * <pre>{@code
     * public static final PacketCodec<RegistryByteBuf, Thing<?>> PACKET_CODEC = PacketCodecs.registryValue(RegistryKeys.THING_TYPE).dispatch(Thing::getType, ThingType::getPacketCodec);
     * }</pre>
     * 
     * @param type a function that, given a value, returns its "type"
     * @param codec a function that, given a "type", returns the codec for encoding/decoding the value
     */
    default <U> PacketCodec<B, U> dispatch(
        final Function<? super U, ? extends V> type, final Function<? super V, ? extends PacketCodec<? super B, ? extends U>> codec
    ) {
        return new PacketCodec<B, U>() {
            @Override
            public U decode(B buf) {
                V object = PacketCodec.this.decode(buf);
                PacketCodec<? super B, ? extends U> packetCodec = (PacketCodec<? super B, ? extends U>)codec.apply(object);
                return (U)packetCodec.decode(buf);
            }

            @Override
            public void encode(B buf, U value) {
                V object = (V)type.apply(value);
                PacketCodec<B, U> packetCodec = (PacketCodec<B, U>)codec.apply(object);
                PacketCodec.this.encode(buf, object);
                packetCodec.encode(buf, value);
            }
        };
    }

    /**
     * {@return a codec for encoding one value}
     */
    static <B, C, T1> PacketCodec<B, C> tuple(final PacketCodec<? super B, T1> codec, final Function<C, T1> from, final Function<T1, C> to) {
        return new PacketCodec<B, C>() {
            @Override
            public C decode(B buf) {
                T1 object = codec.decode(buf);
                return to.apply(object);
            }

            @Override
            public void encode(B buf, C value) {
                codec.encode(buf, from.apply(value));
            }
        };
    }

    /**
     * {@return a codec for encoding two values}
     */
    static <B, C, T1, T2> PacketCodec<B, C> tuple(
        final PacketCodec<? super B, T1> codec1,
        final Function<C, T1> from1,
        final PacketCodec<? super B, T2> codec2,
        final Function<C, T2> from2,
        final BiFunction<T1, T2, C> to
    ) {
        return new PacketCodec<B, C>() {
            @Override
            public C decode(B buf) {
                T1 object = codec1.decode(buf);
                T2 object2 = codec2.decode(buf);
                return to.apply(object, object2);
            }

            @Override
            public void encode(B buf, C value) {
                codec1.encode(buf, from1.apply(value));
                codec2.encode(buf, from2.apply(value));
            }
        };
    }

    /**
     * {@return a codec for encoding three values}
     */
    static <B, C, T1, T2, T3> PacketCodec<B, C> tuple(
        final PacketCodec<? super B, T1> codec1,
        final Function<C, T1> from1,
        final PacketCodec<? super B, T2> codec2,
        final Function<C, T2> from2,
        final PacketCodec<? super B, T3> codec3,
        final Function<C, T3> from3,
        final Function3<T1, T2, T3, C> to
    ) {
        return new PacketCodec<B, C>() {
            @Override
            public C decode(B buf) {
                T1 object = codec1.decode(buf);
                T2 object2 = codec2.decode(buf);
                T3 object3 = codec3.decode(buf);
                return to.apply(object, object2, object3);
            }

            @Override
            public void encode(B buf, C value) {
                codec1.encode(buf, from1.apply(value));
                codec2.encode(buf, from2.apply(value));
                codec3.encode(buf, from3.apply(value));
            }
        };
    }

    /**
     * {@return a codec for encoding four values}
     */
    static <B, C, T1, T2, T3, T4> PacketCodec<B, C> tuple(
        final PacketCodec<? super B, T1> codec1,
        final Function<C, T1> from1,
        final PacketCodec<? super B, T2> codec2,
        final Function<C, T2> from2,
        final PacketCodec<? super B, T3> codec3,
        final Function<C, T3> from3,
        final PacketCodec<? super B, T4> codec4,
        final Function<C, T4> from4,
        final Function4<T1, T2, T3, T4, C> to
    ) {
        return new PacketCodec<B, C>() {
            @Override
            public C decode(B buf) {
                T1 object = codec1.decode(buf);
                T2 object2 = codec2.decode(buf);
                T3 object3 = codec3.decode(buf);
                T4 object4 = codec4.decode(buf);
                return to.apply(object, object2, object3, object4);
            }

            @Override
            public void encode(B buf, C value) {
                codec1.encode(buf, from1.apply(value));
                codec2.encode(buf, from2.apply(value));
                codec3.encode(buf, from3.apply(value));
                codec4.encode(buf, from4.apply(value));
            }
        };
    }

    /**
     * {@return a codec for encoding five values}
     */
    static <B, C, T1, T2, T3, T4, T5> PacketCodec<B, C> tuple(
        final PacketCodec<? super B, T1> codec1,
        final Function<C, T1> from1,
        final PacketCodec<? super B, T2> codec2,
        final Function<C, T2> from2,
        final PacketCodec<? super B, T3> codec3,
        final Function<C, T3> from3,
        final PacketCodec<? super B, T4> codec4,
        final Function<C, T4> from4,
        final PacketCodec<? super B, T5> codec5,
        final Function<C, T5> from5,
        final Function5<T1, T2, T3, T4, T5, C> to
    ) {
        return new PacketCodec<B, C>() {
            @Override
            public C decode(B buf) {
                T1 object = codec1.decode(buf);
                T2 object2 = codec2.decode(buf);
                T3 object3 = codec3.decode(buf);
                T4 object4 = codec4.decode(buf);
                T5 object5 = codec5.decode(buf);
                return to.apply(object, object2, object3, object4, object5);
            }

            @Override
            public void encode(B buf, C value) {
                codec1.encode(buf, from1.apply(value));
                codec2.encode(buf, from2.apply(value));
                codec3.encode(buf, from3.apply(value));
                codec4.encode(buf, from4.apply(value));
                codec5.encode(buf, from5.apply(value));
            }
        };
    }

    /**
     * {@return a codec for encoding six values}
     */
    static <B, C, T1, T2, T3, T4, T5, T6> PacketCodec<B, C> tuple(
        final PacketCodec<? super B, T1> codec1,
        final Function<C, T1> from1,
        final PacketCodec<? super B, T2> codec2,
        final Function<C, T2> from2,
        final PacketCodec<? super B, T3> codec3,
        final Function<C, T3> from3,
        final PacketCodec<? super B, T4> codec4,
        final Function<C, T4> from4,
        final PacketCodec<? super B, T5> codec5,
        final Function<C, T5> from5,
        final PacketCodec<? super B, T6> codec6,
        final Function<C, T6> from6,
        final Function6<T1, T2, T3, T4, T5, T6, C> to
    ) {
        return new PacketCodec<B, C>() {
            @Override
            public C decode(B buf) {
                T1 object = codec1.decode(buf);
                T2 object2 = codec2.decode(buf);
                T3 object3 = codec3.decode(buf);
                T4 object4 = codec4.decode(buf);
                T5 object5 = codec5.decode(buf);
                T6 object6 = codec6.decode(buf);
                return to.apply(object, object2, object3, object4, object5, object6);
            }

            @Override
            public void encode(B buf, C value) {
                codec1.encode(buf, from1.apply(value));
                codec2.encode(buf, from2.apply(value));
                codec3.encode(buf, from3.apply(value));
                codec4.encode(buf, from4.apply(value));
                codec5.encode(buf, from5.apply(value));
                codec6.encode(buf, from6.apply(value));
            }
        };
    }

    /**
     * {@return a codec for encoding seven values}
     */
    static <B, C, T1, T2, T3, T4, T5, T6, T7> PacketCodec<B, C> tuple(
        final PacketCodec<? super B, T1> codec1,
        final Function<C, T1> from1,
        final PacketCodec<? super B, T2> codec2,
        final Function<C, T2> from2,
        final PacketCodec<? super B, T3> codec3,
        final Function<C, T3> from3,
        final PacketCodec<? super B, T4> codec4,
        final Function<C, T4> from4,
        final PacketCodec<? super B, T5> codec5,
        final Function<C, T5> from5,
        final PacketCodec<? super B, T6> codec6,
        final Function<C, T6> from6,
        final PacketCodec<? super B, T7> codec7,
        final Function<C, T7> from7,
        final Function7<T1, T2, T3, T4, T5, T6, T7, C> to
    ) {
        return new PacketCodec<B, C>() {
            @Override
            public C decode(B buf) {
                T1 object = codec1.decode(buf);
                T2 object2 = codec2.decode(buf);
                T3 object3 = codec3.decode(buf);
                T4 object4 = codec4.decode(buf);
                T5 object5 = codec5.decode(buf);
                T6 object6 = codec6.decode(buf);
                T7 object7 = codec7.decode(buf);
                return to.apply(object, object2, object3, object4, object5, object6, object7);
            }

            @Override
            public void encode(B buf, C value) {
                codec1.encode(buf, from1.apply(value));
                codec2.encode(buf, from2.apply(value));
                codec3.encode(buf, from3.apply(value));
                codec4.encode(buf, from4.apply(value));
                codec5.encode(buf, from5.apply(value));
                codec6.encode(buf, from6.apply(value));
                codec7.encode(buf, from7.apply(value));
            }
        };
    }

    /**
     * {@return a codec for encoding eight values}
     */
    static <B, C, T1, T2, T3, T4, T5, T6, T7, T8> PacketCodec<B, C> tuple(
        final PacketCodec<? super B, T1> codec1,
        final Function<C, T1> from1,
        final PacketCodec<? super B, T2> codec2,
        final Function<C, T2> from2,
        final PacketCodec<? super B, T3> codec3,
        final Function<C, T3> from3,
        final PacketCodec<? super B, T4> codec4,
        final Function<C, T4> from4,
        final PacketCodec<? super B, T5> codec5,
        final Function<C, T5> from5,
        final PacketCodec<? super B, T6> codec6,
        final Function<C, T6> from6,
        final PacketCodec<? super B, T7> codec7,
        final Function<C, T7> from7,
        final PacketCodec<? super B, T8> codec8,
        final Function<C, T8> from8,
        final Function8<T1, T2, T3, T4, T5, T6, T7, T8, C> to
    ) {
        return new PacketCodec<B, C>() {
            @Override
            public C decode(B buf) {
                T1 object = codec1.decode(buf);
                T2 object2 = codec2.decode(buf);
                T3 object3 = codec3.decode(buf);
                T4 object4 = codec4.decode(buf);
                T5 object5 = codec5.decode(buf);
                T6 object6 = codec6.decode(buf);
                T7 object7 = codec7.decode(buf);
                T8 object8 = codec8.decode(buf);
                return to.apply(object, object2, object3, object4, object5, object6, object7, object8);
            }

            @Override
            public void encode(B buf, C value) {
                codec1.encode(buf, from1.apply(value));
                codec2.encode(buf, from2.apply(value));
                codec3.encode(buf, from3.apply(value));
                codec4.encode(buf, from4.apply(value));
                codec5.encode(buf, from5.apply(value));
                codec6.encode(buf, from6.apply(value));
                codec7.encode(buf, from7.apply(value));
                codec8.encode(buf, from8.apply(value));
            }
        };
    }

    static <B, T> PacketCodec<B, T> recursive(final UnaryOperator<PacketCodec<B, T>> codecGetter) {
        return new PacketCodec<B, T>() {
            private final Supplier<PacketCodec<B, T>> codecSupplier = Suppliers.memoize(() -> codecGetter.apply(this));

            @Override
            public T decode(B buf) {
                return this.codecSupplier.get().decode(buf);
            }

            @Override
            public void encode(B buf, T value) {
                this.codecSupplier.get().encode(buf, value);
            }
        };
    }

    /**
     * {@return the same codec, casted to work with buffers of type {@code S}}
     * 
     * @apiNote For example, {@link net.minecraft.util.math.BlockPos#PACKET_CODEC}
     * is defined as {@code PacketCodec<ByteBuf, BlockPos>}. To use this codec
     * where {@link net.minecraft.network.PacketByteBuf} is expected, you can call
     * this method for easy casting, like: {@code PACKET_CODEC.cast()}.
     * Doing this is generally safe and will not result in exceptions.
     */
    default <S extends B> PacketCodec<S, V> cast() {
        return this;
    }

    @FunctionalInterface
    interface ResultFunction<B, S, T> {
        PacketCodec<B, T> apply(PacketCodec<B, S> codec);
    }
}

