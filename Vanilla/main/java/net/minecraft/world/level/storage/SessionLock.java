package net.minecraft.world.level.storage;

import com.google.common.base.Charsets;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import net.minecraft.util.PathUtil;

public class SessionLock implements AutoCloseable {
    public static final String SESSION_LOCK = "session.lock";
    private final FileChannel channel;
    private final FileLock lock;
    private static final ByteBuffer SNOWMAN;

    public static SessionLock create(Path path) throws IOException {
        Path path2 = path.resolve("session.lock");
        PathUtil.createDirectories(path);
        FileChannel fileChannel = FileChannel.open(path2, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

        try {
            fileChannel.write(SNOWMAN.duplicate());
            fileChannel.force(true);
            FileLock fileLock = fileChannel.tryLock();
            if (fileLock == null) {
                throw SessionLock.AlreadyLockedException.create(path2);
            } else {
                return new SessionLock(fileChannel, fileLock);
            }
        } catch (IOException iOException) {
            try {
                fileChannel.close();
            } catch (IOException iOException2) {
                iOException.addSuppressed(iOException2);
            }

            throw iOException;
        }
    }

    private SessionLock(FileChannel channel, FileLock lock) {
        this.channel = channel;
        this.lock = lock;
    }

    @Override
    public void close() throws IOException {
        try {
            if (this.lock.isValid()) {
                this.lock.release();
            }
        } finally {
            if (this.channel.isOpen()) {
                this.channel.close();
            }
        }
    }

    public boolean isValid() {
        return this.lock.isValid();
    }

    public static boolean isLocked(Path path) throws IOException {
        Path path2 = path.resolve("session.lock");

        try (
            FileChannel fileChannel = FileChannel.open(path2, StandardOpenOption.WRITE);
            FileLock fileLock = fileChannel.tryLock();
        ) {
            return fileLock == null;
        } catch (AccessDeniedException accessDeniedException) {
            return true;
        } catch (NoSuchFileException noSuchFileException) {
            return false;
        }
    }

    static {
        byte[] bs = "☃".getBytes(Charsets.UTF_8);
        SNOWMAN = ByteBuffer.allocateDirect(bs.length);
        SNOWMAN.put(bs);
        SNOWMAN.flip();
    }

    public static class AlreadyLockedException extends IOException {
        private AlreadyLockedException(Path path, String message) {
            super(path.toAbsolutePath() + ": " + message);
        }

        public static SessionLock.AlreadyLockedException create(Path path) {
            return new SessionLock.AlreadyLockedException(path, "already locked (possibly by other Minecraft instance?)");
        }
    }
}

