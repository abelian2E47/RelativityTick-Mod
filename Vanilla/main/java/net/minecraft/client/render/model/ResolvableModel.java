package net.minecraft.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public interface ResolvableModel {
    void resolve(ResolvableModel.Resolver resolver);

    @Environment(EnvType.CLIENT)
    interface Resolver {
        UnbakedModel resolve(Identifier id);
    }
}

