package net.minecraft.client.particle;

import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.SpriteAtlasTexture;

/**
 * Defines rendering setup and draw logic for particles based on their requirements for depth checking, textures, and transparency.
 * 
 * <p>
 * Each {@link Particle} returns a sheet in {@link Particle#getType()}.
 * When particles are rendered, each sheet will be drawn once.
 * {@link #begin(Tessellator, TextureManager)} is first called to set up render state.
 */
@Environment(EnvType.CLIENT)
public record ParticleTextureSheet(String name, @Nullable RenderLayer renderType) {
    public static final ParticleTextureSheet TERRAIN_SHEET = new ParticleTextureSheet(
        "TERRAIN_SHEET", RenderLayer.getTranslucentParticle(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
    );
    public static final ParticleTextureSheet PARTICLE_SHEET_OPAQUE = new ParticleTextureSheet(
        "PARTICLE_SHEET_OPAQUE", RenderLayer.getOpaqueParticle(SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE)
    );
    public static final ParticleTextureSheet PARTICLE_SHEET_TRANSLUCENT = new ParticleTextureSheet(
        "PARTICLE_SHEET_TRANSLUCENT", RenderLayer.getTranslucentParticle(SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE)
    );
    public static final ParticleTextureSheet CUSTOM = new ParticleTextureSheet("CUSTOM", null);
    public static final ParticleTextureSheet NO_RENDER = new ParticleTextureSheet("NO_RENDER", null);
}

