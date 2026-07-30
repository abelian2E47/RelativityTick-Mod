package net.minecraft.world.gen.noise;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.util.math.noise.InterpolatedNoiseSampler;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;
import net.minecraft.world.gen.surfacebuilder.SurfaceBuilder;

public final class NoiseConfig {
    final RandomSplitter randomDeriver;
    private final RegistryEntryLookup<DoublePerlinNoiseSampler.NoiseParameters> noiseParametersRegistry;
    private final NoiseRouter noiseRouter;
    private final MultiNoiseUtil.MultiNoiseSampler multiNoiseSampler;
    private final SurfaceBuilder surfaceBuilder;
    private final RandomSplitter aquiferRandomDeriver;
    private final RandomSplitter oreRandomDeriver;
    private final Map<RegistryKey<DoublePerlinNoiseSampler.NoiseParameters>, DoublePerlinNoiseSampler> noises;
    private final Map<Identifier, RandomSplitter> randomDerivers;

    public static NoiseConfig create(
        RegistryEntryLookup.RegistryLookup registryLookup, RegistryKey<ChunkGeneratorSettings> chunkGeneratorSettingsKey, long legacyWorldSeed
    ) {
        return create(
            registryLookup.getOrThrow(RegistryKeys.CHUNK_GENERATOR_SETTINGS).getOrThrow(chunkGeneratorSettingsKey).value(),
            registryLookup.getOrThrow(RegistryKeys.NOISE_PARAMETERS),
            legacyWorldSeed
        );
    }

    public static NoiseConfig create(
        ChunkGeneratorSettings chunkGeneratorSettings,
        RegistryEntryLookup<DoublePerlinNoiseSampler.NoiseParameters> noiseParametersLookup,
        long legacyWorldSeed
    ) {
        return new NoiseConfig(chunkGeneratorSettings, noiseParametersLookup, legacyWorldSeed);
    }

    private NoiseConfig(
        ChunkGeneratorSettings chunkGeneratorSettings, RegistryEntryLookup<DoublePerlinNoiseSampler.NoiseParameters> noiseParametersLookup, final long seed
    ) {
        this.randomDeriver = chunkGeneratorSettings.getRandomProvider().create(seed).nextSplitter();
        this.noiseParametersRegistry = noiseParametersLookup;
        this.aquiferRandomDeriver = this.randomDeriver.split(Identifier.ofVanilla("aquifer")).nextSplitter();
        this.oreRandomDeriver = this.randomDeriver.split(Identifier.ofVanilla("ore")).nextSplitter();
        this.noises = new ConcurrentHashMap<>();
        this.randomDerivers = new ConcurrentHashMap<>();
        this.surfaceBuilder = new SurfaceBuilder(this, chunkGeneratorSettings.defaultBlock(), chunkGeneratorSettings.seaLevel(), this.randomDeriver);
        final boolean bl = chunkGeneratorSettings.usesLegacyRandom();

        class LegacyNoiseDensityFunctionVisitor implements DensityFunction.DensityFunctionVisitor {
            private final Map<DensityFunction, DensityFunction> cache = new HashMap<>();

            private Random createRandom(long seedx) {
                return new CheckedRandom(seed + seed);
            }

            @Override
            public DensityFunction.Noise apply(DensityFunction.Noise noiseDensityFunction) {
                RegistryEntry<DoublePerlinNoiseSampler.NoiseParameters> registryEntry = noiseDensityFunction.noiseData();
                if (bl) {
                    if (registryEntry.matchesKey(NoiseParametersKeys.TEMPERATURE)) {
                        DoublePerlinNoiseSampler doublePerlinNoiseSampler = DoublePerlinNoiseSampler.createLegacy(
                            this.createRandom(0L), new DoublePerlinNoiseSampler.NoiseParameters(-7, 1.0, 1.0)
                        );
                        return new DensityFunction.Noise(registryEntry, doublePerlinNoiseSampler);
                    }

                    if (registryEntry.matchesKey(NoiseParametersKeys.VEGETATION)) {
                        DoublePerlinNoiseSampler doublePerlinNoiseSampler2 = DoublePerlinNoiseSampler.createLegacy(
                            this.createRandom(1L), new DoublePerlinNoiseSampler.NoiseParameters(-7, 1.0, 1.0)
                        );
                        return new DensityFunction.Noise(registryEntry, doublePerlinNoiseSampler2);
                    }

                    if (registryEntry.matchesKey(NoiseParametersKeys.OFFSET)) {
                        DoublePerlinNoiseSampler doublePerlinNoiseSampler3 = DoublePerlinNoiseSampler.create(
                            NoiseConfig.this.randomDeriver.split(NoiseParametersKeys.OFFSET.getValue()), new DoublePerlinNoiseSampler.NoiseParameters(0, 0.0)
                        );
                        return new DensityFunction.Noise(registryEntry, doublePerlinNoiseSampler3);
                    }
                }

                DoublePerlinNoiseSampler doublePerlinNoiseSampler4 = NoiseConfig.this.getOrCreateSampler(registryEntry.getKey().orElseThrow());
                return new DensityFunction.Noise(registryEntry, doublePerlinNoiseSampler4);
            }

            private DensityFunction applyNotCached(DensityFunction densityFunction) {
                if (densityFunction instanceof InterpolatedNoiseSampler interpolatedNoiseSampler) {
                    Random random = bl ? this.createRandom(0L) : NoiseConfig.this.randomDeriver.split(Identifier.ofVanilla("terrain"));
                    return interpolatedNoiseSampler.copyWithRandom(random);
                } else {
                    return densityFunction instanceof DensityFunctionTypes.EndIslands ? new DensityFunctionTypes.EndIslands(seed) : densityFunction;
                }
            }

            @Override
            public DensityFunction apply(DensityFunction densityFunction) {
                return this.cache.computeIfAbsent(densityFunction, this::applyNotCached);
            }
        }

        this.noiseRouter = chunkGeneratorSettings.noiseRouter().apply(new LegacyNoiseDensityFunctionVisitor());
        DensityFunction.DensityFunctionVisitor densityFunctionVisitor = new DensityFunction.DensityFunctionVisitor() {
            private final Map<DensityFunction, DensityFunction> unwrapped = new HashMap<>();

            private DensityFunction unwrap(DensityFunction densityFunction) {
                if (densityFunction instanceof DensityFunctionTypes.RegistryEntryHolder registryEntryHolder) {
                    return registryEntryHolder.function().value();
                } else {
                    return densityFunction instanceof DensityFunctionTypes.Wrapping wrapping ? wrapping.wrapped() : densityFunction;
                }
            }

            @Override
            public DensityFunction apply(DensityFunction densityFunction) {
                return this.unwrapped.computeIfAbsent(densityFunction, this::unwrap);
            }
        };
        this.multiNoiseSampler = new MultiNoiseUtil.MultiNoiseSampler(
            this.noiseRouter.temperature().apply(densityFunctionVisitor),
            this.noiseRouter.vegetation().apply(densityFunctionVisitor),
            this.noiseRouter.continents().apply(densityFunctionVisitor),
            this.noiseRouter.erosion().apply(densityFunctionVisitor),
            this.noiseRouter.depth().apply(densityFunctionVisitor),
            this.noiseRouter.ridges().apply(densityFunctionVisitor),
            chunkGeneratorSettings.spawnTarget()
        );
    }

    public DoublePerlinNoiseSampler getOrCreateSampler(RegistryKey<DoublePerlinNoiseSampler.NoiseParameters> noiseParametersKey) {
        return this.noises
            .computeIfAbsent(
                noiseParametersKey, key -> NoiseParametersKeys.createNoiseSampler(this.noiseParametersRegistry, this.randomDeriver, noiseParametersKey)
            );
    }

    public RandomSplitter getOrCreateRandomDeriver(Identifier id) {
        return this.randomDerivers.computeIfAbsent(id, id2 -> this.randomDeriver.split(id).nextSplitter());
    }

    public NoiseRouter getNoiseRouter() {
        return this.noiseRouter;
    }

    public MultiNoiseUtil.MultiNoiseSampler getMultiNoiseSampler() {
        return this.multiNoiseSampler;
    }

    public SurfaceBuilder getSurfaceBuilder() {
        return this.surfaceBuilder;
    }

    public RandomSplitter getAquiferRandomDeriver() {
        return this.aquiferRandomDeriver;
    }

    public RandomSplitter getOreRandomDeriver() {
        return this.oreRandomDeriver;
    }
}

