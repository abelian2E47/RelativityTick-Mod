package net.minecraft.resource.metadata;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.resource.InputSupplier;
import net.minecraft.util.JsonHelper;

public interface ResourceMetadata {
    ResourceMetadata NONE = new ResourceMetadata() {
        @Override
        public <T> Optional<T> decode(ResourceMetadataSerializer<T> serializer) {
            return Optional.empty();
        }
    };
    InputSupplier<ResourceMetadata> NONE_SUPPLIER = () -> NONE;

    static ResourceMetadata create(InputStream stream) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            final JsonObject jsonObject = JsonHelper.deserialize(bufferedReader);
            return new ResourceMetadata() {
                @Override
                public <T> Optional<T> decode(ResourceMetadataSerializer<T> serializer) {
                    String string = serializer.name();
                    if (jsonObject.has(string)) {
                        T object = serializer.codec().parse(JsonOps.INSTANCE, jsonObject.get(string)).getOrThrow(JsonParseException::new);
                        return Optional.of(object);
                    } else {
                        return Optional.empty();
                    }
                }
            };
        }
    }

    <T> Optional<T> decode(ResourceMetadataSerializer<T> serializer);

    default ResourceMetadata copy(Collection<ResourceMetadataSerializer<?>> serializers) {
        ResourceMetadata.Builder builder = new ResourceMetadata.Builder();

        for (ResourceMetadataSerializer<?> resourceMetadataSerializer : serializers) {
            this.decodeAndAdd(builder, resourceMetadataSerializer);
        }

        return builder.build();
    }

    private <T> void decodeAndAdd(ResourceMetadata.Builder builder, ResourceMetadataSerializer<T> serializer) {
        this.decode(serializer).ifPresent(value -> builder.add(serializer, (T)value));
    }

    class Builder {
        private final ImmutableMap.Builder<ResourceMetadataSerializer<?>, Object> values = ImmutableMap.builder();

        public <T> ResourceMetadata.Builder add(ResourceMetadataSerializer<T> serializer, T value) {
            this.values.put(serializer, value);
            return this;
        }

        public ResourceMetadata build() {
            final ImmutableMap<ResourceMetadataSerializer<?>, Object> immutableMap = this.values.build();
            return immutableMap.isEmpty() ? ResourceMetadata.NONE : new ResourceMetadata() {
                @Override
                public <T> Optional<T> decode(ResourceMetadataSerializer<T> serializer) {
                    return Optional.ofNullable((T)immutableMap.get(serializer));
                }
            };
        }
    }
}

