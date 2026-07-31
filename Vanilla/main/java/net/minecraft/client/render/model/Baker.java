package net.minecraft.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelNameSupplier;
import net.minecraft.client.model.SpriteGetter;
import net.minecraft.util.Identifier;
import net.minecraft.util.annotation.Debug;

@Environment(EnvType.CLIENT)
public interface Baker {
    BakedModel bake(Identifier id, ModelBakeSettings settings);

    SpriteGetter getSpriteGetter();

    @Debug
    ModelNameSupplier getModelNameSupplier();
}

