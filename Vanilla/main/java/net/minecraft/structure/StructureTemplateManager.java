package net.minecraft.structure;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.test.StructureTestUtil;
import net.minecraft.util.FixedBufferInputStream;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.PathUtil;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.level.storage.LevelStorage;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class StructureTemplateManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String STRUCTURE_DIRECTORY = "structure";
    private static final String STRUCTURES_DIRECTORY = "structures";
    private static final String NBT_FILE_EXTENSION = ".nbt";
    private static final String SNBT_FILE_EXTENSION = ".snbt";
    private final Map<Identifier, Optional<StructureTemplate>> templates = Maps.newConcurrentMap();
    private final DataFixer dataFixer;
    private ResourceManager resourceManager;
    private final Path generatedPath;
    private final List<StructureTemplateManager.Provider> providers;
    private final RegistryEntryLookup<Block> blockLookup;
    private static final ResourceFinder STRUCTURE_NBT_RESOURCE_FINDER = new ResourceFinder("structure", ".nbt");

    public StructureTemplateManager(ResourceManager resourceManager, LevelStorage.Session session, DataFixer dataFixer, RegistryEntryLookup<Block> blockLookup) {
        this.resourceManager = resourceManager;
        this.dataFixer = dataFixer;
        this.generatedPath = session.getDirectory(WorldSavePath.GENERATED).normalize();
        this.blockLookup = blockLookup;
        Builder<StructureTemplateManager.Provider> builder = ImmutableList.builder();
        builder.add(new StructureTemplateManager.Provider(this::loadTemplateFromFile, this::streamTemplatesFromFile));
        if (SharedConstants.isDevelopment) {
            builder.add(new StructureTemplateManager.Provider(this::loadTemplateFromGameTestFile, this::streamTemplatesFromGameTestFile));
        }

        builder.add(new StructureTemplateManager.Provider(this::loadTemplateFromResource, this::streamTemplatesFromResource));
        this.providers = builder.build();
    }

    public StructureTemplate getTemplateOrBlank(Identifier id) {
        Optional<StructureTemplate> optional = this.getTemplate(id);
        if (optional.isPresent()) {
            return optional.get();
        }

        StructureTemplate structureTemplate = new StructureTemplate();
        this.templates.put(id, Optional.of(structureTemplate));
        return structureTemplate;
    }

    public Optional<StructureTemplate> getTemplate(Identifier id) {
        return this.templates.computeIfAbsent(id, this::loadTemplate);
    }

    public Stream<Identifier> streamTemplates() {
        return this.providers.stream().flatMap(provider -> provider.lister().get()).distinct();
    }

    private Optional<StructureTemplate> loadTemplate(Identifier id) {
        for (StructureTemplateManager.Provider provider : this.providers) {
            try {
                Optional<StructureTemplate> optional = provider.loader().apply(id);
                if (optional.isPresent()) {
                    return optional;
                }
            } catch (Exception var5) {
            }
        }

        return Optional.empty();
    }

    public void setResourceManager(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
        this.templates.clear();
    }

    private Optional<StructureTemplate> loadTemplateFromResource(Identifier id) {
        Identifier identifier = STRUCTURE_NBT_RESOURCE_FINDER.toResourcePath(id);
        return this.loadTemplate(() -> this.resourceManager.open(identifier), throwable -> LOGGER.error("Couldn't load structure {}", id, throwable));
    }

    private Stream<Identifier> streamTemplatesFromResource() {
        return STRUCTURE_NBT_RESOURCE_FINDER.findResources(this.resourceManager).keySet().stream().map(STRUCTURE_NBT_RESOURCE_FINDER::toResourceId);
    }

    private Optional<StructureTemplate> loadTemplateFromGameTestFile(Identifier id) {
        return this.loadTemplateFromSnbt(id, Paths.get(StructureTestUtil.testStructuresDirectoryName));
    }

    private Stream<Identifier> streamTemplatesFromGameTestFile() {
        Path path = Paths.get(StructureTestUtil.testStructuresDirectoryName);
        if (!Files.isDirectory(path)) {
            return Stream.empty();
        }

        List<Identifier> list = new ArrayList<>();
        this.streamTemplates(path, "minecraft", ".snbt", list::add);
        return list.stream();
    }

    private Optional<StructureTemplate> loadTemplateFromFile(Identifier id) {
        if (!Files.isDirectory(this.generatedPath)) {
            return Optional.empty();
        }

        Path path = this.getTemplatePath(id, ".nbt");
        return this.loadTemplate(() -> new FileInputStream(path.toFile()), throwable -> LOGGER.error("Couldn't load structure from {}", path, throwable));
    }

    private Stream<Identifier> streamTemplatesFromFile() {
        if (!Files.isDirectory(this.generatedPath)) {
            return Stream.empty();
        }

        try {
            List<Identifier> list = new ArrayList<>();

            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(this.generatedPath, pathx -> Files.isDirectory(pathx))) {
                for (Path path : directoryStream) {
                    String string = path.getFileName().toString();
                    Path path2 = path.resolve("structures");
                    this.streamTemplates(path2, string, ".nbt", list::add);
                }
            }

            return list.stream();
        } catch (IOException iOException) {
            return Stream.empty();
        }
    }

    private void streamTemplates(Path directory, String namespace, String fileExtension, Consumer<Identifier> idConsumer) {
        int i = fileExtension.length();
        Function<String, String> function = filename -> filename.substring(0, filename.length() - i);

        try (Stream<Path> stream = Files.find(
                directory, Integer.MAX_VALUE, (path, attributes) -> attributes.isRegularFile() && path.toString().endsWith(fileExtension)
            )) {
            stream.forEach(path -> {
                try {
                    idConsumer.accept(Identifier.of(namespace, function.apply(this.toRelativePath(directory, path))));
                } catch (InvalidIdentifierException invalidIdentifierException) {
                    LOGGER.error("Invalid location while listing folder {} contents", directory, invalidIdentifierException);
                }
            });
        } catch (IOException iOException) {
            LOGGER.error("Failed to list folder {} contents", directory, iOException);
        }
    }

    private String toRelativePath(Path root, Path path) {
        return root.relativize(path).toString().replace(File.separator, "/");
    }

    private Optional<StructureTemplate> loadTemplateFromSnbt(Identifier id, Path path) {
        if (!Files.isDirectory(path)) {
            return Optional.empty();
        }

        Path path2 = PathUtil.getResourcePath(path, id.getPath(), ".snbt");

        try (BufferedReader bufferedReader = Files.newBufferedReader(path2)) {
            String string = IOUtils.toString(bufferedReader);
            return Optional.of(this.createTemplate(NbtHelper.fromNbtProviderString(string)));
        } catch (NoSuchFileException noSuchFileException) {
            return Optional.empty();
        } catch (IOException | CommandSyntaxException exception) {
            LOGGER.error("Couldn't load structure from {}", path2, exception);
            return Optional.empty();
        }
    }

    private Optional<StructureTemplate> loadTemplate(StructureTemplateManager.TemplateFileOpener opener, Consumer<Throwable> exceptionConsumer) {
        try (
            InputStream inputStream = opener.open();
            InputStream inputStream2 = new FixedBufferInputStream(inputStream);
        ) {
            return Optional.of(this.readTemplate(inputStream2));
        } catch (FileNotFoundException fileNotFoundException) {
            return Optional.empty();
        } catch (Throwable throwable) {
            exceptionConsumer.accept(throwable);
            return Optional.empty();
        }
    }

    private StructureTemplate readTemplate(InputStream templateIInputStream) throws IOException {
        NbtCompound nbtCompound = NbtIo.readCompressed(templateIInputStream, NbtSizeTracker.ofUnlimitedBytes());
        return this.createTemplate(nbtCompound);
    }

    public StructureTemplate createTemplate(NbtCompound nbt) {
        StructureTemplate structureTemplate = new StructureTemplate();
        int i = NbtHelper.getDataVersion(nbt, 500);
        structureTemplate.readNbt(this.blockLookup, DataFixTypes.STRUCTURE.update(this.dataFixer, nbt, i));
        return structureTemplate;
    }

    public boolean saveTemplate(Identifier id) {
        Optional<StructureTemplate> optional = this.templates.get(id);
        if (optional.isEmpty()) {
            return false;
        }

        StructureTemplate structureTemplate = optional.get();
        Path path = this.getTemplatePath(id, ".nbt");
        Path path2 = path.getParent();
        if (path2 == null) {
            return false;
        }

        try {
            Files.createDirectories(Files.exists(path2) ? path2.toRealPath() : path2);
        } catch (IOException iOException) {
            LOGGER.error("Failed to create parent directory: {}", path2);
            return false;
        }

        NbtCompound nbtCompound = structureTemplate.writeNbt(new NbtCompound());

        try (OutputStream outputStream = new FileOutputStream(path.toFile())) {
            NbtIo.writeCompressed(nbtCompound, outputStream);
            return true;
        } catch (Throwable throwable) {
            return false;
        }
    }

    public Path getTemplatePath(Identifier id, String extension) {
        if (id.getPath().contains("//")) {
            throw new InvalidIdentifierException("Invalid resource path: " + id);
        }

        try {
            Path path = this.generatedPath.resolve(id.getNamespace());
            Path path2 = path.resolve("structures");
            Path path3 = PathUtil.getResourcePath(path2, id.getPath(), extension);
            if (path3.startsWith(this.generatedPath) && PathUtil.isNormal(path3) && PathUtil.isAllowedName(path3)) {
                return path3;
            } else {
                throw new InvalidIdentifierException("Invalid resource path: " + path3);
            }
        } catch (InvalidPathException invalidPathException) {
            throw new InvalidIdentifierException("Invalid resource path: " + id, invalidPathException);
        }
    }

    public void unloadTemplate(Identifier id) {
        this.templates.remove(id);
    }

    record Provider(Function<Identifier, Optional<StructureTemplate>> loader, Supplier<Stream<Identifier>> lister) {
    }

    @FunctionalInterface
    interface TemplateFileOpener {
        InputStream open() throws IOException;
    }
}

