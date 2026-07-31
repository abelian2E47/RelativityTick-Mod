package net.minecraft.client.render.entity.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LlamaEntityModel;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.entity.state.LlamaEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.registry.RegistryKey;

@Environment(EnvType.CLIENT)
public class LlamaDecorFeatureRenderer extends FeatureRenderer<LlamaEntityRenderState, LlamaEntityModel> {
    private final LlamaEntityModel model;
    private final LlamaEntityModel babyModel;
    private final EquipmentRenderer equipmentRenderer;

    public LlamaDecorFeatureRenderer(
        FeatureRendererContext<LlamaEntityRenderState, LlamaEntityModel> context, LoadedEntityModels loader, EquipmentRenderer equipmentRenderer
    ) {
        super(context);
        this.equipmentRenderer = equipmentRenderer;
        this.model = new LlamaEntityModel(loader.getModelPart(EntityModelLayers.LLAMA_DECOR));
        this.babyModel = new LlamaEntityModel(loader.getModelPart(EntityModelLayers.LLAMA_BABY_DECOR));
    }

    public void render(
        MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, LlamaEntityRenderState llamaEntityRenderState, float f, float g
    ) {
        ItemStack itemStack = llamaEntityRenderState.bodyArmor;
        EquippableComponent equippableComponent = itemStack.get(DataComponentTypes.EQUIPPABLE);
        if (equippableComponent != null && equippableComponent.assetId().isPresent()) {
            this.render(matrixStack, vertexConsumerProvider, llamaEntityRenderState, itemStack, equippableComponent.assetId().get(), i);
        } else if (llamaEntityRenderState.trader) {
            this.render(matrixStack, vertexConsumerProvider, llamaEntityRenderState, ItemStack.EMPTY, EquipmentAssetKeys.TRADER_LLAMA, i);
        }
    }

    private void render(
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        LlamaEntityRenderState state,
        ItemStack stack,
        RegistryKey<EquipmentAsset> registryKey,
        int light
    ) {
        LlamaEntityModel llamaEntityModel = state.baby ? this.babyModel : this.model;
        llamaEntityModel.setAngles(state);
        this.equipmentRenderer.render(EquipmentModel.LayerType.LLAMA_BODY, registryKey, llamaEntityModel, stack, matrices, vertexConsumers, light);
    }
}

