package net.minecraft.util.logging;

import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.annotation.Nullable;
import org.slf4j.Logger;

public class LogFileCompressor {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final int COMPRESSION_BUFFER_SIZE = 4096;
    private static final String GZ_EXTENSION = ".gz";
    private final Path directory;
    private final String extension;

    private LogFileCompressor(Path directory, String extension) {
        this.directory = directory;
        this.extension = extension;
    }

    public static LogFileCompressor create(Path directory, String extension) throws IOException {
        Files.createDirectories(directory);
        return new LogFileCompressor(directory, extension);
    }

    public LogFileCompressor.LogFileIterable getAll() throws IOException {
        try (Stream<Path> stream = Files.list(this.directory)) {
            return new LogFileCompressor.LogFileIterable(stream.filter(path -> Files.isRegularFile(path)).map(this::get).filter(Objects::nonNull).toList());
        }
    }

    @Nullable
    private LogFileCompressor.LogFile get(Path path) {
        String string = path.getFileName().toString();
        int i = string.indexOf(46);
        if (i == -1) {
            return null;
        }

        LogFileCompressor.LogId logId = LogFileCompressor.LogId.fromFileName(string.substring(0, i));
        if (logId != null) {
            String string2 = string.substring(i);
            if (string2.equals(this.extension)) {
                return new LogFileCompressor.Uncompressed(path, logId);
            }

            if (string2.equals(this.extension + ".gz")) {
                return new LogFileCompressor.Compressed(path, logId);
            }
        }

        return null;
    }

    static void compress(Path from, Path to) throws IOException {
        if (Files.exists(to)) {
            throw new IOException("Compressed target file already exists: " + to);
        }

        try (FileChannel fileChannel = FileChannel.open(from, StandardOpenOption.WRITE, StandardOpenOption.READ)) {
            FileLock fileLock = fileChannel.tryLock();
            if (fileLock == null) {
                throw new IOException("Raw log file is already locked, cannot compress: " + from);
            }

            compress(fileChannel, to);
            fileChannel.truncate(0L);
        }

        Files.delete(from);
    }

    private static void compress(ReadableByteChannel source, Path outputPath) throws IOException {
        try (OutputStream outputStream = new GZIPOutputStream(Files.newOutputStream(outputPath))) {
            byte[] bs = new byte[4096];
            ByteBuffer byteBuffer = ByteBuffer.wrap(bs);

            while (source.read(byteBuffer) >= 0) {
                byteBuffer.flip();
                outputStream.write(bs, 0, byteBuffer.limit());
                byteBuffer.clear();
            }
        }
    }

    public LogFileCompressor.Uncompressed createLogFile(LocalDate date) throws IOException {
        int i = 1;
        Set<LogFileCompressor.LogId> set = this.getAll().toIdSet();

        LogFileCompressor.LogId logId;
        do {
            logId = new LogFileCompressor.LogId(date, i++);
        } while (set.contains(logId));

        LogFileCompressor.Uncompressed uncompressed = new LogFileCompressor.Uncompressed(this.directory.resolve(logId.getFileName(this.extension)), logId);
        Files.createFile(uncompressed.path());
        return uncompressed;
    }

    public record Compressed(Path path, LogFileCompressor.LogId id) implements LogFileCompressor.LogFile {
        @Nullable
        @Override
        public Reader getReader() throws IOException {
            return !Files.exists(this.path) ? null : new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(this.path))));
        }

        @Override
        public LogFileCompressor.Compressed compress() {
            return this;
        }
    }

    public interface LogFile {
        Path path();

        LogFileCompressor.LogId id();

        @Nullable
        Reader getReader() throws IOException;

        LogFileCompressor.Compressed compress() throws IOException;
    }

    public static class LogFileIterable implements Iterable<LogFileCompressor.LogFile> {
        private final List<LogFileCompressor.LogFile> logs;

        LogFileIterable(List<LogFileCompressor.LogFile> logs) {
            this.logs = new ArrayList<>(logs);
        }

        public LogFileCompressor.LogFileIterable removeExpired(LocalDate currentDate, int retentionDays) {
            this.logs.removeIf(log -> {
                LogFileCompressor.LogId logId = log.id();
                LocalDate localDate2 = logId.date().plusDays(retentionDays);
                if (!currentDate.isBefore(localDate2)) {
                    try {
                        Files.delete(log.path());
                        return true;
                    } catch (IOException iOException) {
                        LogFileCompressor.LOGGER.warn("Failed to delete expired event log file: {}", log.path(), iOException);
                    }
                }

                return false;
            });
            return this;
        }

        public LogFileCompressor.LogFileIterable compressAll() {
            ListIterator<LogFileCompressor.LogFile> listIterator = this.logs.listIterator();

            while (listIterator.hasNext()) {
                LogFileCompressor.LogFile logFile = listIterator.next();

                try {
                    listIterator.set(logFile.compress());
                } catch (IOException iOException) {
                    LogFileCompressor.LOGGER.warn("Failed to compress event log file: {}", logFile.path(), iOException);
                }
            }

            return this;
        }

        @Override
        public Iterator<LogFileCompressor.LogFile> iterator() {
            return this.logs.iterator();
        }

        public Stream<LogFileCompressor.LogFile> stream() {
            return this.logs.stream();
        }

        public Set<LogFileCompressor.LogId> toIdSet() {
            return this.logs.stream().map(LogFileCompressor.LogFile::id).collect(Collectors.toSet());
        }
    }

    public record LogId(LocalDate date, int index) {
        private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

        @Nullable
        public static LogFileCompressor.LogId fromFileName(String fileName) {
            int i = fileName.indexOf("-");
            if (i == -1) {
                return null;
            }

            String string = fileName.substring(0, i);
            String string2 = fileName.substring(i + 1);

            try {
                return new LogFileCompressor.LogId(LocalDate.parse(string, DATE_TIME_FORMATTER), Integer.parseInt(string2));
            } catch (NumberFormatException | DateTimeParseException runtimeException) {
                return null;
            }
        }

        @Override
        public String toString() {
            return DATE_TIME_FORMATTER.format(this.date) + "-" + this.index;
        }

        public String getFileName(String extension) {
            return this + extension;
        }
    }

    public record Uncompressed(Path path, LogFileCompressor.LogId id) implements LogFileCompressor.LogFile {
        public FileChannel open() throws IOException {
            return FileChannel.open(this.path, StandardOpenOption.WRITE, StandardOpenOption.READ);
        }

        @Nullable
        @Override
        public Reader getReader() throws IOException {
            return Files.exists(this.path) ? Files.newBufferedReader(this.path) : null;
        }

        @Override
        public LogFileCompressor.Compressed compress() throws IOException {
            Path path = this.path.resolveSibling(this.path.getFileName().toString() + ".gz");
            LogFileCompressor.compress(this.path, path);
            return new LogFileCompressor.Compressed(path, this.id);
        }
    }
}

