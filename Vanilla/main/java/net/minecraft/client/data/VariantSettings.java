package net.minecraft.client.data;

import com.google.gson.JsonPrimitive;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class VariantSettings {
    public static final VariantSetting<VariantSettings.Rotation> X = new VariantSetting<>("x", rotation -> new JsonPrimitive(rotation.degrees));
    public static final VariantSetting<VariantSettings.Rotation> Y = new VariantSetting<>("y", rotation -> new JsonPrimitive(rotation.degrees));
    public static final VariantSetting<Identifier> MODEL = new VariantSetting<>("model", id -> new JsonPrimitive(id.toString()));
    public static final VariantSetting<Boolean> UVLOCK = new VariantSetting<>("uvlock", JsonPrimitive::new);
    public static final VariantSetting<Integer> WEIGHT = new VariantSetting<>("weight", JsonPrimitive::new);

    @Environment(EnvType.CLIENT)
    public enum Rotation {
        R0(0),
        R90(90),
        R180(180),
        R270(270);

        final int degrees;

        Rotation(final int degrees) {
            this.degrees = degrees;
        }
    }
}

