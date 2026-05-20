package com.palm1.analogaudio.client.gui.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.component.ItemContainerContents;

public record CassetteBagTooltip(ItemContainerContents contents) implements TooltipComponent {}
