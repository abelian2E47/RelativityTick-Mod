package net.minecraft.client.render.model;

import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelNameSupplier;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.item.ModelTransformationMode;

@Environment(EnvType.CLIENT)
public interface UnbakedModel extends ResolvableModel {
    boolean DEFAULT_AMBIENT_OCCLUSION = true;
    UnbakedModel.GuiLight DEFAULT_GUI_LIGHT = UnbakedModel.GuiLight.BLOCK;

    BakedModel bake(
        ModelTextures textures, Baker baker, ModelBakeSettings settings, boolean ambientOcclusion, boolean isSideLit, ModelTransformation transformation
    );

    @Nullable
    default Boolean getAmbientOcclusion() {
        return null;
    }

    @Nullable
    default UnbakedModel.GuiLight getGuiLight() {
        return null;
    }

    @Nullable
    default ModelTransformation getTransformation() {
        return null;
    }

    default ModelTextures.Textures getTextures() {
        return ModelTextures.Textures.EMPTY;
    }

    @Nullable
    default UnbakedModel getParent() {
        return null;
    }

    static BakedModel bake(UnbakedModel model, Baker baker, ModelBakeSettings settings) {
        ModelTextures modelTextures = buildTextures(model, baker.getModelNameSupplier());
        boolean bl = getAmbientOcclusion(model);
        boolean bl2 = getGuiLight(model).isSide();
        ModelTransformation modelTransformation = getTransformations(model);
        return model.bake(modelTextures, baker, settings, bl, bl2, modelTransformation);
    }

    static ModelTextures buildTextures(UnbakedModel model, ModelNameSupplier modelNameSupplier) {
        ModelTextures.Builder builder = new ModelTextures.Builder();

        while (model != null) {
            builder.addLast(model.getTextures());
            model = model.getParent();
        }

        return builder.build(modelNameSupplier);
    }

    static boolean getAmbientOcclusion(UnbakedModel model) {
        while (model != null) {
            Boolean boolean_ = model.getAmbientOcclusion();
            if (boolean_ != null) {
                return boolean_;
            }

            model = model.getParent();
        }

        return true;
    }

    static UnbakedModel.GuiLight getGuiLight(UnbakedModel model) {
        while (model != null) {
            UnbakedModel.GuiLight guiLight = model.getGuiLight();
            if (guiLight != null) {
                return guiLight;
            }

            model = model.getParent();
        }

        return DEFAULT_GUI_LIGHT;
    }

    static Transformation getTransformation(UnbakedModel model, ModelTransformationMode displayContext) {
        while (model != null) {
            ModelTransformation modelTransformation = model.getTransformation();
            if (modelTransformation != null) {
                Transformation transformation = modelTransformation.getTransformation(displayContext);
                if (transformation != Transformation.IDENTITY) {
                    return transformation;
                }
            }

            model = model.getParent();
        }

        return Transformation.IDENTITY;
    }

    static ModelTransformation getTransformations(UnbakedModel model) {
        Transformation transformation = getTransformation(model, ModelTransformationMode.THIRD_PERSON_LEFT_HAND);
        Transformation transformation2 = getTransformation(model, ModelTransformationMode.THIRD_PERSON_RIGHT_HAND);
        Transformation transformation3 = getTransformation(model, ModelTransformationMode.FIRST_PERSON_LEFT_HAND);
        Transformation transformation4 = getTransformation(model, ModelTransformationMode.FIRST_PERSON_RIGHT_HAND);
        Transformation transformation5 = getTransformation(model, ModelTransformationMode.HEAD);
        Transformation transformation6 = getTransformation(model, ModelTransformationMode.GUI);
        Transformation transformation7 = getTransformation(model, ModelTransformationMode.GROUND);
        Transformation transformation8 = getTransformation(model, ModelTransformationMode.FIXED);
        return new ModelTransformation(
            transformation, transformation2, transformation3, transformation4, transformation5, transformation6, transformation7, transformation8
        );
    }

    @Environment(EnvType.CLIENT)
    enum GuiLight {
        /**
         * The model will be shaded from the front, like a basic item
         */
        ITEM("front"),
        /**
         * The model will be shaded from the side, like a block.
         */
        BLOCK("side");

        private final String name;

        GuiLight(final String name) {
            this.name = name;
        }

        public static UnbakedModel.GuiLight byName(String value) {
            for (UnbakedModel.GuiLight guiLight : values()) {
                if (guiLight.name.equals(value)) {
                    return guiLight;
                }
            }

            throw new IllegalArgumentException("Invalid gui light: " + value);
        }

        public boolean isSide() {
            return this == BLOCK;
        }
    }
}

