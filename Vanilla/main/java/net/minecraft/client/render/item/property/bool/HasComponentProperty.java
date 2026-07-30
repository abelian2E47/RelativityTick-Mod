package net.minecraft.client.render.item.property.bool;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.ComponentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.registry.Registries;

@Environment(EnvType.CLIENT)
public record HasComponentProperty(ComponentType<?> componentType, boolean ignoreDefault) implements BooleanProperty {
    public static final MapCodec<HasComponentProperty> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
                Registries.DATA_COMPONENT_TYPE.getCodec().fieldOf("component").forGetter(HasComponentProperty::componentType),
                Codec.BOOL.optionalFieldOf("ignore_default", false).forGetter(HasComponentProperty::ignoreDefault)
            )
            .apply(instance, HasComponentProperty::new)
    );

    @Override
    public boolean getValue(
        ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity user, int seed, ModelTransformationMode modelTransformationMode
    ) {
        return this.ignoreDefault ? stack.hasChangedComponent(this.componentType) : stack.contains(this.componentType);
    }

    @Override
    public MapCodec<HasComponentProperty> getCodec() {
        return CODEC;
    }
}

