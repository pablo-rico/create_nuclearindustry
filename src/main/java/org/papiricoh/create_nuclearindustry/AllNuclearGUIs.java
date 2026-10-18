package org.papiricoh.create_nuclearindustry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.create_nuclearindustry.explosive.gui.NuclearBombMenu;
import org.papiricoh.create_nuclearindustry.reactor.gui.ReactorControlMenu;

public class AllNuclearGUIs {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Create_NuclearIndustry.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ReactorControlMenu>> REACTOR_MENU =
            MENUS.register("reactor_control", () -> IMenuTypeExtension.create(
                    (containerId, inventory, buf) -> new ReactorControlMenu(
                            containerId,
                            inventory,
                            buf.readBlockPos(),
                            ReactorControlMenu.readInitialData(buf))));

    public static final DeferredHolder<MenuType<?>, MenuType<NuclearBombMenu>> NUCLEAR_BOMB_MENU =
            MENUS.register("nuclear_bomb", () -> IMenuTypeExtension.create(NuclearBombMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<org.papiricoh.create_nuclearindustry.controlroom.desk.ControlModuleMenu>> CONTROL_MODULE_MENU =
            MENUS.register("control_module", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(
                    org.papiricoh.create_nuclearindustry.controlroom.desk.ControlModuleMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<org.papiricoh.create_nuclearindustry.fusion.gui.TokamakMenu>> TOKAMAK_MENU =
            MENUS.register("tokamak_control", () -> IMenuTypeExtension.create(
                    org.papiricoh.create_nuclearindustry.fusion.gui.TokamakMenu::read));

    public static void init() {}
}
