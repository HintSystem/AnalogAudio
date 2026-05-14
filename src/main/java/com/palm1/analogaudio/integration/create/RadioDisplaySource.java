package com.palm1.analogaudio.integration.create;

import com.palm1.analogaudio.block.entity.RadioBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import com.palm1.analogaudio.registry.ModDataComponents;
import com.palm1.analogaudio.item.CassetteData;

public class RadioDisplaySource extends SingleLineDisplaySource {
    @Override
    protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
        if (context.getSourceBlockEntity() instanceof RadioBlockEntity radio) {
            ItemStack cassette = radio.getCassette();
            if (cassette.isEmpty()) {
                return Component.empty();
            }

            CassetteData data = cassette.get(ModDataComponents.CASSETTE_DATA.get());
            String name = data != null ? data.name() : cassette.getHoverName().getString();

            if (data != null && data.duration() > 0) {
                long currentMs = radio.getCurrentPositionMs();
                currentMs = Math.min(currentMs, data.duration());

                String current = formatTime(currentMs);
                String total = formatTime(data.duration());

                return Component.literal(name + " - (" + current + "/" + total + ")");
            }

            return Component.literal(name);
        }
        return Component.empty();
    }

    @Override
    protected boolean allowsLabeling(DisplayLinkContext context) {
        return true;
    }

    private String formatTime(long ms) {
        long seconds = (ms / 1000) % 60;
        long minutes = (ms / (1000 * 60));

        return String.format("%02d:%02d", minutes, seconds);
    }
}
