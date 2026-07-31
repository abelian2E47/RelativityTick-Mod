package net.minecraft.client.render.item.property.select;

import com.mojang.serialization.Codec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

@Environment(EnvType.CLIENT)
public class SelectProperties {
    private static final Codecs.IdMapper<Identifier, SelectProperty.Type<?, ?>> ID_MAPPER = new Codecs.IdMapper<>();
    public static final Codec<SelectProperty.Type<?, ?>> CODEC = ID_MAPPER.getCodec(Identifier.CODEC);

    public static void bootstrap() {
        ID_MAPPER.put(Identifier.ofVanilla("custom_model_data"), CustomModelDataStringProperty.TYPE);
        ID_MAPPER.put(Identifier.ofVanilla("main_hand"), MainHandProperty.TYPE);
        ID_MAPPER.put(Identifier.ofVanilla("charge_type"), ChargeTypeProperty.TYPE);
        ID_MAPPER.put(Identifier.ofVanilla("trim_material"), TrimMaterialProperty.TYPE);
        ID_MAPPER.put(Identifier.ofVanilla("block_state"), ItemBlockStateProperty.TYPE);
        ID_MAPPER.put(Identifier.ofVanilla("display_context"), DisplayContextProperty.TYPE);
        ID_MAPPER.put(Identifier.ofVanilla("local_time"), LocalTimeProperty.TYPE);
        ID_MAPPER.put(Identifier.ofVanilla("context_entity_type"), ContextEntityTypeProperty.TYPE);
        ID_MAPPER.put(Identifier.ofVanilla("context_dimension"), ContextDimensionProperty.TYPE);
    }
}

