package net.minecraft.client.render.item.tint;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

@Environment(EnvType.CLIENT)
public interface TintSource {
    int getTint(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity user);

    MapCodec<? extends TintSource> getCodec();
}

