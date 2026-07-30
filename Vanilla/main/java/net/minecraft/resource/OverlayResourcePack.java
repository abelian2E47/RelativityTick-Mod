package net.minecraft.resource;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;
import net.minecraft.util.Identifier;

public class OverlayResourcePack implements ResourcePack {
    private final ResourcePack base;
    private final List<ResourcePack> overlaysAndBase;

    public OverlayResourcePack(ResourcePack base, List<ResourcePack> overlays) {
        this.base = base;
        List<ResourcePack> list = new ArrayList<>(overlays.size() + 1);
        list.addAll(Lists.reverse(overlays));
        list.add(base);
        this.overlaysAndBase = List.copyOf(list);
    }

    @Nullable
    @Override
    public InputSupplier<InputStream> openRoot(String... segments) {
        return this.base.openRoot(segments);
    }

    @Nullable
    @Override
    public InputSupplier<InputStream> open(ResourceType type, Identifier id) {
        for (ResourcePack resourcePack : this.overlaysAndBase) {
            InputSupplier<InputStream> inputSupplier = resourcePack.open(type, id);
            if (inputSupplier != null) {
                return inputSupplier;
            }
        }

        return null;
    }

    @Override
    public void findResources(ResourceType type, String namespace, String prefix, ResourcePack.ResultConsumer consumer) {
        Map<Identifier, InputSupplier<InputStream>> map = new HashMap<>();

        for (ResourcePack resourcePack : this.overlaysAndBase) {
            resourcePack.findResources(type, namespace, prefix, map::putIfAbsent);
        }

        map.forEach(consumer);
    }

    @Override
    public Set<String> getNamespaces(ResourceType type) {
        Set<String> set = new HashSet<>();

        for (ResourcePack resourcePack : this.overlaysAndBase) {
            set.addAll(resourcePack.getNamespaces(type));
        }

        return set;
    }

    @Nullable
    @Override
    public <T> T parseMetadata(ResourceMetadataSerializer<T> metadataSerializer) throws IOException {
        return this.base.parseMetadata(metadataSerializer);
    }

    @Override
    public ResourcePackInfo getInfo() {
        return this.base.getInfo();
    }

    @Override
    public void close() {
        this.overlaysAndBase.forEach(ResourcePack::close);
    }
}

