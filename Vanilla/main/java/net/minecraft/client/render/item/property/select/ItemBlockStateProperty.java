package net.minecraft.client.render.item.property.select;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;

@Environment(EnvType.CLIENT)
public record ItemBlockStateProperty(String property) implements SelectProperty<String> {
    public static final SelectProperty.Type<ItemBlockStateProperty, String> TYPE = SelectProperty.Type.create(
        RecordCodecBuilder.mapCodec(
            instance -> instance.group(Codec.STRING.fieldOf("block_state_property").forGetter(ItemBlockStateProperty::property))
                .apply(instance, ItemBlockStateProperty::new)
        ),
        Codec.STRING
    );

    @Nullable
    public String getValue(
        ItemStack itemStack, @Nullable ClientWorld clientWorld, @Nullable LivingEntity livingEntity, int i, ModelTransformationMode modelTransformationMode
    ) {
        BlockStateComponent blockStateComponent = itemStack.get(DataComponentTypes.BLOCK_STATE);
        return blockStateComponent == null ? null : blockStateComponent.properties().get(this.property);
    }

    @Override
    public SelectProperty.Type<ItemBlockStateProperty, String> getType() {
        return TYPE;
    }
}

