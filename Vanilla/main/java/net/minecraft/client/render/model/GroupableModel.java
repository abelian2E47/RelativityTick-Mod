package net.minecraft.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;

@Environment(EnvType.CLIENT)
public interface GroupableModel extends ResolvableModel {
    BakedModel bake(Baker baker);

    Object getEqualityGroup(BlockState state);
}

