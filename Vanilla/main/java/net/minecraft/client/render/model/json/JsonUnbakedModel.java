package net.minecraft.client.render.model.json;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.Baker;
import net.minecraft.client.render.model.BasicBakedModel;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.ModelTextures;
import net.minecraft.client.render.model.ResolvableModel;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

@Environment(EnvType.CLIENT)
public class JsonUnbakedModel implements UnbakedModel {
    @VisibleForTesting
    static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(JsonUnbakedModel.class, new JsonUnbakedModel.Deserializer())
        .registerTypeAdapter(ModelElement.class, new ModelElement.Deserializer())
        .registerTypeAdapter(ModelElementFace.class, new ModelElementFace.Deserializer())
        .registerTypeAdapter(ModelElementTexture.class, new ModelElementTexture.Deserializer())
        .registerTypeAdapter(Transformation.class, new Transformation.Deserializer())
        .registerTypeAdapter(ModelTransformation.class, new ModelTransformation.Deserializer())
        .create();
    private final List<ModelElement> elements;
    @Nullable
    private final UnbakedModel.GuiLight guiLight;
    @Nullable
    private final Boolean ambientOcclusion;
    @Nullable
    private final ModelTransformation transformations;
    @VisibleForTesting
    private final ModelTextures.Textures textures;
    @Nullable
    private UnbakedModel parent;
    @Nullable
    private final Identifier parentId;

    public static JsonUnbakedModel deserialize(Reader input) {
        return JsonHelper.deserialize(GSON, input, JsonUnbakedModel.class);
    }

    public JsonUnbakedModel(
        @Nullable Identifier parentId,
        List<ModelElement> elements,
        ModelTextures.Textures textures,
        @Nullable Boolean ambientOcclusion,
        @Nullable UnbakedModel.GuiLight guiLight,
        @Nullable ModelTransformation transformations
    ) {
        this.elements = elements;
        this.ambientOcclusion = ambientOcclusion;
        this.guiLight = guiLight;
        this.textures = textures;
        this.parentId = parentId;
        this.transformations = transformations;
    }

    @Nullable
    @Override
    public Boolean getAmbientOcclusion() {
        return this.ambientOcclusion;
    }

    @Nullable
    @Override
    public UnbakedModel.GuiLight getGuiLight() {
        return this.guiLight;
    }

    @Override
    public void resolve(ResolvableModel.Resolver resolver) {
        if (this.parentId != null) {
            this.parent = resolver.resolve(this.parentId);
        }
    }

    @Nullable
    @Override
    public UnbakedModel getParent() {
        return this.parent;
    }

    @Override
    public ModelTextures.Textures getTextures() {
        return this.textures;
    }

    @Nullable
    @Override
    public ModelTransformation getTransformation() {
        return this.transformations;
    }

    @Override
    public BakedModel bake(
        ModelTextures textures, Baker baker, ModelBakeSettings settings, boolean ambientOcclusion, boolean isSideLit, ModelTransformation transformation
    ) {
        return this.elements.isEmpty() && this.parent != null
            ? this.parent.bake(textures, baker, settings, ambientOcclusion, isSideLit, transformation)
            : BasicBakedModel.bake(this.elements, textures, baker.getSpriteGetter(), settings, ambientOcclusion, isSideLit, true, transformation);
    }

    @Nullable
    @VisibleForTesting
    List<ModelElement> getElements() {
        return this.elements;
    }

    @Nullable
    @VisibleForTesting
    Identifier getParentId() {
        return this.parentId;
    }

    @Environment(EnvType.CLIENT)
    public static class Deserializer implements JsonDeserializer<JsonUnbakedModel> {
        public JsonUnbakedModel deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            List<ModelElement> list = this.elementsFromJson(jsonDeserializationContext, jsonObject);
            String string = this.parentFromJson(jsonObject);
            ModelTextures.Textures textures = this.texturesFromJson(jsonObject);
            Boolean boolean_ = this.ambientOcclusionFromJson(jsonObject);
            ModelTransformation modelTransformation = null;
            if (jsonObject.has("display")) {
                JsonObject jsonObject2 = JsonHelper.getObject(jsonObject, "display");
                modelTransformation = jsonDeserializationContext.deserialize(jsonObject2, ModelTransformation.class);
            }

            UnbakedModel.GuiLight guiLight = null;
            if (jsonObject.has("gui_light")) {
                guiLight = UnbakedModel.GuiLight.byName(JsonHelper.getString(jsonObject, "gui_light"));
            }

            Identifier identifier = string.isEmpty() ? null : Identifier.of(string);
            return new JsonUnbakedModel(identifier, list, textures, boolean_, guiLight, modelTransformation);
        }

        private ModelTextures.Textures texturesFromJson(JsonObject object) {
            if (object.has("textures")) {
                JsonObject jsonObject = JsonHelper.getObject(object, "textures");
                return ModelTextures.fromJson(jsonObject, SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
            } else {
                return ModelTextures.Textures.EMPTY;
            }
        }

        private String parentFromJson(JsonObject json) {
            return JsonHelper.getString(json, "parent", "");
        }

        @Nullable
        protected Boolean ambientOcclusionFromJson(JsonObject json) {
            return json.has("ambientocclusion") ? JsonHelper.getBoolean(json, "ambientocclusion") : null;
        }

        protected List<ModelElement> elementsFromJson(JsonDeserializationContext context, JsonObject json) {
            if (!json.has("elements")) {
                return List.of();
            }

            List<ModelElement> list = new ArrayList<>();

            for (JsonElement jsonElement : JsonHelper.getArray(json, "elements")) {
                list.add(context.deserialize(jsonElement, ModelElement.class));
            }

            return list;
        }
    }
}

