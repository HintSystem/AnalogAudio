package com.palm1.analogaudio.registry;

import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;

import com.palm1.analogaudio.client.gui.tooltip.CassetteBagTooltip;
import com.palm1.analogaudio.client.gui.tooltip.ClientCassetteBagTooltip;

public class ModTooltipComponents {
    public static void registerTooltipComponents(final RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(CassetteBagTooltip.class, ClientCassetteBagTooltip::new);
    }
}
