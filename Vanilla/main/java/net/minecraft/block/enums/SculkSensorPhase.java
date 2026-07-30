package net.minecraft.block.enums;

import net.minecraft.util.StringIdentifiable;

public enum SculkSensorPhase implements StringIdentifiable {
    INACTIVE("inactive"),
    ACTIVE("active"),
    COOLDOWN("cooldown");

    private final String name;

    SculkSensorPhase(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public String asString() {
        return this.name;
    }
}

