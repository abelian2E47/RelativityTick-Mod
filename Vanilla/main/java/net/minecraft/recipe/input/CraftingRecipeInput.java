package net.minecraft.recipe.input;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeFinder;

public class CraftingRecipeInput implements RecipeInput {
    public static final CraftingRecipeInput EMPTY = new CraftingRecipeInput(0, 0, List.of());
    private final int width;
    private final int height;
    private final List<ItemStack> stacks;
    private final RecipeFinder matcher = new RecipeFinder();
    private final int stackCount;

    private CraftingRecipeInput(int width, int height, List<ItemStack> stacks) {
        this.width = width;
        this.height = height;
        this.stacks = stacks;
        int i = 0;

        for (ItemStack itemStack : stacks) {
            if (!itemStack.isEmpty()) {
                i++;
                this.matcher.addInput(itemStack, 1);
            }
        }

        this.stackCount = i;
    }

    public static CraftingRecipeInput create(int width, int height, List<ItemStack> stacks) {
        return createPositioned(width, height, stacks).input();
    }

    public static CraftingRecipeInput.Positioned createPositioned(int width, int height, List<ItemStack> stacks) {
        if (width != 0 && height != 0) {
            int i = width - 1;
            int j = 0;
            int k = height - 1;
            int l = 0;

            for (int m = 0; m < height; m++) {
                boolean bl = true;

                for (int n = 0; n < width; n++) {
                    ItemStack itemStack = stacks.get(n + m * width);
                    if (!itemStack.isEmpty()) {
                        i = Math.min(i, n);
                        j = Math.max(j, n);
                        bl = false;
                    }
                }

                if (!bl) {
                    k = Math.min(k, m);
                    l = Math.max(l, m);
                }
            }

            int o = j - i + 1;
            int p = l - k + 1;
            if (o <= 0 || p <= 0) {
                return CraftingRecipeInput.Positioned.EMPTY;
            }

            if (o == width && p == height) {
                return new CraftingRecipeInput.Positioned(new CraftingRecipeInput(width, height, stacks), i, k);
            }

            List<ItemStack> list = new ArrayList<>(o * p);

            for (int q = 0; q < p; q++) {
                for (int r = 0; r < o; r++) {
                    int s = r + i + (q + k) * width;
                    list.add(stacks.get(s));
                }
            }

            return new CraftingRecipeInput.Positioned(new CraftingRecipeInput(o, p, list), i, k);
        } else {
            return CraftingRecipeInput.Positioned.EMPTY;
        }
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return this.stacks.get(slot);
    }

    public ItemStack getStackInSlot(int x, int y) {
        return this.stacks.get(x + y * this.width);
    }

    @Override
    public int size() {
        return this.stacks.size();
    }

    @Override
    public boolean isEmpty() {
        return this.stackCount == 0;
    }

    public RecipeFinder getRecipeMatcher() {
        return this.matcher;
    }

    public List<ItemStack> getStacks() {
        return this.stacks;
    }

    public int getStackCount() {
        return this.stackCount;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else {
            return !(o instanceof CraftingRecipeInput craftingRecipeInput)
                ? false
                : this.width == craftingRecipeInput.width
                    && this.height == craftingRecipeInput.height
                    && this.stackCount == craftingRecipeInput.stackCount
                    && ItemStack.stacksEqual(this.stacks, craftingRecipeInput.stacks);
        }
    }

    @Override
    public int hashCode() {
        int i = ItemStack.listHashCode(this.stacks);
        i = 31 * i + this.width;
        return 31 * i + this.height;
    }

    public record Positioned(CraftingRecipeInput input, int left, int top) {
        public static final CraftingRecipeInput.Positioned EMPTY = new CraftingRecipeInput.Positioned(CraftingRecipeInput.EMPTY, 0, 0);
    }
}

