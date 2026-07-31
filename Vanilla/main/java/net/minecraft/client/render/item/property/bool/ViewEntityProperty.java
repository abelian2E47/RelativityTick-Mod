package net.minecraft.client.render.item.property.bool;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;

@Environment(EnvType.CLIENT)
public record ViewEntityProperty() implements BooleanProperty {
    public static final MapCodec<ViewEntityProperty> CODEC = MapCodec.unit(new ViewEntityProperty());

    @Override
    public boolean getValue(
        ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity user, int seed, ModelTransformationMode modelTransformationMode
    ) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Entity entity = minecraftClient.getCameraEntity();
        return entity != null ? user == entity : user == minecraftClient.player;
    }

    @Override
    public MapCodec<ViewEntityProperty> getCodec() {
        return CODEC;
    }
}

