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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;
import java.util.ArrayList;
import java.util.List;

public class CassetteBagMenu extends AbstractContainerMenu {
    private final ItemStack bagStack;
    private final SimpleContainer inventory;

    public CassetteBagMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, playerInventory.player.getMainHandItem().is(ModItems.CASSETTE_BAG.get())
                ? playerInventory.player.getMainHandItem()
                : playerInventory.player.getOffhandItem());
    }

    public CassetteBagMenu(int id, Inventory playerInventory, ItemStack bagStack) {
        super(ModMenus.CASSETTE_BAG_MENU.get(), id);
        this.bagStack = bagStack;
        this.inventory = new SimpleContainer(18) {
            @Override
            public boolean canPlaceItem(int index, ItemStack stack) {
                return stack.has(ModDataComponents.CASSETTE_DATA.get()) && !stack.is(ModItems.CASSETTE_BAG.get());
            }
        };

        List<ItemStack> contents = bagStack.get(ModDataComponents.BAG_CONTENTS.get());
        if (contents != null) {
            for (int i = 0; i < Math.min(contents.size(), 18); i++) {
                inventory.setItem(i, contents.get(i).copy());
            }
        }

        inventory.addListener(container -> saveContents());

        for (int i = 0; i < 2; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inventory, j + i * 9, 8 + j * 18, 17 + i * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return container.canPlaceItem(getSlotIndex(), stack);
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

    private void saveContents() {
        List<ItemStack> contents = new ArrayList<>();
        for (int i = 0; i < 18; i++) {
            contents.add(inventory.getItem(i).copy());
        }
        bagStack.set(ModDataComponents.BAG_CONTENTS.get(), contents);
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
        return !bagStack.isEmpty() && (player.getMainHandItem() == bagStack || player.getOffhandItem() == bagStack);
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
