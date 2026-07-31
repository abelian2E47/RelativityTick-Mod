package net.minecraft.util.math.noise;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

public class SimplexNoiseSampler {
    protected static final int[][] GRADIENTS = new int[][]{
        {1, 1, 0},
        {-1, 1, 0},
        {1, -1, 0},
        {-1, -1, 0},
        {1, 0, 1},
        {-1, 0, 1},
        {1, 0, -1},
        {-1, 0, -1},
        {0, 1, 1},
        {0, -1, 1},
        {0, 1, -1},
        {0, -1, -1},
        {1, 1, 0},
        {0, -1, 1},
        {-1, 1, 0},
        {0, -1, -1}
    };
    private static final double SQRT_3 = Math.sqrt(3.0);
    private static final double SKEW_FACTOR_2D = 0.5 * (SQRT_3 - 1.0);
    private static final double UNSKEW_FACTOR_2D = (3.0 - SQRT_3) / 6.0;
    private final int[] permutation = new int[512];
    public final double originX;
    public final double originY;
    public final double originZ;

    public SimplexNoiseSampler(Random random) {
        this.originX = random.nextDouble() * 256.0;
        this.originY = random.nextDouble() * 256.0;
        this.originZ = random.nextDouble() * 256.0;
        int i = 0;

        while (i < 256) {
            this.permutation[i] = i++;
        }

        for (int j = 0; j < 256; j++) {
            int k = random.nextInt(256 - j);
            int l = this.permutation[j];
            this.permutation[j] = this.permutation[k + j];
            this.permutation[k + j] = l;
        }
    }

    private int map(int input) {
        return this.permutation[input & 0xFF];
    }

    protected static double dot(int[] gradient, double x, double y, double z) {
        return gradient[0] * x + gradient[1] * y + gradient[2] * z;
    }

    private double grad(int hash, double x, double y, double z, double distance) {
        double d = distance - x * x - y * y - z * z;
        double e;
        if (d < 0.0) {
            e = 0.0;
        } else {
            d *= d;
            e = d * d * dot(GRADIENTS[hash], x, y, z);
        }

        return e;
    }

    public double sample(double x, double y) {
        double d = (x + y) * SKEW_FACTOR_2D;
        int i = MathHelper.floor(x + d);
        int j = MathHelper.floor(y + d);
        double e = (i + j) * UNSKEW_FACTOR_2D;
        double f = i - e;
        double g = j - e;
        double h = x - f;
        double k = y - g;
        int l;
        int m;
        if (h > k) {
            l = 1;
            m = 0;
        } else {
            l = 0;
            m = 1;
        }

        double p = h - l + UNSKEW_FACTOR_2D;
        double q = k - m + UNSKEW_FACTOR_2D;
        double r = h - 1.0 + 2.0 * UNSKEW_FACTOR_2D;
        double s = k - 1.0 + 2.0 * UNSKEW_FACTOR_2D;
        int t = i & 0xFF;
        int u = j & 0xFF;
        int v = this.map(t + this.map(u)) % 12;
        int w = this.map(t + l + this.map(u + m)) % 12;
        int z = this.map(t + 1 + this.map(u + 1)) % 12;
        double ab = this.grad(v, h, k, 0.0, 0.5);
        double bb = this.grad(w, p, q, 0.0, 0.5);
        double cb = this.grad(z, r, s, 0.0, 0.5);
        return 70.0 * (ab + bb + cb);
    }

    public double sample(double x, double y, double z) {
        double d = 0.3333333333333333;
        double e = (x + y + z) * 0.3333333333333333;
        int i = MathHelper.floor(x + e);
        int j = MathHelper.floor(y + e);
        int k = MathHelper.floor(z + e);
        double f = 0.16666666666666666;
        double g = (i + j + k) * 0.16666666666666666;
        double h = i - g;
        double l = j - g;
        double m = k - g;
        double n = x - h;
        double o = y - l;
        double p = z - m;
        int q;
        int r;
        int s;
        int t;
        int u;
        int v;
        if (n >= o) {
            if (o >= p) {
                q = 1;
                r = 0;
                s = 0;
                t = 1;
                u = 1;
                v = 0;
            } else if (n >= p) {
                q = 1;
                r = 0;
                s = 0;
                t = 1;
                u = 0;
                v = 1;
            } else {
                q = 0;
                r = 0;
                s = 1;
                t = 1;
                u = 0;
                v = 1;
            }
        } else if (o < p) {
            q = 0;
            r = 0;
            s = 1;
            t = 0;
            u = 1;
            v = 1;
        } else if (n < p) {
            q = 0;
            r = 1;
            s = 0;
            t = 0;
            u = 1;
            v = 1;
        } else {
            q = 0;
            r = 1;
            s = 0;
            t = 1;
            u = 1;
            v = 0;
        }

        double dc = n - q + 0.16666666666666666;
        double ec = o - r + 0.16666666666666666;
        double fc = p - s + 0.16666666666666666;
        double gc = n - t + 0.3333333333333333;
        double hc = o - u + 0.3333333333333333;
        double ic = p - v + 0.3333333333333333;
        double jc = n - 1.0 + 0.5;
        double kc = o - 1.0 + 0.5;
        double lc = p - 1.0 + 0.5;
        int mc = i & 0xFF;
        int nc = j & 0xFF;
        int oc = k & 0xFF;
        int pc = this.map(mc + this.map(nc + this.map(oc))) % 12;
        int qc = this.map(mc + q + this.map(nc + r + this.map(oc + s))) % 12;
        int rc = this.map(mc + t + this.map(nc + u + this.map(oc + v))) % 12;
        int sc = this.map(mc + 1 + this.map(nc + 1 + this.map(oc + 1))) % 12;
        double tc = this.grad(pc, n, o, p, 0.6);
        double uc = this.grad(qc, dc, ec, fc, 0.6);
        double vc = this.grad(rc, gc, hc, ic, 0.6);
        double wc = this.grad(sc, jc, kc, lc, 0.6);
        return 32.0 * (tc + uc + vc + wc);
    }
}

