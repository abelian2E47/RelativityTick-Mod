package net.minecraft.client.render.model;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelNameSupplier;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class ModelTextures {
    public static final ModelTextures EMPTY = new ModelTextures(Map.of());
    private static final char TEXTURE_REFERENCE_PREFIX = '#';
    private final Map<String, SpriteIdentifier> textures;

    ModelTextures(Map<String, SpriteIdentifier> textures) {
        this.textures = textures;
    }

    @Nullable
    public SpriteIdentifier get(String textureId) {
        if (isTextureReference(textureId)) {
            textureId = textureId.substring(1);
        }

        return this.textures.get(textureId);
    }

    private static boolean isTextureReference(String textureId) {
        return textureId.charAt(0) == '#';
    }

    public static ModelTextures.Textures fromJson(JsonObject json, Identifier atlasTexture) {
        ModelTextures.Textures.Builder builder = new ModelTextures.Textures.Builder();

        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            add(atlasTexture, entry.getKey(), entry.getValue().getAsString(), builder);
        }

        return builder.build();
    }

    private static void add(Identifier atlasTexture, String textureId, String value, ModelTextures.Textures.Builder builder) {
        if (isTextureReference(value)) {
            builder.addTextureReference(textureId, value.substring(1));
        } else {
            Identifier identifier = Identifier.tryParse(value);
            if (identifier == null) {
                throw new JsonParseException(value + " is not valid resource location");
            }

            builder.addSprite(textureId, new SpriteIdentifier(atlasTexture, identifier));
        }
    }

    @Environment(EnvType.CLIENT)
    public static class Builder {
        private static final Logger LOGGER = LogUtils.getLogger();
        private final List<ModelTextures.Textures> textures = new ArrayList<>();

        public ModelTextures.Builder addLast(ModelTextures.Textures textures) {
            this.textures.addLast(textures);
            return this;
        }

        public ModelTextures.Builder addFirst(ModelTextures.Textures textures) {
            this.textures.addFirst(textures);
            return this;
        }

        public ModelTextures build(ModelNameSupplier modelNameSupplier) {
            if (this.textures.isEmpty()) {
                return ModelTextures.EMPTY;
            }

            Object2ObjectMap<String, SpriteIdentifier> object2ObjectMap = new Object2ObjectArrayMap<>();
            Object2ObjectMap<String, ModelTextures.TextureReferenceEntry> object2ObjectMap2 = new Object2ObjectArrayMap<>();

            for (ModelTextures.Textures textures : Lists.reverse(this.textures)) {
                textures.values.forEach((textureId, entryx) -> {
                    switch (entryx) {
                        case ModelTextures.SpriteEntry spriteEntry:
                            object2ObjectMap2.remove(textureId);
                            object2ObjectMap.put(textureId, spriteEntry.material());
                            break;
                        case ModelTextures.TextureReferenceEntry textureReferenceEntry:
                            object2ObjectMap.remove(textureId);
                            object2ObjectMap2.put(textureId, textureReferenceEntry);
                            break;
                        default:
                            throw new MatchException(null, null);
                    }
                });
            }

            if (object2ObjectMap2.isEmpty()) {
                return new ModelTextures(object2ObjectMap);
            }

            boolean bl = true;

            while (bl) {
                bl = false;
                ObjectIterator<Object2ObjectMap.Entry<String, ModelTextures.TextureReferenceEntry>> objectIterator = Object2ObjectMaps.fastIterator(
                    object2ObjectMap2
                );

                while (objectIterator.hasNext()) {
                    Object2ObjectMap.Entry<String, ModelTextures.TextureReferenceEntry> entry = objectIterator.next();
                    SpriteIdentifier spriteIdentifier = object2ObjectMap.get(entry.getValue().target);
                    if (spriteIdentifier != null) {
                        object2ObjectMap.put(entry.getKey(), spriteIdentifier);
                        objectIterator.remove();
                        bl = true;
                    }
                }
            }

            if (!object2ObjectMap2.isEmpty()) {
                LOGGER.warn(
                    "Unresolved texture references in {}:\n{}",
                    modelNameSupplier.get(),
                    object2ObjectMap2.entrySet()
                        .stream()
                        .map(entryx -> "\t#" + (String)entryx.getKey() + "-> #" + ((ModelTextures.TextureReferenceEntry)entryx.getValue()).target + "\n")
                        .collect(Collectors.joining())
                );
            }

            return new ModelTextures(object2ObjectMap);
        }
    }

    @Environment(EnvType.CLIENT)
    public sealed interface Entry permits ModelTextures.SpriteEntry, ModelTextures.TextureReferenceEntry {
    }

    @Environment(EnvType.CLIENT)
    record SpriteEntry(SpriteIdentifier material) implements ModelTextures.Entry {
    }

    @Environment(EnvType.CLIENT)
    record TextureReferenceEntry(String target) implements ModelTextures.Entry {
    }

    @Environment(EnvType.CLIENT)
    public record Textures(Map<String, ModelTextures.Entry> values) {
        public static final ModelTextures.Textures EMPTY = new ModelTextures.Textures(Map.of());

        @Environment(EnvType.CLIENT)
        public static class Builder {
            private final Map<String, ModelTextures.Entry> entries = new HashMap<>();

            public ModelTextures.Textures.Builder addTextureReference(String textureId, String target) {
                this.entries.put(textureId, new ModelTextures.TextureReferenceEntry(target));
                return this;
            }

            public ModelTextures.Textures.Builder addSprite(String textureId, SpriteIdentifier spriteId) {
                this.entries.put(textureId, new ModelTextures.SpriteEntry(spriteId));
                return this;
            }

            public ModelTextures.Textures build() {
                return this.entries.isEmpty() ? ModelTextures.Textures.EMPTY : new ModelTextures.Textures(Map.copyOf(this.entries));
            }
        }
    }
}

