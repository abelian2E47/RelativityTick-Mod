package net.minecraft.client.render.model.json;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ModelTransformationMode;

@Environment(EnvType.CLIENT)
public record ModelTransformation(
    Transformation thirdPersonLeftHand,
    Transformation thirdPersonRightHand,
    Transformation firstPersonLeftHand,
    Transformation firstPersonRightHand,
    Transformation head,
    Transformation gui,
    Transformation ground,
    Transformation fixed
) {
    public static final ModelTransformation NONE = new ModelTransformation(
        Transformation.IDENTITY,
        Transformation.IDENTITY,
        Transformation.IDENTITY,
        Transformation.IDENTITY,
        Transformation.IDENTITY,
        Transformation.IDENTITY,
        Transformation.IDENTITY,
        Transformation.IDENTITY
    );

    public Transformation getTransformation(ModelTransformationMode renderMode) {
        return switch (renderMode) {
            case THIRD_PERSON_LEFT_HAND -> this.thirdPersonLeftHand;
            case THIRD_PERSON_RIGHT_HAND -> this.thirdPersonRightHand;
            case FIRST_PERSON_LEFT_HAND -> this.firstPersonLeftHand;
            case FIRST_PERSON_RIGHT_HAND -> this.firstPersonRightHand;
            case HEAD -> this.head;
            case GUI -> this.gui;
            case GROUND -> this.ground;
            case FIXED -> this.fixed;
            default -> Transformation.IDENTITY;
        };
    }

    @Environment(EnvType.CLIENT)
    protected static class Deserializer implements JsonDeserializer<ModelTransformation> {
        public ModelTransformation deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            Transformation transformation = this.parseModelTransformation(
                jsonDeserializationContext, jsonObject, ModelTransformationMode.THIRD_PERSON_RIGHT_HAND
            );
            Transformation transformation2 = this.parseModelTransformation(
                jsonDeserializationContext, jsonObject, ModelTransformationMode.THIRD_PERSON_LEFT_HAND
            );
            if (transformation2 == Transformation.IDENTITY) {
                transformation2 = transformation;
            }

            Transformation transformation3 = this.parseModelTransformation(
                jsonDeserializationContext, jsonObject, ModelTransformationMode.FIRST_PERSON_RIGHT_HAND
            );
            Transformation transformation4 = this.parseModelTransformation(
                jsonDeserializationContext, jsonObject, ModelTransformationMode.FIRST_PERSON_LEFT_HAND
            );
            if (transformation4 == Transformation.IDENTITY) {
                transformation4 = transformation3;
            }

            Transformation transformation5 = this.parseModelTransformation(jsonDeserializationContext, jsonObject, ModelTransformationMode.HEAD);
            Transformation transformation6 = this.parseModelTransformation(jsonDeserializationContext, jsonObject, ModelTransformationMode.GUI);
            Transformation transformation7 = this.parseModelTransformation(jsonDeserializationContext, jsonObject, ModelTransformationMode.GROUND);
            Transformation transformation8 = this.parseModelTransformation(jsonDeserializationContext, jsonObject, ModelTransformationMode.FIXED);
            return new ModelTransformation(
                transformation2, transformation, transformation4, transformation3, transformation5, transformation6, transformation7, transformation8
            );
        }

        private Transformation parseModelTransformation(JsonDeserializationContext ctx, JsonObject json, ModelTransformationMode displayContext) {
            String string = displayContext.asString();
            return json.has(string) ? ctx.deserialize(json.get(string), Transformation.class) : Transformation.IDENTITY;
        }
    }
}

