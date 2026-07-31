package net.minecraft.client.render.item.property.numeric;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

@Environment(EnvType.CLIENT)
public class NumericProperties {
    private static final Codecs.IdMapper<Identifier, MapCodec<? extends NumericProperty>> ID_MAPPER = new Codecs.IdMapper<>();
    public static final MapCodec<NumericProperty> CODEC = ID_MAPPER.getCodec(Identifier.CODEC)
        .dispatchMap("property", NumericProperty::getCodec, codec -> codec);

    public static void bootstrap() {
        ID_MAPPER.put(Identifier.ofVanilla("custom_model_data"), CustomModelDataFloatProperty.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("bundle/fullness"), BundleFullnessProperty.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("damage"), DamageProperty.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("cooldown"), CooldownProperty.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("time"), TimeProperty.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("compass"), CompassProperty.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("crossbow/pull"), CrossbowPullProperty.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("use_cycle"), UseCycleProperty.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("use_duration"), UseDurationProperty.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("count"), CountProperty.CODEC);
    }
}

