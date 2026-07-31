package net.minecraft.client.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;

@Environment(EnvType.CLIENT)
public interface SpriteGetter {
    Sprite get(SpriteIdentifier spriteId);

    Sprite getMissing(String textureId);
}

