package net.minecraft.client.realms;

import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Moved from RealmsUploadScreen.Unit in 20w10a.
 */
@Environment(EnvType.CLIENT)
public enum SizeUnit {
    B,
    KB,
    MB,
    GB;

    private static final int BASE = 1024;

    public static SizeUnit getLargestUnit(long bytes) {
        if (bytes < 1024L) {
            return B;
        }

        try {
            int i = (int)(Math.log(bytes) / Math.log(1024.0));
            String string = String.valueOf("KMGTPE".charAt(i - 1));
            return valueOf(string + "B");
        } catch (Exception exception) {
            return GB;
        }
    }

    public static double convertToUnit(long bytes, SizeUnit unit) {
        return unit == B ? bytes : bytes / Math.pow(1024.0, unit.ordinal());
    }

    public static String getUserFriendlyString(long bytes) {
        int i = 1024;
        if (bytes < 1024L) {
            return bytes + " B";
        }

        int j = (int)(Math.log(bytes) / Math.log(1024.0));
        String string = "KMGTPE".charAt(j - 1) + "";
        return String.format(Locale.ROOT, "%.1f %sB", bytes / Math.pow(1024.0, j), string);
    }

    public static String humanReadableSize(long bytes, SizeUnit unit) {
        return String.format(Locale.ROOT, "%." + (unit == GB ? "1" : "0") + "f %s", convertToUnit(bytes, unit), unit.name());
    }
}

