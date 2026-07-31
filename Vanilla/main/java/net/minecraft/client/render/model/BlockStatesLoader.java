package net.minecraft.client.render.model;

import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.render.model.json.ModelVariantMap;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.Util;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class BlockStatesLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceFinder FINDER = ResourceFinder.json("blockstates");
    private static final String MAP_KEY = "map";
    private static final String MAP_TRUE_VARIANT = "map=true";
    private static final String MAP_FALSE_VARIANT = "map=false";
    private static final StateManager<Block, BlockState> ITEM_FRAME_STATE_MANAGER = new StateManager.Builder<Block, BlockState>(Blocks.AIR)
        .add(BooleanProperty.of("map"))
        .build(Block::getDefaultState, BlockState::new);
    private static final Identifier GLOW_ITEM_FRAME_ID = Identifier.ofVanilla("glow_item_frame");
    private static final Identifier ITEM_FRAME_ID = Identifier.ofVanilla("item_frame");
    private static final Map<Identifier, StateManager<Block, BlockState>> STATIC_DEFINITIONS = Map.of(
        ITEM_FRAME_ID, ITEM_FRAME_STATE_MANAGER, GLOW_ITEM_FRAME_ID, ITEM_FRAME_STATE_MANAGER
    );
    public static final ModelIdentifier MAP_GLOW_ITEM_FRAME_MODEL_ID = new ModelIdentifier(GLOW_ITEM_FRAME_ID, "map=true");
    public static final ModelIdentifier GLOW_ITEM_FRAME_MODEL_ID = new ModelIdentifier(GLOW_ITEM_FRAME_ID, "map=false");
    public static final ModelIdentifier MAP_ITEM_FRAME_MODEL_ID = new ModelIdentifier(ITEM_FRAME_ID, "map=true");
    public static final ModelIdentifier ITEM_FRAME_MODEL_ID = new ModelIdentifier(ITEM_FRAME_ID, "map=false");

    private static Function<Identifier, StateManager<Block, BlockState>> getIdToStatesConverter() {
        Map<Identifier, StateManager<Block, BlockState>> map = new HashMap<>(STATIC_DEFINITIONS);

        for (Block block : Registries.BLOCK) {
            map.put(block.getRegistryEntry().registryKey().getValue(), block.getStateManager());
        }

        return map::get;
    }

    public static CompletableFuture<BlockStatesLoader.BlockStateDefinition> load(UnbakedModel missingModel, ResourceManager resourceManager, Executor executor) {
        Function<Identifier, StateManager<Block, BlockState>> function = getIdToStatesConverter();
        return CompletableFuture.<Map<Identifier, List<Resource>>>supplyAsync(() -> FINDER.findAllResources(resourceManager), executor)
            .thenCompose(resources -> {
                List<CompletableFuture<BlockStatesLoader.BlockStateDefinition>> list = new ArrayList<>(resources.size());

                for (Entry<Identifier, List<Resource>> entry : resources.entrySet()) {
                    list.add(CompletableFuture.supplyAsync(() -> {
                        Identifier identifier = FINDER.toResourceId(entry.getKey());
                        StateManager<Block, BlockState> stateManager = function.apply(identifier);
                        if (stateManager == null) {
                            LOGGER.debug("Discovered unknown block state definition {}, ignoring", identifier);
                            return null;
                        }

                        List<Resource> listx = entry.getValue();
                        List<BlockStatesLoader.PackBlockStateDefinition> list2 = new ArrayList<>(listx.size());

                        for (Resource resource : listx) {
                            try (Reader reader = resource.getReader()) {
                                JsonObject jsonObject = JsonHelper.deserialize(reader);
                                ModelVariantMap modelVariantMap = ModelVariantMap.fromJson(jsonObject);
                                list2.add(new BlockStatesLoader.PackBlockStateDefinition(resource.getPackId(), modelVariantMap));
                            } catch (Exception exception) {
                                LOGGER.error("Failed to load blockstate definition {} from pack {}", identifier, resource.getPackId(), exception);
                            }
                        }

                        try {
                            return combine(identifier, stateManager, list2, missingModel);
                        } catch (Exception exception2) {
                            LOGGER.error("Failed to load blockstate definition {}", identifier, exception2);
                            return null;
                        }
                    }, executor));
                }

                return Util.combineSafe(list).thenApply(definitions -> {
                    Map<ModelIdentifier, BlockStatesLoader.BlockModel> map = new HashMap<>();

                    for (BlockStatesLoader.BlockStateDefinition blockStateDefinition : definitions) {
                        if (blockStateDefinition != null) {
                            map.putAll(blockStateDefinition.models());
                        }
                    }

                    return new BlockStatesLoader.BlockStateDefinition(map);
                });
            });
    }

    private static BlockStatesLoader.BlockStateDefinition combine(
        Identifier id, StateManager<Block, BlockState> stateManager, List<BlockStatesLoader.PackBlockStateDefinition> definitions, UnbakedModel missingModel
    ) {
        Map<ModelIdentifier, BlockStatesLoader.BlockModel> map = new HashMap<>();

        for (BlockStatesLoader.PackBlockStateDefinition packBlockStateDefinition : definitions) {
            packBlockStateDefinition.contents.parse(stateManager, id + "/" + packBlockStateDefinition.source).forEach((state, model) -> {
                ModelIdentifier modelIdentifier = BlockModels.getModelId(id, state);
                map.put(modelIdentifier, new BlockStatesLoader.BlockModel(state, model));
            });
        }

        return new BlockStatesLoader.BlockStateDefinition(map);
    }

    @Environment(EnvType.CLIENT)
    public record BlockModel(BlockState state, GroupableModel model) {
    }

    @Environment(EnvType.CLIENT)
    public record BlockStateDefinition(Map<ModelIdentifier, BlockStatesLoader.BlockModel> models) {
        public Stream<ResolvableModel> streamModels() {
            return this.models.values().stream().map(BlockStatesLoader.BlockModel::model);
        }

        public Map<ModelIdentifier, GroupableModel> getModels() {
            return Maps.transformValues(this.models, BlockStatesLoader.BlockModel::model);
        }
    }

    @Environment(EnvType.CLIENT)
    record PackBlockStateDefinition(String source, ModelVariantMap contents) {
    }
}

