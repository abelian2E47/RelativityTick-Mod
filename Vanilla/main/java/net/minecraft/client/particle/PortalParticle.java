package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

@Environment(EnvType.CLIENT)
public class PortalParticle extends SpriteBillboardParticle {
    private final double startX;
    private final double startY;
    private final double startZ;

    protected PortalParticle(ClientWorld clientWorld, double d, double e, double f, double g, double h, double i) {
        super(clientWorld, d, e, f);
        this.velocityX = g;
        this.velocityY = h;
        this.velocityZ = i;
        this.x = d;
        this.y = e;
        this.z = f;
        this.startX = this.x;
        this.startY = this.y;
        this.startZ = this.z;
        this.scale = 0.1F * (this.random.nextFloat() * 0.2F + 0.5F);
        float j = this.random.nextFloat() * 0.6F + 0.4F;
        this.red = j * 0.9F;
        this.green = j * 0.3F;
        this.blue = j;
        this.maxAge = (int)(Math.random() * 10.0) + 40;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public void move(double dx, double dy, double dz) {
        this.setBoundingBox(this.getBoundingBox().offset(dx, dy, dz));
        this.repositionFromBoundingBox();
    }

    @Override
    public float getSize(float tickDelta) {
        float f = (this.age + tickDelta) / this.maxAge;
        f = 1.0F - f;
        f *= f;
        f = 1.0F - f;
        return this.scale * f;
    }

    @Override
    public int getBrightness(float tint) {
        int i = super.getBrightness(tint);
        float f = (float)this.age / this.maxAge;
        f *= f;
        f *= f;
        int j = i & 0xFF;
        int k = i >> 16 & 0xFF;
        k += (int)(f * 15.0F * 16.0F);
        if (k > 240) {
            k = 240;
        }

        return j | k << 16;
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;
        if (this.age++ >= this.maxAge) {
            this.markDead();
        } else {
            float f = (float)this.age / this.maxAge;
            float g = f;
            f = -f + f * f * 2.0F;
            f = 1.0F - f;
            this.x = this.startX + this.velocityX * f;
            this.y = this.startY + this.velocityY * f + (1.0F - g);
            this.z = this.startZ + this.velocityZ * f;
        }
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        public Particle createParticle(
            SimpleParticleType simpleParticleType, ClientWorld clientWorld, double d, double e, double f, double g, double h, double i
        ) {
            PortalParticle portalParticle = new PortalParticle(clientWorld, d, e, f, g, h, i);
            portalParticle.setSprite(this.spriteProvider);
            return portalParticle;
        }
    }
}

