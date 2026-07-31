package net.minecraft.item.equipment.trim;

import java.util.Map;
import java.util.Optional;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public class ArmorTrimMaterials {
    public static final RegistryKey<ArmorTrimMaterial> QUARTZ = of("quartz");
    public static final RegistryKey<ArmorTrimMaterial> IRON = of("iron");
    public static final RegistryKey<ArmorTrimMaterial> NETHERITE = of("netherite");
    public static final RegistryKey<ArmorTrimMaterial> REDSTONE = of("redstone");
    public static final RegistryKey<ArmorTrimMaterial> COPPER = of("copper");
    public static final RegistryKey<ArmorTrimMaterial> GOLD = of("gold");
    public static final RegistryKey<ArmorTrimMaterial> EMERALD = of("emerald");
    public static final RegistryKey<ArmorTrimMaterial> DIAMOND = of("diamond");
    public static final RegistryKey<ArmorTrimMaterial> LAPIS = of("lapis");
    public static final RegistryKey<ArmorTrimMaterial> AMETHYST = of("amethyst");
    public static final RegistryKey<ArmorTrimMaterial> RESIN = of("resin");

    public static void bootstrap(Registerable<ArmorTrimMaterial> registry) {
        register(registry, QUARTZ, Items.QUARTZ, Style.EMPTY.withColor(14931140));
        register(registry, IRON, Items.IRON_INGOT, Style.EMPTY.withColor(15527148), Map.of(EquipmentAssetKeys.IRON, "iron_darker"));
        register(registry, NETHERITE, Items.NETHERITE_INGOT, Style.EMPTY.withColor(6445145), Map.of(EquipmentAssetKeys.NETHERITE, "netherite_darker"));
        register(registry, REDSTONE, Items.REDSTONE, Style.EMPTY.withColor(9901575));
        register(registry, COPPER, Items.COPPER_INGOT, Style.EMPTY.withColor(11823181));
        register(registry, GOLD, Items.GOLD_INGOT, Style.EMPTY.withColor(14594349), Map.of(EquipmentAssetKeys.GOLD, "gold_darker"));
        register(registry, EMERALD, Items.EMERALD, Style.EMPTY.withColor(1155126));
        register(registry, DIAMOND, Items.DIAMOND, Style.EMPTY.withColor(7269586), Map.of(EquipmentAssetKeys.DIAMOND, "diamond_darker"));
        register(registry, LAPIS, Items.LAPIS_LAZULI, Style.EMPTY.withColor(4288151));
        register(registry, AMETHYST, Items.AMETHYST_SHARD, Style.EMPTY.withColor(10116294));
        register(registry, RESIN, Items.RESIN_BRICK, Style.EMPTY.withColor(16545810));
    }

    public static Optional<RegistryEntry.Reference<ArmorTrimMaterial>> get(RegistryWrapper.WrapperLookup registries, ItemStack stack) {
        return registries.getOrThrow(RegistryKeys.TRIM_MATERIAL).streamEntries().filter(recipe -> stack.itemMatches(recipe.value().ingredient())).findFirst();
    }

    private static void register(Registerable<ArmorTrimMaterial> registry, RegistryKey<ArmorTrimMaterial> key, Item ingredient, Style style) {
        register(registry, key, ingredient, style, Map.of());
    }

    private static void register(
        Registerable<ArmorTrimMaterial> registry,
        RegistryKey<ArmorTrimMaterial> key,
        Item ingredient,
        Style style,
        Map<RegistryKey<EquipmentAsset>, String> overrideArmorAssets
    ) {
        ArmorTrimMaterial armorTrimMaterial = ArmorTrimMaterial.of(
            key.getValue().getPath(),
            ingredient,
            Text.translatable(Util.createTranslationKey("trim_material", key.getValue())).fillStyle(style),
            overrideArmorAssets
        );
        registry.register(key, armorTrimMaterial);
    }

    private static RegistryKey<ArmorTrimMaterial> of(String id) {
        return RegistryKey.of(RegistryKeys.TRIM_MATERIAL, Identifier.ofVanilla(id));
    }
}

