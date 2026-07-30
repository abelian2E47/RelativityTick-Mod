package net.minecraft.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;

@Environment(EnvType.CLIENT)
public class BakedQuad {
    protected final int[] vertexData;
    protected final int tintIndex;
    protected final Direction face;
    protected final Sprite sprite;
    private final boolean shade;
    private final int lightEmission;

    public BakedQuad(int[] vertexData, int tintIndex, Direction face, Sprite sprite, boolean shade, int lightEmission) {
        this.vertexData = vertexData;
        this.tintIndex = tintIndex;
        this.face = face;
        this.sprite = sprite;
        this.shade = shade;
        this.lightEmission = lightEmission;
    }

    public Sprite getSprite() {
        return this.sprite;
    }

    public int[] getVertexData() {
        return this.vertexData;
    }

    public boolean hasTint() {
        return this.tintIndex != -1;
    }

    public int getTintIndex() {
        return this.tintIndex;
    }

    public Direction getFace() {
        return this.face;
    }

    public boolean hasShade() {
        return this.shade;
    }

    public int getLightEmission() {
        return this.lightEmission;
    }
}

