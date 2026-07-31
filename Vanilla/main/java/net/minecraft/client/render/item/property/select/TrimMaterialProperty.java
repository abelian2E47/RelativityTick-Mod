package net.minecraft.client.render.item.property.select;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.item.equipment.trim.ArmorTrim;
import net.minecraft.item.equipment.trim.ArmorTrimMaterial;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

@Environment(EnvType.CLIENT)
public record TrimMaterialProperty() implements SelectProperty<RegistryKey<ArmorTrimMaterial>> {
    public static final SelectProperty.Type<TrimMaterialProperty, RegistryKey<ArmorTrimMaterial>> TYPE = SelectProperty.Type.create(
        MapCodec.unit(new TrimMaterialProperty()), RegistryKey.createCodec(RegistryKeys.TRIM_MATERIAL)
    );

    @Nullable
    public RegistryKey<ArmorTrimMaterial> getValue(
        ItemStack itemStack, @Nullable ClientWorld clientWorld, @Nullable LivingEntity livingEntity, int i, ModelTransformationMode modelTransformationMode
    ) {
        ArmorTrim armorTrim = itemStack.get(DataComponentTypes.TRIM);
        return armorTrim == null ? null : armorTrim.material().getKey().orElse(null);
    }

    @Override
    public SelectProperty.Type<TrimMaterialProperty, RegistryKey<ArmorTrimMaterial>> getType() {
        return TYPE;
    }
}

