package net.minecraft.client.render.item.property.numeric;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

@Environment(EnvType.CLIENT)
public class CompassProperty implements NumericProperty {
    public static final MapCodec<CompassProperty> CODEC = CompassState.CODEC.xmap(CompassProperty::new, property -> property.state);
    private final CompassState state;

    public CompassProperty(boolean wobble, CompassState.Target target) {
        this(new CompassState(wobble, target));
    }

    private CompassProperty(CompassState state) {
        this.state = state;
    }

    @Override
    public float getValue(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity holder, int seed) {
        return this.state.getValue(stack, world, holder, seed);
    }

    @Override
    public MapCodec<CompassProperty> getCodec() {
        return CODEC;
    }
}

