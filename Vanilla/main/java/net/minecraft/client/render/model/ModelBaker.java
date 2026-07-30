package net.minecraft.client.render.model;

import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemAsset;
import net.minecraft.client.model.ModelNameSupplier;
import net.minecraft.client.model.SpriteGetter;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.render.item.model.MissingItemModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.AffineTransformation;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class ModelBaker {
    public static final SpriteIdentifier FIRE_0 = new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, Identifier.ofVanilla("block/fire_0"));
    public static final SpriteIdentifier FIRE_1 = new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, Identifier.ofVanilla("block/fire_1"));
    public static final SpriteIdentifier LAVA_FLOW = new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, Identifier.ofVanilla("block/lava_flow"));
    public static final SpriteIdentifier WATER_FLOW = new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, Identifier.ofVanilla("block/water_flow"));
    public static final SpriteIdentifier WATER_OVERLAY = new SpriteIdentifier(
        SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, Identifier.ofVanilla("block/water_overlay")
    );
    public static final SpriteIdentifier BANNER_BASE = new SpriteIdentifier(
        TexturedRenderLayers.BANNER_PATTERNS_ATLAS_TEXTURE, Identifier.ofVanilla("entity/banner_base")
    );
    public static final SpriteIdentifier SHIELD_BASE = new SpriteIdentifier(
        TexturedRenderLayers.SHIELD_PATTERNS_ATLAS_TEXTURE, Identifier.ofVanilla("entity/shield_base")
    );
    public static final SpriteIdentifier SHIELD_BASE_NO_PATTERN = new SpriteIdentifier(
        TexturedRenderLayers.SHIELD_PATTERNS_ATLAS_TEXTURE, Identifier.ofVanilla("entity/shield_base_nopattern")
    );
    public static final int field_32983 = 10;
    public static final List<Identifier> BLOCK_DESTRUCTION_STAGES = IntStream.range(0, 10)
        .mapToObj(stage -> Identifier.ofVanilla("block/destroy_stage_" + stage))
        .collect(Collectors.toList());
    public static final List<Identifier> BLOCK_DESTRUCTION_STAGE_TEXTURES = BLOCK_DESTRUCTION_STAGES.stream()
        .map(id -> id.withPath(path -> "textures/" + path + ".png"))
        .collect(Collectors.toList());
    public static final List<RenderLayer> BLOCK_DESTRUCTION_RENDER_LAYERS = BLOCK_DESTRUCTION_STAGE_TEXTURES.stream()
        .map(RenderLayer::getBlockBreaking)
        .collect(Collectors.toList());
    static final Logger LOGGER = LogUtils.getLogger();
    private final LoadedEntityModels entityModels;
    final Map<ModelBaker.BakedModelCacheKey, BakedModel> bakedModelCache = new HashMap<>();
    private final Map<ModelIdentifier, GroupableModel> blockModels;
    private final Map<Identifier, ItemAsset> itemAssets;
    final Map<Identifier, UnbakedModel> allModels;
    final UnbakedModel missingModel;

    public ModelBaker(
        LoadedEntityModels entityModels,
        Map<ModelIdentifier, GroupableModel> blockModels,
        Map<Identifier, ItemAsset> itemModels,
        Map<Identifier, UnbakedModel> allModels,
        UnbakedModel missingModel
    ) {
        this.entityModels = entityModels;
        this.blockModels = blockModels;
        this.itemAssets = itemModels;
        this.allModels = allModels;
        this.missingModel = missingModel;
    }

    public ModelBaker.BakedModels bake(ModelBaker.ErrorCollectingSpriteGetter spriteGetter) {
        BakedModel bakedModel = UnbakedModel.bake(this.missingModel, new ModelBaker.BakerImpl(spriteGetter, () -> "missing"), ModelRotation.X0_Y0);
        Map<ModelIdentifier, BakedModel> map = new HashMap<>(this.blockModels.size());
        this.blockModels.forEach((id, model) -> {
            try {
                BakedModel bakedModelx = model.bake(new ModelBaker.BakerImpl(spriteGetter, id::toString));
                map.put(id, bakedModelx);
            } catch (Exception exception) {
                LOGGER.warn("Unable to bake model: '{}': {}", id, exception);
            }
        });
        ItemModel itemModel = new MissingItemModel(bakedModel);
        Map<Identifier, ItemModel> map2 = new HashMap<>(this.itemAssets.size());
        Map<Identifier, ItemAsset.Properties> map3 = new HashMap<>(this.itemAssets.size());
        this.itemAssets.forEach((id, item) -> {
            ModelNameSupplier modelNameSupplier = () -> id + "#inventory";
            ModelBaker.BakerImpl bakerImpl = new ModelBaker.BakerImpl(spriteGetter, modelNameSupplier);
            ItemModel.BakeContext bakeContext = new ItemModel.BakeContext(bakerImpl, this.entityModels, itemModel);

            try {
                ItemModel itemModel2 = item.model().bake(bakeContext);
                map2.put(id, itemModel2);
                if (!item.properties().equals(ItemAsset.Properties.DEFAULT)) {
                    map3.put(id, item.properties());
                }
            } catch (Exception exception) {
                LOGGER.warn("Unable to bake item model: '{}'", id, exception);
            }
        });
        return new ModelBaker.BakedModels(bakedModel, map, itemModel, map2, map3);
    }

    @Environment(EnvType.CLIENT)
    record BakedModelCacheKey(Identifier id, AffineTransformation transformation, boolean isUvLocked) {
    }

    @Environment(EnvType.CLIENT)
    public record BakedModels(
        BakedModel missingModel,
        Map<ModelIdentifier, BakedModel> blockStateModels,
        ItemModel missingItemModel,
        Map<Identifier, ItemModel> itemStackModels,
        Map<Identifier, ItemAsset.Properties> itemProperties
    ) {
    }

    @Environment(EnvType.CLIENT)
    class BakerImpl implements Baker {
        private final ModelNameSupplier modelNameSupplier;
        private final SpriteGetter spriteGetter;

        BakerImpl(final ModelBaker.ErrorCollectingSpriteGetter spriteGetter, final ModelNameSupplier modelNameSupplier) {
            this.spriteGetter = spriteGetter.toSpriteGetter(modelNameSupplier);
            this.modelNameSupplier = modelNameSupplier;
        }

        @Override
        public SpriteGetter getSpriteGetter() {
            return this.spriteGetter;
        }

        private UnbakedModel getModel(Identifier id) {
            UnbakedModel unbakedModel = ModelBaker.this.allModels.get(id);
            if (unbakedModel == null) {
                ModelBaker.LOGGER.warn("Requested a model that was not discovered previously: {}", id);
                return ModelBaker.this.missingModel;
            } else {
                return unbakedModel;
            }
        }

        @Override
        public BakedModel bake(Identifier id, ModelBakeSettings settings) {
            ModelBaker.BakedModelCacheKey bakedModelCacheKey = new ModelBaker.BakedModelCacheKey(id, settings.getRotation(), settings.isUvLocked());
            BakedModel bakedModel = ModelBaker.this.bakedModelCache.get(bakedModelCacheKey);
            if (bakedModel != null) {
                return bakedModel;
            }

            UnbakedModel unbakedModel = this.getModel(id);
            BakedModel bakedModel2 = UnbakedModel.bake(unbakedModel, this, settings);
            ModelBaker.this.bakedModelCache.put(bakedModelCacheKey, bakedModel2);
            return bakedModel2;
        }

        @Override
        public ModelNameSupplier getModelNameSupplier() {
            return this.modelNameSupplier;
        }
    }

    @Environment(EnvType.CLIENT)
    public interface ErrorCollectingSpriteGetter {
        Sprite get(ModelNameSupplier modelNameSupplier, SpriteIdentifier spriteId);

        Sprite getMissing(ModelNameSupplier modelNameSupplier, String textureId);

        default SpriteGetter toSpriteGetter(final ModelNameSupplier modelNameSupplier) {
            return new SpriteGetter() {
                @Override
                public Sprite get(SpriteIdentifier spriteId) {
                    return ErrorCollectingSpriteGetter.this.get(modelNameSupplier, spriteId);
                }

                @Override
                public Sprite getMissing(String textureId) {
                    return ErrorCollectingSpriteGetter.this.getMissing(modelNameSupplier, textureId);
                }
            };
        }
    }
}

