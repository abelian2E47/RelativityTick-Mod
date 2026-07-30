package net.minecraft.client.render.entity.equipment;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.registry.RegistryKey;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

@Environment(EnvType.CLIENT)
public class EquipmentModelLoader extends JsonDataLoader<EquipmentModel> {
    public static final EquipmentModel EMPTY = new EquipmentModel(Map.of());
    private static final ResourceFinder FINDER = ResourceFinder.json("equipment");
    private Map<RegistryKey<EquipmentAsset>, EquipmentModel> models = Map.of();

    public EquipmentModelLoader() {
        super(EquipmentModel.CODEC, FINDER);
    }

    protected void apply(Map<Identifier, EquipmentModel> map, ResourceManager resourceManager, Profiler profiler) {
        this.models = map.entrySet()
            .stream()
            .collect(Collectors.toUnmodifiableMap(entry -> RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, entry.getKey()), Entry::getValue));
    }

    public EquipmentModel get(RegistryKey<EquipmentAsset> assetKey) {
        return this.models.getOrDefault(assetKey, EMPTY);
    }
}

