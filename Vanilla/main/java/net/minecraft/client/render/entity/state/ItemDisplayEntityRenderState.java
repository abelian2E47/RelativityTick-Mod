package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.ItemRenderState;

@Environment(EnvType.CLIENT)
public class ItemDisplayEntityRenderState extends DisplayEntityRenderState {
    public final ItemRenderState itemRenderState = new ItemRenderState();

    @Override
    public boolean canRender() {
        return !this.itemRenderState.isEmpty();
    }
}

