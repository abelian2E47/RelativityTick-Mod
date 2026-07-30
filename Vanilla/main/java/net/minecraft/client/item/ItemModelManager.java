package net.minecraft.client.item;

import java.util.function.Function;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public class ItemModelManager {
    private final Function<Identifier, ItemModel> modelGetter;
    private final Function<Identifier, ItemAsset.Properties> propertiesGetter;

    public ItemModelManager(BakedModelManager bakedModelManager) {
        this.modelGetter = bakedModelManager::getItemModel;
        this.propertiesGetter = bakedModelManager::getItemProperties;
    }

    public void updateForLivingEntity(
        ItemRenderState renderState, ItemStack stack, ModelTransformationMode transformationMode, boolean leftHand, LivingEntity entity
    ) {
        this.update(renderState, stack, transformationMode, leftHand, entity.getWorld(), entity, entity.getId() + transformationMode.ordinal());
    }

    public void updateForNonLivingEntity(ItemRenderState renderState, ItemStack stack, ModelTransformationMode transformationMode, Entity entity) {
        this.update(renderState, stack, transformationMode, false, entity.getWorld(), null, entity.getId());
    }

    public void update(
        ItemRenderState renderState,
        ItemStack stack,
        ModelTransformationMode transformationMode,
        boolean leftHand,
        @Nullable World world,
        @Nullable LivingEntity entity,
        int seed
    ) {
        renderState.clear();
        if (!stack.isEmpty()) {
            renderState.modelTransformationMode = transformationMode;
            renderState.leftHand = leftHand;
            this.update(renderState, stack, transformationMode, world, entity, seed);
        }
    }

    private static void resolveProfileComponent(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof AbstractSkullBlock) {
            ProfileComponent profileComponent = stack.get(DataComponentTypes.PROFILE);
            if (profileComponent != null && !profileComponent.isCompleted()) {
                stack.remove(DataComponentTypes.PROFILE);
                profileComponent.getFuture().thenAcceptAsync(profile -> stack.set(DataComponentTypes.PROFILE, profile), MinecraftClient.getInstance());
            }
        }
    }

    public void update(
        ItemRenderState renderState,
        ItemStack stack,
        ModelTransformationMode transformationMode,
        @Nullable World world,
        @Nullable LivingEntity entity,
        int seed
    ) {
        resolveProfileComponent(stack);
        Identifier identifier = stack.get(DataComponentTypes.ITEM_MODEL);
        if (identifier != null) {
            this.modelGetter
                .apply(identifier)
                .update(renderState, stack, this, transformationMode, world instanceof ClientWorld clientWorld ? clientWorld : null, entity, seed);
        }
    }

    public boolean hasHandAnimationOnSwap(ItemStack stack) {
        Identifier identifier = stack.get(DataComponentTypes.ITEM_MODEL);
        return identifier == null ? true : this.propertiesGetter.apply(identifier).handAnimationOnSwap();
    }
}

