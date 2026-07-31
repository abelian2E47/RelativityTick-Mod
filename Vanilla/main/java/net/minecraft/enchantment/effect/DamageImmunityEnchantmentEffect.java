package net.minecraft.enchantment.effect;

import com.mojang.serialization.Codec;

public record DamageImmunityEnchantmentEffect() {
    public static final DamageImmunityEnchantmentEffect INSTANCE = new DamageImmunityEnchantmentEffect();
    public static final Codec<DamageImmunityEnchantmentEffect> CODEC = Codec.unit(() -> INSTANCE);
}

