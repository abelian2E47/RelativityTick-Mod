package net.minecraft.client.render.item.property.bool;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.dynamic.Codecs;

@Environment(EnvType.CLIENT)
public record CustomModelDataFlagProperty(int index) implements BooleanProperty {
    public static final MapCodec<CustomModelDataFlagProperty> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(Codecs.NON_NEGATIVE_INT.optionalFieldOf("index", 0).forGetter(CustomModelDataFlagProperty::index))
            .apply(instance, CustomModelDataFlagProperty::new)
    );

    @Override
    public boolean getValue(
        ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity user, int seed, ModelTransformationMode modelTransformationMode
    ) {
        CustomModelDataComponent customModelDataComponent = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        return customModelDataComponent != null ? customModelDataComponent.getFlag(this.index) == Boolean.TRUE : false;
    }

    @Override
    public MapCodec<CustomModelDataFlagProperty> getCodec() {
        return CODEC;
    }
}

