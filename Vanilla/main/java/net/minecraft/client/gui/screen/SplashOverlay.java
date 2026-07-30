package net.minecraft.client.gui.screen;

import com.mojang.blaze3d.platform.GlStateManager;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.resource.metadata.TextureResourceMetadata;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.ReloadableTexture;
import net.minecraft.client.texture.TextureContents;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class SplashOverlay extends Overlay {
    public static final Identifier LOGO = Identifier.ofVanilla("textures/gui/title/mojangstudios.png");
    private static final int MOJANG_RED = ColorHelper.getArgb(255, 239, 50, 61);
    private static final int MONOCHROME_BLACK = ColorHelper.getArgb(255, 0, 0, 0);
    private static final IntSupplier BRAND_ARGB = () -> MinecraftClient.getInstance().options.getMonochromeLogo().getValue() ? MONOCHROME_BLACK : MOJANG_RED;
    private static final int field_32251 = 240;
    private static final float LOGO_RIGHT_HALF_V = 60.0F;
    private static final int field_32253 = 60;
    private static final int field_32254 = 120;
    private static final float LOGO_OVERLAP = 0.0625F;
    private static final float PROGRESS_LERP_DELTA = 0.95F;
    public static final long RELOAD_COMPLETE_FADE_DURATION = 1000L;
    public static final long RELOAD_START_FADE_DURATION = 500L;
    private final MinecraftClient client;
    private final ResourceReload reload;
    private final Consumer<Optional<Throwable>> exceptionHandler;
    private final boolean reloading;
    private float progress;
    private long reloadCompleteTime = -1L;
    private long reloadStartTime = -1L;

    public SplashOverlay(MinecraftClient client, ResourceReload monitor, Consumer<Optional<Throwable>> exceptionHandler, boolean reloading) {
        this.client = client;
        this.reload = monitor;
        this.exceptionHandler = exceptionHandler;
        this.reloading = reloading;
    }

    public static void init(TextureManager textureManager) {
        textureManager.registerTexture(LOGO, new SplashOverlay.LogoTexture());
    }

    private static int withAlpha(int color, int alpha) {
        return color & 16777215 | alpha << 24;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int i = context.getScaledWindowWidth();
        int j = context.getScaledWindowHeight();
        long l = Util.getMeasuringTimeMs();
        if (this.reloading && this.reloadStartTime == -1L) {
            this.reloadStartTime = l;
        }

        float f = this.reloadCompleteTime > -1L ? (float)(l - this.reloadCompleteTime) / 1000.0F : -1.0F;
        float g = this.reloadStartTime > -1L ? (float)(l - this.reloadStartTime) / 500.0F : -1.0F;
        float h;
        if (f >= 1.0F) {
            if (this.client.currentScreen != null) {
                this.client.currentScreen.render(context, 0, 0, delta);
            }

            int k = MathHelper.ceil((1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F)) * 255.0F);
            context.fill(RenderLayer.getGuiOverlay(), 0, 0, i, j, withAlpha(BRAND_ARGB.getAsInt(), k));
            h = 1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F);
        } else if (this.reloading) {
            if (this.client.currentScreen != null && g < 1.0F) {
                this.client.currentScreen.render(context, mouseX, mouseY, delta);
            }

            int m = MathHelper.ceil(MathHelper.clamp(g, 0.15, 1.0) * 255.0);
            context.fill(RenderLayer.getGuiOverlay(), 0, 0, i, j, withAlpha(BRAND_ARGB.getAsInt(), m));
            h = MathHelper.clamp(g, 0.0F, 1.0F);
        } else {
            int o = BRAND_ARGB.getAsInt();
            float p = (o >> 16 & 0xFF) / 255.0F;
            float q = (o >> 8 & 0xFF) / 255.0F;
            float r = (o & 0xFF) / 255.0F;
            GlStateManager._clearColor(p, q, r, 1.0F);
            GlStateManager._clear(16384);
            h = 1.0F;
        }

        int t = (int)(context.getScaledWindowWidth() * 0.5);
        int u = (int)(context.getScaledWindowHeight() * 0.5);
        double d = Math.min(context.getScaledWindowWidth() * 0.75, context.getScaledWindowHeight()) * 0.25;
        int v = (int)(d * 0.5);
        double e = d * 4.0;
        int w = (int)(e * 0.5);
        int x = ColorHelper.getWhite(h);
        context.drawTexture(identifier -> RenderLayer.getMojangLogo(), LOGO, t - w, u - v, -0.0625F, 0.0F, w, (int)d, 120, 60, 120, 120, x);
        context.drawTexture(identifier -> RenderLayer.getMojangLogo(), LOGO, t, u - v, 0.0625F, 60.0F, w, (int)d, 120, 60, 120, 120, x);
        int y = (int)(context.getScaledWindowHeight() * 0.8325);
        float z = this.reload.getProgress();
        this.progress = MathHelper.clamp(this.progress * 0.95F + z * 0.050000012F, 0.0F, 1.0F);
        if (f < 1.0F) {
            this.renderProgressBar(context, i / 2 - w, y - 5, i / 2 + w, y + 5, 1.0F - MathHelper.clamp(f, 0.0F, 1.0F));
        }

        if (f >= 2.0F) {
            this.client.setOverlay(null);
        }

        if (this.reloadCompleteTime == -1L && this.reload.isComplete() && (!this.reloading || g >= 2.0F)) {
            try {
                this.reload.throwException();
                this.exceptionHandler.accept(Optional.empty());
            } catch (Throwable throwable) {
                this.exceptionHandler.accept(Optional.of(throwable));
            }

            this.reloadCompleteTime = Util.getMeasuringTimeMs();
            if (this.client.currentScreen != null) {
                this.client.currentScreen.init(this.client, context.getScaledWindowWidth(), context.getScaledWindowHeight());
            }
        }
    }

    private void renderProgressBar(DrawContext context, int minX, int minY, int maxX, int maxY, float opacity) {
        int i = MathHelper.ceil((maxX - minX - 2) * this.progress);
        int j = Math.round(opacity * 255.0F);
        int k = ColorHelper.getArgb(j, 255, 255, 255);
        context.fill(minX + 2, minY + 2, minX + i, maxY - 2, k);
        context.fill(minX + 1, minY, maxX - 1, minY + 1, k);
        context.fill(minX + 1, maxY, maxX - 1, maxY - 1, k);
        context.fill(minX, minY, minX + 1, maxY, k);
        context.fill(maxX, minY, maxX - 1, maxY, k);
    }

    @Override
    public boolean pausesGame() {
        return true;
    }

    @Environment(EnvType.CLIENT)
    static class LogoTexture extends ReloadableTexture {
        public LogoTexture() {
            super(SplashOverlay.LOGO);
        }

        @Override
        public TextureContents loadContents(ResourceManager resourceManager) throws IOException {
            ResourceFactory resourceFactory = MinecraftClient.getInstance().getDefaultResourcePack().getFactory();

            try (InputStream inputStream = resourceFactory.open(SplashOverlay.LOGO)) {
                return new TextureContents(NativeImage.read(inputStream), new TextureResourceMetadata(true, true));
            }
        }
    }
}

