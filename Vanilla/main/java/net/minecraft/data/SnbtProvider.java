package net.minecraft.data;

import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.Util;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class SnbtProvider implements DataProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final DataOutput output;
    private final Iterable<Path> paths;
    private final List<SnbtProvider.Tweaker> write = Lists.newArrayList();

    public SnbtProvider(DataOutput output, Iterable<Path> paths) {
        this.output = output;
        this.paths = paths;
    }

    public SnbtProvider addWriter(SnbtProvider.Tweaker tweaker) {
        this.write.add(tweaker);
        return this;
    }

    private NbtCompound write(String key, NbtCompound compound) {
        NbtCompound nbtCompound = compound;

        for (SnbtProvider.Tweaker tweaker : this.write) {
            nbtCompound = tweaker.write(key, nbtCompound);
        }

        return nbtCompound;
    }

    @Override
    public CompletableFuture<?> run(DataWriter writer) {
        Path path = this.output.getPath();
        List<CompletableFuture<?>> list = Lists.newArrayList();

        for (Path path2 : this.paths) {
            list.add(CompletableFuture.<CompletableFuture<Void>>supplyAsync(() -> {
                try (Stream<Path> stream = Files.walk(path2)) {
                    return CompletableFuture.allOf(
                        stream.filter(pathxx -> pathxx.toString().endsWith(".snbt")).map(pathxx -> CompletableFuture.runAsync(() -> {
                            SnbtProvider.CompressedData compressedData = this.toCompressedNbt(pathxx, this.getFileName(path2, pathxx));
                            this.write(writer, compressedData, path);
                        }, Util.getMainWorkerExecutor().named("SnbtToNbt"))).toArray(CompletableFuture[]::new)
                    );
                } catch (Exception exception) {
                    throw new RuntimeException("Failed to read structure input directory, aborting", exception);
                }
            }, Util.getMainWorkerExecutor().named("SnbtToNbt")).thenCompose(future -> (CompletionStage<Void>)future));
        }

        return Util.combine(list);
    }

    @Override
    public final String getName() {
        return "SNBT -> NBT";
    }

    private String getFileName(Path root, Path file) {
        String string = root.relativize(file).toString().replaceAll("\\\\", "/");
        return string.substring(0, string.length() - ".snbt".length());
    }

    private SnbtProvider.CompressedData toCompressedNbt(Path path, String name) {
        try (BufferedReader bufferedReader = Files.newBufferedReader(path)) {
            String string = IOUtils.toString(bufferedReader);
            NbtCompound nbtCompound = this.write(name, NbtHelper.fromNbtProviderString(string));
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            HashingOutputStream hashingOutputStream = new HashingOutputStream(Hashing.sha1(), byteArrayOutputStream);
            NbtIo.writeCompressed(nbtCompound, hashingOutputStream);
            byte[] bs = byteArrayOutputStream.toByteArray();
            HashCode hashCode = hashingOutputStream.hash();
            return new SnbtProvider.CompressedData(name, bs, hashCode);
        } catch (Throwable throwable) {
            throw new SnbtProvider.CompressionException(path, throwable);
        }
    }

    private void write(DataWriter cache, SnbtProvider.CompressedData data, Path root) {
        Path path = root.resolve(data.name + ".nbt");

        try {
            cache.write(path, data.bytes, data.sha1);
        } catch (IOException iOException) {
            LOGGER.error("Couldn't write structure {} at {}", data.name, path, iOException);
        }
    }

    record CompressedData(String name, byte[] bytes, HashCode sha1) {
    }

    static class CompressionException extends RuntimeException {
        public CompressionException(Path path, Throwable cause) {
            super(path.toAbsolutePath().toString(), cause);
        }
    }

    @FunctionalInterface
    public interface Tweaker {
        NbtCompound write(String name, NbtCompound nbt);
    }
}

