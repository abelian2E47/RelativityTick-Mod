package net.minecraft.client.resource.metadata;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.dynamic.Codecs;

@Environment(EnvType.CLIENT)
public record AnimationFrameResourceMetadata(int index, Optional<Integer> time) {
    public static final Codec<AnimationFrameResourceMetadata> BASE_CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
                Codecs.NON_NEGATIVE_INT.fieldOf("index").forGetter(AnimationFrameResourceMetadata::index),
                Codecs.POSITIVE_INT.optionalFieldOf("time").forGetter(AnimationFrameResourceMetadata::time)
            )
            .apply(instance, AnimationFrameResourceMetadata::new)
    );
    public static final Codec<AnimationFrameResourceMetadata> CODEC = Codec.either(Codecs.NON_NEGATIVE_INT, BASE_CODEC)
        .xmap(
            either -> either.map(AnimationFrameResourceMetadata::new, metadata -> (AnimationFrameResourceMetadata)metadata),
            metadatax -> metadatax.time.isPresent() ? Either.right(metadatax) : Either.left(metadatax.index)
        );

    public AnimationFrameResourceMetadata(int index) {
        this(index, Optional.empty());
    }

    public int getTime(int defaultTime) {
        return this.time.orElse(defaultTime);
    }
}

