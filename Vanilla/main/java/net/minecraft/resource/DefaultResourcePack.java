package net.minecraft.resource;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.resource.metadata.ResourceMetadataMap;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;
import net.minecraft.util.Identifier;
import net.minecraft.util.PathUtil;
import org.slf4j.Logger;

public class DefaultResourcePack implements ResourcePack {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ResourcePackInfo info;
    private final ResourceMetadataMap metadata;
    private final Set<String> namespaces;
    private final List<Path> rootPaths;
    private final Map<ResourceType, List<Path>> namespacePaths;

    DefaultResourcePack(
        ResourcePackInfo info, ResourceMetadataMap metadata, Set<String> namespaces, List<Path> rootPaths, Map<ResourceType, List<Path>> namespacePaths
    ) {
        this.info = info;
        this.metadata = metadata;
        this.namespaces = namespaces;
        this.rootPaths = rootPaths;
        this.namespacePaths = namespacePaths;
    }

    @Nullable
    @Override
    public InputSupplier<InputStream> openRoot(String... segments) {
        PathUtil.validatePath(segments);
        List<String> list = List.of(segments);

        for (Path path : this.rootPaths) {
            Path path2 = PathUtil.getPath(path, list);
            if (Files.exists(path2) && DirectoryResourcePack.isValidPath(path2)) {
                return InputSupplier.create(path2);
            }
        }

        return null;
    }

    public void forEachNamespacedPath(ResourceType type, Identifier path, Consumer<Path> consumer) {
        PathUtil.split(path.getPath()).ifSuccess(segments -> {
            String string = path.getNamespace();

            for (Path pathx : this.namespacePaths.get(type)) {
                Path path2 = pathx.resolve(string);
                consumer.accept(PathUtil.getPath(path2, (List<String>)segments));
            }
        }).ifError(error -> LOGGER.error("Invalid path {}: {}", path, error.message()));
    }

    @Override
    public void findResources(ResourceType type, String namespace, String prefix, ResourcePack.ResultConsumer consumer) {
        PathUtil.split(prefix).ifSuccess(segments -> {
            List<Path> list = this.namespacePaths.get(type);
            int i = list.size();
            if (i == 1) {
                collectIdentifiers(consumer, namespace, list.get(0), (List<String>)segments);
            } else if (i > 1) {
                Map<Identifier, InputSupplier<InputStream>> map = new HashMap<>();

                for (int j = 0; j < i - 1; j++) {
                    collectIdentifiers(map::putIfAbsent, namespace, list.get(j), (List<String>)segments);
                }

                Path path = list.get(i - 1);
                if (map.isEmpty()) {
                    collectIdentifiers(consumer, namespace, path, (List<String>)segments);
                } else {
                    collectIdentifiers(map::putIfAbsent, namespace, path, (List<String>)segments);
                    map.forEach(consumer);
                }
            }
        }).ifError(error -> LOGGER.error("Invalid path {}: {}", prefix, error.message()));
    }

    private static void collectIdentifiers(ResourcePack.ResultConsumer consumer, String namespace, Path root, List<String> prefixSegments) {
        Path path = root.resolve(namespace);
        DirectoryResourcePack.findResources(namespace, path, prefixSegments, consumer);
    }

    @Nullable
    @Override
    public InputSupplier<InputStream> open(ResourceType type, Identifier id) {
        return PathUtil.split(id.getPath()).mapOrElse(segments -> {
            String string = id.getNamespace();

            for (Path path : this.namespacePaths.get(type)) {
                Path path2 = PathUtil.getPath(path.resolve(string), (List<String>)segments);
                if (Files.exists(path2) && DirectoryResourcePack.isValidPath(path2)) {
                    return InputSupplier.create(path2);
                }
            }

            return null;
        }, error -> {
            LOGGER.error("Invalid path {}: {}", id, error.message());
            return null;
        });
    }

    @Override
    public Set<String> getNamespaces(ResourceType type) {
        return this.namespaces;
    }

    @Nullable
    @Override
    public <T> T parseMetadata(ResourceMetadataSerializer<T> metadataSerializer) {
        InputSupplier<InputStream> inputSupplier = this.openRoot("pack.mcmeta");
        if (inputSupplier != null) {
            try (InputStream inputStream = inputSupplier.get()) {
                T object = AbstractFileResourcePack.parseMetadata(metadataSerializer, inputStream);
                if (object != null) {
                    return object;
                }
            } catch (IOException var8) {
            }
        }

        return this.metadata.get(metadataSerializer);
    }

    @Override
    public ResourcePackInfo getInfo() {
        return this.info;
    }

    @Override
    public void close() {
    }

    public ResourceFactory getFactory() {
        return id -> Optional.ofNullable(this.open(ResourceType.CLIENT_RESOURCES, id)).map(stream -> new Resource(this, (InputSupplier<InputStream>)stream));
    }
}

