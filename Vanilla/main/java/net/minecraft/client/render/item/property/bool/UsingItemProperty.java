package net.minecraft.client.render.item.property.bool;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;

@Environment(EnvType.CLIENT)
public record UsingItemProperty() implements BooleanProperty {
    public static final MapCodec<UsingItemProperty> CODEC = MapCodec.unit(new UsingItemProperty());

    @Override
    public boolean getValue(
        ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity user, int seed, ModelTransformationMode modelTransformationMode
    ) {
        return user == null ? false : user.isUsingItem() && user.getActiveItem() == stack;
    }

    @Override
    public MapCodec<UsingItemProperty> getCodec() {
        return CODEC;
    }
}

