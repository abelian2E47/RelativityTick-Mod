package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.ItemRenderState;

@Environment(EnvType.CLIENT)
public class FlyingItemEntityRenderState extends EntityRenderState {
    public final ItemRenderState itemRenderState = new ItemRenderState();
}

