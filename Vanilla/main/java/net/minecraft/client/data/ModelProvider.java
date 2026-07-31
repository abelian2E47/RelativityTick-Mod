package net.minecraft.client.data;

import com.google.gson.JsonElement;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.item.ItemAsset;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class ModelProvider implements DataProvider {
    private final DataOutput.PathResolver blockstatesPathResolver;
    private final DataOutput.PathResolver itemsPathResolver;
    private final DataOutput.PathResolver modelsPathResolver;

    public ModelProvider(DataOutput output) {
        this.blockstatesPathResolver = output.getResolver(DataOutput.OutputType.RESOURCE_PACK, "blockstates");
        this.itemsPathResolver = output.getResolver(DataOutput.OutputType.RESOURCE_PACK, "items");
        this.modelsPathResolver = output.getResolver(DataOutput.OutputType.RESOURCE_PACK, "models");
    }

    @Override
    public CompletableFuture<?> run(DataWriter writer) {
        ModelProvider.ItemAssets itemAssets = new ModelProvider.ItemAssets();
        ModelProvider.BlockStateSuppliers blockStateSuppliers = new ModelProvider.BlockStateSuppliers();
        ModelProvider.ModelSuppliers modelSuppliers = new ModelProvider.ModelSuppliers();
        new BlockStateModelGenerator(blockStateSuppliers, itemAssets, modelSuppliers).register();
        new ItemModelGenerator(itemAssets, modelSuppliers).register();
        blockStateSuppliers.validate();
        itemAssets.resolveAndValidate();
        return CompletableFuture.allOf(
            blockStateSuppliers.writeAllToPath(writer, this.blockstatesPathResolver),
            modelSuppliers.writeAllToPath(writer, this.modelsPathResolver),
            itemAssets.writeAllToPath(writer, this.itemsPathResolver)
        );
    }

    static <T> CompletableFuture<?> writeAllToPath(DataWriter writer, Function<T, Path> pathResolver, Map<T, ? extends Supplier<JsonElement>> idsToValues) {
        return DataProvider.writeAllToPath(writer, Supplier::get, pathResolver, idsToValues);
    }

    @Override
    public final String getName() {
        return "Model Definitions";
    }

    @Environment(EnvType.CLIENT)
    static class BlockStateSuppliers implements Consumer<BlockStateSupplier> {
        private final Map<Block, BlockStateSupplier> blockStateSuppliers = new HashMap<>();

        public void accept(BlockStateSupplier blockStateSupplier) {
            Block block = blockStateSupplier.getBlock();
            BlockStateSupplier blockStateSupplier2 = this.blockStateSuppliers.put(block, blockStateSupplier);
            if (blockStateSupplier2 != null) {
                throw new IllegalStateException("Duplicate blockstate definition for " + block);
            }
        }

        public void validate() {
            Stream<RegistryEntry.Reference<Block>> stream = Registries.BLOCK.streamEntries().filter(entry -> true);
            List<Identifier> list = stream.filter(entry -> !this.blockStateSuppliers.containsKey(entry.value()))
                .map(entryx -> entryx.registryKey().getValue())
                .toList();
            if (!list.isEmpty()) {
                throw new IllegalStateException("Missing blockstate definitions for: " + list);
            }
        }

        public CompletableFuture<?> writeAllToPath(DataWriter writer, DataOutput.PathResolver pathResolver) {
            return ModelProvider.writeAllToPath(
                writer, block -> pathResolver.resolveJson(block.getRegistryEntry().registryKey().getValue()), this.blockStateSuppliers
            );
        }
    }

    @Environment(EnvType.CLIENT)
    static class ItemAssets implements ItemModelOutput {
        private final Map<Item, ItemAsset> itemAssets = new HashMap<>();
        private final Map<Item, Item> aliasedAssets = new HashMap<>();

        @Override
        public void accept(Item item, ItemModel.Unbaked model) {
            this.accept(item, new ItemAsset(model, ItemAsset.Properties.DEFAULT));
        }

        private void accept(Item item, ItemAsset asset) {
            ItemAsset itemAsset = this.itemAssets.put(item, asset);
            if (itemAsset != null) {
                throw new IllegalStateException("Duplicate item model definition for " + item);
            }
        }

        @Override
        public void acceptAlias(Item base, Item alias) {
            this.aliasedAssets.put(alias, base);
        }

        public void resolveAndValidate() {
            Registries.ITEM.forEach(item -> {
                if (!this.aliasedAssets.containsKey(item)) {
                    if (item instanceof BlockItem blockItem && !this.itemAssets.containsKey(blockItem)) {
                        Identifier identifier = ModelIds.getBlockModelId(blockItem.getBlock());
                        this.accept(blockItem, ItemModels.basic(identifier));
                    }
                }
            });
            this.aliasedAssets.forEach((base, alias) -> {
                ItemAsset itemAsset = this.itemAssets.get(alias);
                if (itemAsset == null) {
                    throw new IllegalStateException("Missing donor: " + alias + " -> " + base);
                }

                this.accept(base, itemAsset);
            });
            List<Identifier> list = Registries.ITEM
                .streamEntries()
                .filter(entry -> !this.itemAssets.containsKey(entry.value()))
                .map(entryx -> entryx.registryKey().getValue())
                .toList();
            if (!list.isEmpty()) {
                throw new IllegalStateException("Missing item model definitions for: " + list);
            }
        }

        public CompletableFuture<?> writeAllToPath(DataWriter writer, DataOutput.PathResolver pathResolver) {
            return DataProvider.writeAllToPath(
                writer, ItemAsset.CODEC, item -> pathResolver.resolveJson(item.getRegistryEntry().registryKey().getValue()), this.itemAssets
            );
        }
    }

    @Environment(EnvType.CLIENT)
    static class ModelSuppliers implements BiConsumer<Identifier, ModelSupplier> {
        private final Map<Identifier, ModelSupplier> modelSuppliers = new HashMap<>();

        public void accept(Identifier identifier, ModelSupplier modelSupplier) {
            Supplier<JsonElement> supplier = this.modelSuppliers.put(identifier, modelSupplier);
            if (supplier != null) {
                throw new IllegalStateException("Duplicate model definition for " + identifier);
            }
        }

        public CompletableFuture<?> writeAllToPath(DataWriter writer, DataOutput.PathResolver pathResolver) {
            return ModelProvider.writeAllToPath(writer, pathResolver::resolveJson, this.modelSuppliers);
        }
    }
}

