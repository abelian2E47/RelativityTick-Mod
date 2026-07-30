package net.minecraft.util.thread;

import com.mojang.logging.LogUtils;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import org.slf4j.Logger;

/**
 * A functional interface that can enqueue completable futures.
 */
@FunctionalInterface
public interface FutureQueue {
    Logger LOGGER = LogUtils.getLogger();

    static FutureQueue immediate(final Executor executor) {
        return new FutureQueue() {
            @Override
            public <T> void append(CompletableFuture<T> future, Consumer<T> callback) {
                future.thenAcceptAsync(callback, executor).exceptionally(throwable -> {
                    LOGGER.error("Task failed", throwable);
                    return null;
                });
            }
        };
    }

    default void append(Runnable callback) {
        this.append(CompletableFuture.completedFuture(null), current -> callback.run());
    }

    <T> void append(CompletableFuture<T> future, Consumer<T> callback);
}

