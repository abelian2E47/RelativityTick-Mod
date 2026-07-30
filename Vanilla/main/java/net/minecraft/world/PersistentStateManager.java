package net.minecraft.world;

import com.google.common.collect.Iterables;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.FixedBufferInputStream;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.slf4j.Logger;

public class PersistentStateManager implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Map<String, Optional<PersistentState>> loadedStates = new HashMap<>();
    private final DataFixer dataFixer;
    private final RegistryWrapper.WrapperLookup registries;
    private final Path directory;
    private CompletableFuture<?> savingFuture = CompletableFuture.completedFuture(null);

    public PersistentStateManager(Path directory, DataFixer dataFixer, RegistryWrapper.WrapperLookup registries) {
        this.dataFixer = dataFixer;
        this.directory = directory;
        this.registries = registries;
    }

    private Path getFile(String id) {
        return this.directory.resolve(id + ".dat");
    }

    public <T extends PersistentState> T getOrCreate(PersistentState.Type<T> type, String id) {
        T persistentState = this.get(type, id);
        if (persistentState != null) {
            return persistentState;
        }

        T persistentState2 = (T)type.constructor().get();
        this.set(id, persistentState2);
        return persistentState2;
    }

    @Nullable
    public <T extends PersistentState> T get(PersistentState.Type<T> type, String id) {
        Optional<PersistentState> optional = this.loadedStates.get(id);
        if (optional == null) {
            optional = Optional.ofNullable(this.readFromFile(type.deserializer(), type.type(), id));
            this.loadedStates.put(id, optional);
        }

        return (T)optional.orElse(null);
    }

    @Nullable
    private <T extends PersistentState> T readFromFile(
        BiFunction<NbtCompound, RegistryWrapper.WrapperLookup, T> readFunction, DataFixTypes dataFixTypes, String id
    ) {
        try {
            Path path = this.getFile(id);
            if (Files.exists(path)) {
                NbtCompound nbtCompound = this.readNbt(id, dataFixTypes, SharedConstants.getGameVersion().getSaveVersion().getId());
                return readFunction.apply(nbtCompound.getCompound("data"), this.registries);
            }
        } catch (Exception exception) {
            LOGGER.error("Error loading saved data: {}", id, exception);
        }

        return null;
    }

    public void set(String id, PersistentState state) {
        this.loadedStates.put(id, Optional.of(state));
        state.markDirty();
    }

    public NbtCompound readNbt(String id, DataFixTypes dataFixTypes, int currentSaveVersion) throws IOException {
        try (
            InputStream inputStream = Files.newInputStream(this.getFile(id));
            PushbackInputStream pushbackInputStream = new PushbackInputStream(new FixedBufferInputStream(inputStream), 2);
        ) {
            NbtCompound nbtCompound;
            if (this.isCompressed(pushbackInputStream)) {
                nbtCompound = NbtIo.readCompressed(pushbackInputStream, NbtSizeTracker.ofUnlimitedBytes());
            } else {
                try (DataInputStream dataInputStream = new DataInputStream(pushbackInputStream)) {
                    nbtCompound = NbtIo.readCompound(dataInputStream);
                }
            }

            int i = NbtHelper.getDataVersion(nbtCompound, 1343);
            return dataFixTypes.update(this.dataFixer, nbtCompound, i, currentSaveVersion);
        }
    }

    private boolean isCompressed(PushbackInputStream stream) throws IOException {
        byte[] bs = new byte[2];
        boolean bl = false;
        int i = stream.read(bs, 0, 2);
        if (i == 2) {
            int j = (bs[1] & 255) << 8 | bs[0] & 255;
            if (j == 35615) {
                bl = true;
            }
        }

        if (i != 0) {
            stream.unread(bs, 0, i);
        }

        return bl;
    }

    public CompletableFuture<?> startSaving() {
        Map<Path, NbtCompound> map = this.collectStatesToSave();
        if (map.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        int i = Util.getAvailableBackgroundThreads();
        int j = map.size();
        if (j > i) {
            this.savingFuture = this.savingFuture.thenCompose(object -> {
                List<CompletableFuture<?>> list = new ArrayList<>(i);
                int k = MathHelper.ceilDiv(j, i);

                for (List<Entry<Path, NbtCompound>> list2 : Iterables.partition(map.entrySet(), k)) {
                    list.add(CompletableFuture.runAsync(() -> {
                        for (Entry<Path, NbtCompound> entry : list2) {
                            save(entry.getKey(), entry.getValue());
                        }
                    }, Util.getIoWorkerExecutor()));
                }

                return CompletableFuture.allOf(list.toArray(CompletableFuture[]::new));
            });
        } else {
            this.savingFuture = this.savingFuture
                .thenCompose(
                    v -> CompletableFuture.allOf(
                        map.entrySet()
                            .stream()
                            .map(entry -> CompletableFuture.runAsync(() -> save(entry.getKey(), entry.getValue()), Util.getIoWorkerExecutor()))
                            .toArray(CompletableFuture[]::new)
                    )
                );
        }

        return this.savingFuture;
    }

    private Map<Path, NbtCompound> collectStatesToSave() {
        Map<Path, NbtCompound> map = new Object2ObjectArrayMap<>();
        this.loadedStates
            .forEach((id, state) -> state.filter(PersistentState::isDirty).ifPresent(state2 -> map.put(this.getFile(id), state2.toNbt(this.registries))));
        return map;
    }

    private static void save(Path path, NbtCompound nbt) {
        try {
            NbtIo.writeCompressed(nbt, path);
        } catch (IOException iOException) {
            LOGGER.error("Could not save data to {}", path.getFileName(), iOException);
        }
    }

    public void save() {
        this.startSaving().join();
    }

    @Override
    public void close() {
        this.save();
    }
}

