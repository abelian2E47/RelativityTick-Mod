package net.minecraft.client.render.item.property.select;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ModelTransformationMode;

@Environment(EnvType.CLIENT)
public record ChargeTypeProperty() implements SelectProperty<CrossbowItem.ChargeType> {
    public static final SelectProperty.Type<ChargeTypeProperty, CrossbowItem.ChargeType> TYPE = SelectProperty.Type.create(
        MapCodec.unit(new ChargeTypeProperty()), CrossbowItem.ChargeType.CODEC
    );

    public CrossbowItem.ChargeType getValue(
        ItemStack itemStack, @Nullable ClientWorld clientWorld, @Nullable LivingEntity livingEntity, int i, ModelTransformationMode modelTransformationMode
    ) {
        ChargedProjectilesComponent chargedProjectilesComponent = itemStack.get(DataComponentTypes.CHARGED_PROJECTILES);
        if (chargedProjectilesComponent == null || chargedProjectilesComponent.isEmpty()) {
            return CrossbowItem.ChargeType.NONE;
        } else {
            return chargedProjectilesComponent.contains(Items.FIREWORK_ROCKET) ? CrossbowItem.ChargeType.ROCKET : CrossbowItem.ChargeType.ARROW;
        }
    }

    @Override
    public SelectProperty.Type<ChargeTypeProperty, CrossbowItem.ChargeType> getType() {
        return TYPE;
    }
}

