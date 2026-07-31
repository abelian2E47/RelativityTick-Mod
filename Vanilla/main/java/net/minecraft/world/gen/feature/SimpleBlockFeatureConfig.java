package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

public record SimpleBlockFeatureConfig(BlockStateProvider toPlace, boolean scheduleTick) implements FeatureConfig {
    public static final Codec<SimpleBlockFeatureConfig> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
                BlockStateProvider.TYPE_CODEC.fieldOf("to_place").forGetter(config -> config.toPlace),
                Codec.BOOL.optionalFieldOf("schedule_tick", false).forGetter(simpleBlockFeatureConfig -> simpleBlockFeatureConfig.scheduleTick)
            )
            .apply(instance, SimpleBlockFeatureConfig::new)
    );

    public SimpleBlockFeatureConfig(BlockStateProvider toPlace) {
        this(toPlace, false);
    }
}

