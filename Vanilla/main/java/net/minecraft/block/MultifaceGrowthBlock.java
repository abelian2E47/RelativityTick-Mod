package net.minecraft.block;

import com.mojang.serialization.MapCodec;

public abstract class MultifaceGrowthBlock extends MultifaceBlock {
    public MultifaceGrowthBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public abstract MapCodec<? extends MultifaceGrowthBlock> getCodec();

    public abstract MultifaceGrower getGrower();
}

