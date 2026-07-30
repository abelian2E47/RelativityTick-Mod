package net.minecraft.client.render.item.property.select;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

@Environment(EnvType.CLIENT)
public record ContextEntityTypeProperty() implements SelectProperty<RegistryKey<EntityType<?>>> {
    public static final SelectProperty.Type<ContextEntityTypeProperty, RegistryKey<EntityType<?>>> TYPE = SelectProperty.Type.create(
        MapCodec.unit(new ContextEntityTypeProperty()), RegistryKey.createCodec(RegistryKeys.ENTITY_TYPE)
    );

    @Nullable
    public RegistryKey<EntityType<?>> getValue(
        ItemStack itemStack, @Nullable ClientWorld clientWorld, @Nullable LivingEntity livingEntity, int i, ModelTransformationMode modelTransformationMode
    ) {
        return livingEntity == null ? null : livingEntity.getType().getRegistryEntry().registryKey();
    }

    @Override
    public SelectProperty.Type<ContextEntityTypeProperty, RegistryKey<EntityType<?>>> getType() {
        return TYPE;
    }
}

