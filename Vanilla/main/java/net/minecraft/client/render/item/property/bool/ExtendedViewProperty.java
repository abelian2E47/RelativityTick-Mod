package net.minecraft.client.render.item.property.bool;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;

@Environment(EnvType.CLIENT)
public record ExtendedViewProperty() implements BooleanProperty {
    public static final MapCodec<ExtendedViewProperty> CODEC = MapCodec.unit(new ExtendedViewProperty());

    @Override
    public boolean getValue(
        ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity user, int seed, ModelTransformationMode modelTransformationMode
    ) {
        return modelTransformationMode == ModelTransformationMode.GUI && Screen.hasShiftDown();
    }

    @Override
    public MapCodec<ExtendedViewProperty> getCodec() {
        return CODEC;
    }
}

