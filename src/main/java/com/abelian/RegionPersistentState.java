package com.abelian;

import com.abelian.regionTick.RegionTickManager;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

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
    private static final PersistentStateType<RegionPersistentState> TYPE = new PersistentStateType<>(
            ID,
            RegionPersistentState::new,
            Codec.of(RegionPersistentState::encode, RegionPersistentState::decode),
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private final Map<String, RegionData> regions = new HashMap<>();

    public static PersistentStateType<RegionPersistentState> getType() {
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
            NbtCompound data = root.getCompoundOrEmpty("data");
            if (!data.contains("regions")) return;

            NbtCompound payload = new NbtCompound();
            payload.put("regions", data.get("regions").copy());
            NbtCompound contents = data.contains(CONTENTS_KEY)
                    ? data.getCompoundOrEmpty(CONTENTS_KEY)
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
        NbtCompound payload = nbt.getCompoundOrEmpty(CONTENTS_KEY).getCompoundOrEmpty(CONTENT_KEY);
        NbtList regionList = payload.getListOrEmpty("regions");
        for (int i = 0; i < regionList.size(); i++) {
            NbtCompound regionNbt = regionList.getCompoundOrEmpty(i);
            String id = regionNbt.getString("id", "");
            if (id.isEmpty()) continue;

            RegistryKey<World> dimension = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(regionNbt.getString("dimension", World.OVERWORLD.getValue().toString())));
            Set<Long> chunks = new HashSet<>();
            for (long chunk : regionNbt.getLongArray("chunks").orElseGet(() -> new long[0])) {
                chunks.add(chunk);
            }

            if (chunks.isEmpty()) continue;

            double rate = regionNbt.getDouble("rate", 20.0);
            double maxRegionCostMs = regionNbt.getDouble("tickDurationLimit ", 10.0);
            int priority = regionNbt.getInt("regionPriority", 1);
            int regionTime = regionNbt.getInt("regionTime", 0);
            RegionTickManager.RegionState regionState = readRegionState(regionNbt);
            boolean hasTimeline = regionNbt.contains("freezeStartTime") && regionNbt.contains("stepped");
            long freezeStartTime = regionNbt.getLong("freezeStartTime", 0L);
            int stepped = regionNbt.getInt("stepped", 0);
            state.regions.put(id, new RegionData(dimension, chunks, rate, maxRegionCostMs, priority, regionState, regionTime, freezeStartTime, stepped, hasTimeline));
        }
        return state;
    }

    private static RegionTickManager.RegionState readRegionState(NbtCompound regionNbt) {
        if (!regionNbt.contains("state")) return RegionTickManager.RegionState.RELEASED;

        try {
            return RegionTickManager.RegionState.valueOf(regionNbt.getString("state", ""));
        } catch (IllegalArgumentException ignored) {
            return RegionTickManager.RegionState.RELEASED;
        }
    }
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        return writeNbt(nbt);
    }

    private NbtCompound writeNbt(NbtCompound nbt) {
        NbtList regionList = new NbtList();
        for (Map.Entry<String, RegionData> entry : regions.entrySet()) {
            RegionData region = entry.getValue();
            NbtCompound regionNbt = new NbtCompound();
            regionNbt.putString("id", entry.getKey());
            regionNbt.putString("dimension", region.dimension().getValue().toString());
            regionNbt.putLongArray("chunks", region.chunks().stream().mapToLong(Long::longValue).toArray());
            regionNbt.putDouble("rate", region.rate());
            regionNbt.putDouble("tickDurationLimit ", region.tickDurationLimit());
            regionNbt.putInt("regionPriority", region.regionPriority());
            regionNbt.putString("state", region.state().name());
            regionNbt.putLong("freezeStartTime", region.freezeStartTime());
            regionNbt.putInt("regionTime", region.regionTime());
            regionNbt.putInt("stepped", region.stepped());
            regionList.add(regionNbt);
        }

        NbtCompound payload = new NbtCompound();
        payload.put("regions", regionList);
        NbtCompound contents = new NbtCompound();
        contents.put(CONTENT_KEY, payload);
        nbt.put(CONTENTS_KEY, contents);
        return nbt;
    }

    private static <T> DataResult<T> encode(RegionPersistentState state, DynamicOps<T> ops, T prefix) {
        NbtCompound nbt = state.writeNbt(new NbtCompound());
        return DataResult.success(NbtOps.INSTANCE.convertTo(ops, nbt));
    }

    private static <T> DataResult<Pair<RegionPersistentState, T>> decode(DynamicOps<T> ops, T input) {
        NbtElement nbt = ops.convertTo(NbtOps.INSTANCE, input);
        if (!(nbt instanceof NbtCompound compound)) {
            return DataResult.error(() -> "Expected compound for RelativityTick state");
        }
        return DataResult.success(Pair.of(fromNbt(compound, null), ops.empty()));
    }

    public Map<String, RegionData> getRegions() {
        return Map.copyOf(regions);
    }

    public void replaceRegions(Map<String, RegionData> regions) {
        if (this.regions.equals(regions)) return;

        this.regions.clear();
        this.regions.putAll(regions);
        markDirty();
    }

    public record RegionData(RegistryKey<World> dimension, Set<Long> chunks, double rate, double tickDurationLimit , int regionPriority, RegionTickManager.RegionState state, int regionTime, long freezeStartTime, int stepped, boolean hasTimeline) {
        public RegionData {
            chunks = Set.copyOf(chunks);
        }
    }
}
