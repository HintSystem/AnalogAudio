package com.palm1.analogaudio.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import com.palm1.analogaudio.registry.ModDataComponents;

import net.minecraft.client.Minecraft;

import java.util.List;

public class WalkieTalkieItem extends Item {
    public WalkieTalkieItem(Properties properties) {
        super(properties.component(ModDataComponents.FREQUENCY.get(), 1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemstack = player.getItemInHand(usedHand);

        if (level.isClientSide) {
            openFrequencyScreen(itemstack, usedHand);
        }

        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int freq = stack.getOrDefault(ModDataComponents.FREQUENCY.get(), 1);
        int insertIndex = Math.min(tooltip.size(), 1);
        tooltip.add(insertIndex, Component.translatable("tooltip.analogaudio.walkie_talkie.frequency", freq)
                .withStyle(ChatFormatting.GRAY));
    }

    private void openFrequencyScreen(ItemStack stack, InteractionHand hand) {
        int currentFreq = stack.getOrDefault(ModDataComponents.FREQUENCY.get(), 1);
        Minecraft.getInstance().setScreen(new com.palm1.analogaudio.client.gui.RotaryTunerScreen(currentFreq, freq -> {
            PacketDistributor
                    .sendToServer(new com.palm1.analogaudio.network.packet.SetFrequencyC2SPacket(hand, freq));
        }));
    }
}
