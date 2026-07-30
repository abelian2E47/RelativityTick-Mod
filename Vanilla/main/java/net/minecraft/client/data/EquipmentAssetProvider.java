package net.minecraft.client.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class EquipmentAssetProvider implements DataProvider {
    private final DataOutput.PathResolver pathResolver;

    public EquipmentAssetProvider(DataOutput output) {
        this.pathResolver = output.getResolver(DataOutput.OutputType.RESOURCE_PACK, "equipment");
    }

    private static void bootstrap(BiConsumer<RegistryKey<EquipmentAsset>, EquipmentModel> equipmentBiConsumer) {
        equipmentBiConsumer.accept(
            EquipmentAssetKeys.LEATHER,
            EquipmentModel.builder()
                .addHumanoidLayers(Identifier.ofVanilla("leather"), true)
                .addHumanoidLayers(Identifier.ofVanilla("leather_overlay"), false)
                .addLayers(EquipmentModel.LayerType.HORSE_BODY, EquipmentModel.Layer.createWithLeatherColor(Identifier.ofVanilla("leather"), true))
                .build()
        );
        equipmentBiConsumer.accept(EquipmentAssetKeys.CHAINMAIL, createHumanoidOnlyModel("chainmail"));
        equipmentBiConsumer.accept(EquipmentAssetKeys.IRON, createHumanoidAndHorseModel("iron"));
        equipmentBiConsumer.accept(EquipmentAssetKeys.GOLD, createHumanoidAndHorseModel("gold"));
        equipmentBiConsumer.accept(EquipmentAssetKeys.DIAMOND, createHumanoidAndHorseModel("diamond"));
        equipmentBiConsumer.accept(
            EquipmentAssetKeys.TURTLE_SCUTE, EquipmentModel.builder().addMainHumanoidLayer(Identifier.ofVanilla("turtle_scute"), false).build()
        );
        equipmentBiConsumer.accept(EquipmentAssetKeys.NETHERITE, createHumanoidOnlyModel("netherite"));
        equipmentBiConsumer.accept(
            EquipmentAssetKeys.ARMADILLO_SCUTE,
            EquipmentModel.builder()
                .addLayers(EquipmentModel.LayerType.WOLF_BODY, EquipmentModel.Layer.create(Identifier.ofVanilla("armadillo_scute"), false))
                .addLayers(EquipmentModel.LayerType.WOLF_BODY, EquipmentModel.Layer.create(Identifier.ofVanilla("armadillo_scute_overlay"), true))
                .build()
        );
        equipmentBiConsumer.accept(
            EquipmentAssetKeys.ELYTRA,
            EquipmentModel.builder()
                .addLayers(EquipmentModel.LayerType.WINGS, new EquipmentModel.Layer(Identifier.ofVanilla("elytra"), Optional.empty(), true))
                .build()
        );

        for (Entry<DyeColor, RegistryKey<EquipmentAsset>> entry : EquipmentAssetKeys.CARPET_FROM_COLOR.entrySet()) {
            DyeColor dyeColor = entry.getKey();
            RegistryKey<EquipmentAsset> registryKey = entry.getValue();
            equipmentBiConsumer.accept(
                registryKey,
                EquipmentModel.builder()
                    .addLayers(EquipmentModel.LayerType.LLAMA_BODY, new EquipmentModel.Layer(Identifier.ofVanilla(dyeColor.asString())))
                    .build()
            );
        }

        equipmentBiConsumer.accept(
            EquipmentAssetKeys.TRADER_LLAMA,
            EquipmentModel.builder().addLayers(EquipmentModel.LayerType.LLAMA_BODY, new EquipmentModel.Layer(Identifier.ofVanilla("trader_llama"))).build()
        );
    }

    private static EquipmentModel createHumanoidOnlyModel(String id) {
        return EquipmentModel.builder().addHumanoidLayers(Identifier.ofVanilla(id)).build();
    }

    private static EquipmentModel createHumanoidAndHorseModel(String id) {
        return EquipmentModel.builder()
            .addHumanoidLayers(Identifier.ofVanilla(id))
            .addLayers(EquipmentModel.LayerType.HORSE_BODY, EquipmentModel.Layer.createWithLeatherColor(Identifier.ofVanilla(id), false))
            .build();
    }

    @Override
    public CompletableFuture<?> run(DataWriter writer) {
        Map<RegistryKey<EquipmentAsset>, EquipmentModel> map = new HashMap<>();
        bootstrap((key, model) -> {
            if (map.putIfAbsent(key, model) != null) {
                throw new IllegalStateException("Tried to register equipment asset twice for id: " + key);
            }
        });
        return DataProvider.writeAllToPath(writer, EquipmentModel.CODEC, this.pathResolver::resolveJson, map);
    }

    @Override
    public String getName() {
        return "Equipment Asset Definitions";
    }
}

