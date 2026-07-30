package net.minecraft.client.render.model;

import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.json.GeneratedItemModel;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class ReferencedModelsCollector {
    static final Logger LOGGER = LogUtils.getLogger();
    private final Map<Identifier, UnbakedModel> inputs;
    final UnbakedModel missingModel;
    private final List<ResolvableModel> topLevelModels = new ArrayList<>();
    private final Map<Identifier, UnbakedModel> resolvedModels = new HashMap<>();

    public ReferencedModelsCollector(Map<Identifier, UnbakedModel> inputs, UnbakedModel missingModel) {
        this.inputs = inputs;
        this.missingModel = missingModel;
        this.resolvedModels.put(MissingModel.ID, missingModel);
    }

    public void addGenerated() {
        this.resolvedModels.put(GeneratedItemModel.GENERATED, new GeneratedItemModel());
    }

    public void add(ResolvableModel model) {
        this.topLevelModels.add(model);
    }

    public void resolveAll() {
        this.topLevelModels.forEach(model -> model.resolve(new ReferencedModelsCollector.ResolverImpl()));
    }

    public Map<Identifier, UnbakedModel> getResolvedModels() {
        return this.resolvedModels;
    }

    public Set<Identifier> getUnresolved() {
        return Sets.difference(this.inputs.keySet(), this.resolvedModels.keySet());
    }

    UnbakedModel computeResolvedModel(Identifier id) {
        return this.resolvedModels.computeIfAbsent(id, this::getModel);
    }

    private UnbakedModel getModel(Identifier id) {
        UnbakedModel unbakedModel = this.inputs.get(id);
        if (unbakedModel == null) {
            LOGGER.warn("Missing block model: '{}'", id);
            return this.missingModel;
        } else {
            return unbakedModel;
        }
    }

    @Environment(EnvType.CLIENT)
    class ResolverImpl implements ResolvableModel.Resolver {
        private final List<Identifier> stack = new ArrayList<>();
        private final Set<Identifier> visited = new HashSet<>();

        @Override
        public UnbakedModel resolve(Identifier id) {
            if (this.stack.contains(id)) {
                ReferencedModelsCollector.LOGGER.warn("Detected model loading loop: {}->{}", this.getPath(), id);
                return ReferencedModelsCollector.this.missingModel;
            }

            UnbakedModel unbakedModel = ReferencedModelsCollector.this.computeResolvedModel(id);
            if (this.visited.add(id)) {
                this.stack.add(id);
                unbakedModel.resolve(this);
                this.stack.remove(id);
            }

            return unbakedModel;
        }

        private String getPath() {
            return this.stack.stream().map(Identifier::toString).collect(Collectors.joining("->"));
        }
    }
}

