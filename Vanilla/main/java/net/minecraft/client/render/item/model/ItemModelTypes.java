package net.minecraft.client.render.item.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

@Environment(EnvType.CLIENT)
public class ItemModelTypes {
    private static final Codecs.IdMapper<Identifier, MapCodec<? extends ItemModel.Unbaked>> ID_MAPPER = new Codecs.IdMapper<>();
    public static final Codec<ItemModel.Unbaked> CODEC = ID_MAPPER.getCodec(Identifier.CODEC).dispatch(ItemModel.Unbaked::getCodec, codec -> codec);

    public static void bootstrap() {
        ID_MAPPER.put(Identifier.ofVanilla("empty"), EmptyItemModel.Unbaked.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("model"), BasicItemModel.Unbaked.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("range_dispatch"), RangeDispatchItemModel.Unbaked.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("special"), SpecialItemModel.Unbaked.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("composite"), CompositeItemModel.Unbaked.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("bundle/selected_item"), BundleSelectedItemModel.Unbaked.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("select"), SelectItemModel.Unbaked.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("condition"), ConditionItemModel.Unbaked.CODEC);
    }
}

