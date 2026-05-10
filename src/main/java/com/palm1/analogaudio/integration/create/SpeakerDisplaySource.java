package com.palm1.analogaudio.integration.create;

import com.palm1.analogaudio.block.entity.SpeakerBlockEntity;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

public class SpeakerDisplaySource extends DisplaySource {
    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        if (context.getSourceBlockEntity() instanceof SpeakerBlockEntity speaker) {
            List<String> sources = speaker.getActiveSources();
            if (sources.isEmpty()) {
                return List.of(Component.empty());
            }

            List<MutableComponent> lines = new ArrayList<>();
            String label = context.sourceConfig().getString("Label");

            for (int i = 0; i < sources.size(); i++) {
                MutableComponent line = Component.literal(sources.get(i));
                if (i == 0 && !label.isEmpty()) {
                    line = Component.literal(label + " ").append(line);
                }
                lines.add(line);
            }
            return lines;
        }
        return List.of(Component.empty());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initConfigurationWidgets(DisplayLinkContext context, ModularGuiLineBuilder builder,
            boolean isFirstLine) {
        if (isFirstLine) {
            builder.addTextInput(0, 137, (e, t) -> {
                e.setValue("");
                t.withTooltip(List.of(
                        Component.translatable("create.display_source.label").withStyle(s -> s.withColor(0x5391E1)),
                        Component.translatable("create.gui.schedule.lmb_edit").withStyle(ChatFormatting.DARK_GRAY,
                                ChatFormatting.ITALIC)));
            }, "Label");
        }
    }

    @Override
    public int getPassiveRefreshTicks() {
        return 100;
    }
}
