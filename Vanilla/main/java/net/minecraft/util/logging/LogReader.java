package net.minecraft.util.logging;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import javax.annotation.Nullable;

public interface LogReader<T> extends Closeable {
    static <T> LogReader<T> create(final Codec<T> codec, Reader reader) {
        final JsonReader jsonReader = new JsonReader(reader);
        jsonReader.setLenient(true);
        return new LogReader<T>() {
            @Nullable
            @Override
            public T read() throws IOException {
                try {
                    if (!jsonReader.hasNext()) {
                        return null;
                    }

                    JsonElement jsonElement = JsonParser.parseReader(jsonReader);
                    return codec.parse(JsonOps.INSTANCE, jsonElement).getOrThrow(IOException::new);
                } catch (JsonParseException jsonParseException) {
                    throw new IOException(jsonParseException);
                } catch (EOFException eOFException) {
                    return null;
                }
            }

            @Override
            public void close() throws IOException {
                jsonReader.close();
            }
        };
    }

    @Nullable
    T read() throws IOException;
}

