package net.minecraft.client.sound;

import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.MusicSound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public class MusicTracker {
    private static final int DEFAULT_TIME_UNTIL_NEXT_SONG = 100;
    private final Random random = Random.create();
    private final MinecraftClient client;
    @Nullable
    private SoundInstance current;
    private float volume = 1.0F;
    private int timeUntilNextSong = 100;

    public MusicTracker(MinecraftClient client) {
        this.client = client;
    }

    public void tick() {
        MusicInstance musicInstance = this.client.getMusicInstance();
        float f = musicInstance.volume();
        if (this.current != null && this.volume != f) {
            boolean bl = this.canFadeTowardsVolume(f);
            if (!bl) {
                return;
            }
        }

        MusicSound musicSound = musicInstance.music();
        if (musicSound == null) {
            this.timeUntilNextSong = Math.max(this.timeUntilNextSong, 100);
        } else {
            if (this.current != null) {
                if (musicInstance.shouldReplace(this.current)) {
                    this.client.getSoundManager().stop(this.current);
                    this.timeUntilNextSong = MathHelper.nextInt(this.random, 0, musicSound.getMinDelay() / 2);
                }

                if (!this.client.getSoundManager().isPlaying(this.current)) {
                    this.current = null;
                    this.timeUntilNextSong = Math.min(
                        this.timeUntilNextSong, MathHelper.nextInt(this.random, musicSound.getMinDelay(), musicSound.getMaxDelay())
                    );
                }
            }

            this.timeUntilNextSong = Math.min(this.timeUntilNextSong, musicSound.getMaxDelay());
            if (this.current == null && this.timeUntilNextSong-- <= 0) {
                this.play(musicInstance);
            }
        }
    }

    public void play(MusicInstance music) {
        this.current = PositionedSoundInstance.music(music.music().getSound().value());
        if (this.current.getSound() != SoundManager.MISSING_SOUND) {
            this.client.getSoundManager().play(this.current);
            this.client.getSoundManager().setVolume(this.current, music.volume());
        }

        this.timeUntilNextSong = Integer.MAX_VALUE;
        this.volume = music.volume();
    }

    public void stop(MusicSound type) {
        if (this.isPlayingType(type)) {
            this.stop();
        }
    }

    public void stop() {
        if (this.current != null) {
            this.client.getSoundManager().stop(this.current);
            this.current = null;
        }

        this.timeUntilNextSong += 100;
    }

    private boolean canFadeTowardsVolume(float volume) {
        if (this.current == null) {
            return false;
        }

        if (this.volume == volume) {
            return true;
        }

        if (this.volume < volume) {
            this.volume = this.volume + MathHelper.clamp(this.volume, 5.0E-4F, 0.005F);
            if (this.volume > volume) {
                this.volume = volume;
            }
        } else {
            this.volume = 0.03F * volume + 0.97F * this.volume;
            if (Math.abs(this.volume - volume) < 1.0E-4F || this.volume < volume) {
                this.volume = volume;
            }
        }

        this.volume = MathHelper.clamp(this.volume, 0.0F, 1.0F);
        if (this.volume <= 1.0E-4F) {
            this.stop();
            return false;
        } else {
            this.client.getSoundManager().setVolume(this.current, this.volume);
            return true;
        }
    }

    public boolean isPlayingType(MusicSound type) {
        return this.current == null ? false : type.getSound().value().id().equals(this.current.getId());
    }
}

