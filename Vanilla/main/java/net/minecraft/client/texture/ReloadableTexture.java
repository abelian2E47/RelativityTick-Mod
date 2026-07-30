package net.minecraft.client.texture;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public abstract class ReloadableTexture extends AbstractTexture {
    private final Identifier textureId;

    public ReloadableTexture(Identifier textureId) {
        this.textureId = textureId;
    }

    public Identifier getId() {
        return this.textureId;
    }

    public void reload(TextureContents contents) {
        boolean bl = contents.clamp();
        boolean bl2 = contents.blur();
        this.bilinear = bl2;
        NativeImage nativeImage = contents.image();
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(() -> this.load(nativeImage, bl2, bl));
        } else {
            this.load(nativeImage, bl2, bl);
        }
    }

    private void load(NativeImage image, boolean blur, boolean clamp) {
        TextureUtil.prepareImage(this.getGlId(), 0, image.getWidth(), image.getHeight());
        this.setFilter(blur, false);
        this.setClamp(clamp);
        image.upload(0, 0, 0, 0, 0, image.getWidth(), image.getHeight(), true);
    }

    public abstract TextureContents loadContents(ResourceManager resourceManager) throws IOException;
}

