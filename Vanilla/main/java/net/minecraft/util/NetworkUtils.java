package net.minecraft.util;

import com.google.common.hash.Funnels;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Map;
import java.util.OptionalLong;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

/**
 * A few networking utilities.
 */
public class NetworkUtils {
    private static final Logger LOGGER = LogUtils.getLogger();

    private NetworkUtils() {
    }

    public static Path download(
        Path path,
        URL url,
        Map<String, String> headers,
        HashFunction hashFunction,
        @Nullable HashCode hashCode,
        int maxBytes,
        Proxy proxy,
        NetworkUtils.DownloadListener listener
    ) {
        HttpURLConnection httpURLConnection = null;
        InputStream inputStream = null;
        listener.onStart();
        Path path2;
        if (hashCode != null) {
            path2 = resolve(path, hashCode);

            try {
                if (validateHash(path2, hashFunction, hashCode)) {
                    LOGGER.info("Returning cached file since actual hash matches requested");
                    listener.onFinish(true);
                    updateModificationTime(path2);
                    return path2;
                }
            } catch (IOException iOException) {
                LOGGER.warn("Failed to check cached file {}", path2, iOException);
            }

            try {
                LOGGER.warn("Existing file {} not found or had mismatched hash", path2);
                Files.deleteIfExists(path2);
            } catch (IOException iOException2) {
                listener.onFinish(false);
                throw new UncheckedIOException("Failed to remove existing file " + path2, iOException2);
            }
        } else {
            path2 = null;
        }

        try {
            httpURLConnection = (HttpURLConnection)url.openConnection(proxy);
            httpURLConnection.setInstanceFollowRedirects(true);
            headers.forEach(httpURLConnection::setRequestProperty);
            inputStream = httpURLConnection.getInputStream();
            long l = httpURLConnection.getContentLengthLong();
            OptionalLong optionalLong = l != -1L ? OptionalLong.of(l) : OptionalLong.empty();
            PathUtil.createDirectories(path);
            listener.onContentLength(optionalLong);
            if (optionalLong.isPresent() && optionalLong.getAsLong() > maxBytes) {
                throw new IOException("Filesize is bigger than maximum allowed (file is " + optionalLong + ", limit is " + maxBytes + ")");
            }

            if (path2 != null) {
                HashCode hashCode2 = write(hashFunction, maxBytes, listener, inputStream, path2);
                if (!hashCode2.equals(hashCode)) {
                    throw new IOException("Hash of downloaded file (" + hashCode2 + ") did not match requested (" + hashCode + ")");
                }

                listener.onFinish(true);
                return path2;
            } else {
                Path path4 = Files.createTempFile(path, "download", ".tmp");

                try {
                    HashCode hashCode3 = write(hashFunction, maxBytes, listener, inputStream, path4);
                    Path path5 = resolve(path, hashCode3);
                    if (!validateHash(path5, hashFunction, hashCode3)) {
                        Files.move(path4, path5, StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        updateModificationTime(path5);
                    }

                    listener.onFinish(true);
                    return path5;
                } finally {
                    Files.deleteIfExists(path4);
                }
            }
        } catch (Throwable throwable) {
            if (httpURLConnection != null) {
                InputStream inputStream2 = httpURLConnection.getErrorStream();
                if (inputStream2 != null) {
                    try {
                        LOGGER.error("HTTP response error: {}", IOUtils.toString(inputStream2, StandardCharsets.UTF_8));
                    } catch (Exception exception) {
                        LOGGER.error("Failed to read response from server");
                    }
                }
            }

            listener.onFinish(false);
            throw new IllegalStateException("Failed to download file " + url, throwable);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private static void updateModificationTime(Path path) {
        try {
            Files.setLastModifiedTime(path, FileTime.from(Instant.now()));
        } catch (IOException iOException) {
            LOGGER.warn("Failed to update modification time of {}", path, iOException);
        }
    }

    private static HashCode hash(Path path, HashFunction hashFunction) throws IOException {
        Hasher hasher = hashFunction.newHasher();

        try (
            OutputStream outputStream = Funnels.asOutputStream(hasher);
            InputStream inputStream = Files.newInputStream(path);
        ) {
            inputStream.transferTo(outputStream);
        }

        return hasher.hash();
    }

    private static boolean validateHash(Path path, HashFunction hashFunction, HashCode hashCode) throws IOException {
        if (Files.exists(path)) {
            HashCode hashCode2 = hash(path, hashFunction);
            if (hashCode2.equals(hashCode)) {
                return true;
            }

            LOGGER.warn("Mismatched hash of file {}, expected {} but found {}", path, hashCode, hashCode2);
        }

        return false;
    }

    private static Path resolve(Path path, HashCode hashCode) {
        return path.resolve(hashCode.toString());
    }

    private static HashCode write(HashFunction hashFunction, int maxBytes, NetworkUtils.DownloadListener listener, InputStream stream, Path path) throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(path, StandardOpenOption.CREATE)) {
            Hasher hasher = hashFunction.newHasher();
            byte[] bs = new byte[8196];
            long l = 0L;

            int i;
            while ((i = stream.read(bs)) >= 0) {
                l += i;
                listener.onProgress(l);
                if (l > maxBytes) {
                    throw new IOException("Filesize was bigger than maximum allowed (got >= " + l + ", limit was " + maxBytes + ")");
                }

                if (Thread.interrupted()) {
                    LOGGER.error("INTERRUPTED");
                    throw new IOException("Download interrupted");
                }

                outputStream.write(bs, 0, i);
                hasher.putBytes(bs, 0, i);
            }

            return hasher.hash();
        }
    }

    public static int findLocalPort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (IOException iOException) {
            return 25564;
        }
    }

    public static boolean isPortAvailable(int port) {
        if (port >= 0 && port <= 65535) {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                return serverSocket.getLocalPort() == port;
            } catch (IOException iOException) {
                return false;
            }
        } else {
            return false;
        }
    }

    public interface DownloadListener {
        void onStart();

        void onContentLength(OptionalLong contentLength);

        void onProgress(long writtenBytes);

        void onFinish(boolean success);
    }
}

