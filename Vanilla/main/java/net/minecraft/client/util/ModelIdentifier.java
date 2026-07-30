package net.minecraft.client.util;

import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public record ModelIdentifier(Identifier id, String variant) {
    public ModelIdentifier {
        variant = toLowerCase(variant);
    }

    private static String toLowerCase(String string) {
        return string.toLowerCase(Locale.ROOT);
    }

    public String getVariant() {
        return this.variant;
    }

    @Override
    public String toString() {
        return this.id + "#" + this.variant;
    }
}

