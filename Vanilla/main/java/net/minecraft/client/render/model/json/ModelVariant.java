package net.minecraft.client.render.model.json;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.AffineTransformation;

@Environment(EnvType.CLIENT)
public record ModelVariant(Identifier location, AffineTransformation rotation, boolean uvLock, int weight) implements ModelBakeSettings {
    @Override
    public AffineTransformation getRotation() {
        return this.rotation;
    }

    @Override
    public boolean isUvLocked() {
        return this.uvLock;
    }

    @Environment(EnvType.CLIENT)
    public static class Deserializer implements JsonDeserializer<ModelVariant> {
        @VisibleForTesting
        static final boolean DEFAULT_UV_LOCK = false;
        @VisibleForTesting
        static final int DEFAULT_WEIGHT = 1;
        @VisibleForTesting
        static final int DEFAULT_X_ROTATION = 0;
        @VisibleForTesting
        static final int DEFAULT_Y_ROTATION = 0;

        public ModelVariant deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            Identifier identifier = this.deserializeModel(jsonObject);
            net.minecraft.client.render.model.ModelRotation modelRotation = this.deserializeRotation(jsonObject);
            boolean bl = this.deserializeUvLock(jsonObject);
            int i = this.deserializeWeight(jsonObject);
            return new ModelVariant(identifier, modelRotation.getRotation(), bl, i);
        }

        private boolean deserializeUvLock(JsonObject object) {
            return JsonHelper.getBoolean(object, "uvlock", false);
        }

        protected net.minecraft.client.render.model.ModelRotation deserializeRotation(JsonObject object) {
            int i = JsonHelper.getInt(object, "x", 0);
            int j = JsonHelper.getInt(object, "y", 0);
            net.minecraft.client.render.model.ModelRotation modelRotation = net.minecraft.client.render.model.ModelRotation.get(i, j);
            if (modelRotation == null) {
                throw new JsonParseException("Invalid BlockModelRotation x: " + i + ", y: " + j);
            } else {
                return modelRotation;
            }
        }

        protected Identifier deserializeModel(JsonObject object) {
            return Identifier.of(JsonHelper.getString(object, "model"));
        }

        protected int deserializeWeight(JsonObject object) {
            int i = JsonHelper.getInt(object, "weight", 1);
            if (i < 1) {
                throw new JsonParseException("Invalid weight " + i + " found, expected integer >= 1");
            } else {
                return i;
            }
        }
    }
}

