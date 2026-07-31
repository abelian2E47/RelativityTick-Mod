package net.minecraft.client.render.item.tint;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapColorComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.ColorHelper;

@Environment(EnvType.CLIENT)
public record MapColorTintSource(int defaultColor) implements TintSource {
    public static final MapCodec<MapColorTintSource> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(Codecs.RGB.fieldOf("default").forGetter(MapColorTintSource::defaultColor)).apply(instance, MapColorTintSource::new)
    );

    public MapColorTintSource() {
        this(MapColorComponent.DEFAULT.rgb());
    }

    @Override
    public int getTint(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity user) {
        MapColorComponent mapColorComponent = stack.get(DataComponentTypes.MAP_COLOR);
        return mapColorComponent != null ? ColorHelper.fullAlpha(mapColorComponent.rgb()) : ColorHelper.fullAlpha(this.defaultColor);
    }

    @Override
    public MapCodec<MapColorTintSource> getCodec() {
        return CODEC;
    }
}

