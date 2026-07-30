package net.minecraft.client.gui.screen.recipebook;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.RecipeFinder;
import net.minecraft.recipe.display.RecipeDisplay;

@Environment(EnvType.CLIENT)
public class RecipeResultCollection {
    public static final RecipeResultCollection EMPTY = new RecipeResultCollection(List.of());
    private final List<RecipeDisplayEntry> entries;
    private final Set<NetworkRecipeId> craftableRecipes = new HashSet<>();
    private final Set<NetworkRecipeId> displayableRecipes = new HashSet<>();

    public RecipeResultCollection(List<RecipeDisplayEntry> entries) {
        this.entries = entries;
    }

    public void populateRecipes(RecipeFinder finder, Predicate<RecipeDisplay> displayablePredicate) {
        for (RecipeDisplayEntry recipeDisplayEntry : this.entries) {
            boolean bl = displayablePredicate.test(recipeDisplayEntry.display());
            if (bl) {
                this.displayableRecipes.add(recipeDisplayEntry.id());
            } else {
                this.displayableRecipes.remove(recipeDisplayEntry.id());
            }

            if (bl && recipeDisplayEntry.isCraftable(finder)) {
                this.craftableRecipes.add(recipeDisplayEntry.id());
            } else {
                this.craftableRecipes.remove(recipeDisplayEntry.id());
            }
        }
    }

    public boolean isCraftable(NetworkRecipeId recipeId) {
        return this.craftableRecipes.contains(recipeId);
    }

    public boolean hasCraftableRecipes() {
        return !this.craftableRecipes.isEmpty();
    }

    public boolean hasDisplayableRecipes() {
        return !this.displayableRecipes.isEmpty();
    }

    public List<RecipeDisplayEntry> getAllRecipes() {
        return this.entries;
    }

    public List<RecipeDisplayEntry> filter(RecipeResultCollection.RecipeFilterMode filterMode) {
        Predicate<NetworkRecipeId> predicate = switch (filterMode) {
            case ANY -> this.displayableRecipes::contains;
            case CRAFTABLE -> this.craftableRecipes::contains;
            case NOT_CRAFTABLE -> recipeId -> this.displayableRecipes.contains(recipeId) && !this.craftableRecipes.contains(recipeId);
        };
        List<RecipeDisplayEntry> list = new ArrayList<>();

        for (RecipeDisplayEntry recipeDisplayEntry : this.entries) {
            if (predicate.test(recipeDisplayEntry.id())) {
                list.add(recipeDisplayEntry);
            }
        }

        return list;
    }

    @Environment(EnvType.CLIENT)
    public enum RecipeFilterMode {
        ANY,
        CRAFTABLE,
        NOT_CRAFTABLE;
    }
}

