package net.minecraft.client.render.item.property.select;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;

@Environment(EnvType.CLIENT)
public record DisplayContextProperty() implements SelectProperty<ModelTransformationMode> {
    public static final SelectProperty.Type<DisplayContextProperty, ModelTransformationMode> TYPE = SelectProperty.Type.create(
        MapCodec.unit(new DisplayContextProperty()), ModelTransformationMode.CODEC
    );

    public ModelTransformationMode getValue(
        ItemStack itemStack, @Nullable ClientWorld clientWorld, @Nullable LivingEntity livingEntity, int i, ModelTransformationMode modelTransformationMode
    ) {
        return modelTransformationMode;
    }

    @Override
    public SelectProperty.Type<DisplayContextProperty, ModelTransformationMode> getType() {
        return TYPE;
    }
}

