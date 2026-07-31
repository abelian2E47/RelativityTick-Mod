package net.minecraft.client.data;

import com.google.gson.JsonElement;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;

/**
 * A supplier of a block state JSON definition.
 */
@Environment(EnvType.CLIENT)
public interface BlockStateSupplier extends Supplier<JsonElement> {
    Block getBlock();
}

