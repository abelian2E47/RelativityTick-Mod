package net.minecraft.client.render.item.property.numeric;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BundleItem;
import net.minecraft.item.ItemStack;

@Environment(EnvType.CLIENT)
public record BundleFullnessProperty() implements NumericProperty {
    public static final MapCodec<BundleFullnessProperty> CODEC = MapCodec.unit(new BundleFullnessProperty());

    @Override
    public float getValue(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity holder, int seed) {
        return BundleItem.getAmountFilled(stack);
    }

    @Override
    public MapCodec<BundleFullnessProperty> getCodec() {
        return CODEC;
    }
}

