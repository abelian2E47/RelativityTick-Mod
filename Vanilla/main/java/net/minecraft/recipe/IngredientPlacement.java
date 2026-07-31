package net.minecraft.recipe;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IngredientPlacement {
    public static final int field_55495 = -1;
    public static final IngredientPlacement NONE = new IngredientPlacement(List.of(), IntList.of());
    private final List<Ingredient> ingredients;
    private final IntList placementSlots;

    private IngredientPlacement(List<Ingredient> ingredients, IntList placementSlots) {
        this.ingredients = ingredients;
        this.placementSlots = placementSlots;
    }

    public static IngredientPlacement forSingleSlot(Ingredient ingredient) {
        return ingredient.isEmpty() ? NONE : new IngredientPlacement(List.of(ingredient), IntList.of(0));
    }

    public static IngredientPlacement forMultipleSlots(List<Optional<Ingredient>> ingredients) {
        int i = ingredients.size();
        List<Ingredient> list = new ArrayList<>(i);
        IntList intList = new IntArrayList(i);
        int j = 0;

        for (Optional<Ingredient> optional : ingredients) {
            if (optional.isPresent()) {
                Ingredient ingredient = optional.get();
                if (ingredient.isEmpty()) {
                    return NONE;
                }

                list.add(ingredient);
                intList.add(j++);
            } else {
                intList.add(-1);
            }
        }

        return new IngredientPlacement(list, intList);
    }

    public static IngredientPlacement forShapeless(List<Ingredient> ingredients) {
        int i = ingredients.size();
        IntList intList = new IntArrayList(i);

        for (int j = 0; j < i; j++) {
            Ingredient ingredient = ingredients.get(j);
            if (ingredient.isEmpty()) {
                return NONE;
            }

            intList.add(j);
        }

        return new IngredientPlacement(ingredients, intList);
    }

    public IntList getPlacementSlots() {
        return this.placementSlots;
    }

    public List<Ingredient> getIngredients() {
        return this.ingredients;
    }

    public boolean hasNoPlacement() {
        return this.placementSlots.isEmpty();
    }
}

