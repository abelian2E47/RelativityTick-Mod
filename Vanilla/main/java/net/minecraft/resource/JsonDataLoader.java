package net.minecraft.resource;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.slf4j.Logger;

/**
 * An abstract implementation of resource reloader that reads JSON files
 * into values in the prepare stage.
 */
public abstract class JsonDataLoader<T> extends SinglePreparationResourceReloader<Map<Identifier, T>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final DynamicOps<JsonElement> ops;
    private final Codec<T> codec;
    private final ResourceFinder finder;

    protected JsonDataLoader(RegistryWrapper.WrapperLookup registries, Codec<T> codec, RegistryKey<? extends Registry<T>> registryRef) {
        this(registries.getOps(JsonOps.INSTANCE), codec, ResourceFinder.json(registryRef));
    }

    protected JsonDataLoader(Codec<T> codec, ResourceFinder finder) {
        this(JsonOps.INSTANCE, codec, finder);
    }

    private JsonDataLoader(DynamicOps<JsonElement> ops, Codec<T> codec, ResourceFinder finder) {
        this.ops = ops;
        this.codec = codec;
        this.finder = finder;
    }

    protected Map<Identifier, T> prepare(ResourceManager resourceManager, Profiler profiler) {
        Map<Identifier, T> map = new HashMap<>();
        load(resourceManager, this.finder, this.ops, this.codec, map);
        return map;
    }

    public static <T> void load(
        ResourceManager manager, RegistryKey<? extends Registry<T>> registryRef, DynamicOps<JsonElement> ops, Codec<T> codec, Map<Identifier, T> results
    ) {
        load(manager, ResourceFinder.json(registryRef), ops, codec, results);
    }

    public static <T> void load(ResourceManager manager, ResourceFinder finder, DynamicOps<JsonElement> ops, Codec<T> codec, Map<Identifier, T> results) {
        for (Entry<Identifier, Resource> entry : finder.findResources(manager).entrySet()) {
            Identifier identifier = entry.getKey();
            Identifier identifier2 = finder.toResourceId(identifier);

            try (Reader reader = entry.getValue().getReader()) {
                codec.parse(ops, JsonParser.parseReader(reader)).ifSuccess(value -> {
                    if (results.putIfAbsent(identifier2, (T)value) != null) {
                        throw new IllegalStateException("Duplicate data file ignored with ID " + identifier2);
                    }
                }).ifError(error -> LOGGER.error("Couldn't parse data file '{}' from '{}': {}", identifier2, identifier, error));
            } catch (JsonParseException | IllegalArgumentException | IOException exception) {
                LOGGER.error("Couldn't parse data file '{}' from '{}'", identifier2, identifier, exception);
            }
        }
    }
}

