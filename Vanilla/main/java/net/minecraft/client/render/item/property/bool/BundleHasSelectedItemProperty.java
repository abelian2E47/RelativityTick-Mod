package net.minecraft.client.render.item.property.bool;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BundleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;

@Environment(EnvType.CLIENT)
public record BundleHasSelectedItemProperty() implements BooleanProperty {
    public static final MapCodec<BundleHasSelectedItemProperty> CODEC = MapCodec.unit(new BundleHasSelectedItemProperty());

    @Override
    public boolean getValue(
        ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity user, int seed, ModelTransformationMode modelTransformationMode
    ) {
        return BundleItem.hasSelectedStack(stack);
    }

    @Override
    public MapCodec<BundleHasSelectedItemProperty> getCodec() {
        return CODEC;
    }
}

