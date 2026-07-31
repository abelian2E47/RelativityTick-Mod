package net.minecraft.block.enums;

import net.minecraft.util.StringIdentifiable;

public enum DoorHinge implements StringIdentifiable {
    LEFT,
    RIGHT;

    @Override
    public String toString() {
        return this.asString();
    }

    @Override
    public String asString() {
        return this == LEFT ? "left" : "right";
    }
}

