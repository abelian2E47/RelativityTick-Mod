package net.minecraft.client.sound;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.Camera;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.floatprovider.ConstantFloatProvider;
import net.minecraft.util.math.floatprovider.MultipliedFloatSupplier;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.ScopedProfiler;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class SoundManager extends SinglePreparationResourceReloader<SoundManager.SoundList> {
    public static final Identifier EMPTY_ID = Identifier.ofVanilla("empty");
    public static final Sound MISSING_SOUND = new Sound(
        EMPTY_ID, ConstantFloatProvider.create(1.0F), ConstantFloatProvider.create(1.0F), 1, Sound.RegistrationType.FILE, false, false, 16
    );
    public static final Identifier INTENTIONALLY_EMPTY_ID = Identifier.ofVanilla("intentionally_empty");
    public static final WeightedSoundSet INTENTIONALLY_EMPTY_SOUND_SET = new WeightedSoundSet(INTENTIONALLY_EMPTY_ID, null);
    public static final Sound INTENTIONALLY_EMPTY_SOUND = new Sound(
        INTENTIONALLY_EMPTY_ID, ConstantFloatProvider.create(1.0F), ConstantFloatProvider.create(1.0F), 1, Sound.RegistrationType.FILE, false, false, 16
    );
    static final Logger LOGGER = LogUtils.getLogger();
    private static final String SOUNDS_JSON = "sounds.json";
    private static final Gson GSON = new GsonBuilder()
        .registerTypeHierarchyAdapter(Text.class, new Text.Serializer(DynamicRegistryManager.EMPTY))
        .registerTypeAdapter(SoundEntry.class, new SoundEntryDeserializer())
        .create();
    private static final TypeToken<Map<String, SoundEntry>> TYPE = new TypeToken<Map<String, SoundEntry>>() {};
    private final Map<Identifier, WeightedSoundSet> sounds = Maps.newHashMap();
    private final SoundSystem soundSystem;
    private final Map<Identifier, Resource> soundResources = new HashMap<>();

    public SoundManager(GameOptions gameOptions) {
        this.soundSystem = new SoundSystem(this, gameOptions, ResourceFactory.fromMap(this.soundResources));
    }

    protected SoundManager.SoundList prepare(ResourceManager resourceManager, Profiler profiler) {
        SoundManager.SoundList soundList = new SoundManager.SoundList();

        try (ScopedProfiler scopedProfiler = profiler.scoped("list")) {
            soundList.findSounds(resourceManager);
        }

        for (String string : resourceManager.getAllNamespaces()) {
            try (ScopedProfiler scopedProfiler2 = profiler.scoped(string)) {
                for (Resource resource : resourceManager.getAllResources(Identifier.of(string, "sounds.json"))) {
                    profiler.push(resource.getPackId());

                    try (Reader reader = resource.getReader()) {
                        profiler.push("parse");
                        Map<String, SoundEntry> map = JsonHelper.deserialize(GSON, reader, TYPE);
                        profiler.swap("register");

                        for (Entry<String, SoundEntry> entry : map.entrySet()) {
                            soundList.register(Identifier.of(string, entry.getKey()), entry.getValue());
                        }

                        profiler.pop();
                    } catch (RuntimeException runtimeException) {
                        LOGGER.warn("Invalid {} in resourcepack: '{}'", "sounds.json", resource.getPackId(), runtimeException);
                    }

                    profiler.pop();
                }
            } catch (IOException var21) {
            }
        }

        return soundList;
    }

    protected void apply(SoundManager.SoundList soundList, ResourceManager resourceManager, Profiler profiler) {
        soundList.reload(this.sounds, this.soundResources, this.soundSystem);
        if (SharedConstants.isDevelopment) {
            for (Identifier identifier : this.sounds.keySet()) {
                WeightedSoundSet weightedSoundSet = this.sounds.get(identifier);
                if (!Texts.hasTranslation(weightedSoundSet.getSubtitle()) && Registries.SOUND_EVENT.containsId(identifier)) {
                    LOGGER.error("Missing subtitle {} for sound event: {}", weightedSoundSet.getSubtitle(), identifier);
                }
            }
        }

        if (LOGGER.isDebugEnabled()) {
            for (Identifier identifier2 : this.sounds.keySet()) {
                if (!Registries.SOUND_EVENT.containsId(identifier2)) {
                    LOGGER.debug("Not having sound event for: {}", identifier2);
                }
            }
        }

        this.soundSystem.reloadSounds();
    }

    public List<String> getSoundDevices() {
        return this.soundSystem.getSoundDevices();
    }

    public SoundListenerTransform getListenerTransform() {
        return this.soundSystem.getListenerTransform();
    }

    static boolean isSoundResourcePresent(Sound sound, Identifier id, ResourceFactory resourceFactory) {
        Identifier identifier = sound.getLocation();
        if (resourceFactory.getResource(identifier).isEmpty()) {
            LOGGER.warn("File {} does not exist, cannot add it to event {}", identifier, id);
            return false;
        } else {
            return true;
        }
    }

    @Nullable
    public WeightedSoundSet get(Identifier id) {
        return this.sounds.get(id);
    }

    public Collection<Identifier> getKeys() {
        return this.sounds.keySet();
    }

    public void playNextTick(TickableSoundInstance sound) {
        this.soundSystem.playNextTick(sound);
    }

    public void play(SoundInstance sound) {
        this.soundSystem.play(sound);
    }

    public void play(SoundInstance sound, int delay) {
        this.soundSystem.play(sound, delay);
    }

    public void updateListenerPosition(Camera camera) {
        this.soundSystem.updateListenerPosition(camera);
    }

    public void pauseAll() {
        this.soundSystem.pauseAll();
    }

    public void stopAll() {
        this.soundSystem.stopAll();
    }

    public void close() {
        this.soundSystem.stop();
    }

    public void stopAbruptly() {
        this.soundSystem.stopAbruptly();
    }

    public void tick(boolean paused) {
        this.soundSystem.tick(paused);
    }

    public void resumeAll() {
        this.soundSystem.resumeAll();
    }

    public void updateSoundVolume(SoundCategory category, float volume) {
        if (category == SoundCategory.MASTER && volume <= 0.0F) {
            this.stopAll();
        }

        this.soundSystem.updateSoundVolume(category, volume);
    }

    public void stop(SoundInstance sound) {
        this.soundSystem.stop(sound);
    }

    public void setVolume(SoundInstance sound, float volume) {
        this.soundSystem.setVolume(sound, volume);
    }

    public boolean isPlaying(SoundInstance sound) {
        return this.soundSystem.isPlaying(sound);
    }

    public void registerListener(SoundInstanceListener listener) {
        this.soundSystem.registerListener(listener);
    }

    public void unregisterListener(SoundInstanceListener listener) {
        this.soundSystem.unregisterListener(listener);
    }

    public void stopSounds(@Nullable Identifier id, @Nullable SoundCategory soundCategory) {
        this.soundSystem.stopSounds(id, soundCategory);
    }

    public String getDebugString() {
        return this.soundSystem.getDebugString();
    }

    public void reloadSounds() {
        this.soundSystem.reloadSounds();
    }

    @Environment(EnvType.CLIENT)
    protected static class SoundList {
        final Map<Identifier, WeightedSoundSet> loadedSounds = Maps.newHashMap();
        private Map<Identifier, Resource> foundSounds = Map.of();

        void findSounds(ResourceManager resourceManager) {
            this.foundSounds = Sound.FINDER.findResources(resourceManager);
        }

        void register(Identifier id, SoundEntry entry) {
            WeightedSoundSet weightedSoundSet = this.loadedSounds.get(id);
            boolean bl = weightedSoundSet == null;
            if (bl || entry.canReplace()) {
                if (!bl) {
                    SoundManager.LOGGER.debug("Replaced sound event location {}", id);
                }

                weightedSoundSet = new WeightedSoundSet(id, entry.getSubtitle());
                this.loadedSounds.put(id, weightedSoundSet);
            }

            ResourceFactory resourceFactory = ResourceFactory.fromMap(this.foundSounds);

            for (final Sound sound : entry.getSounds()) {
                final Identifier identifier = sound.getIdentifier();
                SoundContainer<Sound> soundContainer2;
                switch (sound.getRegistrationType()) {
                    case FILE:
                        if (!SoundManager.isSoundResourcePresent(sound, id, resourceFactory)) {
                            continue;
                        }

                        soundContainer2 = sound;
                        break;
                    case SOUND_EVENT:
                        soundContainer2 = new SoundContainer<Sound>() {
                            @Override
                            public int getWeight() {
                                WeightedSoundSet weightedSoundSetx = SoundList.this.loadedSounds.get(identifier);
                                return weightedSoundSetx == null ? 0 : weightedSoundSetx.getWeight();
                            }

                            public Sound getSound(Random random) {
                                WeightedSoundSet weightedSoundSetx = SoundList.this.loadedSounds.get(identifier);
                                if (weightedSoundSetx == null) {
                                    return SoundManager.MISSING_SOUND;
                                }

                                Sound soundx = weightedSoundSetx.getSound(random);
                                return new Sound(
                                    soundx.getIdentifier(),
                                    new MultipliedFloatSupplier(soundx.getVolume(), sound.getVolume()),
                                    new MultipliedFloatSupplier(soundx.getPitch(), sound.getPitch()),
                                    sound.getWeight(),
                                    Sound.RegistrationType.FILE,
                                    soundx.isStreamed() || sound.isStreamed(),
                                    soundx.isPreloaded(),
                                    soundx.getAttenuation()
                                );
                            }

                            @Override
                            public void preload(SoundSystem soundSystem) {
                                WeightedSoundSet weightedSoundSetx = SoundList.this.loadedSounds.get(identifier);
                                if (weightedSoundSetx != null) {
                                    weightedSoundSetx.preload(soundSystem);
                                }
                            }
                        };
                        break;
                    default:
                        throw new IllegalStateException("Unknown SoundEventRegistration type: " + sound.getRegistrationType());
                }

                weightedSoundSet.add(soundContainer2);
            }
        }

        public void reload(Map<Identifier, WeightedSoundSet> sounds, Map<Identifier, Resource> soundResources, SoundSystem system) {
            sounds.clear();
            soundResources.clear();
            soundResources.putAll(this.foundSounds);

            for (Entry<Identifier, WeightedSoundSet> entry : this.loadedSounds.entrySet()) {
                sounds.put(entry.getKey(), entry.getValue());
                entry.getValue().preload(system);
            }
        }
    }
}

