package net.minecraft.client.render.item.property.numeric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

@Environment(EnvType.CLIENT)
public record UseDurationProperty(boolean remaining) implements NumericProperty {
    public static final MapCodec<UseDurationProperty> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(Codec.BOOL.optionalFieldOf("remaining", false).forGetter(UseDurationProperty::remaining))
            .apply(instance, UseDurationProperty::new)
    );

    @Override
    public float getValue(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity holder, int seed) {
        if (holder != null && holder.getActiveItem() == stack) {
            return this.remaining ? holder.getItemUseTimeLeft() : getTicksUsedSoFar(stack, holder);
        } else {
            return 0.0F;
        }
    }

    @Override
    public MapCodec<UseDurationProperty> getCodec() {
        return CODEC;
    }

    public static int getTicksUsedSoFar(ItemStack stack, LivingEntity user) {
        return stack.getMaxUseTime(user) - user.getItemUseTimeLeft();
    }
}

