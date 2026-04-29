package com.palm1.analogaudio.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import com.palm1.analogaudio.AnalogAudio;
import com.palm1.analogaudio.inventory.CassetteDeckMenu;
import com.palm1.analogaudio.inventory.RadioMenu;

public class ModMenus {
        public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU,
                        AnalogAudio.MODID);

        public static final Supplier<MenuType<CassetteDeckMenu>> CASSETTE_DECK_MENU = MENUS.register(
                        "cassette_deck_menu",
                        () -> new MenuType<>(CassetteDeckMenu::new, FeatureFlags.DEFAULT_FLAGS));

        public static final Supplier<MenuType<RadioMenu>> RADIO_MENU = MENUS.register("radio_menu",
                        () -> IMenuTypeExtension.create(RadioMenu::new));

        public static final Supplier<MenuType<com.palm1.analogaudio.inventory.CassetteBagMenu>> CASSETTE_BAG_MENU = MENUS
                        .register(
                                        "cassette_bag_menu",
                                        () -> new MenuType<>(com.palm1.analogaudio.inventory.CassetteBagMenu::new,
                                                        net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
}
