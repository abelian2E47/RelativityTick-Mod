package net.minecraft.item.equipment.trim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import net.minecraft.item.Item;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryElementCodec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.dynamic.Codecs;

public record ArmorTrimMaterial(String assetName, RegistryEntry<Item> ingredient, Map<RegistryKey<EquipmentAsset>, String> overrideArmorAssets, Text description) {
    public static final Codec<ArmorTrimMaterial> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
                Codecs.IDENTIFIER_PATH.fieldOf("asset_name").forGetter(ArmorTrimMaterial::assetName),
                Item.ENTRY_CODEC.fieldOf("ingredient").forGetter(ArmorTrimMaterial::ingredient),
                Codec.unboundedMap(RegistryKey.createCodec(EquipmentAssetKeys.REGISTRY_KEY), Codec.STRING)
                    .optionalFieldOf("override_armor_assets", Map.of())
                    .forGetter(ArmorTrimMaterial::overrideArmorAssets),
                TextCodecs.CODEC.fieldOf("description").forGetter(ArmorTrimMaterial::description)
            )
            .apply(instance, ArmorTrimMaterial::new)
    );
    public static final PacketCodec<RegistryByteBuf, ArmorTrimMaterial> PACKET_CODEC = PacketCodec.tuple(
        PacketCodecs.STRING,
        ArmorTrimMaterial::assetName,
        PacketCodecs.registryEntry(RegistryKeys.ITEM),
        ArmorTrimMaterial::ingredient,
        PacketCodecs.map(Object2ObjectOpenHashMap::new, RegistryKey.createPacketCodec(EquipmentAssetKeys.REGISTRY_KEY), PacketCodecs.STRING),
        ArmorTrimMaterial::overrideArmorAssets,
        TextCodecs.REGISTRY_PACKET_CODEC,
        ArmorTrimMaterial::description,
        ArmorTrimMaterial::new
    );
    public static final Codec<RegistryEntry<ArmorTrimMaterial>> ENTRY_CODEC = RegistryElementCodec.of(RegistryKeys.TRIM_MATERIAL, CODEC);
    public static final PacketCodec<RegistryByteBuf, RegistryEntry<ArmorTrimMaterial>> ENTRY_PACKET_CODEC = PacketCodecs.registryEntry(
        RegistryKeys.TRIM_MATERIAL, PACKET_CODEC
    );

    public static ArmorTrimMaterial of(String assetName, Item ingredient, Text description, Map<RegistryKey<EquipmentAsset>, String> overrideArmorAssets) {
        return new ArmorTrimMaterial(assetName, Registries.ITEM.getEntry(ingredient), overrideArmorAssets, description);
    }
}

