package net.minecraft.client.realms.dto;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class RealmsServerList extends ValueObject {
    private static final Logger LOGGER = LogUtils.getLogger();
    public List<RealmsServer> servers;

    public static RealmsServerList parse(String json) {
        RealmsServerList realmsServerList = new RealmsServerList();
        realmsServerList.servers = new ArrayList<>();

        try {
            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
            if (jsonObject.get("servers").isJsonArray()) {
                for (JsonElement jsonElement : jsonObject.get("servers").getAsJsonArray()) {
                    realmsServerList.servers.add(RealmsServer.parse(jsonElement.getAsJsonObject()));
                }
            }
        } catch (Exception exception) {
            LOGGER.error("Could not parse McoServerList: {}", exception.getMessage());
        }

        return realmsServerList;
    }
}

