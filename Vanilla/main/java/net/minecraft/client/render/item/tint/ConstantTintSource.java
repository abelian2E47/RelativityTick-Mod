package net.minecraft.client.render.item.tint;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.ColorHelper;

@Environment(EnvType.CLIENT)
public record ConstantTintSource(int value) implements TintSource {
    public static final MapCodec<ConstantTintSource> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(Codecs.RGB.fieldOf("value").forGetter(ConstantTintSource::value)).apply(instance, ConstantTintSource::new)
    );

    public ConstantTintSource {
        value = ColorHelper.fullAlpha(value);
    }

    @Override
    public int getTint(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity user) {
        return this.value;
    }

    @Override
    public MapCodec<ConstantTintSource> getCodec() {
        return CODEC;
    }
}

