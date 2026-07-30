package net.minecraft.client.render.item.property.select;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public record ContextDimensionProperty() implements SelectProperty<RegistryKey<World>> {
    public static final SelectProperty.Type<ContextDimensionProperty, RegistryKey<World>> TYPE = SelectProperty.Type.create(
        MapCodec.unit(new ContextDimensionProperty()), RegistryKey.createCodec(RegistryKeys.WORLD)
    );

    @Nullable
    public RegistryKey<World> getValue(
        ItemStack itemStack, @Nullable ClientWorld clientWorld, @Nullable LivingEntity livingEntity, int i, ModelTransformationMode modelTransformationMode
    ) {
        return clientWorld != null ? clientWorld.getRegistryKey() : null;
    }

    @Override
    public SelectProperty.Type<ContextDimensionProperty, RegistryKey<World>> getType() {
        return TYPE;
    }
}

