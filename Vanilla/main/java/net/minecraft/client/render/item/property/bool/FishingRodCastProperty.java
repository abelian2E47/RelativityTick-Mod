package net.minecraft.client.render.item.property.bool;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.FishingBobberEntityRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Arm;

@Environment(EnvType.CLIENT)
public record FishingRodCastProperty() implements BooleanProperty {
    public static final MapCodec<FishingRodCastProperty> CODEC = MapCodec.unit(new FishingRodCastProperty());

    @Override
    public boolean getValue(
        ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity user, int seed, ModelTransformationMode modelTransformationMode
    ) {
        if (user instanceof PlayerEntity playerEntity && playerEntity.fishHook != null) {
            Arm arm = FishingBobberEntityRenderer.getArmHoldingRod(playerEntity);
            return user.getStackInArm(arm) == stack;
        } else {
            return false;
        }
    }

    @Override
    public MapCodec<FishingRodCastProperty> getCodec() {
        return CODEC;
    }
}

