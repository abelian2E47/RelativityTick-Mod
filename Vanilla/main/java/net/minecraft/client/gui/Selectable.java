package net.minecraft.client.gui;

import java.util.Collection;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.Navigable;

@Environment(EnvType.CLIENT)
public interface Selectable extends Navigable, Narratable {
    Selectable.SelectionType getType();

    default boolean isNarratable() {
        return true;
    }

    default Collection<? extends Selectable> getNarratedParts() {
        return List.of(this);
    }

    @Environment(EnvType.CLIENT)
    enum SelectionType {
        NONE,
        HOVERED,
        FOCUSED;

        public boolean isFocused() {
            return this == FOCUSED;
        }
    }
}

