package net.minecraft.block.enums;

import net.minecraft.util.StringIdentifiable;

public enum BambooLeaves implements StringIdentifiable {
    NONE("none"),
    SMALL("small"),
    LARGE("large");

    private final String name;

    BambooLeaves(final String name) {
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

