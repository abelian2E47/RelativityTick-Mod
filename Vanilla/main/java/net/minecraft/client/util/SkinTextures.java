package net.minecraft.client.util;

import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public record SkinTextures(
    Identifier texture,
    @Nullable String textureUrl,
    @Nullable Identifier capeTexture,
    @Nullable Identifier elytraTexture,
    SkinTextures.Model model,
    boolean secure
) {
    @Environment(EnvType.CLIENT)
    public enum Model {
        SLIM("slim"),
        WIDE("default");

        private final String name;

        Model(final String name) {
            this.name = name;
        }

        public static SkinTextures.Model fromName(@Nullable String name) {
            if (name == null) {
                return WIDE;
            }

            return switch (name) {
                case "slim" -> SLIM;
                default -> WIDE;
            };
        }

        public String getName() {
            return this.name;
        }
    }
}

