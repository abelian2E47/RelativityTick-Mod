package com.abelian.client.config;

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

public final class RelativityTickClientConfig {
    public static final boolean DEFAULT_RENDER_SCHEDULED_TICKS = true;
    public static final double DEFAULT_SCHEDULED_TICK_TEXT_SCALE = 0.03;
    public static final double DEFAULT_REGION_LINE_WIDTH = 2.5;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("relativitytick-client.json");

    private static boolean renderScheduledTicks = DEFAULT_RENDER_SCHEDULED_TICKS;
    private static double scheduledTickTextScale = DEFAULT_SCHEDULED_TICK_TEXT_SCALE;
    private static double regionLineWidth = DEFAULT_REGION_LINE_WIDTH;

    private RelativityTickClientConfig() {
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
            renderScheduledTicks = readBoolean(root, "renderScheduledTicks", DEFAULT_RENDER_SCHEDULED_TICKS);
            scheduledTickTextScale = readNumber(root, "scheduledTickTextScale", DEFAULT_SCHEDULED_TICK_TEXT_SCALE);
            regionLineWidth = readNumber(root, "regionLineWidth", DEFAULT_REGION_LINE_WIDTH);
            writeConfig();
        } catch (IOException | RuntimeException e) {
            setDefaults();
            saveDefaultConfig();
        }
    }

    public static boolean isRenderScheduledTicksEnabled() {
        return renderScheduledTicks;
    }

    public static void setRenderScheduledTicksEnabled(boolean value) throws IOException {
        renderScheduledTicks = value;
        writeConfig();
    }

    public static double getScheduledTickTextScale() {
        return scheduledTickTextScale;
    }

    public static void setScheduledTickTextScale(double value) throws IOException {
        scheduledTickTextScale = value;
        writeConfig();
    }

    public static double getRegionLineWidth() {
        return regionLineWidth;
    }

    public static void setRegionLineWidth(double value) throws IOException {
        regionLineWidth = value;
        writeConfig();
    }

    private static void setDefaults() {
        renderScheduledTicks = DEFAULT_RENDER_SCHEDULED_TICKS;
        scheduledTickTextScale = DEFAULT_SCHEDULED_TICK_TEXT_SCALE;
        regionLineWidth = DEFAULT_REGION_LINE_WIDTH;
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
            root.addProperty("renderScheduledTicks", renderScheduledTicks);
            root.addProperty("scheduledTickTextScale", scheduledTickTextScale);
            root.addProperty("regionLineWidth", regionLineWidth);
            GSON.toJson(root, writer);
        }
    }
}
