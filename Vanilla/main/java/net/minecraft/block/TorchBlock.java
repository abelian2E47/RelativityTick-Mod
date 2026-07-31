package net.minecraft.block;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class TorchBlock extends AbstractTorchBlock {
    protected static final MapCodec<SimpleParticleType> PARTICLE_TYPE_CODEC = Registries.PARTICLE_TYPE
        .getCodec()
        .comapFlatMap(
            particleType -> particleType instanceof SimpleParticleType simpleParticleType
                ? DataResult.success(simpleParticleType)
                : DataResult.error(() -> "Not a SimpleParticleType: " + particleType),
            particleType -> (ParticleType<?>)particleType
        )
        .fieldOf("particle_options");
    public static final MapCodec<TorchBlock> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(PARTICLE_TYPE_CODEC.forGetter(block -> block.particle), createSettingsCodec()).apply(instance, TorchBlock::new)
    );
    protected final SimpleParticleType particle;

    @Override
    public MapCodec<? extends TorchBlock> getCodec() {
        return CODEC;
    }

    protected TorchBlock(SimpleParticleType particle, AbstractBlock.Settings settings) {
        super(settings);
        this.particle = particle;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        double d = pos.getX() + 0.5;
        double e = pos.getY() + 0.7;
        double f = pos.getZ() + 0.5;
        world.addParticle(ParticleTypes.SMOKE, d, e, f, 0.0, 0.0, 0.0);
        world.addParticle(this.particle, d, e, f, 0.0, 0.0, 0.0);
    }
}

