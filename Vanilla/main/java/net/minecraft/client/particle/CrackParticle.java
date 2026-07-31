package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.SimpleParticleType;

@Environment(EnvType.CLIENT)
public class CrackParticle extends SpriteBillboardParticle {
    private final float sampleU;
    private final float sampleV;

    CrackParticle(ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, ItemRenderState itemRenderState) {
        this(world, x, y, z, itemRenderState);
        this.velocityX *= 0.1F;
        this.velocityY *= 0.1F;
        this.velocityZ *= 0.1F;
        this.velocityX += velocityX;
        this.velocityY += velocityY;
        this.velocityZ += velocityZ;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.TERRAIN_SHEET;
    }

    protected CrackParticle(ClientWorld world, double x, double y, double z, ItemRenderState itemRenderState) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        Sprite sprite = itemRenderState.getParticleSprite(this.random);
        if (sprite != null) {
            this.setSprite(sprite);
        } else {
            this.setSprite(MinecraftClient.getInstance().getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).apply(MissingSprite.getMissingSpriteId()));
        }

        this.gravityStrength = 1.0F;
        this.scale /= 2.0F;
        this.sampleU = this.random.nextFloat() * 3.0F;
        this.sampleV = this.random.nextFloat() * 3.0F;
    }

    @Override
    protected float getMinU() {
        return this.sprite.getFrameU((this.sampleU + 1.0F) / 4.0F);
    }

    @Override
    protected float getMaxU() {
        return this.sprite.getFrameU(this.sampleU / 4.0F);
    }

    @Override
    protected float getMinV() {
        return this.sprite.getFrameV(this.sampleV / 4.0F);
    }

    @Override
    protected float getMaxV() {
        return this.sprite.getFrameV((this.sampleV + 1.0F) / 4.0F);
    }

    @Environment(EnvType.CLIENT)
    public static class CobwebFactory extends CrackParticle.Factory<SimpleParticleType> {
        public Particle createParticle(
            SimpleParticleType simpleParticleType, ClientWorld clientWorld, double d, double e, double f, double g, double h, double i
        ) {
            return new CrackParticle(clientWorld, d, e, f, this.getItemRenderState(new ItemStack(Items.COBWEB), clientWorld));
        }
    }

    @Environment(EnvType.CLIENT)
    public abstract static class Factory<T extends ParticleEffect> implements ParticleFactory<T> {
        private final ItemRenderState itemRenderState = new ItemRenderState();

        protected ItemRenderState getItemRenderState(ItemStack stack, ClientWorld world) {
            MinecraftClient.getInstance().getItemModelManager().update(this.itemRenderState, stack, ModelTransformationMode.GROUND, false, world, null, 0);
            return this.itemRenderState;
        }
    }

    @Environment(EnvType.CLIENT)
    public static class ItemFactory extends CrackParticle.Factory<ItemStackParticleEffect> {
        public Particle createParticle(
            ItemStackParticleEffect itemStackParticleEffect, ClientWorld clientWorld, double d, double e, double f, double g, double h, double i
        ) {
            return new CrackParticle(clientWorld, d, e, f, g, h, i, this.getItemRenderState(itemStackParticleEffect.getItemStack(), clientWorld));
        }
    }

    @Environment(EnvType.CLIENT)
    public static class SlimeballFactory extends CrackParticle.Factory<SimpleParticleType> {
        public Particle createParticle(
            SimpleParticleType simpleParticleType, ClientWorld clientWorld, double d, double e, double f, double g, double h, double i
        ) {
            return new CrackParticle(clientWorld, d, e, f, this.getItemRenderState(new ItemStack(Items.SLIME_BALL), clientWorld));
        }
    }

    @Environment(EnvType.CLIENT)
    public static class SnowballFactory extends CrackParticle.Factory<SimpleParticleType> {
        public Particle createParticle(
            SimpleParticleType simpleParticleType, ClientWorld clientWorld, double d, double e, double f, double g, double h, double i
        ) {
            return new CrackParticle(clientWorld, d, e, f, this.getItemRenderState(new ItemStack(Items.SNOWBALL), clientWorld));
        }
    }
}

