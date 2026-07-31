package net.minecraft.world.biome;

public class FoliageColors {
    public static final int SPRUCE = -10380959;
    public static final int BIRCH = -8345771;
    public static final int DEFAULT = -12012264;
    public static final int MANGROVE = -7158200;
    private static int[] colorMap = new int[65536];

    public static void setColorMap(int[] pixels) {
        colorMap = pixels;
    }

    public static int getColor(double temperature, double humidity) {
        humidity *= temperature;
        int i = (int)((1.0 - temperature) * 255.0);
        int j = (int)((1.0 - humidity) * 255.0);
        int k = j << 8 | i;
        return k >= colorMap.length ? -12012264 : colorMap[k];
    }
}

