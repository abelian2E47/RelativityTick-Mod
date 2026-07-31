package net.minecraft.client.texture;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class SpriteAtlasTexture extends AbstractTexture implements DynamicTexture, TextureTickListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    @Deprecated
    public static final Identifier BLOCK_ATLAS_TEXTURE = Identifier.ofVanilla("textures/atlas/blocks.png");
    @Deprecated
    public static final Identifier PARTICLE_ATLAS_TEXTURE = Identifier.ofVanilla("textures/atlas/particles.png");
    private List<SpriteContents> spritesToLoad = List.of();
    private List<Sprite.TickableAnimation> animatedSprites = List.of();
    private Map<Identifier, Sprite> sprites = Map.of();
    @Nullable
    private Sprite missingSprite;
    private final Identifier id;
    private final int maxTextureSize;
    private int width;
    private int height;
    private int mipLevel;

    public SpriteAtlasTexture(Identifier id) {
        this.id = id;
        this.maxTextureSize = RenderSystem.maxSupportedTextureSize();
    }

    public void upload(SpriteLoader.StitchResult stitchResult) {
        LOGGER.info("Created: {}x{}x{} {}-atlas", stitchResult.width(), stitchResult.height(), stitchResult.mipLevel(), this.id);
        TextureUtil.prepareImage(this.getGlId(), stitchResult.mipLevel(), stitchResult.width(), stitchResult.height());
        this.width = stitchResult.width();
        this.height = stitchResult.height();
        this.mipLevel = stitchResult.mipLevel();
        this.clear();
        this.setFilter(false, this.mipLevel > 1);
        this.sprites = Map.copyOf(stitchResult.regions());
        this.missingSprite = this.sprites.get(MissingSprite.getMissingSpriteId());
        if (this.missingSprite == null) {
            throw new IllegalStateException("Atlas '" + this.id + "' (" + this.sprites.size() + " sprites) has no missing texture sprite");
        }

        List<SpriteContents> list = new ArrayList<>();
        List<Sprite.TickableAnimation> list2 = new ArrayList<>();

        for (Sprite sprite : stitchResult.regions().values()) {
            list.add(sprite.getContents());

            try {
                sprite.upload();
            } catch (Throwable throwable) {
                CrashReport crashReport = CrashReport.create(throwable, "Stitching texture atlas");
                CrashReportSection crashReportSection = crashReport.addElement("Texture being stitched together");
                crashReportSection.add("Atlas path", this.id);
                crashReportSection.add("Sprite", sprite);
                throw new CrashException(crashReport);
            }

            Sprite.TickableAnimation tickableAnimation = sprite.createAnimation();
            if (tickableAnimation != null) {
                list2.add(tickableAnimation);
            }
        }

        this.spritesToLoad = List.copyOf(list);
        this.animatedSprites = List.copyOf(list2);
    }

    @Override
    public void save(Identifier id, Path path) throws IOException {
        String string = id.toUnderscoreSeparatedString();
        TextureUtil.writeAsPNG(path, string, this.getGlId(), this.mipLevel, this.width, this.height);
        dumpAtlasInfos(path, string, this.sprites);
    }

    private static void dumpAtlasInfos(Path path, String id, Map<Identifier, Sprite> sprites) {
        Path path2 = path.resolve(id + ".txt");

        try (Writer writer = Files.newBufferedWriter(path2)) {
            for (Entry<Identifier, Sprite> entry : sprites.entrySet().stream().sorted(Entry.comparingByKey()).toList()) {
                Sprite sprite = entry.getValue();
                writer.write(
                    String.format(
                        Locale.ROOT,
                        "%s\tx=%d\ty=%d\tw=%d\th=%d%n",
                        entry.getKey(),
                        sprite.getX(),
                        sprite.getY(),
                        sprite.getContents().getWidth(),
                        sprite.getContents().getHeight()
                    )
                );
            }
        } catch (IOException iOException) {
            LOGGER.warn("Failed to write file {}", path2, iOException);
        }
    }

    public void tickAnimatedSprites() {
        this.bindTexture();

        for (Sprite.TickableAnimation tickableAnimation : this.animatedSprites) {
            tickableAnimation.tick();
        }
    }

    @Override
    public void method_22604() {
        this.tickAnimatedSprites();
    }

    public Sprite getSprite(Identifier id) {
        Sprite sprite = this.sprites.getOrDefault(id, this.missingSprite);
        if (sprite == null) {
            throw new IllegalStateException("Tried to lookup sprite, but atlas is not initialized");
        } else {
            return sprite;
        }
    }

    public void clear() {
        this.spritesToLoad.forEach(SpriteContents::close);
        this.animatedSprites.forEach(Sprite.TickableAnimation::close);
        this.spritesToLoad = List.of();
        this.animatedSprites = List.of();
        this.sprites = Map.of();
        this.missingSprite = null;
    }

    public Identifier getId() {
        return this.id;
    }

    public int getMaxTextureSize() {
        return this.maxTextureSize;
    }

    int getWidth() {
        return this.width;
    }

    int getHeight() {
        return this.height;
    }
}

