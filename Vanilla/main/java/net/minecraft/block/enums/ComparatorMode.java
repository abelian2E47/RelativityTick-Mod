package net.minecraft.block.enums;

import net.minecraft.util.StringIdentifiable;

public enum ComparatorMode implements StringIdentifiable {
    COMPARE("compare"),
    SUBTRACT("subtract");

    private final String name;

    ComparatorMode(final String name) {
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

