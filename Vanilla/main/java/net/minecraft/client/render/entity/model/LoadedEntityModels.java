package net.minecraft.client.render.entity.model;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;

@Environment(EnvType.CLIENT)
public class LoadedEntityModels {
    public static final LoadedEntityModels EMPTY = new LoadedEntityModels(Map.of());
    private final Map<EntityModelLayer, TexturedModelData> modelParts;

    public LoadedEntityModels(Map<EntityModelLayer, TexturedModelData> modelParts) {
        this.modelParts = modelParts;
    }

    public ModelPart getModelPart(EntityModelLayer layer) {
        TexturedModelData texturedModelData = this.modelParts.get(layer);
        if (texturedModelData == null) {
            throw new IllegalArgumentException("No model for layer " + layer);
        } else {
            return texturedModelData.createModel();
        }
    }

    public static LoadedEntityModels copy() {
        return new LoadedEntityModels(ImmutableMap.copyOf(EntityModels.getModels()));
    }
}

