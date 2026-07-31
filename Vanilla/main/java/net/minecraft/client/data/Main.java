package net.minecraft.client.data;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.client.ClientBootstrap;
import net.minecraft.data.DataGenerator;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.util.annotation.UsesSystemOut;

@Environment(EnvType.CLIENT)
public class Main {
    @DontObfuscate
    @UsesSystemOut(a = "System.out needed before bootstrap")
    public static void main(String[] args) throws IOException {
        SharedConstants.createGameVersion();
        OptionParser optionParser = new OptionParser();
        OptionSpec<Void> optionSpec = optionParser.accepts("help", "Show the help menu").forHelp();
        OptionSpec<Void> optionSpec2 = optionParser.accepts("client", "Include client generators");
        OptionSpec<Void> optionSpec3 = optionParser.accepts("all", "Include all generators");
        OptionSpec<String> optionSpec4 = optionParser.accepts("output", "Output folder").withRequiredArg().defaultsTo("generated");
        OptionSet optionSet = optionParser.parse(args);
        if (!optionSet.has(optionSpec) && optionSet.hasOptions()) {
            Path path = Paths.get(optionSpec4.value(optionSet));
            boolean bl = optionSet.has(optionSpec3);
            boolean bl2 = bl || optionSet.has(optionSpec2);
            Bootstrap.initialize();
            ClientBootstrap.initialize();
            DataGenerator dataGenerator = new DataGenerator(path, SharedConstants.getGameVersion(), true);
            create(dataGenerator, bl2);
            dataGenerator.run();
        } else {
            optionParser.printHelpOn(System.out);
        }
    }

    public static void create(DataGenerator dataGenerator, boolean includeClient) {
        DataGenerator.Pack pack = dataGenerator.createVanillaPack(includeClient);
        pack.addProvider(ModelProvider::new);
        pack.addProvider(EquipmentAssetProvider::new);
    }
}

