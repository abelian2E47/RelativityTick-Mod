package net.minecraft.recipe;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

public record RecipeEntry<T extends Recipe<?>>(RegistryKey<Recipe<?>> id, T value) {
    public static final PacketCodec<RegistryByteBuf, RecipeEntry<?>> PACKET_CODEC = PacketCodec.tuple(
        RegistryKey.createPacketCodec(RegistryKeys.RECIPE), RecipeEntry::id, Recipe.PACKET_CODEC, RecipeEntry::value, RecipeEntry::new
    );

    @Override
    public boolean equals(Object o) {
        return this == o ? true : o instanceof RecipeEntry<?> recipeEntry && this.id == recipeEntry.id;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return this.id.toString();
    }
}

