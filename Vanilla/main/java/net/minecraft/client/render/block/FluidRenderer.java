package net.minecraft.client.render.block;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.TranslucentBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.texture.Sprite;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockRenderView;

@Environment(EnvType.CLIENT)
public class FluidRenderer {
    private static final float field_32781 = 0.8888889F;
    private final Sprite[] lavaSprites = new Sprite[2];
    private final Sprite[] waterSprites = new Sprite[2];
    private Sprite waterOverlaySprite;

    protected void onResourceReload() {
        this.lavaSprites[0] = MinecraftClient.getInstance().getBakedModelManager().getBlockModels().getModel(Blocks.LAVA.getDefaultState()).getParticleSprite();
        this.lavaSprites[1] = ModelBaker.LAVA_FLOW.getSprite();
        this.waterSprites[0] = MinecraftClient.getInstance()
            .getBakedModelManager()
            .getBlockModels()
            .getModel(Blocks.WATER.getDefaultState())
            .getParticleSprite();
        this.waterSprites[1] = ModelBaker.WATER_FLOW.getSprite();
        this.waterOverlaySprite = ModelBaker.WATER_OVERLAY.getSprite();
    }

    private static boolean isSameFluid(FluidState a, FluidState b) {
        return b.getFluid().matchesType(a.getFluid());
    }

    private static boolean isSideCovered(Direction direction, float f, BlockState blockState) {
        VoxelShape voxelShape = blockState.getCullingFace(direction.getOpposite());
        if (voxelShape == VoxelShapes.empty()) {
            return false;
        } else if (voxelShape == VoxelShapes.fullCube()) {
            boolean bl = f == 1.0F;
            return direction != Direction.UP || bl;
        } else {
            VoxelShape voxelShape2 = VoxelShapes.cuboid(0.0, 0.0, 0.0, 1.0, f, 1.0);
            return VoxelShapes.isSideCovered(voxelShape2, voxelShape, direction);
        }
    }

    private static boolean method_3344(Direction direction, float f, BlockState blockState) {
        return isSideCovered(direction, f, blockState);
    }

    private static boolean isOppositeSideCovered(BlockState blockState, Direction direction) {
        return isSideCovered(direction.getOpposite(), 1.0F, blockState);
    }

    public static boolean shouldRenderSide(FluidState fluidState, BlockState blockState, Direction direction, FluidState fluidState2) {
        return !isOppositeSideCovered(blockState, direction) && !isSameFluid(fluidState, fluidState2);
    }

    public void render(BlockRenderView world, BlockPos pos, VertexConsumer vertexConsumer, BlockState blockState, FluidState fluidState) {
        boolean bl = fluidState.isIn(FluidTags.LAVA);
        Sprite[] sprites = bl ? this.lavaSprites : this.waterSprites;
        int i = bl ? 16777215 : BiomeColors.getWaterColor(world, pos);
        float f = (i >> 16 & 0xFF) / 255.0F;
        float g = (i >> 8 & 0xFF) / 255.0F;
        float h = (i & 0xFF) / 255.0F;
        BlockState blockState2 = world.getBlockState(pos.offset(Direction.DOWN));
        FluidState fluidState2 = blockState2.getFluidState();
        BlockState blockState3 = world.getBlockState(pos.offset(Direction.UP));
        FluidState fluidState3 = blockState3.getFluidState();
        BlockState blockState4 = world.getBlockState(pos.offset(Direction.NORTH));
        FluidState fluidState4 = blockState4.getFluidState();
        BlockState blockState5 = world.getBlockState(pos.offset(Direction.SOUTH));
        FluidState fluidState5 = blockState5.getFluidState();
        BlockState blockState6 = world.getBlockState(pos.offset(Direction.WEST));
        FluidState fluidState6 = blockState6.getFluidState();
        BlockState blockState7 = world.getBlockState(pos.offset(Direction.EAST));
        FluidState fluidState7 = blockState7.getFluidState();
        boolean bl2 = !isSameFluid(fluidState, fluidState3);
        boolean bl3 = shouldRenderSide(fluidState, blockState, Direction.DOWN, fluidState2) && !method_3344(Direction.DOWN, 0.8888889F, blockState2);
        boolean bl4 = shouldRenderSide(fluidState, blockState, Direction.NORTH, fluidState4);
        boolean bl5 = shouldRenderSide(fluidState, blockState, Direction.SOUTH, fluidState5);
        boolean bl6 = shouldRenderSide(fluidState, blockState, Direction.WEST, fluidState6);
        boolean bl7 = shouldRenderSide(fluidState, blockState, Direction.EAST, fluidState7);
        if (bl2 || bl3 || bl7 || bl6 || bl4 || bl5) {
            float j = world.getBrightness(Direction.DOWN, true);
            float k = world.getBrightness(Direction.UP, true);
            float l = world.getBrightness(Direction.NORTH, true);
            float m = world.getBrightness(Direction.WEST, true);
            Fluid fluid = fluidState.getFluid();
            float n = this.getFluidHeight(world, fluid, pos, blockState, fluidState);
            float o;
            float p;
            float q;
            float r;
            if (n >= 1.0F) {
                o = 1.0F;
                p = 1.0F;
                q = 1.0F;
                r = 1.0F;
            } else {
                float s = this.getFluidHeight(world, fluid, pos.north(), blockState4, fluidState4);
                float t = this.getFluidHeight(world, fluid, pos.south(), blockState5, fluidState5);
                float u = this.getFluidHeight(world, fluid, pos.east(), blockState7, fluidState7);
                float v = this.getFluidHeight(world, fluid, pos.west(), blockState6, fluidState6);
                o = this.calculateFluidHeight(world, fluid, n, s, u, pos.offset(Direction.NORTH).offset(Direction.EAST));
                p = this.calculateFluidHeight(world, fluid, n, s, v, pos.offset(Direction.NORTH).offset(Direction.WEST));
                q = this.calculateFluidHeight(world, fluid, n, t, u, pos.offset(Direction.SOUTH).offset(Direction.EAST));
                r = this.calculateFluidHeight(world, fluid, n, t, v, pos.offset(Direction.SOUTH).offset(Direction.WEST));
            }

            float ab = pos.getX() & 15;
            float bb = pos.getY() & 15;
            float cb = pos.getZ() & 15;
            float db = 0.001F;
            float eb = bl3 ? 0.001F : 0.0F;
            if (bl2 && !method_3344(Direction.UP, Math.min(Math.min(p, r), Math.min(q, o)), blockState3)) {
                p -= 0.001F;
                r -= 0.001F;
                q -= 0.001F;
                o -= 0.001F;
                Vec3d vec3d = fluidState.getVelocity(world, pos);
                float fb;
                float hb;
                float jb;
                float lb;
                float gb;
                float ib;
                float kb;
                float mb;
                if (vec3d.x == 0.0 && vec3d.z == 0.0) {
                    Sprite sprite = sprites[0];
                    fb = sprite.getFrameU(0.0F);
                    gb = sprite.getFrameV(0.0F);
                    hb = fb;
                    ib = sprite.getFrameV(1.0F);
                    jb = sprite.getFrameU(1.0F);
                    kb = ib;
                    lb = jb;
                    mb = gb;
                } else {
                    Sprite sprite2 = sprites[1];
                    float nb = (float)MathHelper.atan2(vec3d.z, vec3d.x) - (float) (Math.PI / 2);
                    float ob = MathHelper.sin(nb) * 0.25F;
                    float pb = MathHelper.cos(nb) * 0.25F;
                    float qb = 0.5F;
                    fb = sprite2.getFrameU(0.5F + (-pb - ob));
                    gb = sprite2.getFrameV(0.5F + (-pb + ob));
                    hb = sprite2.getFrameU(0.5F + (-pb + ob));
                    ib = sprite2.getFrameV(0.5F + (pb + ob));
                    jb = sprite2.getFrameU(0.5F + (pb + ob));
                    kb = sprite2.getFrameV(0.5F + (pb - ob));
                    lb = sprite2.getFrameU(0.5F + (pb - ob));
                    mb = sprite2.getFrameV(0.5F + (-pb - ob));
                }

                float zb = (fb + hb + jb + lb) / 4.0F;
                float ac = (gb + ib + kb + mb) / 4.0F;
                float bc = sprites[0].getAnimationFrameDelta();
                fb = MathHelper.lerp(bc, fb, zb);
                hb = MathHelper.lerp(bc, hb, zb);
                jb = MathHelper.lerp(bc, jb, zb);
                lb = MathHelper.lerp(bc, lb, zb);
                gb = MathHelper.lerp(bc, gb, ac);
                ib = MathHelper.lerp(bc, ib, ac);
                kb = MathHelper.lerp(bc, kb, ac);
                mb = MathHelper.lerp(bc, mb, ac);
                int cc = this.getLight(world, pos);
                float dc = k * f;
                float ec = k * g;
                float fc = k * h;
                this.vertex(vertexConsumer, ab + 0.0F, bb + p, cb + 0.0F, dc, ec, fc, fb, gb, cc);
                this.vertex(vertexConsumer, ab + 0.0F, bb + r, cb + 1.0F, dc, ec, fc, hb, ib, cc);
                this.vertex(vertexConsumer, ab + 1.0F, bb + q, cb + 1.0F, dc, ec, fc, jb, kb, cc);
                this.vertex(vertexConsumer, ab + 1.0F, bb + o, cb + 0.0F, dc, ec, fc, lb, mb, cc);
                if (fluidState.canFlowTo(world, pos.up())) {
                    this.vertex(vertexConsumer, ab + 0.0F, bb + p, cb + 0.0F, dc, ec, fc, fb, gb, cc);
                    this.vertex(vertexConsumer, ab + 1.0F, bb + o, cb + 0.0F, dc, ec, fc, lb, mb, cc);
                    this.vertex(vertexConsumer, ab + 1.0F, bb + q, cb + 1.0F, dc, ec, fc, jb, kb, cc);
                    this.vertex(vertexConsumer, ab + 0.0F, bb + r, cb + 1.0F, dc, ec, fc, hb, ib, cc);
                }
            }

            if (bl3) {
                float gc = sprites[0].getMinU();
                float hc = sprites[0].getMaxU();
                float ic = sprites[0].getMinV();
                float jc = sprites[0].getMaxV();
                int kc = this.getLight(world, pos.down());
                float lc = j * f;
                float mc = j * g;
                float nc = j * h;
                this.vertex(vertexConsumer, ab, bb + eb, cb + 1.0F, lc, mc, nc, gc, jc, kc);
                this.vertex(vertexConsumer, ab, bb + eb, cb, lc, mc, nc, gc, ic, kc);
                this.vertex(vertexConsumer, ab + 1.0F, bb + eb, cb, lc, mc, nc, hc, ic, kc);
                this.vertex(vertexConsumer, ab + 1.0F, bb + eb, cb + 1.0F, lc, mc, nc, hc, jc, kc);
            }

            int oc = this.getLight(world, pos);

            for (Direction direction : Direction.Type.HORIZONTAL) {
                float pc;
                float qc;
                float rc;
                float tc;
                float sc;
                float uc;
                boolean bl8;
                switch (direction) {
                    case NORTH:
                        pc = p;
                        qc = o;
                        rc = ab;
                        sc = ab + 1.0F;
                        tc = cb + 0.001F;
                        uc = cb + 0.001F;
                        bl8 = bl4;
                        break;
                    case SOUTH:
                        pc = q;
                        qc = r;
                        rc = ab + 1.0F;
                        sc = ab;
                        tc = cb + 1.0F - 0.001F;
                        uc = cb + 1.0F - 0.001F;
                        bl8 = bl5;
                        break;
                    case WEST:
                        pc = r;
                        qc = p;
                        rc = ab + 0.001F;
                        sc = ab + 0.001F;
                        tc = cb + 1.0F;
                        uc = cb;
                        bl8 = bl6;
                        break;
                    default:
                        pc = o;
                        qc = q;
                        rc = ab + 1.0F - 0.001F;
                        sc = ab + 1.0F - 0.001F;
                        tc = cb;
                        uc = cb + 1.0F;
                        bl8 = bl7;
                }

                if (bl8 && !method_3344(direction, Math.max(pc, qc), world.getBlockState(pos.offset(direction)))) {
                    BlockPos blockPos = pos.offset(direction);
                    Sprite sprite3 = sprites[1];
                    if (!bl) {
                        Block block = world.getBlockState(blockPos).getBlock();
                        if (block instanceof TranslucentBlock || block instanceof LeavesBlock) {
                            sprite3 = this.waterOverlaySprite;
                        }
                    }

                    float nd = sprite3.getFrameU(0.0F);
                    float od = sprite3.getFrameU(0.5F);
                    float pd = sprite3.getFrameV((1.0F - pc) * 0.5F);
                    float qd = sprite3.getFrameV((1.0F - qc) * 0.5F);
                    float rd = sprite3.getFrameV(0.5F);
                    float sd = direction.getAxis() == Direction.Axis.Z ? l : m;
                    float td = k * sd * f;
                    float ud = k * sd * g;
                    float vd = k * sd * h;
                    this.vertex(vertexConsumer, rc, bb + pc, tc, td, ud, vd, nd, pd, oc);
                    this.vertex(vertexConsumer, sc, bb + qc, uc, td, ud, vd, od, qd, oc);
                    this.vertex(vertexConsumer, sc, bb + eb, uc, td, ud, vd, od, rd, oc);
                    this.vertex(vertexConsumer, rc, bb + eb, tc, td, ud, vd, nd, rd, oc);
                    if (sprite3 != this.waterOverlaySprite) {
                        this.vertex(vertexConsumer, rc, bb + eb, tc, td, ud, vd, nd, rd, oc);
                        this.vertex(vertexConsumer, sc, bb + eb, uc, td, ud, vd, od, rd, oc);
                        this.vertex(vertexConsumer, sc, bb + qc, uc, td, ud, vd, od, qd, oc);
                        this.vertex(vertexConsumer, rc, bb + pc, tc, td, ud, vd, nd, pd, oc);
                    }
                }
            }
        }
    }

    private float calculateFluidHeight(BlockRenderView world, Fluid fluid, float originHeight, float northSouthHeight, float eastWestHeight, BlockPos pos) {
        if (!(eastWestHeight >= 1.0F) && !(northSouthHeight >= 1.0F)) {
            float[] fs = new float[2];
            if (eastWestHeight > 0.0F || northSouthHeight > 0.0F) {
                float f = this.getFluidHeight(world, fluid, pos);
                if (f >= 1.0F) {
                    return 1.0F;
                }

                this.addHeight(fs, f);
            }

            this.addHeight(fs, originHeight);
            this.addHeight(fs, eastWestHeight);
            this.addHeight(fs, northSouthHeight);
            return fs[0] / fs[1];
        } else {
            return 1.0F;
        }
    }

    private void addHeight(float[] weightedAverageHeight, float height) {
        if (height >= 0.8F) {
            weightedAverageHeight[0] += height * 10.0F;
            weightedAverageHeight[1] += 10.0F;
        } else if (height >= 0.0F) {
            weightedAverageHeight[0] += height;
            weightedAverageHeight[1]++;
        }
    }

    private float getFluidHeight(BlockRenderView world, Fluid fluid, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        return this.getFluidHeight(world, fluid, pos, blockState, blockState.getFluidState());
    }

    private float getFluidHeight(BlockRenderView world, Fluid fluid, BlockPos pos, BlockState blockState, FluidState fluidState) {
        if (fluid.matchesType(fluidState.getFluid())) {
            BlockState blockState2 = world.getBlockState(pos.up());
            return fluid.matchesType(blockState2.getFluidState().getFluid()) ? 1.0F : fluidState.getHeight();
        } else {
            return !blockState.isSolid() ? 0.0F : -1.0F;
        }
    }

    private void vertex(VertexConsumer vertexConsumer, float f, float g, float h, float i, float j, float k, float l, float m, int n) {
        vertexConsumer.vertex(f, g, h).color(i, j, k, 1.0F).texture(l, m).light(n).normal(0.0F, 1.0F, 0.0F);
    }

    private int getLight(BlockRenderView world, BlockPos pos) {
        int i = WorldRenderer.getLightmapCoordinates(world, pos);
        int j = WorldRenderer.getLightmapCoordinates(world, pos.up());
        int k = i & (LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE | 15);
        int l = j & (LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE | 15);
        int m = i >> 16 & (LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE | 15);
        int n = j >> 16 & (LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE | 15);
        return (k > l ? k : l) | (m > n ? m : n) << 16;
    }
}

