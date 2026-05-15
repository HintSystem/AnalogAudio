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
        if (data != null) {
            if (!data.name().isEmpty()) {
                int insertIndex = Math.min(tooltip.size(), 1);
                tooltip.add(insertIndex, Component.literal(data.name()).withStyle(ChatFormatting.GRAY));
            }
            if (com.palm1.analogaudio.config.ModConfig.Client.cassetteTapeDisclaimers) {
                if (data.url().startsWith("client:") || data.url().startsWith("file:///")) {
                    boolean isAuthor = false;
                    if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                        var player = net.minecraft.client.Minecraft.getInstance().player;
                        if (player != null && player.getUUID().toString().equals(data.authorUuid())) {
                            isAuthor = true;
                        }
                    }

                    if (isAuthor) {
                        tooltip.add(Component.translatable("tooltip.analogaudio.local_only_author").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
                    } else {
                        tooltip.add(Component.translatable("tooltip.analogaudio.local_only_other").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
                    }
                }
                if (data.url().startsWith("server:")) {
                    tooltip.add(Component.translatable("tooltip.analogaudio.server_audio").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
                }
            }

        }
    }
}
