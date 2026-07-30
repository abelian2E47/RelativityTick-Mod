package net.minecraft.entity.player;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.s2c.play.SetPlayerInventoryS2CPacket;
import net.minecraft.recipe.RecipeFinder;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Nameable;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;

public class PlayerInventory implements Inventory, Nameable {
    /**
     * The maximum cooldown ({@value} ticks) applied to timed use items such as the Eye of Ender.
     */
    public static final int ITEM_USAGE_COOLDOWN = 5;
    /**
     * The number of slots ({@value}) in the main (non-hotbar) section of the inventory.
     */
    public static final int MAIN_SIZE = 36;
    /**
     * The number of columns ({@value}) in the inventory.
     * 
     * <p>The same value dictates the size of the player's hotbar, excluding the offhand slot.
     */
    public static final int HOTBAR_SIZE = 9;
    /**
     * Zero-based index of the offhand slot.
     * 
     * <p>This value is the result of the sum {@code MAIN_SIZE (36) + ARMOR_SIZE (4)}.
     */
    public static final int OFF_HAND_SLOT = 40;
    /**
     * The slot index ({@value}) used to indicate no result
     * (item not present / no available space) when querying the inventory's contents
     * or to indicate no preference when inserting an item into the inventory.
     */
    public static final int NOT_FOUND = -1;
    public final DefaultedList<ItemStack> main = DefaultedList.ofSize(36, ItemStack.EMPTY);
    public final DefaultedList<ItemStack> armor = DefaultedList.ofSize(4, ItemStack.EMPTY);
    public final DefaultedList<ItemStack> offHand = DefaultedList.ofSize(1, ItemStack.EMPTY);
    private final List<DefaultedList<ItemStack>> combinedInventory = ImmutableList.of(this.main, this.armor, this.offHand);
    public int selectedSlot;
    public final PlayerEntity player;
    private int changeCount;

    public PlayerInventory(PlayerEntity player) {
        this.player = player;
    }

    public ItemStack getMainHandStack() {
        return isValidHotbarIndex(this.selectedSlot) ? this.main.get(this.selectedSlot) : ItemStack.EMPTY;
    }

    public static int getHotbarSize() {
        return 9;
    }

    private boolean canStackAddMore(ItemStack existingStack, ItemStack stack) {
        return !existingStack.isEmpty()
            && ItemStack.areItemsAndComponentsEqual(existingStack, stack)
            && existingStack.isStackable()
            && existingStack.getCount() < this.getMaxCount(existingStack);
    }

    public int getEmptySlot() {
        for (int i = 0; i < this.main.size(); i++) {
            if (this.main.get(i).isEmpty()) {
                return i;
            }
        }

        return -1;
    }

    public void swapStackWithHotbar(ItemStack stack) {
        this.selectedSlot = this.getSwappableHotbarSlot();
        if (!this.main.get(this.selectedSlot).isEmpty()) {
            int i = this.getEmptySlot();
            if (i != -1) {
                this.main.set(i, this.main.get(this.selectedSlot));
            }
        }

        this.main.set(this.selectedSlot, stack);
    }

    public void swapSlotWithHotbar(int slot) {
        this.selectedSlot = this.getSwappableHotbarSlot();
        ItemStack itemStack = this.main.get(this.selectedSlot);
        this.main.set(this.selectedSlot, this.main.get(slot));
        this.main.set(slot, itemStack);
    }

    public static boolean isValidHotbarIndex(int slot) {
        return slot >= 0 && slot < 9;
    }

    public int getSlotWithStack(ItemStack stack) {
        for (int i = 0; i < this.main.size(); i++) {
            if (!this.main.get(i).isEmpty() && ItemStack.areItemsAndComponentsEqual(stack, this.main.get(i))) {
                return i;
            }
        }

        return -1;
    }

    public static boolean usableWhenFillingSlot(ItemStack stack) {
        return !stack.isDamaged() && !stack.hasEnchantments() && !stack.contains(DataComponentTypes.CUSTOM_NAME);
    }

    public int getMatchingSlot(RegistryEntry<Item> item, ItemStack stack) {
        for (int i = 0; i < this.main.size(); i++) {
            ItemStack itemStack = this.main.get(i);
            if (!itemStack.isEmpty()
                && itemStack.itemMatches(item)
                && usableWhenFillingSlot(itemStack)
                && (stack.isEmpty() || ItemStack.areItemsAndComponentsEqual(stack, itemStack))) {
                return i;
            }
        }

        return -1;
    }

    public int getSwappableHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            int j = (this.selectedSlot + i) % 9;
            if (this.main.get(j).isEmpty()) {
                return j;
            }
        }

        for (int k = 0; k < 9; k++) {
            int l = (this.selectedSlot + k) % 9;
            if (!this.main.get(l).hasEnchantments()) {
                return l;
            }
        }

        return this.selectedSlot;
    }

    public void setSelectedSlot(int slot) {
        this.selectedSlot = slot;
    }

    public int remove(Predicate<ItemStack> shouldRemove, int maxCount, Inventory craftingInventory) {
        int i = 0;
        boolean bl = maxCount == 0;
        i += Inventories.remove(this, shouldRemove, maxCount - i, bl);
        i += Inventories.remove(craftingInventory, shouldRemove, maxCount - i, bl);
        ItemStack itemStack = this.player.currentScreenHandler.getCursorStack();
        i += Inventories.remove(itemStack, shouldRemove, maxCount - i, bl);
        if (itemStack.isEmpty()) {
            this.player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
        }

        return i;
    }

    private int addStack(ItemStack stack) {
        int i = this.getOccupiedSlotWithRoomForStack(stack);
        if (i == -1) {
            i = this.getEmptySlot();
        }

        return i == -1 ? stack.getCount() : this.addStack(i, stack);
    }

    private int addStack(int slot, ItemStack stack) {
        int i = stack.getCount();
        ItemStack itemStack = this.getStack(slot);
        if (itemStack.isEmpty()) {
            itemStack = stack.copyWithCount(0);
            this.setStack(slot, itemStack);
        }

        int j = this.getMaxCount(itemStack) - itemStack.getCount();
        int k = Math.min(i, j);
        if (k == 0) {
            return i;
        }

        i -= k;
        itemStack.increment(k);
        itemStack.setBobbingAnimationTime(5);
        return i;
    }

    public int getOccupiedSlotWithRoomForStack(ItemStack stack) {
        if (this.canStackAddMore(this.getStack(this.selectedSlot), stack)) {
            return this.selectedSlot;
        }

        if (this.canStackAddMore(this.getStack(40), stack)) {
            return 40;
        }

        for (int i = 0; i < this.main.size(); i++) {
            if (this.canStackAddMore(this.main.get(i), stack)) {
                return i;
            }
        }

        return -1;
    }

    public void updateItems() {
        for (DefaultedList<ItemStack> defaultedList : this.combinedInventory) {
            for (int i = 0; i < defaultedList.size(); i++) {
                if (!defaultedList.get(i).isEmpty()) {
                    defaultedList.get(i).inventoryTick(this.player.getWorld(), this.player, i, this.selectedSlot == i);
                }
            }
        }
    }

    public boolean insertStack(ItemStack stack) {
        return this.insertStack(-1, stack);
    }

    public boolean insertStack(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        try {
            if (stack.isDamaged()) {
                if (slot == -1) {
                    slot = this.getEmptySlot();
                }

                if (slot >= 0) {
                    this.main.set(slot, stack.copyAndEmpty());
                    this.main.get(slot).setBobbingAnimationTime(5);
                    return true;
                } else if (this.player.isInCreativeMode()) {
                    stack.setCount(0);
                    return true;
                } else {
                    return false;
                }
            } else {
                int i;
                do {
                    i = stack.getCount();
                    if (slot == -1) {
                        stack.setCount(this.addStack(stack));
                    } else {
                        stack.setCount(this.addStack(slot, stack));
                    }
                } while (!stack.isEmpty() && stack.getCount() < i);

                if (stack.getCount() == i && this.player.isInCreativeMode()) {
                    stack.setCount(0);
                    return true;
                } else {
                    return stack.getCount() < i;
                }
            }
        } catch (Throwable throwable) {
            CrashReport crashReport = CrashReport.create(throwable, "Adding item to inventory");
            CrashReportSection crashReportSection = crashReport.addElement("Item being added");
            crashReportSection.add("Item ID", Item.getRawId(stack.getItem()));
            crashReportSection.add("Item data", stack.getDamage());
            crashReportSection.add("Item name", () -> stack.getName().getString());
            throw new CrashException(crashReport);
        }
    }

    public void offerOrDrop(ItemStack stack) {
        this.offer(stack, true);
    }

    public void offer(ItemStack stack, boolean notifiesClient) {
        while (!stack.isEmpty()) {
            int i = this.getOccupiedSlotWithRoomForStack(stack);
            if (i == -1) {
                i = this.getEmptySlot();
            }

            if (i == -1) {
                this.player.dropItem(stack, false);
                break;
            }

            int j = stack.getMaxCount() - this.getStack(i).getCount();
            if (this.insertStack(i, stack.split(j)) && notifiesClient && this.player instanceof ServerPlayerEntity serverPlayerEntity) {
                serverPlayerEntity.networkHandler.sendPacket(this.createSlotSetPacket(i));
            }
        }
    }

    public SetPlayerInventoryS2CPacket createSlotSetPacket(int slot) {
        return new SetPlayerInventoryS2CPacket(slot, this.getStack(slot).copy());
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        List<ItemStack> list = null;

        for (DefaultedList<ItemStack> defaultedList : this.combinedInventory) {
            if (slot < defaultedList.size()) {
                list = defaultedList;
                break;
            }

            slot -= defaultedList.size();
        }

        return list != null && !list.get(slot).isEmpty() ? Inventories.splitStack(list, slot, amount) : ItemStack.EMPTY;
    }

    public void removeOne(ItemStack stack) {
        for (DefaultedList<ItemStack> defaultedList : this.combinedInventory) {
            for (int i = 0; i < defaultedList.size(); i++) {
                if (defaultedList.get(i) == stack) {
                    defaultedList.set(i, ItemStack.EMPTY);
                    break;
                }
            }
        }
    }

    @Override
    public ItemStack removeStack(int slot) {
        DefaultedList<ItemStack> defaultedList = null;

        for (DefaultedList<ItemStack> defaultedList2 : this.combinedInventory) {
            if (slot < defaultedList2.size()) {
                defaultedList = defaultedList2;
                break;
            }

            slot -= defaultedList2.size();
        }

        if (defaultedList != null && !defaultedList.get(slot).isEmpty()) {
            ItemStack itemStack = defaultedList.get(slot);
            defaultedList.set(slot, ItemStack.EMPTY);
            return itemStack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        DefaultedList<ItemStack> defaultedList = null;

        for (DefaultedList<ItemStack> defaultedList2 : this.combinedInventory) {
            if (slot < defaultedList2.size()) {
                defaultedList = defaultedList2;
                break;
            }

            slot -= defaultedList2.size();
        }

        if (defaultedList != null) {
            defaultedList.set(slot, stack);
        }
    }

    public float getBlockBreakingSpeed(BlockState block) {
        return this.main.get(this.selectedSlot).getMiningSpeedMultiplier(block);
    }

    public NbtList writeNbt(NbtList nbtList) {
        for (int i = 0; i < this.main.size(); i++) {
            if (!this.main.get(i).isEmpty()) {
                NbtCompound nbtCompound = new NbtCompound();
                nbtCompound.putByte("Slot", (byte)i);
                nbtList.add(this.main.get(i).toNbt(this.player.getRegistryManager(), nbtCompound));
            }
        }

        for (int j = 0; j < this.armor.size(); j++) {
            if (!this.armor.get(j).isEmpty()) {
                NbtCompound nbtCompound2 = new NbtCompound();
                nbtCompound2.putByte("Slot", (byte)(j + 100));
                nbtList.add(this.armor.get(j).toNbt(this.player.getRegistryManager(), nbtCompound2));
            }
        }

        for (int k = 0; k < this.offHand.size(); k++) {
            if (!this.offHand.get(k).isEmpty()) {
                NbtCompound nbtCompound3 = new NbtCompound();
                nbtCompound3.putByte("Slot", (byte)(k + 150));
                nbtList.add(this.offHand.get(k).toNbt(this.player.getRegistryManager(), nbtCompound3));
            }
        }

        return nbtList;
    }

    public void readNbt(NbtList nbtList) {
        this.main.clear();
        this.armor.clear();
        this.offHand.clear();

        for (int i = 0; i < nbtList.size(); i++) {
            NbtCompound nbtCompound = nbtList.getCompound(i);
            int j = nbtCompound.getByte("Slot") & 255;
            ItemStack itemStack = ItemStack.fromNbt(this.player.getRegistryManager(), nbtCompound).orElse(ItemStack.EMPTY);
            if (j >= 0 && j < this.main.size()) {
                this.main.set(j, itemStack);
            } else if (j >= 100 && j < this.armor.size() + 100) {
                this.armor.set(j - 100, itemStack);
            } else if (j >= 150 && j < this.offHand.size() + 150) {
                this.offHand.set(j - 150, itemStack);
            }
        }
    }

    @Override
    public int size() {
        return this.main.size() + this.armor.size() + this.offHand.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemStack : this.main) {
            if (!itemStack.isEmpty()) {
                return false;
            }
        }

        for (ItemStack itemStack2 : this.armor) {
            if (!itemStack2.isEmpty()) {
                return false;
            }
        }

        for (ItemStack itemStack3 : this.offHand) {
            if (!itemStack3.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        List<ItemStack> list = null;

        for (DefaultedList<ItemStack> defaultedList : this.combinedInventory) {
            if (slot < defaultedList.size()) {
                list = defaultedList;
                break;
            }

            slot -= defaultedList.size();
        }

        return list == null ? ItemStack.EMPTY : list.get(slot);
    }

    @Override
    public Text getName() {
        return Text.translatable("container.inventory");
    }

    public ItemStack getArmorStack(int slot) {
        return this.armor.get(slot);
    }

    public void dropAll() {
        for (List<ItemStack> list : this.combinedInventory) {
            for (int i = 0; i < list.size(); i++) {
                ItemStack itemStack = list.get(i);
                if (!itemStack.isEmpty()) {
                    this.player.dropItem(itemStack, true, false);
                    list.set(i, ItemStack.EMPTY);
                }
            }
        }
    }

    @Override
    public void markDirty() {
        this.changeCount++;
    }

    public int getChangeCount() {
        return this.changeCount;
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return player.canInteractWithEntity(this.player, 4.0);
    }

    public boolean contains(ItemStack stack) {
        for (List<ItemStack> list : this.combinedInventory) {
            for (ItemStack itemStack : list) {
                if (!itemStack.isEmpty() && ItemStack.areItemsAndComponentsEqual(itemStack, stack)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean contains(TagKey<Item> tag) {
        for (List<ItemStack> list : this.combinedInventory) {
            for (ItemStack itemStack : list) {
                if (!itemStack.isEmpty() && itemStack.isIn(tag)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean contains(Predicate<ItemStack> predicate) {
        for (List<ItemStack> list : this.combinedInventory) {
            for (ItemStack itemStack : list) {
                if (predicate.test(itemStack)) {
                    return true;
                }
            }
        }

        return false;
    }

    public void clone(PlayerInventory other) {
        for (int i = 0; i < this.size(); i++) {
            this.setStack(i, other.getStack(i));
        }

        this.selectedSlot = other.selectedSlot;
    }

    @Override
    public void clear() {
        for (List<ItemStack> list : this.combinedInventory) {
            list.clear();
        }
    }

    public void populateRecipeFinder(RecipeFinder finder) {
        for (ItemStack itemStack : this.main) {
            finder.addInputIfUsable(itemStack);
        }
    }

    public ItemStack dropSelectedItem(boolean entireStack) {
        ItemStack itemStack = this.getMainHandStack();
        return itemStack.isEmpty() ? ItemStack.EMPTY : this.removeStack(this.selectedSlot, entireStack ? itemStack.getCount() : 1);
    }
}

