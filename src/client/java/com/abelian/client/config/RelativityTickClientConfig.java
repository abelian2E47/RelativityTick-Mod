package com.abelian.client.config;

import com.abelian.client.gui.RegionPanelColor;
import com.abelian.client.gui.RegionPanelStyle;
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
import java.util.EnumMap;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RelativityTickClientConfig {
    public static final boolean DEFAULT_RENDER_SCHEDULED_TICKS = true;
    public static final double DEFAULT_SCHEDULED_TICK_TEXT_SCALE = 0.03;
    public static final double DEFAULT_REGION_LINE_WIDTH = 2.5;
    public static final String DEFAULT_REGION_RENDER_MODE = "lines";
    public static final boolean DEFAULT_ENTITY_RENDER_INTERPOLATION = true;
    /** 面板配色主题的 id，见 RegionPanelStyle.Theme。 */
    public static final String DEFAULT_PANEL_THEME = RegionPanelStyle.defaultThemeId();
    /** panelTheme 取这个值表示"用户在面板设置里改过色位"，配色由 panelColors 现算。 */
    public static final String PANEL_THEME_CUSTOM = "custom";

    /** 点一次 step 按钮 / 按一次 step 快捷键推进的区域刻数。 */
    public static final int DEFAULT_STEP_SIZE = 1;
    public static final int STEP_SIZE_MIN = 1;
    public static final int STEP_SIZE_MAX = 1000;
    /** 空串表示没绑键（默认）。 */
    public static final String DEFAULT_STEP_KEY = "";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("relativitytick-client.json");

    private static boolean renderScheduledTicks = DEFAULT_RENDER_SCHEDULED_TICKS;
    private static double scheduledTickTextScale = DEFAULT_SCHEDULED_TICK_TEXT_SCALE;
    private static double regionLineWidth = DEFAULT_REGION_LINE_WIDTH;
    private static String regionRenderMode = DEFAULT_REGION_RENDER_MODE;
    private static boolean entityRenderInterpolation = DEFAULT_ENTITY_RENDER_INTERPOLATION;
    private static String panelTheme = DEFAULT_PANEL_THEME;
    /** 六个可改色位的当前值（RGB），选预设时同步成预设的值，之后用户改的那一格覆盖它。 */
    private static final EnumMap<RegionPanelStyle.Slot, Integer> panelColors = new EnumMap<>(RegionPanelStyle.Slot.class);
    private static int stepSize = DEFAULT_STEP_SIZE;
    private static String stepKey = DEFAULT_STEP_KEY;

    private RelativityTickClientConfig() {
    }

    public static void initialize() {
        setDefaults();
        if (Files.notExists(CONFIG_PATH)) {
            saveDefaultConfig();
            applyPanelStyle();
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
            regionRenderMode = readRenderMode(root, "regionRenderMode", DEFAULT_REGION_RENDER_MODE);
            entityRenderInterpolation = readBoolean(root, "entityRenderInterpolation", DEFAULT_ENTITY_RENDER_INTERPOLATION);
            panelTheme = readTheme(root, "panelTheme", DEFAULT_PANEL_THEME);
            readPanelColors(root, "panelColors");
            stepSize = readStepSize(root, "stepSize", DEFAULT_STEP_SIZE);
            stepKey = readString(root, "stepKey", DEFAULT_STEP_KEY);
            writeConfig();
        } catch (IOException | RuntimeException e) {
            setDefaults();
            saveDefaultConfig();
        }
        applyPanelStyle();
    }

    /**
     * 把配置里的配色推给面板样式；改主题或改色位后都必须调它。
     *
     * <p>选的是预设时，顺手把六个色位同步成预设的值：这样"选了预设再改一格"是从预设出发的，
     * 而不是从上次自定义的残留出发。
     */
    public static void applyPanelStyle() {
        RegionPanelStyle.Theme preset = RegionPanelStyle.preset(panelTheme);
        if (preset == null) {
            RegionPanelStyle.setCurrent(RegionPanelStyle.of(PANEL_THEME_CUSTOM,
                    color(RegionPanelStyle.Slot.BG), color(RegionPanelStyle.Slot.HEADER),
                    color(RegionPanelStyle.Slot.BORDER), color(RegionPanelStyle.Slot.ACCENT),
                    color(RegionPanelStyle.Slot.TEXT), color(RegionPanelStyle.Slot.ENABLED)));
            return;
        }
        for (RegionPanelStyle.Slot slot : RegionPanelStyle.Slot.values()) {
            panelColors.put(slot, preset.color(slot));
        }
        RegionPanelStyle.setCurrent(preset.style());
    }

    public static String getPanelTheme() {
        return panelTheme;
    }

    /** 选一套预设；不是预设 id 就回落默认预设（旧配置里的已删除主题也走这条路）。 */
    public static void setPanelTheme(String value) throws IOException {
        RegionPanelStyle.Theme preset = RegionPanelStyle.preset(value);
        panelTheme = preset == null ? DEFAULT_PANEL_THEME : preset.style().id();
        applyPanelStyle();
        writeConfig();
    }

    /** 某一格当前生效的颜色（RGB），面板设置里的色块和输入框都读它。 */
    public static int getPanelColor(RegionPanelStyle.Slot slot) {
        return color(slot);
    }

    public static String getPanelColorHex(RegionPanelStyle.Slot slot) {
        return RegionPanelColor.format(color(slot));
    }

    /** 改一格颜色：配色立刻转成"自定义"，并立即生效、落盘。 */
    public static void setPanelColor(RegionPanelStyle.Slot slot, int rgb) throws IOException {
        panelColors.put(slot, rgb & 0xFFFFFF);
        panelTheme = PANEL_THEME_CUSTOM;
        applyPanelStyle();
        writeConfig();
    }

    private static int color(RegionPanelStyle.Slot slot) {
        Integer value = panelColors.get(slot);
        if (value != null) return value;
        return RegionPanelStyle.byId(DEFAULT_PANEL_THEME).color(slot);
    }

    public static int getStepSize() {
        return stepSize;
    }

    public static void setStepSize(int value) throws IOException {
        stepSize = Math.clamp(value, STEP_SIZE_MIN, STEP_SIZE_MAX);
        writeConfig();
    }

    /** 绑定的键，形如 {@code key.keyboard.g}；空串表示未绑定。 */
    public static String getStepKey() {
        return stepKey;
    }

    public static void setStepKey(String value) throws IOException {
        stepKey = value == null ? DEFAULT_STEP_KEY : value;
        writeConfig();
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

    public static String getRegionRenderMode() {
        return regionRenderMode;
    }

    public static boolean isRegionFaceRenderingEnabled() {
        return "faces".equals(regionRenderMode);
    }

    public static void setRegionRenderMode(String value) throws IOException {
        regionRenderMode = normalizeRenderMode(value);
        writeConfig();
    }

    public static boolean isEntityRenderInterpolationEnabled() {
        return entityRenderInterpolation;
    }

    public static void setEntityRenderInterpolationEnabled(boolean value) throws IOException {
        entityRenderInterpolation = value;
        writeConfig();
    }

    private static void setDefaults() {
        renderScheduledTicks = DEFAULT_RENDER_SCHEDULED_TICKS;
        scheduledTickTextScale = DEFAULT_SCHEDULED_TICK_TEXT_SCALE;
        regionLineWidth = DEFAULT_REGION_LINE_WIDTH;
        regionRenderMode = DEFAULT_REGION_RENDER_MODE;
        entityRenderInterpolation = DEFAULT_ENTITY_RENDER_INTERPOLATION;
        panelTheme = DEFAULT_PANEL_THEME;
        panelColors.clear();
        for (RegionPanelStyle.Slot slot : RegionPanelStyle.Slot.values()) {
            panelColors.put(slot, RegionPanelStyle.byId(DEFAULT_PANEL_THEME).color(slot));
        }
        stepSize = DEFAULT_STEP_SIZE;
        stepKey = DEFAULT_STEP_KEY;
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

    private static String readString(JsonObject object, String key, String defaultValue) {
        JsonElement element = object.get(key);
        if (element == null) return defaultValue;
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new JsonParseException(key + " must be a string");
        }
        return element.getAsString();
    }

    /** 步长容忍手改配置写出的小数/负数：就地夹到合法区间，不因为一个数字让整份配置回落到默认。 */
    private static int readStepSize(JsonObject object, String key, int defaultValue) {
        return Math.clamp((int) Math.round(readNumber(object, key, defaultValue)), STEP_SIZE_MIN, STEP_SIZE_MAX);
    }

    private static String readRenderMode(JsonObject object, String key, String defaultValue) {
        return normalizeRenderMode(readString(object, key, defaultValue));
    }

    /** 主题只认预设 id 与 {@code custom}：旧版本里删掉的主题 id 直接回落默认预设。 */
    private static String readTheme(JsonObject object, String key, String defaultValue) {
        String value = readString(object, key, defaultValue);
        if (PANEL_THEME_CUSTOM.equals(value)) return PANEL_THEME_CUSTOM;
        return RegionPanelStyle.preset(value) != null ? value : defaultValue;
    }

    /**
     * 读各个色位。缺项和坏色号都只影响那一格（保留预设的值），不让一个手改坏的色号把整份配置打回默认。
     */
    private static void readPanelColors(JsonObject root, String key) {
        JsonElement element = root.get(key);
        if (element == null) return;
        if (!element.isJsonObject()) {
            throw new JsonParseException(key + " must be an object");
        }
        JsonObject colors = element.getAsJsonObject();
        for (RegionPanelStyle.Slot slot : RegionPanelStyle.Slot.values()) {
            JsonElement value = colors.get(slot.id());
            if (value == null) continue;
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                throw new JsonParseException(key + "." + slot.id() + " must be a string");
            }
            Integer rgb = RegionPanelColor.parse(value.getAsString());
            if (rgb != null) {
                panelColors.put(slot, rgb);
            }
        }
    }

    private static String normalizeRenderMode(String value) {
        return "faces".equalsIgnoreCase(value) ? "faces" : DEFAULT_REGION_RENDER_MODE;
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
            root.addProperty("regionRenderMode", regionRenderMode);
            root.addProperty("entityRenderInterpolation", entityRenderInterpolation);
            root.addProperty("panelTheme", panelTheme);
            JsonObject colors = new JsonObject();
            for (RegionPanelStyle.Slot slot : RegionPanelStyle.Slot.values()) {
                colors.addProperty(slot.id(), getPanelColorHex(slot));
            }
            root.add("panelColors", colors);
            root.addProperty("stepSize", stepSize);
            root.addProperty("stepKey", stepKey);
            GSON.toJson(root, writer);
        }
    }
}
