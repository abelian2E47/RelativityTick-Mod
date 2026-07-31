package net.minecraft.client.render.item.tint;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

@Environment(EnvType.CLIENT)
public class TintSourceTypes {
    private static final Codecs.IdMapper<Identifier, MapCodec<? extends TintSource>> ID_MAPPER = new Codecs.IdMapper<>();
    public static final Codec<TintSource> CODEC = ID_MAPPER.getCodec(Identifier.CODEC).dispatch(TintSource::getCodec, codec -> codec);

    public static void bootstrap() {
        ID_MAPPER.put(Identifier.ofVanilla("custom_model_data"), CustomModelDataTintSource.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("constant"), ConstantTintSource.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("dye"), DyeTintSource.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("grass"), GrassTintSource.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("firework"), FireworkTintSource.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("potion"), PotionTintSource.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("map_color"), MapColorTintSource.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("team"), TeamTintSource.CODEC);
    }
}

