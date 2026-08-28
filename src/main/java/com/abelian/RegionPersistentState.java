package com.abelian;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import net.minecraft.util.WorldSavePath;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipException;

public class RegionPersistentState extends PersistentState {
    public static final String ID = "relativitytick_regions";
    private static final String CONTENTS_KEY = "contents";
    private static final String CONTENT_KEY = "relativitytick";
    private static final PersistentState.Type<RegionPersistentState> TYPE = new PersistentState.Type<>(
            RegionPersistentState::new,
            RegionPersistentState::fromNbt,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private final Map<String, RegionData> regions = new HashMap<>();

    public static PersistentState.Type<RegionPersistentState> getType() {
        return TYPE;
    }

    public static void migrateLegacyFile(MinecraftServer server) {
        Path file = server.getSavePath(WorldSavePath.ROOT).resolve("data").resolve(ID + ".dat");
        if (!Files.exists(file)) return;

        try {
            NbtCompound root;
            try {
                root = NbtIo.readCompressed(file, NbtSizeTracker.ofUnlimitedBytes());
            } catch (ZipException ignored) {
                root = NbtIo.read(file);
            }
            NbtCompound data = root.getCompound("data");
            if (!data.contains("regions", NbtElement.LIST_TYPE)) return;

            NbtCompound payload = new NbtCompound();
            payload.put("regions", data.get("regions").copy());
            NbtCompound contents = data.contains(CONTENTS_KEY, NbtElement.COMPOUND_TYPE)
                    ? data.getCompound(CONTENTS_KEY)
                    : new NbtCompound();
            contents.put(CONTENT_KEY, payload);
            data.remove("regions");
            data.put(CONTENTS_KEY, contents);

            Path temporaryFile = file.resolveSibling(file.getFileName() + ".tmp");
            NbtIo.writeCompressed(root, temporaryFile);
            try {
                Files.move(temporaryFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to migrate RelativityTick region state", e);
        }
    }

    public static RegionPersistentState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        RegionPersistentState state = new RegionPersistentState();
        NbtCompound payload = nbt.getCompound(CONTENTS_KEY).getCompound(CONTENT_KEY);
        NbtList regionList = payload.getList("regions", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < regionList.size(); i++) {
            NbtCompound regionNbt = regionList.getCompound(i);
            String id = regionNbt.getString("id");
            if (id.isEmpty()) continue;

            RegistryKey<World> dimension = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(regionNbt.getString("dimension")));
            Set<Long> chunks = new HashSet<>();
            long[] chunkArray = regionNbt.getLongArray("chunks");
            for (long chunk : chunkArray) {
                chunks.add(chunk);
            }

            if (chunks.isEmpty()) continue;

            state.regions.put(id, new RegionData(dimension, chunks));
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList regionList = new NbtList();
        for (Map.Entry<String, RegionData> entry : regions.entrySet()) {
            RegionData region = entry.getValue();
            NbtCompound regionNbt = new NbtCompound();
            regionNbt.putString("id", entry.getKey());
            regionNbt.putString("dimension", region.dimension().getValue().toString());
            regionNbt.putLongArray("chunks", region.chunks().stream().mapToLong(Long::longValue).toArray());
            regionList.add(regionNbt);
        }

        NbtCompound payload = new NbtCompound();
        payload.put("regions", regionList);
        NbtCompound contents = new NbtCompound();
        contents.put(CONTENT_KEY, payload);
        nbt.put(CONTENTS_KEY, contents);
        return nbt;
    }

    public Map<String, RegionData> getRegions() {
        return Map.copyOf(regions);
    }

    public void replaceRegions(Map<String, RegionData> regions) {
        this.regions.clear();
        this.regions.putAll(regions);
        markDirty();
    }

    public record RegionData(RegistryKey<World> dimension, Set<Long> chunks) {
        public RegionData {
            chunks = Set.copyOf(chunks);
        }
    }
}
