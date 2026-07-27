package com.abelian.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RelativityTickConfig {
    public static final double DEFAULT_MAX_MSPT = 45.0;
    public static final boolean DEFAULT_CHUNK_TICK_ENABLED = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("relativitytick.json");

    private static double maxMspt = DEFAULT_MAX_MSPT;
    private static boolean chunkTickEnabled = DEFAULT_CHUNK_TICK_ENABLED;

    private RelativityTickConfig() {
    }

    public static void initialize() {
        setDefaults();
        if (Files.notExists(CONFIG_PATH)) {
            saveDefaultConfig();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) {
                throw new JsonParseException("The root value must be a JSON object");
            }

            JsonObject root = parsed.getAsJsonObject();
            maxMspt = readNumber(root, "maxMspt", DEFAULT_MAX_MSPT);
            JsonObject chunkTick = root.has("chunkTick") && root.get("chunkTick").isJsonObject()
                    ? root.getAsJsonObject("chunkTick")
                    : new JsonObject();
            chunkTickEnabled = readBoolean(chunkTick, "enabled", DEFAULT_CHUNK_TICK_ENABLED);
            writeConfig();
        } catch (IOException | RuntimeException e) {
            setDefaults();
            saveDefaultConfig();
        }
    }

    public static double getMaxMspt() {
        return maxMspt;
    }

    public static void setMaxMspt(double value) throws IOException {
        maxMspt = value;
        writeConfig();
    }

    public static boolean isChunkTickEnabled() {
        return chunkTickEnabled;
    }

    public static void setChunkTickEnabled(boolean value) throws IOException {
        chunkTickEnabled = value;
        writeConfig();
    }


    private static void setDefaults() {
        maxMspt = DEFAULT_MAX_MSPT;
        chunkTickEnabled = DEFAULT_CHUNK_TICK_ENABLED;
    }

    private static double readNumber(JsonObject object, String key, double defaultValue) {
        JsonElement element = object.get(key);
        if (element == null) return defaultValue;
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException(key + " must be a number");
        }
        return element.getAsDouble();
    }

    private static boolean readBoolean(JsonObject object, String key, boolean defaultValue) {
        JsonElement element = object.get(key);
        if (element == null) return defaultValue;
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
            throw new JsonParseException(key + " must be a boolean");
        }
        return element.getAsBoolean();
    }

    private static void saveDefaultConfig() {
        try {
            writeConfig();
        } catch (IOException ignored) {
        }
    }

    private static void writeConfig() throws IOException {
        Files.createDirectories(CONFIG_PATH.getParent());
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
            JsonObject root = new JsonObject();
            root.addProperty("maxMspt", maxMspt);

            JsonObject chunkTick = new JsonObject();
            chunkTick.addProperty("enabled", chunkTickEnabled);
            root.add("chunkTick", chunkTick);

            GSON.toJson(root, writer);
        }
    }
}
