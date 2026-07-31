package net.minecraft.client.render.item.property.select;

import com.mojang.serialization.Codec;
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
public record CustomModelDataStringProperty(int index) implements SelectProperty<String> {
    public static final SelectProperty.Type<CustomModelDataStringProperty, String> TYPE = SelectProperty.Type.create(
        RecordCodecBuilder.mapCodec(
            instance -> instance.group(Codecs.NON_NEGATIVE_INT.optionalFieldOf("index", 0).forGetter(CustomModelDataStringProperty::index))
                .apply(instance, CustomModelDataStringProperty::new)
        ),
        Codec.STRING
    );

    @Nullable
    public String getValue(
        ItemStack itemStack, @Nullable ClientWorld clientWorld, @Nullable LivingEntity livingEntity, int i, ModelTransformationMode modelTransformationMode
    ) {
        CustomModelDataComponent customModelDataComponent = itemStack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        return customModelDataComponent != null ? customModelDataComponent.getString(this.index) : null;
    }

    @Override
    public SelectProperty.Type<CustomModelDataStringProperty, String> getType() {
        return TYPE;
    }
}

