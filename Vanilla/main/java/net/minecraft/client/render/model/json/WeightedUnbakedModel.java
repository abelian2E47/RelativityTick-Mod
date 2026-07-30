package net.minecraft.client.render.model.json;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.Baker;
import net.minecraft.client.render.model.GroupableModel;
import net.minecraft.client.render.model.ResolvableModel;
import net.minecraft.client.render.model.WeightedBakedModel;
import net.minecraft.util.collection.DataPool;

@Environment(EnvType.CLIENT)
public record WeightedUnbakedModel(List<ModelVariant> variants) implements GroupableModel {
    public WeightedUnbakedModel {
        if (variants.isEmpty()) {
            throw new IllegalArgumentException("Variant list must contain at least one element");
        }
    }

    @Override
    public Object getEqualityGroup(BlockState state) {
        return this;
    }

    @Override
    public void resolve(ResolvableModel.Resolver resolver) {
        this.variants.forEach(variant -> resolver.resolve(variant.location()));
    }

    @Override
    public BakedModel bake(Baker baker) {
        if (this.variants.size() == 1) {
            ModelVariant modelVariant = this.variants.getFirst();
            return baker.bake(modelVariant.location(), modelVariant);
        }

        DataPool.Builder<BakedModel> builder = DataPool.builder();

        for (ModelVariant modelVariant2 : this.variants) {
            BakedModel bakedModel = baker.bake(modelVariant2.location(), modelVariant2);
            builder.add(bakedModel, modelVariant2.weight());
        }

        return new WeightedBakedModel(builder.build());
    }

    @Environment(EnvType.CLIENT)
    public static class Deserializer implements JsonDeserializer<WeightedUnbakedModel> {
        public WeightedUnbakedModel deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            List<ModelVariant> list = Lists.newArrayList();
            if (jsonElement.isJsonArray()) {
                JsonArray jsonArray = jsonElement.getAsJsonArray();
                if (jsonArray.isEmpty()) {
                    throw new JsonParseException("Empty variant array");
                }

                for (JsonElement jsonElement2 : jsonArray) {
                    list.add(jsonDeserializationContext.deserialize(jsonElement2, ModelVariant.class));
                }
            } else {
                list.add(jsonDeserializationContext.deserialize(jsonElement, ModelVariant.class));
            }

            return new WeightedUnbakedModel(list);
        }
    }
}

