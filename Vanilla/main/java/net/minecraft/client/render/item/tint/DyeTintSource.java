package net.minecraft.client.render.item.tint;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.dynamic.Codecs;

@Environment(EnvType.CLIENT)
public record DyeTintSource(int defaultColor) implements TintSource {
    public static final MapCodec<DyeTintSource> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(Codecs.RGB.fieldOf("default").forGetter(DyeTintSource::defaultColor)).apply(instance, DyeTintSource::new)
    );

    @Override
    public int getTint(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity user) {
        return DyedColorComponent.getColor(stack, this.defaultColor);
    }

    @Override
    public MapCodec<DyeTintSource> getCodec() {
        return CODEC;
    }
}

