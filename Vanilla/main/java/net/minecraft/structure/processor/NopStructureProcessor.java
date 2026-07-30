package net.minecraft.structure.processor;

import com.mojang.serialization.MapCodec;

public class NopStructureProcessor extends StructureProcessor {
    public static final MapCodec<NopStructureProcessor> CODEC = MapCodec.unit(() -> NopStructureProcessor.INSTANCE);
    public static final NopStructureProcessor INSTANCE = new NopStructureProcessor();

    private NopStructureProcessor() {
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return StructureProcessorType.NOP;
    }
}

