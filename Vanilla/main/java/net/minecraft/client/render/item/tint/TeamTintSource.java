package net.minecraft.client.render.item.tint;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.util.Formatting;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.ColorHelper;

@Environment(EnvType.CLIENT)
public record TeamTintSource(int defaultColor) implements TintSource {
    public static final MapCodec<TeamTintSource> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(Codecs.RGB.fieldOf("default").forGetter(TeamTintSource::defaultColor)).apply(instance, TeamTintSource::new)
    );

    @Override
    public int getTint(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity user) {
        if (user != null) {
            AbstractTeam abstractTeam = user.getScoreboardTeam();
            if (abstractTeam != null) {
                Formatting formatting = abstractTeam.getColor();
                if (formatting.getColorValue() != null) {
                    return ColorHelper.fullAlpha(formatting.getColorValue());
                }
            }
        }

        return ColorHelper.fullAlpha(this.defaultColor);
    }

    @Override
    public MapCodec<TeamTintSource> getCodec() {
        return CODEC;
    }
}

