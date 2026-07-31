package net.minecraft.client.data;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.item.Item;

@Environment(EnvType.CLIENT)
public interface ItemModelOutput {
    void accept(Item item, ItemModel.Unbaked model);

    void acceptAlias(Item base, Item alias);
}

