package org.papiricoh.create_nuclearindustry.integration.jei;

import net.minecraft.world.item.ItemStack;
import org.papiricoh.create_nuclearindustry.reactor.visual.ReactorVisualsRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * The guide's JEI information pages: the parts of the mod that had none (weapons, reactor ports,
 * tokamak structure...) and the reactor viewport. Free of JEI classes, like {@link PlantJeiInfo},
 * which lists these pages with its own.
 */
public final class GuideJeiInfo {

    private GuideJeiInfo() {}

    public static List<PlantJeiInfo.Page> pages() {
        List<PlantJeiInfo.Page> pages = new ArrayList<>();
        pages.add(new PlantJeiInfo.Page("reactor_viewport", List.of(new ItemStack(ReactorVisualsRegistry.REACTOR_VIEWPORT_ITEM.get())),
                List.of()));
        return pages;
    }
}
