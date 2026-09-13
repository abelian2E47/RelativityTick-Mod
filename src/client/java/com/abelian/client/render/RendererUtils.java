package com.abelian.client.render;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

import java.util.Set;

public class RendererUtils {
    //把 0~1 的 rgb 归一化颜色转成 gizmo 需要的 ARGB 整数
    public static int toArgb(float r, float g, float b) {
        return ColorHelper.fromFloats(1.0F, r, g, b);
    }

    //画单个区块的边框：与相邻受控区块共享的边不画，竖边四角按邻接情况取舍
    public static void renderChunkLines(long currentPos, float minY, float maxY, int argb, float width, Set<Long> chunks) {
        int chunkX = ChunkPos.getPackedX(currentPos);
        int chunkZ = ChunkPos.getPackedZ(currentPos);
        double x = chunkX << 4;
        double z = chunkZ << 4;

        boolean hasNorth = chunks.contains(ChunkPos.toLong(chunkX, chunkZ - 1));
        boolean hasSouth = chunks.contains(ChunkPos.toLong(chunkX, chunkZ + 1));
        boolean hasWest  = chunks.contains(ChunkPos.toLong(chunkX - 1, chunkZ));
        boolean hasEast  = chunks.contains(ChunkPos.toLong(chunkX + 1, chunkZ));

        drawVerticalIfNecessary(x, z, minY, maxY, argb, width, hasWest, hasNorth);
        drawVerticalIfNecessary(x + 16, z, minY, maxY, argb, width, hasEast, hasNorth);
        drawVerticalIfNecessary(x, z + 16, minY, maxY, argb, width, hasWest, hasSouth);
        drawVerticalIfNecessary(x + 16, z + 16, minY, maxY, argb, width, hasEast, hasSouth);

        for (float y = minY; y <= maxY; y += 4) {
            if (!hasNorth) drawLine(x, y, z, x + 16, y, z, argb, width);
            if (!hasSouth) drawLine(x, y, z + 16, x + 16, y, z + 16, argb, width);
            if (!hasWest)  drawLine(x, y, z, x, y, z + 16, argb, width);
            if (!hasEast)  drawLine(x + 16, y, z, x + 16, y, z + 16, argb, width);
        }
    }

    private static void drawVerticalIfNecessary(double x, double z, float minY, float maxY, int argb, float width, boolean n1, boolean n2) {
        if (!(n1 && n2)) {
            drawLine(x, minY, z, x, maxY, z, argb, width);
        }
    }

    private static void drawLine(double x1, double y1, double z1, double x2, double y2, double z2, int argb, float width) {
        GizmoDrawing.line(new Vec3d(x1, y1, z1), new Vec3d(x2, y2, z2), argb, width).ignoreOcclusion();
    }
}
