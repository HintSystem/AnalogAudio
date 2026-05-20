package com.palm1.analogaudio.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import java.util.List;
import java.util.Optional;

import com.palm1.analogaudio.client.gui.tooltip.CassetteBagTooltip;
import com.palm1.analogaudio.inventory.CassetteBagMenu;
import com.palm1.analogaudio.registry.ModDataComponents;

public class CassetteBagItem extends Item {
    public CassetteBagItem(Properties properties) {
        super(properties);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return !stack.has(DataComponents.HIDE_TOOLTIP) && !stack.has(DataComponents.HIDE_ADDITIONAL_TOOLTIP)
            ? Optional.ofNullable(stack.get(ModDataComponents.BAG_CONTENTS)).map(CassetteBagTooltip::new)
            : Optional.empty();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new CassetteBagMenu(id, inv, stack),
                    Component.translatable("item.analogaudio.cassette_bag")));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ItemContainerContents contents = stack.get(ModDataComponents.BAG_CONTENTS.get());
        int count = 0;
        if (contents != null) {
            for (ItemStack s : contents.stream().toList()) {
                if (!s.isEmpty()) {
                    count++;
                }
            }
        }

        tooltip.add(
            Component.translatable("tooltip.analogaudio.cassette_bag.count", count).withStyle(ChatFormatting.GRAY));
    }
}
