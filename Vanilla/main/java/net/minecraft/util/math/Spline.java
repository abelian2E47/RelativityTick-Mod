package net.minecraft.util.math;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.function.ToFloatFunction;
import org.apache.commons.lang3.mutable.MutableObject;

public interface Spline<C, I extends ToFloatFunction<C>> extends ToFloatFunction<C> {
    @Debug
    String getDebugString();

    Spline<C, I> apply(Spline.Visitor<I> visitor);

    static <C, I extends ToFloatFunction<C>> Codec<Spline<C, I>> createCodec(Codec<I> locationFunctionCodec) {
        MutableObject<Codec<Spline<C, I>>> mutableObject = new MutableObject<>();

        record Serialized<C, I extends ToFloatFunction<C>>(float location, Spline<C, I> value, float derivative) {
        }

        Codec<Serialized<C, I>> codec = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.FLOAT.fieldOf("location").forGetter(Serialized::location),
                    Codec.lazyInitialized(mutableObject::getValue).fieldOf("value").forGetter(Serialized::value),
                    Codec.FLOAT.fieldOf("derivative").forGetter(Serialized::derivative)
                )
                .apply(instance, (location, value, derivative) -> new Serialized<>(location, value, derivative))
        );
        Codec<Spline.Implementation<C, I>> codec2 = RecordCodecBuilder.create(
            instance -> instance.group(
                    locationFunctionCodec.fieldOf("coordinate").forGetter(Spline.Implementation::locationFunction),
                    Codecs.nonEmptyList(codec.listOf())
                        .fieldOf("points")
                        .forGetter(
                            spline -> IntStream.range(0, spline.locations.length)
                                .mapToObj(index -> new Serialized<>(spline.locations()[index], spline.values().get(index), spline.derivatives()[index]))
                                .toList()
                        )
                )
                .apply(instance, (locationFunction, splines) -> {
                    float[] fs = new float[splines.size()];
                    ImmutableList.Builder<Spline<C, I>> builder = ImmutableList.builder();
                    float[] gs = new float[splines.size()];

                    for (int i = 0; i < splines.size(); i++) {
                        Serialized<C, I> serialized = splines.get(i);
                        fs[i] = serialized.location();
                        builder.add(serialized.value());
                        gs[i] = serialized.derivative();
                    }

                    return Spline.Implementation.build(locationFunction, fs, builder.build(), gs);
                })
        );
        mutableObject.setValue(
            Codec.either(Codec.FLOAT, codec2)
                .xmap(
                    either -> either.map(Spline.FixedFloatFunction::new, spline -> spline),
                    spline -> spline instanceof Spline.FixedFloatFunction<C, I> fixedFloatFunction
                        ? Either.left(fixedFloatFunction.value())
                        : Either.right((Spline.Implementation<C, I>)spline)
                )
        );
        return mutableObject.getValue();
    }

    static <C, I extends ToFloatFunction<C>> Spline<C, I> fixedFloatFunction(float value) {
        return new Spline.FixedFloatFunction<>(value);
    }

    static <C, I extends ToFloatFunction<C>> Spline.Builder<C, I> builder(I locationFunction) {
        return new Spline.Builder<>(locationFunction);
    }

    static <C, I extends ToFloatFunction<C>> Spline.Builder<C, I> builder(I locationFunction, ToFloatFunction<Float> amplifier) {
        return new Spline.Builder<>(locationFunction, amplifier);
    }

    final class Builder<C, I extends ToFloatFunction<C>> {
        private final I locationFunction;
        private final ToFloatFunction<Float> amplifier;
        private final FloatList locations = new FloatArrayList();
        private final List<Spline<C, I>> values = Lists.newArrayList();
        private final FloatList derivatives = new FloatArrayList();

        protected Builder(I locationFunction) {
            this(locationFunction, ToFloatFunction.IDENTITY);
        }

        protected Builder(I locationFunction, ToFloatFunction<Float> amplifier) {
            this.locationFunction = locationFunction;
            this.amplifier = amplifier;
        }

        public Spline.Builder<C, I> add(float location, float value) {
            return this.addPoint(location, new Spline.FixedFloatFunction<>(this.amplifier.apply(value)), 0.0F);
        }

        public Spline.Builder<C, I> add(float location, float value, float derivative) {
            return this.addPoint(location, new Spline.FixedFloatFunction<>(this.amplifier.apply(value)), derivative);
        }

        public Spline.Builder<C, I> add(float location, Spline<C, I> value) {
            return this.addPoint(location, value, 0.0F);
        }

        private Spline.Builder<C, I> addPoint(float location, Spline<C, I> value, float derivative) {
            if (!this.locations.isEmpty() && location <= this.locations.getFloat(this.locations.size() - 1)) {
                throw new IllegalArgumentException("Please register points in ascending order");
            }

            this.locations.add(location);
            this.values.add(value);
            this.derivatives.add(derivative);
            return this;
        }

        public Spline<C, I> build() {
            if (this.locations.isEmpty()) {
                throw new IllegalStateException("No elements added");
            } else {
                return Spline.Implementation.build(
                    this.locationFunction, this.locations.toFloatArray(), ImmutableList.copyOf(this.values), this.derivatives.toFloatArray()
                );
            }
        }
    }

    @Debug
    record FixedFloatFunction<C, I extends ToFloatFunction<C>>(float value) implements Spline<C, I> {
        @Override
        public float apply(C x) {
            return this.value;
        }

        @Override
        public String getDebugString() {
            return String.format(Locale.ROOT, "k=%.3f", this.value);
        }

        @Override
        public float min() {
            return this.value;
        }

        @Override
        public float max() {
            return this.value;
        }

        @Override
        public Spline<C, I> apply(Spline.Visitor<I> visitor) {
            return this;
        }
    }

    @Debug
    record Implementation<C, I extends ToFloatFunction<C>>(
        I locationFunction, float[] locations, List<Spline<C, I>> values, float[] derivatives, float min, float max
    ) implements Spline<C, I> {
        public Implementation {
            assertParametersValid(locations, values, derivatives);
        }

        static <C, I extends ToFloatFunction<C>> Spline.Implementation<C, I> build(
            I locationFunction, float[] locations, List<Spline<C, I>> values, float[] derivatives
        ) {
            assertParametersValid(locations, values, derivatives);
            int i = locations.length - 1;
            float f = Float.POSITIVE_INFINITY;
            float g = Float.NEGATIVE_INFINITY;
            float h = locationFunction.min();
            float j = locationFunction.max();
            if (h < locations[0]) {
                float k = sampleOutsideRange(h, locations, values.get(0).min(), derivatives, 0);
                float l = sampleOutsideRange(h, locations, values.get(0).max(), derivatives, 0);
                f = Math.min(f, Math.min(k, l));
                g = Math.max(g, Math.max(k, l));
            }

            if (j > locations[i]) {
                float m = sampleOutsideRange(j, locations, values.get(i).min(), derivatives, i);
                float n = sampleOutsideRange(j, locations, values.get(i).max(), derivatives, i);
                f = Math.min(f, Math.min(m, n));
                g = Math.max(g, Math.max(m, n));
            }

            for (Spline<C, I> spline : values) {
                f = Math.min(f, spline.min());
                g = Math.max(g, spline.max());
            }

            for (int o = 0; o < i; o++) {
                float p = locations[o];
                float q = locations[o + 1];
                float r = q - p;
                Spline<C, I> spline2 = values.get(o);
                Spline<C, I> spline3 = values.get(o + 1);
                float s = spline2.min();
                float t = spline2.max();
                float u = spline3.min();
                float v = spline3.max();
                float w = derivatives[o];
                float x = derivatives[o + 1];
                if (w != 0.0F || x != 0.0F) {
                    float y = w * r;
                    float z = x * r;
                    float ab = Math.min(s, u);
                    float bb = Math.max(t, v);
                    float cb = y - v + s;
                    float db = y - u + t;
                    float eb = -z + u - t;
                    float fb = -z + v - s;
                    float gb = Math.min(cb, eb);
                    float hb = Math.max(db, fb);
                    f = Math.min(f, ab + 0.25F * gb);
                    g = Math.max(g, bb + 0.25F * hb);
                }
            }

            return new Spline.Implementation<>(locationFunction, locations, values, derivatives, f, g);
        }

        private static float sampleOutsideRange(float point, float[] locations, float value, float[] derivatives, int i) {
            float f = derivatives[i];
            return f == 0.0F ? value : value + f * (point - locations[i]);
        }

        private static <C, I extends ToFloatFunction<C>> void assertParametersValid(float[] locations, List<Spline<C, I>> values, float[] derivatives) {
            if (locations.length != values.size() || locations.length != derivatives.length) {
                throw new IllegalArgumentException("All lengths must be equal, got: " + locations.length + " " + values.size() + " " + derivatives.length);
            }

            if (locations.length == 0) {
                throw new IllegalArgumentException("Cannot create a multipoint spline with no points");
            }
        }

        @Override
        public float apply(C x) {
            float f = this.locationFunction.apply(x);
            int i = findRangeForLocation(this.locations, f);
            int j = this.locations.length - 1;
            if (i < 0) {
                return sampleOutsideRange(f, this.locations, this.values.get(0).apply(x), this.derivatives, 0);
            }

            if (i == j) {
                return sampleOutsideRange(f, this.locations, this.values.get(j).apply(x), this.derivatives, j);
            }

            float g = this.locations[i];
            float h = this.locations[i + 1];
            float k = (f - g) / (h - g);
            ToFloatFunction<C> toFloatFunction = (ToFloatFunction<C>)this.values.get(i);
            ToFloatFunction<C> toFloatFunction2 = (ToFloatFunction<C>)this.values.get(i + 1);
            float l = this.derivatives[i];
            float m = this.derivatives[i + 1];
            float n = toFloatFunction.apply(x);
            float o = toFloatFunction2.apply(x);
            float p = l * (h - g) - (o - n);
            float q = -m * (h - g) + (o - n);
            return MathHelper.lerp(k, n, o) + k * (1.0F - k) * MathHelper.lerp(k, p, q);
        }

        private static int findRangeForLocation(float[] locations, float x) {
            return MathHelper.binarySearch(0, locations.length, i -> x < locations[i]) - 1;
        }

        @VisibleForTesting
        @Override
        public String getDebugString() {
            return "Spline{coordinate="
                + this.locationFunction
                + ", locations="
                + this.format(this.locations)
                + ", derivatives="
                + this.format(this.derivatives)
                + ", values="
                + this.values.stream().map(Spline::getDebugString).collect(Collectors.joining(", ", "[", "]"))
                + "}";
        }

        private String format(float[] values) {
            return "["
                + IntStream.range(0, values.length)
                    .mapToDouble(index -> values[index])
                    .mapToObj(value -> String.format(Locale.ROOT, "%.3f", value))
                    .collect(Collectors.joining(", "))
                + "]";
        }

        @Override
        public Spline<C, I> apply(Spline.Visitor<I> visitor) {
            return build(
                visitor.visit(this.locationFunction), this.locations, this.values().stream().map(value -> value.apply(visitor)).toList(), this.derivatives
            );
        }
    }

    interface Visitor<I> {
        I visit(I value);
    }
}

