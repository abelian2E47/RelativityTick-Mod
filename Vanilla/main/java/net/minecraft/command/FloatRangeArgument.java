package net.minecraft.command;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.predicate.NumberRange;
import net.minecraft.text.Text;
import net.minecraft.util.JsonHelper;

public record FloatRangeArgument(@Nullable Float min, @Nullable Float max) {
    public static final FloatRangeArgument ANY = new FloatRangeArgument(null, null);
    public static final SimpleCommandExceptionType ONLY_INTS_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.range.ints"));

    public static FloatRangeArgument exactly(float value) {
        return new FloatRangeArgument(value, value);
    }

    public static FloatRangeArgument between(float min, float max) {
        return new FloatRangeArgument(min, max);
    }

    public static FloatRangeArgument atLeast(float value) {
        return new FloatRangeArgument(value, null);
    }

    public static FloatRangeArgument atMost(float value) {
        return new FloatRangeArgument(null, value);
    }

    public boolean isInRange(float value) {
        if (this.min != null && this.max != null && this.min > this.max && this.min > value && this.max < value) {
            return false;
        } else {
            return this.min != null && this.min > value ? false : this.max == null || !(this.max < value);
        }
    }

    public boolean isInSquaredRange(double value) {
        if (this.min != null && this.max != null && this.min > this.max && this.min * this.min > value && this.max * this.max < value) {
            return false;
        } else {
            return this.min != null && this.min * this.min > value ? false : this.max == null || !(this.max * this.max < value);
        }
    }

    public JsonElement toJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        }

        if (this.min != null && this.max != null && this.min.equals(this.max)) {
            return new JsonPrimitive(this.min);
        }

        JsonObject jsonObject = new JsonObject();
        if (this.min != null) {
            jsonObject.addProperty("min", this.min);
        }

        if (this.max != null) {
            jsonObject.addProperty("max", this.min);
        }

        return jsonObject;
    }

    public static FloatRangeArgument fromJson(@Nullable JsonElement json) {
        if (json == null || json.isJsonNull()) {
            return ANY;
        } else if (JsonHelper.isNumber(json)) {
            float f = JsonHelper.asFloat(json, "value");
            return new FloatRangeArgument(f, f);
        } else {
            JsonObject jsonObject = JsonHelper.asObject(json, "value");
            Float float_ = jsonObject.has("min") ? JsonHelper.getFloat(jsonObject, "min") : null;
            Float float_2 = jsonObject.has("max") ? JsonHelper.getFloat(jsonObject, "max") : null;
            return new FloatRangeArgument(float_, float_2);
        }
    }

    public static FloatRangeArgument parse(StringReader reader, boolean allowFloats) throws CommandSyntaxException {
        return parse(reader, allowFloats, value -> value);
    }

    public static FloatRangeArgument parse(StringReader reader, boolean allowFloats, Function<Float, Float> transform) throws CommandSyntaxException {
        if (!reader.canRead()) {
            throw NumberRange.EXCEPTION_EMPTY.createWithContext(reader);
        }

        int i = reader.getCursor();
        Float float_ = mapFloat(parseFloat(reader, allowFloats), transform);
        Float float_2;
        if (reader.canRead(2) && reader.peek() == '.' && reader.peek(1) == '.') {
            reader.skip();
            reader.skip();
            float_2 = mapFloat(parseFloat(reader, allowFloats), transform);
            if (float_ == null && float_2 == null) {
                reader.setCursor(i);
                throw NumberRange.EXCEPTION_EMPTY.createWithContext(reader);
            }
        } else {
            if (!allowFloats && reader.canRead() && reader.peek() == '.') {
                reader.setCursor(i);
                throw ONLY_INTS_EXCEPTION.createWithContext(reader);
            }

            float_2 = float_;
        }

        if (float_ == null && float_2 == null) {
            reader.setCursor(i);
            throw NumberRange.EXCEPTION_EMPTY.createWithContext(reader);
        } else {
            return new FloatRangeArgument(float_, float_2);
        }
    }

    @Nullable
    private static Float parseFloat(StringReader reader, boolean allowFloats) throws CommandSyntaxException {
        int i = reader.getCursor();

        while (reader.canRead() && peekDigit(reader, allowFloats)) {
            reader.skip();
        }

        String string = reader.getString().substring(i, reader.getCursor());
        if (string.isEmpty()) {
            return null;
        }

        try {
            return Float.parseFloat(string);
        } catch (NumberFormatException numberFormatException) {
            if (allowFloats) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidDouble().createWithContext(reader, string);
            } else {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidInt().createWithContext(reader, string);
            }
        }
    }

    private static boolean peekDigit(StringReader reader, boolean allowFloats) {
        char c = reader.peek();
        if ((c < '0' || c > '9') && c != '-') {
            return allowFloats && c == '.' ? !reader.canRead(2) || reader.peek(1) != '.' : false;
        } else {
            return true;
        }
    }

    @Nullable
    private static Float mapFloat(@Nullable Float value, Function<Float, Float> function) {
        return value == null ? null : function.apply(value);
    }
}

