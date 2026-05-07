package org.papiricoh.create_nuclearindustry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.create_nuclearindustry.reactor.gui.ReactorControlMenu;

public class AllNuclearGUIs {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Create_NuclearIndustry.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ReactorControlMenu>> REACTOR_MENU =
            MENUS.register("reactor_control", () -> new MenuType<ReactorControlMenu>((containerId, inventory) -> new ReactorControlMenu(containerId, inventory, null), FeatureFlags.DEFAULT_FLAGS));

    public static void init() {}
}
