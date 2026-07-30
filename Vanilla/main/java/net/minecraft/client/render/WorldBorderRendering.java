package net.minecraft.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.border.WorldBorder;

@Environment(EnvType.CLIENT)
public class WorldBorderRendering {
    public static final Identifier FORCEFIELD = Identifier.ofVanilla("textures/misc/forcefield.png");

    public void render(WorldBorder border, Vec3d vec3d, double d, double e) {
        double f = border.getBoundWest();
        double g = border.getBoundEast();
        double h = border.getBoundNorth();
        double i = border.getBoundSouth();
        if (!(vec3d.x < g - d) || !(vec3d.x > f + d) || !(vec3d.z < i - d) || !(vec3d.z > h + d)) {
            double j = 1.0 - border.getDistanceInsideBorder(vec3d.x, vec3d.z) / d;
            j = Math.pow(j, 4.0);
            j = MathHelper.clamp(j, 0.0, 1.0);
            double k = vec3d.x;
            double l = vec3d.z;
            float m = (float)e;
            RenderLayer renderLayer = RenderLayer.getWorldBorder(MinecraftClient.isFabulousGraphicsOrBetter());
            renderLayer.startDrawing();
            int n = border.getStage().getColor();
            float o = ColorHelper.getRed(n) / 255.0F;
            float p = ColorHelper.getGreen(n) / 255.0F;
            float q = ColorHelper.getBlue(n) / 255.0F;
            RenderSystem.setShaderColor(o, p, q, (float)j);
            float r = (float)(Util.getMeasuringTimeMs() % 3000L) / 3000.0F;
            float s = (float)(-MathHelper.fractionalPart(vec3d.y * 0.5));
            float t = s + m;
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
            double u = Math.max(MathHelper.floor(l - d), h);
            double v = Math.min(MathHelper.ceil(l + d), i);
            float w = (MathHelper.floor(u) & 1) * 0.5F;
            if (k > g - d) {
                float x = w;

                for (double y = u; y < v; x += 0.5F) {
                    double z = Math.min(1.0, v - y);
                    float ab = (float)z * 0.5F;
                    bufferBuilder.vertex((float)(g - k), -m, (float)(y - l)).texture(r - x, r + t);
                    bufferBuilder.vertex((float)(g - k), -m, (float)(y + z - l)).texture(r - (ab + x), r + t);
                    bufferBuilder.vertex((float)(g - k), m, (float)(y + z - l)).texture(r - (ab + x), r + s);
                    bufferBuilder.vertex((float)(g - k), m, (float)(y - l)).texture(r - x, r + s);
                    y++;
                }
            }

            if (k < f + d) {
                float bb = w;

                for (double cb = u; cb < v; bb += 0.5F) {
                    double db = Math.min(1.0, v - cb);
                    float eb = (float)db * 0.5F;
                    bufferBuilder.vertex((float)(f - k), -m, (float)(cb - l)).texture(r + bb, r + t);
                    bufferBuilder.vertex((float)(f - k), -m, (float)(cb + db - l)).texture(r + eb + bb, r + t);
                    bufferBuilder.vertex((float)(f - k), m, (float)(cb + db - l)).texture(r + eb + bb, r + s);
                    bufferBuilder.vertex((float)(f - k), m, (float)(cb - l)).texture(r + bb, r + s);
                    cb++;
                }
            }

            u = Math.max(MathHelper.floor(k - d), f);
            v = Math.min(MathHelper.ceil(k + d), g);
            w = (MathHelper.floor(u) & 1) * 0.5F;
            if (l > i - d) {
                float fb = w;

                for (double gb = u; gb < v; fb += 0.5F) {
                    double hb = Math.min(1.0, v - gb);
                    float ib = (float)hb * 0.5F;
                    bufferBuilder.vertex((float)(gb - k), -m, (float)(i - l)).texture(r + fb, r + t);
                    bufferBuilder.vertex((float)(gb + hb - k), -m, (float)(i - l)).texture(r + ib + fb, r + t);
                    bufferBuilder.vertex((float)(gb + hb - k), m, (float)(i - l)).texture(r + ib + fb, r + s);
                    bufferBuilder.vertex((float)(gb - k), m, (float)(i - l)).texture(r + fb, r + s);
                    gb++;
                }
            }

            if (l < h + d) {
                float jb = w;

                for (double kb = u; kb < v; jb += 0.5F) {
                    double lb = Math.min(1.0, v - kb);
                    float mb = (float)lb * 0.5F;
                    bufferBuilder.vertex((float)(kb - k), -m, (float)(h - l)).texture(r - jb, r + t);
                    bufferBuilder.vertex((float)(kb + lb - k), -m, (float)(h - l)).texture(r - (mb + jb), r + t);
                    bufferBuilder.vertex((float)(kb + lb - k), m, (float)(h - l)).texture(r - (mb + jb), r + s);
                    bufferBuilder.vertex((float)(kb - k), m, (float)(h - l)).texture(r - jb, r + s);
                    kb++;
                }
            }

            BuiltBuffer builtBuffer = bufferBuilder.endNullable();
            if (builtBuffer != null) {
                BufferRenderer.drawWithGlobalProgram(builtBuffer);
            }

            renderLayer.endDrawing();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}

