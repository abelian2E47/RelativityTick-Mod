package net.minecraft.resource;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;
import net.minecraft.util.JsonHelper;
import org.slf4j.Logger;

public abstract class AbstractFileResourcePack implements ResourcePack {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ResourcePackInfo info;

    protected AbstractFileResourcePack(ResourcePackInfo info) {
        this.info = info;
    }

    @Nullable
    @Override
    public <T> T parseMetadata(ResourceMetadataSerializer<T> metadataSerializer) throws IOException {
        InputSupplier<InputStream> inputSupplier = this.openRoot("pack.mcmeta");
        if (inputSupplier == null) {
            return null;
        }

        try (InputStream inputStream = inputSupplier.get()) {
            return parseMetadata(metadataSerializer, inputStream);
        }
    }

    @Nullable
    public static <T> T parseMetadata(ResourceMetadataSerializer<T> metadataSerializer, InputStream inputStream) {
        JsonObject jsonObject;
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            jsonObject = JsonHelper.deserialize(bufferedReader);
        } catch (Exception exception) {
            LOGGER.error("Couldn't load {} metadata", metadataSerializer.name(), exception);
            return null;
        }

        return !jsonObject.has(metadataSerializer.name())
            ? null
            : metadataSerializer.codec()
                .parse(JsonOps.INSTANCE, jsonObject.get(metadataSerializer.name()))
                .ifError(error -> LOGGER.error("Couldn't load {} metadata: {}", metadataSerializer.name(), error))
                .result()
                .orElse(null);
    }

    @Override
    public ResourcePackInfo getInfo() {
        return this.info;
    }
}

