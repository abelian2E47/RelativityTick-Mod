package net.minecraft.client.texture;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resource.metadata.ResourceMetadata;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class MissingSprite {
    private static final int WIDTH = 16;
    private static final int HEIGHT = 16;
    private static final String MISSINGNO_ID = "missingno";
    private static final Identifier MISSINGNO = Identifier.ofVanilla("missingno");

    public static NativeImage createImage() {
        return createImage(16, 16);
    }

    public static NativeImage createImage(int width, int height) {
        NativeImage nativeImage = new NativeImage(width, height, false);
        int i = -524040;

        for (int j = 0; j < height; j++) {
            for (int k = 0; k < width; k++) {
                if (j < height / 2 ^ k < width / 2) {
                    nativeImage.setColorArgb(k, j, -524040);
                } else {
                    nativeImage.setColorArgb(k, j, -16777216);
                }
            }
        }

        return nativeImage;
    }

    public static SpriteContents createSpriteContents() {
        NativeImage nativeImage = createImage(16, 16);
        return new SpriteContents(MISSINGNO, new SpriteDimensions(16, 16), nativeImage, ResourceMetadata.NONE);
    }

    public static Identifier getMissingSpriteId() {
        return MISSINGNO;
    }
}

