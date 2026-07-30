package net.minecraft.client.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.render.item.model.ItemModelTypes;

@Environment(EnvType.CLIENT)
public record ItemAsset(ItemModel.Unbaked model, ItemAsset.Properties properties) {
    public static final Codec<ItemAsset> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
                ItemModelTypes.CODEC.fieldOf("model").forGetter(ItemAsset::model), ItemAsset.Properties.CODEC.forGetter(ItemAsset::properties)
            )
            .apply(instance, ItemAsset::new)
    );

    @Environment(EnvType.CLIENT)
    public record Properties(boolean handAnimationOnSwap) {
        public static final ItemAsset.Properties DEFAULT = new ItemAsset.Properties(true);
        public static final MapCodec<ItemAsset.Properties> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(Codec.BOOL.optionalFieldOf("hand_animation_on_swap", true).forGetter(ItemAsset.Properties::handAnimationOnSwap))
                .apply(instance, ItemAsset.Properties::new)
        );
    }
}

