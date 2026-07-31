package net.minecraft.screen;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.ArmorSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

public class HorseScreenHandler extends ScreenHandler {
    static final Identifier EMPTY_SADDLE_SLOT_TEXTURE = Identifier.ofVanilla("container/slot/saddle");
    private static final Identifier EMPTY_LLAMA_ARMOR_SLOT_TEXTURE = Identifier.ofVanilla("container/slot/llama_armor");
    private static final Identifier EMPTY_HORSE_ARMOR_SLOT_TEXTURE = Identifier.ofVanilla("container/slot/horse_armor");
    private final Inventory inventory;
    private final Inventory horseArmorInventory;
    private final AbstractHorseEntity entity;
    private static final int field_48835 = 1;
    private static final int field_48836 = 2;

    public HorseScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, final AbstractHorseEntity entity, int slotColumnCount) {
        super(null, syncId);
        this.inventory = inventory;
        this.horseArmorInventory = entity.getArmorInventory();
        this.entity = entity;
        inventory.onOpen(playerInventory.player);
        this.addSlot(new Slot(inventory, 0, 8, 18) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(Items.SADDLE) && !this.hasStack() && entity.canBeSaddled();
            }

            @Override
            public boolean isEnabled() {
                return entity.canBeSaddled();
            }

            @Override
            public Identifier getBackgroundSprite() {
                return HorseScreenHandler.EMPTY_SADDLE_SLOT_TEXTURE;
            }
        });
        Identifier identifier = entity instanceof LlamaEntity ? EMPTY_LLAMA_ARMOR_SLOT_TEXTURE : EMPTY_HORSE_ARMOR_SLOT_TEXTURE;
        this.addSlot(new ArmorSlot(this.horseArmorInventory, entity, EquipmentSlot.BODY, 0, 8, 36, identifier) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return entity.canEquip(stack, EquipmentSlot.BODY);
            }

            @Override
            public boolean isEnabled() {
                return entity.canUseSlot(EquipmentSlot.BODY);
            }
        });
        if (slotColumnCount > 0) {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < slotColumnCount; j++) {
                    this.addSlot(new Slot(inventory, 1 + j + i * slotColumnCount, 80 + j * 18, 18 + i * 18));
                }
            }
        }

        this.addPlayerSlots(playerInventory, 8, 84);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return !this.entity.areInventoriesDifferent(this.inventory)
            && this.inventory.canPlayerUse(player)
            && this.horseArmorInventory.canPlayerUse(player)
            && this.entity.isAlive()
            && player.canInteractWithEntity(this.entity, 4.0);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot2 = this.slots.get(slot);
        if (slot2 != null && slot2.hasStack()) {
            ItemStack itemStack2 = slot2.getStack();
            itemStack = itemStack2.copy();
            int i = this.inventory.size() + 1;
            if (slot < i) {
                if (!this.insertItem(itemStack2, i, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.getSlot(1).canInsert(itemStack2) && !this.getSlot(1).hasStack()) {
                if (!this.insertItem(itemStack2, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.getSlot(0).canInsert(itemStack2)) {
                if (!this.insertItem(itemStack2, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (i <= 1 || !this.insertItem(itemStack2, 2, i, false)) {
                int j = i;
                int k = j + 27;
                int l = k;
                int m = l + 9;
                if (slot >= l && slot < m) {
                    if (!this.insertItem(itemStack2, j, k, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slot >= j && slot < k) {
                    if (!this.insertItem(itemStack2, l, m, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.insertItem(itemStack2, l, k, false)) {
                    return ItemStack.EMPTY;
                }

                return ItemStack.EMPTY;
            }

            if (itemStack2.isEmpty()) {
                slot2.setStack(ItemStack.EMPTY);
            } else {
                slot2.markDirty();
            }
        }

        return itemStack;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.inventory.onClose(player);
    }
}

