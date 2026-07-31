package net.minecraft.client.render.item.property.numeric;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.dynamic.Codecs;

@Environment(EnvType.CLIENT)
public record UseCycleProperty(float period) implements NumericProperty {
    public static final MapCodec<UseCycleProperty> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(Codecs.POSITIVE_FLOAT.optionalFieldOf("period", 1.0F).forGetter(UseCycleProperty::period))
            .apply(instance, UseCycleProperty::new)
    );

    @Override
    public float getValue(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity holder, int seed) {
        return holder != null && holder.getActiveItem() == stack ? holder.getItemUseTimeLeft() % this.period : 0.0F;
    }

    @Override
    public MapCodec<UseCycleProperty> getCodec() {
        return CODEC;
    }
}

