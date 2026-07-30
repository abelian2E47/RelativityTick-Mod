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
import net.minecraft.world.biome.GrassColors;

@Environment(EnvType.CLIENT)
public record GrassTintSource(float temperature, float downfall) implements TintSource {
    public static final MapCodec<GrassTintSource> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
                Codecs.rangedInclusiveFloat(0.0F, 1.0F).fieldOf("temperature").forGetter(GrassTintSource::temperature),
                Codecs.rangedInclusiveFloat(0.0F, 1.0F).fieldOf("downfall").forGetter(GrassTintSource::downfall)
            )
            .apply(instance, GrassTintSource::new)
    );

    public GrassTintSource() {
        this(0.5F, 1.0F);
    }

    @Override
    public int getTint(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity user) {
        return GrassColors.getColor(this.temperature, this.downfall);
    }

    @Override
    public MapCodec<GrassTintSource> getCodec() {
        return CODEC;
    }
}

