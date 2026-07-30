package net.minecraft.client.render.item.tint;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.ColorHelper;

@Environment(EnvType.CLIENT)
public record PotionTintSource(int defaultColor) implements TintSource {
    public static final MapCodec<PotionTintSource> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(Codecs.RGB.fieldOf("default").forGetter(PotionTintSource::defaultColor)).apply(instance, PotionTintSource::new)
    );

    public PotionTintSource() {
        this(-13083194);
    }

    @Override
    public int getTint(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity user) {
        PotionContentsComponent potionContentsComponent = stack.get(DataComponentTypes.POTION_CONTENTS);
        return potionContentsComponent != null
            ? ColorHelper.fullAlpha(potionContentsComponent.getColor(this.defaultColor))
            : ColorHelper.fullAlpha(this.defaultColor);
    }

    @Override
    public MapCodec<PotionTintSource> getCodec() {
        return CODEC;
    }
}

