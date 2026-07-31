package net.minecraft.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Box;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
public class Frustum {
    public static final int RECESSION_SCALE = 4;
    private final FrustumIntersection frustumIntersection = new FrustumIntersection();
    private final Matrix4f positionProjectionMatrix = new Matrix4f();
    /**
     * The vector corresponding to the direction toward the far plane of the frustum.
     */
    private Vector4f recession;
    private double x;
    private double y;
    private double z;

    public Frustum(Matrix4f positionMatrix, Matrix4f projectionMatrix) {
        this.init(positionMatrix, projectionMatrix);
    }

    public Frustum(Frustum frustum) {
        this.frustumIntersection.set(frustum.positionProjectionMatrix);
        this.positionProjectionMatrix.set(frustum.positionProjectionMatrix);
        this.x = frustum.x;
        this.y = frustum.y;
        this.z = frustum.z;
        this.recession = frustum.recession;
    }

    /**
     * Moves the frustum backwards until it entirely covers the cell containing the
     * current position in a cubic lattice with cell size {@code boxSize}.
     */
    public Frustum coverBoxAroundSetPosition(int boxSize) {
        double d = Math.floor(this.x / boxSize) * boxSize;
        double e = Math.floor(this.y / boxSize) * boxSize;
        double f = Math.floor(this.z / boxSize) * boxSize;
        double g = Math.ceil(this.x / boxSize) * boxSize;
        double h = Math.ceil(this.y / boxSize) * boxSize;

        for (double i = Math.ceil(this.z / boxSize) * boxSize;
            this.frustumIntersection
                    .intersectAab((float)(d - this.x), (float)(e - this.y), (float)(f - this.z), (float)(g - this.x), (float)(h - this.y), (float)(i - this.z))
                != -2;
            this.z = this.z - this.recession.z() * 4.0F
        ) {
            this.x = this.x - this.recession.x() * 4.0F;
            this.y = this.y - this.recession.y() * 4.0F;
        }

        return this;
    }

    public void setPosition(double cameraX, double cameraY, double cameraZ) {
        this.x = cameraX;
        this.y = cameraY;
        this.z = cameraZ;
    }

    /**
     * @implNote The upper-left 3x3 matrix of {@code positionMatrix * projectionMatrix}
     * should be orthogonal for {@link Frustum#recession} to be set to a meaningful value.
     */
    private void init(Matrix4f positionMatrix, Matrix4f projectionMatrix) {
        projectionMatrix.mul(positionMatrix, this.positionProjectionMatrix);
        this.frustumIntersection.set(this.positionProjectionMatrix);
        this.recession = this.positionProjectionMatrix.transformTranspose(new Vector4f(0.0F, 0.0F, 1.0F, 0.0F));
    }

    public boolean isVisible(Box box) {
        int i = this.intersectAab(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        return i == -2 || i == -1;
    }

    public int intersectAab(BlockBox box) {
        return this.intersectAab(box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX() + 1, box.getMaxY() + 1, box.getMaxZ() + 1);
    }

    private int intersectAab(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        float f = (float)(minX - this.x);
        float g = (float)(minY - this.y);
        float h = (float)(minZ - this.z);
        float i = (float)(maxX - this.x);
        float j = (float)(maxY - this.y);
        float k = (float)(maxZ - this.z);
        return this.frustumIntersection.intersectAab(f, g, h, i, j, k);
    }

    public Vector4f[] getBoundaryPoints() {
        Vector4f[] vector4fs = new Vector4f[]{
            new Vector4f(-1.0F, -1.0F, -1.0F, 1.0F),
            new Vector4f(1.0F, -1.0F, -1.0F, 1.0F),
            new Vector4f(1.0F, 1.0F, -1.0F, 1.0F),
            new Vector4f(-1.0F, 1.0F, -1.0F, 1.0F),
            new Vector4f(-1.0F, -1.0F, 1.0F, 1.0F),
            new Vector4f(1.0F, -1.0F, 1.0F, 1.0F),
            new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
            new Vector4f(-1.0F, 1.0F, 1.0F, 1.0F)
        };
        Matrix4f matrix4f = this.positionProjectionMatrix.invert(new Matrix4f());

        for (int i = 0; i < 8; i++) {
            matrix4f.transform(vector4fs[i]);
            vector4fs[i].div(vector4fs[i].w());
        }

        return vector4fs;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }
}

