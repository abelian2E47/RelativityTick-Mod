package com.abelian;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Vec3d;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class RelativityTickUtils {
    private static MinecraftServer server;

    public static void set(MinecraftServer serverInstance) {
        server = serverInstance;
    }

    public static void clear() {
        server = null;
    }

    public static MinecraftServer getServer() {
        return server;
    }


    public static double truncate(double value,int accuracy) {
        BigDecimal bd = new BigDecimal(String.valueOf(value));
        bd = bd.setScale(accuracy, RoundingMode.DOWN);
        return bd.doubleValue();
    }

    public static Vec3d truncate(Vec3d vec, int accuracy) {
        double x = truncate(vec.x, accuracy);
        double y = truncate(vec.y, accuracy);
        double z = truncate(vec.z, accuracy);
        return new Vec3d(x, y, z);
    }


    public static int accumulateSteps(double rate, double[] accumulator) {
        double currentAcc = accumulator[0] + (rate / 20.0);
        int steps = 0;
        while (currentAcc >= 1.0) {
            steps++;
            currentAcc -= 1.0;
        }
        accumulator[0] = currentAcc;
        return steps;
    }



    public static double getServerMspt(MinecraftServer server) {
        double MSPT = server.getAverageTickTime();
        return truncate(MSPT,4);
    }

}
