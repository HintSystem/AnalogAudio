package com.palm1.analogaudio.inventory;

import com.palm1.analogaudio.block.entity.RadioBlockEntity;
import com.palm1.analogaudio.registry.ModMenus;
import com.palm1.analogaudio.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class RadioMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final RadioBlockEntity blockEntity;

    public RadioMenu(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        this(id, playerInventory, buf.readBlockPos());
    }

    public RadioMenu(int id, Inventory playerInventory, BlockPos pos) {
        super(ModMenus.RADIO_MENU.get(), id);
        this.pos = pos;
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof RadioBlockEntity radio) {
            this.blockEntity = radio;

            this.addSlot(new Slot(radio.inventory, 1, 113, 45) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(ModItems.CASSETTE_BAG.get());
                }
            });
            this.addSlot(new CassetteSlot(radio.inventory, 0, 14, 29));
        } else {
            this.blockEntity = null;
        }

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 85 + i * 18));
            }
        }
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 143));
        }
    }

    public BlockPos getPos() {
        return pos;
    }

    public RadioBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 2) {
                if (!this.moveItemStackTo(itemstack1, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (itemstack1.is(ModItems.CASSETTE_BAG.get())) {
                    if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemstack1, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
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
        return blockEntity != null && player.level().getBlockEntity(pos) == blockEntity;
    }
}
