package net.minecraft.client.render.item.property.select;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Arm;

@Environment(EnvType.CLIENT)
public record MainHandProperty() implements SelectProperty<Arm> {
    public static final SelectProperty.Type<MainHandProperty, Arm> TYPE = SelectProperty.Type.create(MapCodec.unit(new MainHandProperty()), Arm.CODEC);

    @Nullable
    public Arm getValue(
        ItemStack itemStack, @Nullable ClientWorld clientWorld, @Nullable LivingEntity livingEntity, int i, ModelTransformationMode modelTransformationMode
    ) {
        return livingEntity == null ? null : livingEntity.getMainArm();
    }

    @Override
    public SelectProperty.Type<MainHandProperty, Arm> getType() {
        return TYPE;
    }
}

