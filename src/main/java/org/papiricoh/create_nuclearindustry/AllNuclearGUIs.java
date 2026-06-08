package org.papiricoh.create_nuclearindustry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.create_nuclearindustry.explosive.gui.NuclearBombMenu;
import org.papiricoh.create_nuclearindustry.missile.gui.LaunchPadMenu;
import org.papiricoh.create_nuclearindustry.reactor.gui.ReactorControlMenu;

public class AllNuclearGUIs {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Create_NuclearIndustry.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ReactorControlMenu>> REACTOR_MENU =
            MENUS.register("reactor_control", () -> new MenuType<ReactorControlMenu>((containerId, inventory) -> new ReactorControlMenu(containerId, inventory, null), FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<NuclearBombMenu>> NUCLEAR_BOMB_MENU =
            MENUS.register("nuclear_bomb", () -> new MenuType<NuclearBombMenu>((containerId, inventory) -> new NuclearBombMenu(containerId, inventory, null), FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<LaunchPadMenu>> LAUNCH_PAD_MENU =
            MENUS.register("launch_pad", () -> new MenuType<LaunchPadMenu>((containerId, inventory) -> new LaunchPadMenu(containerId, inventory, null), FeatureFlags.DEFAULT_FLAGS));

    public static void init() {}
}
