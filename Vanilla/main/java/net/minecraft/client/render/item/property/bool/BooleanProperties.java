package net.minecraft.client.render.item.property.bool;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

@Environment(EnvType.CLIENT)
public class BooleanProperties {
    private static final Codecs.IdMapper<Identifier, MapCodec<? extends BooleanProperty>> ID_MAPPER = new Codecs.IdMapper<>();
    public static final MapCodec<BooleanProperty> CODEC = ID_MAPPER.getCodec(Identifier.CODEC)
        .dispatchMap("property", BooleanProperty::getCodec, codec -> codec);

    public static void bootstrap() {
        ID_MAPPER.put(Identifier.ofVanilla("custom_model_data"), CustomModelDataFlagProperty.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("using_item"), UsingItemProperty.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("broken"), BrokenProperty.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("damaged"), DamagedProperty.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("fishing_rod/cast"), FishingRodCastProperty.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("has_component"), HasComponentProperty.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("bundle/has_selected_item"), BundleHasSelectedItemProperty.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("selected"), SelectedProperty.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("carried"), CarriedProperty.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("extended_view"), ExtendedViewProperty.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("keybind_down"), KeybindDownProperty.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("view_entity"), ViewEntityProperty.CODEC);
    }
}

