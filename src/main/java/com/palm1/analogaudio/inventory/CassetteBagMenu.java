package com.palm1.analogaudio.inventory;

import com.palm1.analogaudio.registry.ModDataComponents;
import com.palm1.analogaudio.registry.ModMenus;
import com.palm1.analogaudio.registry.ModItems;
import com.palm1.analogaudio.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.SimpleContainer;
import java.util.ArrayList;
import java.util.List;

public class CassetteBagMenu extends AbstractContainerMenu {
    private final Inventory playerInventory;
    private final int bagSlotIndex;
    private final SimpleContainer inventory;

    public CassetteBagMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, playerInventory.player.getMainHandItem().is(ModItems.CASSETTE_BAG.get())
                ? playerInventory.player.getMainHandItem()
                : playerInventory.player.getOffhandItem());
    }

    public CassetteBagMenu(int id, Inventory playerInventory, ItemStack bagStack) {
        this(id, playerInventory, findBagSlot(playerInventory, bagStack));
    }

    public CassetteBagMenu(int id, Inventory playerInventory, int bagSlotIndex) {
        super(ModMenus.CASSETTE_BAG_MENU.get(), id);
        this.playerInventory = playerInventory;
        this.bagSlotIndex = bagSlotIndex;
        this.inventory = new SimpleContainer(18) {
            @Override
            public boolean canPlaceItem(int index, ItemStack stack) {
                return stack.has(ModDataComponents.CASSETTE_DATA.get()) && !stack.is(ModItems.CASSETTE_BAG.get());
            }
        };

        if (bagSlotIndex >= 0) {
            ItemStack bagStack = playerInventory.getItem(bagSlotIndex);
            if (!bagStack.isEmpty()) {
                ItemContainerContents contents = bagStack.get(ModDataComponents.BAG_CONTENTS.get());
                if (contents != null) {
                    List<ItemStack> itemStacks = contents.stream().toList();
                    for (int i = 0; i < Math.min(itemStacks.size(), 18); i++) {
                        inventory.setItem(i, itemStacks.get(i).copy());
                    }
                }
            }
        }

        inventory.addListener(container -> saveContents());

        for (int i = 0; i < 2; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inventory, j + i * 9, 8 + j * 18, 17 + i * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return inventory.canPlaceItem(getSlotIndex(), stack);
                    }
                });
            }
        }

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 58 + i * 18));
            }
        }
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 116));
        }

        if (!playerInventory.player.level().isClientSide()) {
            playerInventory.player.level().playSound(null, playerInventory.player.getX(), playerInventory.player.getY(),
                    playerInventory.player.getZ(), ModSounds.BAG_OPEN.get(), SoundSource.PLAYERS, 0.5f, 1.0f);
        }
    }

    private static int findBagSlot(Inventory playerInventory, ItemStack bagStack) {
        for (int i = 0; i < playerInventory.getContainerSize(); i++) {
            if (playerInventory.getItem(i) == bagStack) {
                return i;
            }
        }
        for (int i = 0; i < playerInventory.getContainerSize(); i++) {
            if (ItemStack.isSameItemSameComponents(playerInventory.getItem(i), bagStack)) {
                return i;
            }
        }
        return -1;
    }

    private void saveContents() {
        if (bagSlotIndex >= 0) {
            ItemStack bagStack = playerInventory.getItem(bagSlotIndex);
            if (bagStack.is(ModItems.CASSETTE_BAG.get())) {
                List<ItemStack> contents = new ArrayList<>();
                for (int i = 0; i < 18; i++) {
                    contents.add(inventory.getItem(i).copy());
                }
                bagStack.set(ModDataComponents.BAG_CONTENTS.get(), ItemContainerContents.fromItems(contents));
            }
        }
    }

    private boolean isBagSlot(Slot slot) {
        return slot != null && slot.container == playerInventory && slot.getSlotIndex() == bagSlotIndex;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < this.slots.size()) {
            Slot slot = this.slots.get(slotId);
            if (isBagSlot(slot)) {
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 18) {
                if (!this.moveItemStackTo(itemstack1, 18, 54, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, 18, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        if (bagSlotIndex < 0) {
            return false;
        }
        ItemStack bagStack = player.getInventory().getItem(bagSlotIndex);
        return !bagStack.isEmpty() && bagStack.is(ModItems.CASSETTE_BAG.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.BAG_CLOSE.get(),
                    SoundSource.PLAYERS, 0.5f, 1.0f);
        }
    }
}
