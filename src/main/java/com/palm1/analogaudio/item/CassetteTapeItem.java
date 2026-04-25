package com.palm1.analogaudio.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

import com.palm1.analogaudio.registry.ModDataComponents;

public class CassetteTapeItem extends Item {
    public CassetteTapeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        var data = stack.get(ModDataComponents.CASSETTE_DATA.get());
        if (data != null && !data.name().isEmpty()) {
            int insertIndex = Math.min(tooltip.size(), 1);
            tooltip.add(insertIndex, Component.literal(data.name()).withStyle(ChatFormatting.GRAY));
        }
    }
}
