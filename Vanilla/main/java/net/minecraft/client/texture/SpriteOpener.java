package net.minecraft.client.texture;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resource.metadata.AnimationResourceMetadata;
import net.minecraft.resource.Resource;
import net.minecraft.resource.metadata.ResourceMetadata;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.slf4j.Logger;

@FunctionalInterface
@Environment(EnvType.CLIENT)
public interface SpriteOpener {
    Logger LOGGER = LogUtils.getLogger();

    static SpriteOpener create(Collection<ResourceMetadataSerializer<?>> metadatas) {
        return (id, resource) -> {
            ResourceMetadata resourceMetadata;
            try {
                resourceMetadata = resource.getMetadata().copy(metadatas);
            } catch (Exception exception) {
                LOGGER.error("Unable to parse metadata from {}", id, exception);
                return null;
            }

            NativeImage nativeImage;
            try (InputStream inputStream = resource.getInputStream()) {
                nativeImage = NativeImage.read(inputStream);
            } catch (IOException iOException) {
                LOGGER.error("Using missing texture, unable to load {}", id, iOException);
                return null;
            }

            Optional<AnimationResourceMetadata> optional = resourceMetadata.decode(AnimationResourceMetadata.SERIALIZER);
            SpriteDimensions spriteDimensions;
            if (optional.isPresent()) {
                spriteDimensions = optional.get().getSize(nativeImage.getWidth(), nativeImage.getHeight());
                if (!MathHelper.isMultipleOf(nativeImage.getWidth(), spriteDimensions.width())
                    || !MathHelper.isMultipleOf(nativeImage.getHeight(), spriteDimensions.height())) {
                    LOGGER.error(
                        "Image {} size {},{} is not multiple of frame size {},{}",
                        id,
                        nativeImage.getWidth(),
                        nativeImage.getHeight(),
                        spriteDimensions.width(),
                        spriteDimensions.height()
                    );
                    nativeImage.close();
                    return null;
                }
            } else {
                spriteDimensions = new SpriteDimensions(nativeImage.getWidth(), nativeImage.getHeight());
            }

            return new SpriteContents(id, spriteDimensions, nativeImage, resourceMetadata);
        };
    }

    @Nullable
    SpriteContents loadSprite(Identifier id, Resource resource);
}

