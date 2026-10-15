package org.papiricoh.create_nuclearindustry.isotope;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry;
import org.papiricoh.create_nuclearindustry.integration.jei.PlantJeiInfo;
import org.papiricoh.create_nuclearindustry.maintenance.MaintenanceRegistry;

import java.util.List;

/**
 * JEI information pages of isotopes and nuclear medicine, and of plant operation and maintenance. Free of JEI classes, like
 * {@link PlantJeiInfo}, which lists them with its own.
 */
public final class IsotopeInfo {
    private IsotopeInfo() {}

    public static List<PlantJeiInfo.Page> pages() {
        return List.of(
                page("isotope_decay", stack(IsotopeRegistry.COBALT_60.get()), stack(IsotopeRegistry.STRONTIUM_90.get()),
                        stack(IsotopeRegistry.AMERICIUM_241.get()), stack(IsotopeRegistry.TECHNETIUM_99M.get())),
                page("cobalt", stack(IsotopeRegistry.COBALT_NUGGET.get()), stack(IsotopeRegistry.COBALT_INGOT.get())),
                page("reactor_irradiation_channel", stack(IsotopeRegistry.IRRADIATION_CHANNEL_ITEM.get())),
                page("gamma_irradiator", stack(IsotopeRegistry.GAMMA_IRRADIATOR_ITEM.get())),
                page("smoke_detector", stack(IsotopeRegistry.SMOKE_DETECTOR_ITEM.get())),
                page("strontium_90", stack(IsotopeRegistry.STRONTIUM_RTG_PELLET.get())),
                page("americium_241", stack(IsotopeRegistry.AMERICIUM_241.get())),
                page("technetium_generator", stack(IsotopeRegistry.TECHNETIUM_GENERATOR.get())),
                page("medical_isotope_kit", stack(IsotopeRegistry.MEDICAL_ISOTOPE_KIT.get())),
                page("radiography_camera", stack(IsotopeRegistry.RADIOGRAPHY_CAMERA.get())),
                // Operation and maintenance (text in MaintenanceDataGen)
                page("maintenance_kit", stack(MaintenanceRegistry.MAINTENANCE_KIT.get())),
                page("refuelling_outage", stack(AllNuclearItems.REACTOR_FUEL_PORT.get())),
                page("incident_log", stack(CivilDefenseRegistry.SEISMIC_DETECTOR_ITEM.get())));
    }

    private static ItemStack stack(ItemLike item) {
        return new ItemStack(item);
    }

    private static PlantJeiInfo.Page page(String id, ItemStack... stacks) {
        return new PlantJeiInfo.Page(id, List.of(stacks), List.of());
    }
}
