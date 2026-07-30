package net.minecraft.client.sound;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public class EntityTrackingSoundInstance extends MovingSoundInstance {
    private final Entity entity;

    public EntityTrackingSoundInstance(SoundEvent sound, SoundCategory category, float volume, float pitch, Entity entity, long seed) {
        super(sound, category, Random.create(seed));
        this.volume = volume;
        this.pitch = pitch;
        this.entity = entity;
        this.x = (float)this.entity.getX();
        this.y = (float)this.entity.getY();
        this.z = (float)this.entity.getZ();
    }

    @Override
    public boolean canPlay() {
        return !this.entity.isSilent();
    }

    @Override
    public void tick() {
        if (this.entity.isRemoved()) {
            this.setDone();
        } else {
            this.x = (float)this.entity.getX();
            this.y = (float)this.entity.getY();
            this.z = (float)this.entity.getZ();
        }
    }
}

