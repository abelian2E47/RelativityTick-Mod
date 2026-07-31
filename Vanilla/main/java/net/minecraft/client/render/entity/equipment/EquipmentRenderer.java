package net.minecraft.client.render.entity.equipment;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.trim.ArmorTrim;
import net.minecraft.item.equipment.trim.ArmorTrimMaterial;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;

@Environment(EnvType.CLIENT)
public class EquipmentRenderer {
    private static final int field_54178 = 0;
    private final EquipmentModelLoader equipmentModelLoader;
    private final Function<EquipmentRenderer.LayerTextureKey, Identifier> layerTextures;
    private final Function<EquipmentRenderer.TrimSpriteKey, Sprite> trimSprites;

    public EquipmentRenderer(EquipmentModelLoader equipmentModelLoader, SpriteAtlasTexture armorTrimsAtlas) {
        this.equipmentModelLoader = equipmentModelLoader;
        this.layerTextures = Util.memoize(key -> key.layer.getFullTextureId(key.layerType));
        this.trimSprites = Util.memoize(key -> armorTrimsAtlas.getSprite(key.getTexture()));
    }

    public void render(
        EquipmentModel.LayerType layerType,
        RegistryKey<EquipmentAsset> assetKey,
        Model model,
        ItemStack stack,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light
    ) {
        this.render(layerType, assetKey, model, stack, matrices, vertexConsumers, light, null);
    }

    public void render(
        EquipmentModel.LayerType layerType,
        RegistryKey<EquipmentAsset> assetKey,
        Model model,
        ItemStack stack,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light,
        @Nullable Identifier texture
    ) {
        List<EquipmentModel.Layer> list = this.equipmentModelLoader.get(assetKey).getLayers(layerType);
        if (!list.isEmpty()) {
            int i = stack.isIn(ItemTags.DYEABLE) ? DyedColorComponent.getColor(stack, 0) : 0;
            boolean bl = stack.hasGlint();

            for (EquipmentModel.Layer layer : list) {
                int j = getDyeColor(layer, i);
                if (j != 0) {
                    Identifier identifier = layer.usePlayerTexture() && texture != null
                        ? texture
                        : this.layerTextures.apply(new EquipmentRenderer.LayerTextureKey(layerType, layer));
                    VertexConsumer vertexConsumer = ItemRenderer.getArmorGlintConsumer(vertexConsumers, RenderLayer.getArmorCutoutNoCull(identifier), bl);
                    model.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV, j);
                    bl = false;
                }
            }

            ArmorTrim armorTrim = stack.get(DataComponentTypes.TRIM);
            if (armorTrim != null) {
                Sprite sprite = this.trimSprites.apply(new EquipmentRenderer.TrimSpriteKey(armorTrim, layerType, assetKey));
                VertexConsumer vertexConsumer2 = sprite.getTextureSpecificVertexConsumer(
                    vertexConsumers.getBuffer(TexturedRenderLayers.getArmorTrims(armorTrim.pattern().value().decal()))
                );
                model.render(matrices, vertexConsumer2, light, OverlayTexture.DEFAULT_UV);
            }
        }
    }

    private static int getDyeColor(EquipmentModel.Layer layer, int dyeColor) {
        Optional<EquipmentModel.Dyeable> optional = layer.dyeable();
        if (optional.isPresent()) {
            int i = optional.get().colorWhenUndyed().map(ColorHelper::fullAlpha).orElse(0);
            return dyeColor != 0 ? dyeColor : i;
        } else {
            return -1;
        }
    }

    @Environment(EnvType.CLIENT)
    record LayerTextureKey(EquipmentModel.LayerType layerType, EquipmentModel.Layer layer) {
    }

    @Environment(EnvType.CLIENT)
    record TrimSpriteKey(ArmorTrim trim, EquipmentModel.LayerType layerType, RegistryKey<EquipmentAsset> equipmentAssetId) {
        private static String getAssetName(RegistryEntry<ArmorTrimMaterial> material, RegistryKey<EquipmentAsset> assetKey) {
            String string = material.value().overrideArmorAssets().get(assetKey);
            return string != null ? string : material.value().assetName();
        }

        public Identifier getTexture() {
            Identifier identifier = this.trim.pattern().value().assetId();
            String string = getAssetName(this.trim.material(), this.equipmentAssetId);
            return identifier.withPath(path -> "trims/entity/" + this.layerType.asString() + "/" + path + "_" + string);
        }
    }
}

