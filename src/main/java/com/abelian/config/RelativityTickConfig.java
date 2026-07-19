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

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("relativitytick.json");

    private static double maxMspt = DEFAULT_MAX_MSPT;

    private RelativityTickConfig() {
    }

    public static void initialize() {
        maxMspt = DEFAULT_MAX_MSPT;
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
            JsonElement maxMsptElement = root.get("maxMspt");
            if (maxMsptElement == null || !maxMsptElement.isJsonPrimitive() || !maxMsptElement.getAsJsonPrimitive().isNumber()) {
                throw new JsonParseException("maxMspt must be a number");
            }

            double loadedMaxMspt = maxMsptElement.getAsDouble();
            maxMspt = loadedMaxMspt;
            writeConfig(loadedMaxMspt);
        } catch (IOException | RuntimeException e) {
            maxMspt = DEFAULT_MAX_MSPT;
            saveDefaultConfig();
        }
    }

    public static double getMaxMspt() {
        return maxMspt;
    }

    public static void setMaxMspt(double value) throws IOException {
        writeConfig(value);
        maxMspt = value;
    }


    private static void saveDefaultConfig() {
        try {
            writeConfig(DEFAULT_MAX_MSPT);
        } catch (IOException ignored) {
        }
    }

    private static void writeConfig(double value) throws IOException {
        Files.createDirectories(CONFIG_PATH.getParent());
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
            JsonObject root = new JsonObject();
            root.addProperty("maxMspt", value);
            GSON.toJson(root, writer);
        }
    }
}
