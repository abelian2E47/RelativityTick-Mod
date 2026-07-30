package net.minecraft;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.JsonHelper;
import org.slf4j.Logger;

public class MinecraftVersion implements GameVersion {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final GameVersion CURRENT = new MinecraftVersion();
    private final String id;
    private final String name;
    private final boolean stable;
    private final SaveVersion saveVersion;
    private final int protocolVersion;
    private final int resourcePackVersion;
    private final int dataPackVersion;
    private final Date buildTime;

    private MinecraftVersion() {
        this.id = UUID.randomUUID().toString().replaceAll("-", "");
        this.name = "1.21.4";
        this.stable = true;
        this.saveVersion = new SaveVersion(4189, "main");
        this.protocolVersion = SharedConstants.getProtocolVersion();
        this.resourcePackVersion = 46;
        this.dataPackVersion = 61;
        this.buildTime = new Date();
    }

    private MinecraftVersion(JsonObject json) {
        this.id = JsonHelper.getString(json, "id");
        this.name = JsonHelper.getString(json, "name");
        this.stable = JsonHelper.getBoolean(json, "stable");
        this.saveVersion = new SaveVersion(JsonHelper.getInt(json, "world_version"), JsonHelper.getString(json, "series_id", SaveVersion.MAIN_SERIES));
        this.protocolVersion = JsonHelper.getInt(json, "protocol_version");
        JsonObject jsonObject = JsonHelper.getObject(json, "pack_version");
        this.resourcePackVersion = JsonHelper.getInt(jsonObject, "resource");
        this.dataPackVersion = JsonHelper.getInt(jsonObject, "data");
        this.buildTime = Date.from(ZonedDateTime.parse(JsonHelper.getString(json, "build_time")).toInstant());
    }

    public static GameVersion create() {
        try (InputStream inputStream = MinecraftVersion.class.getResourceAsStream("/version.json")) {
            if (inputStream == null) {
                LOGGER.warn("Missing version information!");
                return CURRENT;
            }

            try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream)) {
                return new MinecraftVersion(JsonHelper.deserialize(inputStreamReader));
            }
        } catch (IOException | JsonParseException exception) {
            throw new IllegalStateException("Game version information is corrupt", exception);
        }
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public SaveVersion getSaveVersion() {
        return this.saveVersion;
    }

    @Override
    public int getProtocolVersion() {
        return this.protocolVersion;
    }

    @Override
    public int getResourceVersion(ResourceType type) {
        return type == ResourceType.SERVER_DATA ? this.dataPackVersion : this.resourcePackVersion;
    }

    @Override
    public Date getBuildTime() {
        return this.buildTime;
    }

    @Override
    public boolean isStable() {
        return this.stable;
    }
}

