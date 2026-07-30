package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;

@Environment(EnvType.CLIENT)
public class ItemStackEntityRenderState extends EntityRenderState {
    public final ItemRenderState itemRenderState = new ItemRenderState();
    public int renderedAmount;
    public int seed;

    public void update(Entity entity, ItemStack stack, ItemModelManager itemModelManager) {
        itemModelManager.updateForNonLivingEntity(this.itemRenderState, stack, ModelTransformationMode.GROUND, entity);
        this.renderedAmount = getRenderedAmount(stack.getCount());
        this.seed = getSeed(stack);
    }

    public static int getSeed(ItemStack stack) {
        return stack.isEmpty() ? 187 : Item.getRawId(stack.getItem()) + stack.getDamage();
    }

    public static int getRenderedAmount(int count) {
        if (count <= 1) {
            return 1;
        } else if (count <= 16) {
            return 2;
        } else if (count <= 32) {
            return 3;
        } else {
            return count <= 48 ? 4 : 5;
        }
    }
}

