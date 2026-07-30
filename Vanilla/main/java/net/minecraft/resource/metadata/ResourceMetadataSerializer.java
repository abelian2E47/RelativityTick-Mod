package net.minecraft.resource.metadata;

import com.mojang.serialization.Codec;

public record ResourceMetadataSerializer<T>(String name, Codec<T> codec) {
}

