package net.minecraft.item.equipment;

import java.util.Map;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public interface EquipmentAssetKeys {
    RegistryKey<? extends Registry<EquipmentAsset>> REGISTRY_KEY = RegistryKey.ofRegistry(Identifier.ofVanilla("equipment_asset"));
    RegistryKey<EquipmentAsset> LEATHER = register("leather");
    RegistryKey<EquipmentAsset> CHAINMAIL = register("chainmail");
    RegistryKey<EquipmentAsset> IRON = register("iron");
    RegistryKey<EquipmentAsset> GOLD = register("gold");
    RegistryKey<EquipmentAsset> DIAMOND = register("diamond");
    RegistryKey<EquipmentAsset> TURTLE_SCUTE = register("turtle_scute");
    RegistryKey<EquipmentAsset> NETHERITE = register("netherite");
    RegistryKey<EquipmentAsset> ARMADILLO_SCUTE = register("armadillo_scute");
    RegistryKey<EquipmentAsset> ELYTRA = register("elytra");
    Map<DyeColor, RegistryKey<EquipmentAsset>> CARPET_FROM_COLOR = Util.mapEnum(DyeColor.class, color -> register(color.asString() + "_carpet"));
    RegistryKey<EquipmentAsset> TRADER_LLAMA = register("trader_llama");

    static RegistryKey<EquipmentAsset> register(String name) {
        return RegistryKey.of(REGISTRY_KEY, Identifier.ofVanilla(name));
    }
}

