package net.minecraft.data;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.data.advancement.vanilla.VanillaAdvancementProviders;
import net.minecraft.data.dev.NbtProvider;
import net.minecraft.data.loottable.rebalance.TradeRebalanceLootTableProviders;
import net.minecraft.data.loottable.vanilla.VanillaLootTableProviders;
import net.minecraft.data.recipe.VanillaRecipeGenerator;
import net.minecraft.data.report.BiomeParametersProvider;
import net.minecraft.data.report.BlockListProvider;
import net.minecraft.data.report.CommandSyntaxProvider;
import net.minecraft.data.report.DataPackStructureProvider;
import net.minecraft.data.report.ItemListProvider;
import net.minecraft.data.report.PacketReportProvider;
import net.minecraft.data.report.RegistryDumpProvider;
import net.minecraft.data.tag.TagProvider;
import net.minecraft.data.tag.rebalance.TradeRebalanceEnchantmentTagProvider;
import net.minecraft.data.tag.rebalance.TradeRebalanceStructureTagProvider;
import net.minecraft.data.tag.vanilla.VanillaBannerPatternTagProvider;
import net.minecraft.data.tag.vanilla.VanillaBiomeTagProvider;
import net.minecraft.data.tag.vanilla.VanillaBlockTagProvider;
import net.minecraft.data.tag.vanilla.VanillaCatVariantTagProvider;
import net.minecraft.data.tag.vanilla.VanillaDamageTypeTagProvider;
import net.minecraft.data.tag.vanilla.VanillaEnchantmentTagProvider;
import net.minecraft.data.tag.vanilla.VanillaEntityTypeTagProvider;
import net.minecraft.data.tag.vanilla.VanillaFlatLevelGeneratorPresetTagProvider;
import net.minecraft.data.tag.vanilla.VanillaFluidTagProvider;
import net.minecraft.data.tag.vanilla.VanillaGameEventTagProvider;
import net.minecraft.data.tag.vanilla.VanillaInstrumentTagProvider;
import net.minecraft.data.tag.vanilla.VanillaItemTagProvider;
import net.minecraft.data.tag.vanilla.VanillaPaintingVariantTagProvider;
import net.minecraft.data.tag.vanilla.VanillaPointOfInterestTypeTagProvider;
import net.minecraft.data.tag.vanilla.VanillaStructureTagProvider;
import net.minecraft.data.tag.vanilla.VanillaWorldPresetTagProvider;
import net.minecraft.data.validate.StructureValidatorProvider;
import net.minecraft.item.Item;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.RegistryBuilder;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.TradeRebalanceBuiltinRegistries;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.annotation.UsesSystemOut;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.Structure;

public class Main {
    @UsesSystemOut(a = "System.out needed before bootstrap")
    @DontObfuscate
    public static void main(String[] args) throws IOException {
        SharedConstants.createGameVersion();
        OptionParser optionParser = new OptionParser();
        OptionSpec<Void> optionSpec = optionParser.accepts("help", "Show the help menu").forHelp();
        OptionSpec<Void> optionSpec2 = optionParser.accepts("server", "Include server generators");
        OptionSpec<Void> optionSpec3 = optionParser.accepts("dev", "Include development tools");
        OptionSpec<Void> optionSpec4 = optionParser.accepts("reports", "Include data reports");
        optionParser.accepts("validate", "Validate inputs");
        OptionSpec<Void> optionSpec5 = optionParser.accepts("all", "Include all generators");
        OptionSpec<String> optionSpec6 = optionParser.accepts("output", "Output folder").withRequiredArg().defaultsTo("generated");
        OptionSpec<String> optionSpec7 = optionParser.accepts("input", "Input folder").withRequiredArg();
        OptionSet optionSet = optionParser.parse(args);
        if (!optionSet.has(optionSpec) && optionSet.hasOptions()) {
            Path path = Paths.get(optionSpec6.value(optionSet));
            boolean bl = optionSet.has(optionSpec5);
            boolean bl2 = bl || optionSet.has(optionSpec2);
            boolean bl3 = bl || optionSet.has(optionSpec3);
            boolean bl4 = bl || optionSet.has(optionSpec4);
            Collection<Path> collection = optionSet.valuesOf(optionSpec7).stream().map(input -> Paths.get(input)).toList();
            DataGenerator dataGenerator = new DataGenerator(path, SharedConstants.getGameVersion(), true);
            create(dataGenerator, collection, bl2, bl3, bl4);
            dataGenerator.run();
        } else {
            optionParser.printHelpOn(System.out);
        }
    }

    private static <T extends DataProvider> DataProvider.Factory<T> toFactory(
        BiFunction<DataOutput, CompletableFuture<RegistryWrapper.WrapperLookup>, T> baseFactory,
        CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture
    ) {
        return output -> baseFactory.apply(output, registriesFuture);
    }

    public static void create(DataGenerator dataGenerator, Collection<Path> inputs, boolean includeClient, boolean includeServer, boolean includeDev) {
        DataGenerator.Pack pack = dataGenerator.createVanillaPack(includeClient);
        pack.addProvider(output -> new SnbtProvider(output, inputs).addWriter(new StructureValidatorProvider()));
        CompletableFuture<RegistryWrapper.WrapperLookup> completableFuture = CompletableFuture.supplyAsync(
            BuiltinRegistries::createWrapperLookup, Util.getMainWorkerExecutor()
        );
        DataGenerator.Pack pack2 = dataGenerator.createVanillaPack(includeClient);
        pack2.addProvider(toFactory(DynamicRegistriesProvider::new, completableFuture));
        pack2.addProvider(toFactory(VanillaAdvancementProviders::createVanillaProvider, completableFuture));
        pack2.addProvider(toFactory(VanillaLootTableProviders::createVanillaProvider, completableFuture));
        pack2.addProvider(toFactory(VanillaRecipeGenerator.Provider::new, completableFuture));
        TagProvider<Block> tagProvider = pack2.addProvider(toFactory(VanillaBlockTagProvider::new, completableFuture));
        TagProvider<Item> tagProvider2 = pack2.addProvider(output -> new VanillaItemTagProvider(output, completableFuture, tagProvider.getTagLookupFuture()));
        TagProvider<Biome> tagProvider3 = pack2.addProvider(toFactory(VanillaBiomeTagProvider::new, completableFuture));
        TagProvider<BannerPattern> tagProvider4 = pack2.addProvider(toFactory(VanillaBannerPatternTagProvider::new, completableFuture));
        TagProvider<Structure> tagProvider5 = pack2.addProvider(toFactory(VanillaStructureTagProvider::new, completableFuture));
        pack2.addProvider(toFactory(VanillaCatVariantTagProvider::new, completableFuture));
        pack2.addProvider(toFactory(VanillaDamageTypeTagProvider::new, completableFuture));
        pack2.addProvider(toFactory(VanillaEntityTypeTagProvider::new, completableFuture));
        pack2.addProvider(toFactory(VanillaFlatLevelGeneratorPresetTagProvider::new, completableFuture));
        pack2.addProvider(toFactory(VanillaFluidTagProvider::new, completableFuture));
        pack2.addProvider(toFactory(VanillaGameEventTagProvider::new, completableFuture));
        pack2.addProvider(toFactory(VanillaInstrumentTagProvider::new, completableFuture));
        pack2.addProvider(toFactory(VanillaPaintingVariantTagProvider::new, completableFuture));
        pack2.addProvider(toFactory(VanillaPointOfInterestTypeTagProvider::new, completableFuture));
        pack2.addProvider(toFactory(VanillaWorldPresetTagProvider::new, completableFuture));
        pack2.addProvider(toFactory(VanillaEnchantmentTagProvider::new, completableFuture));
        pack2 = dataGenerator.createVanillaPack(includeServer);
        pack2.addProvider(output -> new NbtProvider(output, inputs));
        pack2 = dataGenerator.createVanillaPack(includeDev);
        pack2.addProvider(toFactory(BiomeParametersProvider::new, completableFuture));
        pack2.addProvider(toFactory(ItemListProvider::new, completableFuture));
        pack2.addProvider(toFactory(BlockListProvider::new, completableFuture));
        pack2.addProvider(toFactory(CommandSyntaxProvider::new, completableFuture));
        pack2.addProvider(RegistryDumpProvider::new);
        pack2.addProvider(PacketReportProvider::new);
        pack2.addProvider(DataPackStructureProvider::new);
        CompletableFuture<RegistryBuilder.FullPatchesRegistriesPair> completableFuture2 = TradeRebalanceBuiltinRegistries.validate(completableFuture);
        CompletableFuture<RegistryWrapper.WrapperLookup> completableFuture3 = completableFuture2.thenApply(RegistryBuilder.FullPatchesRegistriesPair::patches);
        DataGenerator.Pack pack5 = dataGenerator.createVanillaSubPack(includeClient, "trade_rebalance");
        pack5.addProvider(toFactory(DynamicRegistriesProvider::new, completableFuture3));
        pack5.addProvider(
            output -> MetadataProvider.create(output, Text.translatable("dataPack.trade_rebalance.description"), FeatureSet.of(FeatureFlags.TRADE_REBALANCE))
        );
        pack5.addProvider(toFactory(TradeRebalanceLootTableProviders::createTradeRebalanceProvider, completableFuture));
        pack5.addProvider(toFactory(TradeRebalanceStructureTagProvider::new, completableFuture));
        pack5.addProvider(toFactory(TradeRebalanceEnchantmentTagProvider::new, completableFuture));
        pack2 = dataGenerator.createVanillaSubPack(includeClient, "redstone_experiments");
        pack2.addProvider(
            output -> MetadataProvider.create(
                output, Text.translatable("dataPack.redstone_experiments.description"), FeatureSet.of(FeatureFlags.REDSTONE_EXPERIMENTS)
            )
        );
        pack2 = dataGenerator.createVanillaSubPack(includeClient, "minecart_improvements");
        pack2.addProvider(
            output -> MetadataProvider.create(
                output, Text.translatable("dataPack.minecart_improvements.description"), FeatureSet.of(FeatureFlags.MINECART_IMPROVEMENTS)
            )
        );
    }
}

