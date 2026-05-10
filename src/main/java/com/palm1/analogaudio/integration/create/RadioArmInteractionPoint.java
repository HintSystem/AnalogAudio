package com.palm1.analogaudio.integration.create;

import com.palm1.analogaudio.block.entity.RadioBlockEntity;
import com.palm1.analogaudio.registry.ModBlocks;
import com.palm1.analogaudio.registry.ModItems;
import com.simibubi.create.content.kinetics.mechanicalArm.AllArmInteractionPointTypes;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class RadioArmInteractionPoint extends AllArmInteractionPointTypes.JukeboxPoint {

    public RadioArmInteractionPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
        super(type, level, pos, state);
    }

    public static class Type extends ArmInteractionPointType {
        @Override
        public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
            return state.is(ModBlocks.RADIO.get());
        }

        @Override
        public RadioArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
            return new RadioArmInteractionPoint(this, level, pos, state);
        }

        @Override
        public int getPriority() {
            return 100;
        }
    }

    @Override
    public int getSlotCount(ArmBlockEntity armBlockEntity) {
        return 2;
    }

    @Override
    public ItemStack insert(ArmBlockEntity armBlockEntity, ItemStack stack, boolean simulate) {
        if (!(level.getBlockEntity(pos) instanceof RadioBlockEntity radio))
            return stack;

        if (stack.is(ModItems.CASSETTE_TAPE.get())) {
            if (!radio.getItem(0).isEmpty())
                return stack;
            ItemStack remainder = stack.copy();
            ItemStack toInsert = remainder.split(1);
            if (!simulate)
                radio.setItem(0, toInsert);
            return remainder;
        }

        if (stack.is(ModItems.CASSETTE_BAG.get())) {
            if (!radio.getItem(1).isEmpty())
                return stack;
            ItemStack remainder = stack.copy();
            ItemStack toInsert = remainder.split(1);
            if (!simulate)
                radio.setItem(1, toInsert);
            return remainder;
        }

        return stack;
    }

    @Override
    public ItemStack extract(ArmBlockEntity armBlockEntity, int slot, int amount, boolean simulate) {
        if (!(level.getBlockEntity(pos) instanceof RadioBlockEntity radio))
            return ItemStack.EMPTY;

        if (simulate)
            return radio.getItem(slot).copy();

        return radio.removeItem(slot, amount);
    }
}
