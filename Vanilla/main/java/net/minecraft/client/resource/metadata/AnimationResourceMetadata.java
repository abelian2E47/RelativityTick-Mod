package net.minecraft.client.resource.metadata;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.SpriteDimensions;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;
import net.minecraft.util.dynamic.Codecs;

@Environment(EnvType.CLIENT)
public record AnimationResourceMetadata(
    Optional<List<AnimationFrameResourceMetadata>> frames, Optional<Integer> width, Optional<Integer> height, int defaultFrameTime, boolean interpolate
) {
    public static final Codec<AnimationResourceMetadata> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
                AnimationFrameResourceMetadata.CODEC.listOf().optionalFieldOf("frames").forGetter(AnimationResourceMetadata::frames),
                Codecs.POSITIVE_INT.optionalFieldOf("width").forGetter(AnimationResourceMetadata::width),
                Codecs.POSITIVE_INT.optionalFieldOf("height").forGetter(AnimationResourceMetadata::height),
                Codecs.POSITIVE_INT.optionalFieldOf("frametime", 1).forGetter(AnimationResourceMetadata::defaultFrameTime),
                Codec.BOOL.optionalFieldOf("interpolate", false).forGetter(AnimationResourceMetadata::interpolate)
            )
            .apply(instance, AnimationResourceMetadata::new)
    );
    public static final ResourceMetadataSerializer<AnimationResourceMetadata> SERIALIZER = new ResourceMetadataSerializer<>("animation", CODEC);

    public SpriteDimensions getSize(int defaultWidth, int defaultHeight) {
        if (this.width.isPresent()) {
            return this.height.isPresent() ? new SpriteDimensions(this.width.get(), this.height.get()) : new SpriteDimensions(this.width.get(), defaultHeight);
        }

        if (this.height.isPresent()) {
            return new SpriteDimensions(defaultWidth, this.height.get());
        }

        int i = Math.min(defaultWidth, defaultHeight);
        return new SpriteDimensions(i, i);
    }
}

