package net.minecraft.client.texture;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.PathUtil;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class PlayerSkinTextureDownloader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SKIN_WIDTH = 64;
    private static final int SKIN_HEIGHT = 64;
    private static final int OLD_SKIN_HEIGHT = 32;

    public static CompletableFuture<Identifier> downloadAndRegisterTexture(Identifier textureId, Path path, String uri, boolean remap) {
        return CompletableFuture.<NativeImage>supplyAsync(() -> {
            NativeImage nativeImage;
            try {
                nativeImage = download(path, uri);
            } catch (IOException iOException) {
                throw new UncheckedIOException(iOException);
            }

            return remap ? remapTexture(nativeImage, uri) : nativeImage;
        }, Util.getDownloadWorkerExecutor().named("downloadTexture")).thenCompose(image -> registerTexture(textureId, image));
    }

    private static NativeImage download(Path path, String uri) throws IOException {
        if (Files.isRegularFile(path)) {
            LOGGER.debug("Loading HTTP texture from local cache ({})", path);

            try (InputStream inputStream = Files.newInputStream(path)) {
                return NativeImage.read(inputStream);
            }
        } else {
            HttpURLConnection httpURLConnection = null;
            LOGGER.debug("Downloading HTTP texture from {} to {}", uri, path);
            URI uRI = URI.create(uri);

            try {
                httpURLConnection = (HttpURLConnection)uRI.toURL().openConnection(MinecraftClient.getInstance().getNetworkProxy());
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(false);
                httpURLConnection.connect();
                int i = httpURLConnection.getResponseCode();
                if (i / 100 != 2) {
                    throw new IOException("Failed to open " + uRI + ", HTTP error code: " + i);
                }

                byte[] bs = httpURLConnection.getInputStream().readAllBytes();

                try {
                    PathUtil.createDirectories(path.getParent());
                    Files.write(path, bs);
                } catch (IOException iOException) {
                    LOGGER.warn("Failed to cache texture {} in {}", uri, path);
                }

                return NativeImage.read(bs);
            } finally {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
            }
        }
    }

    private static CompletableFuture<Identifier> registerTexture(Identifier textureId, NativeImage image) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        return CompletableFuture.supplyAsync(() -> {
            minecraftClient.getTextureManager().registerTexture(textureId, new NativeImageBackedTexture(image));
            return textureId;
        }, minecraftClient);
    }

    private static NativeImage remapTexture(NativeImage image, String uri) {
        int i = image.getHeight();
        int j = image.getWidth();
        if (j == 64 && (i == 32 || i == 64)) {
            boolean bl = i == 32;
            if (bl) {
                NativeImage nativeImage = new NativeImage(64, 64, true);
                nativeImage.copyFrom(image);
                image.close();
                image = nativeImage;
                image.fillRect(0, 32, 64, 32, 0);
                image.copyRect(4, 16, 16, 32, 4, 4, true, false);
                image.copyRect(8, 16, 16, 32, 4, 4, true, false);
                image.copyRect(0, 20, 24, 32, 4, 12, true, false);
                image.copyRect(4, 20, 16, 32, 4, 12, true, false);
                image.copyRect(8, 20, 8, 32, 4, 12, true, false);
                image.copyRect(12, 20, 16, 32, 4, 12, true, false);
                image.copyRect(44, 16, -8, 32, 4, 4, true, false);
                image.copyRect(48, 16, -8, 32, 4, 4, true, false);
                image.copyRect(40, 20, 0, 32, 4, 12, true, false);
                image.copyRect(44, 20, -8, 32, 4, 12, true, false);
                image.copyRect(48, 20, -16, 32, 4, 12, true, false);
                image.copyRect(52, 20, -8, 32, 4, 12, true, false);
            }

            stripAlpha(image, 0, 0, 32, 16);
            if (bl) {
                stripColor(image, 32, 0, 64, 32);
            }

            stripAlpha(image, 0, 16, 64, 32);
            stripAlpha(image, 16, 48, 48, 64);
            return image;
        } else {
            image.close();
            throw new IllegalStateException("Discarding incorrectly sized (" + j + "x" + i + ") skin texture from " + uri);
        }
    }

    private static void stripColor(NativeImage image, int x1, int y1, int x2, int y2) {
        for (int i = x1; i < x2; i++) {
            for (int j = y1; j < y2; j++) {
                int k = image.getColorArgb(i, j);
                if (ColorHelper.getAlpha(k) < 128) {
                    return;
                }
            }
        }

        for (int l = x1; l < x2; l++) {
            for (int m = y1; m < y2; m++) {
                image.setColorArgb(l, m, image.getColorArgb(l, m) & 16777215);
            }
        }
    }

    private static void stripAlpha(NativeImage image, int x1, int y1, int x2, int y2) {
        for (int i = x1; i < x2; i++) {
            for (int j = y1; j < y2; j++) {
                image.setColorArgb(i, j, ColorHelper.fullAlpha(image.getColorArgb(i, j)));
            }
        }
    }
}

