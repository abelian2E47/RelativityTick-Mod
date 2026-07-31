package net.minecraft.client.render.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.json.MultipartModelComponent;
import net.minecraft.client.render.model.json.WeightedUnbakedModel;
import net.minecraft.state.StateManager;

@Environment(EnvType.CLIENT)
public class MultipartUnbakedModel implements GroupableModel {
    private final List<MultipartUnbakedModel.Selector> selectors;

    MultipartUnbakedModel(List<MultipartUnbakedModel.Selector> selectors) {
        this.selectors = selectors;
    }

    @Override
    public Object getEqualityGroup(BlockState state) {
        IntList intList = new IntArrayList();

        for (int i = 0; i < this.selectors.size(); i++) {
            if (this.selectors.get(i).predicate.test(state)) {
                intList.add(i);
            }
        }

        @Environment(EnvType.CLIENT)
        record EqualityGroup(MultipartUnbakedModel model, IntList selectors) {
        }

        return new EqualityGroup(this, intList);
    }

    @Override
    public void resolve(ResolvableModel.Resolver resolver) {
        this.selectors.forEach(selector -> selector.variant.resolve(resolver));
    }

    @Override
    public BakedModel bake(Baker baker) {
        List<MultipartBakedModel.Selector> list = new ArrayList<>(this.selectors.size());

        for (MultipartUnbakedModel.Selector selector : this.selectors) {
            BakedModel bakedModel = selector.variant.bake(baker);
            list.add(new MultipartBakedModel.Selector(selector.predicate, bakedModel));
        }

        return new MultipartBakedModel(list);
    }

    @Environment(EnvType.CLIENT)
    public static class Deserializer implements JsonDeserializer<MultipartUnbakedModel.Serialized> {
        public MultipartUnbakedModel.Serialized deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            return new MultipartUnbakedModel.Serialized(this.deserializeComponents(jsonDeserializationContext, jsonElement.getAsJsonArray()));
        }

        private List<MultipartModelComponent> deserializeComponents(JsonDeserializationContext context, JsonArray array) {
            List<MultipartModelComponent> list = new ArrayList<>();
            if (array.isEmpty()) {
                throw new JsonSyntaxException("Empty selector array");
            }

            for (JsonElement jsonElement : array) {
                list.add(context.deserialize(jsonElement, MultipartModelComponent.class));
            }

            return list;
        }
    }

    @Environment(EnvType.CLIENT)
    record Selector(Predicate<BlockState> predicate, WeightedUnbakedModel variant) {
    }

    @Environment(EnvType.CLIENT)
    public record Serialized(List<MultipartModelComponent> selectors) {
        public MultipartUnbakedModel toModel(StateManager<Block, BlockState> stateManager) {
            List<MultipartUnbakedModel.Selector> list = this.selectors
                .stream()
                .map(selector -> new MultipartUnbakedModel.Selector(selector.getPredicate(stateManager), selector.getModel()))
                .toList();
            return new MultipartUnbakedModel(list);
        }

        public Set<WeightedUnbakedModel> getBackingModels() {
            return this.selectors.stream().map(MultipartModelComponent::getModel).collect(Collectors.toSet());
        }
    }
}

