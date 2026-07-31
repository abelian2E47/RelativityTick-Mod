package net.minecraft.client.data;

import com.google.gson.JsonElement;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface ModelSupplier extends Supplier<JsonElement> {
}

