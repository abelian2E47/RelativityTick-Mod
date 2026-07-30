package net.minecraft.client.resource;

import java.io.IOException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.RawTextureDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.biome.FoliageColors;

@Environment(EnvType.CLIENT)
public class FoliageColormapResourceSupplier extends SinglePreparationResourceReloader<int[]> {
    private static final Identifier FOLIAGE_COLORMAP = Identifier.ofVanilla("textures/colormap/foliage.png");

    protected int[] reload(ResourceManager resourceManager, Profiler profiler) {
        try {
            return RawTextureDataLoader.loadRawTextureData(resourceManager, FOLIAGE_COLORMAP);
        } catch (IOException iOException) {
            throw new IllegalStateException("Failed to load foliage color texture", iOException);
        }
    }

    protected void apply(int[] is, ResourceManager resourceManager, Profiler profiler) {
        FoliageColors.setColorMap(is);
    }
}

