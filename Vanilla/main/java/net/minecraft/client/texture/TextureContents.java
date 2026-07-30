package net.minecraft.client.texture;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resource.metadata.TextureResourceMetadata;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public record TextureContents(NativeImage image, @Nullable TextureResourceMetadata metadata) implements Closeable {
    public static TextureContents load(ResourceManager resourceManager, Identifier textureId) throws IOException {
        Resource resource = resourceManager.getResourceOrThrow(textureId);

        NativeImage nativeImage;
        try (InputStream inputStream = resource.getInputStream()) {
            nativeImage = NativeImage.read(inputStream);
        }

        TextureResourceMetadata textureResourceMetadata = resource.getMetadata().decode(TextureResourceMetadata.SERIALIZER).orElse(null);
        return new TextureContents(nativeImage, textureResourceMetadata);
    }

    public static TextureContents createMissing() {
        return new TextureContents(MissingSprite.createImage(), null);
    }

    public boolean blur() {
        return this.metadata != null ? this.metadata.blur() : false;
    }

    public boolean clamp() {
        return this.metadata != null ? this.metadata.clamp() : false;
    }

    @Override
    public void close() {
        this.image.close();
    }
}

