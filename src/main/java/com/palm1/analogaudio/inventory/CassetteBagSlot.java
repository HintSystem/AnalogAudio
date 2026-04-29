package com.palm1.analogaudio.inventory;

import com.palm1.analogaudio.registry.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CassetteBagSlot extends Slot {
    public CassetteBagSlot(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.is(ModItems.CASSETTE_BAG.get());
    }
}
