package org.papiricoh.create_nuclearindustry.maintenance;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearDisplaySources;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.NuclearFeature;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry;
import org.papiricoh.create_nuclearindustry.creativetab.NuclearTabSection;
import org.papiricoh.create_nuclearindustry.creativetab.SectionedOutput;
import org.papiricoh.create_nuclearindustry.explosive.NuclearDetonationEvent;
import org.papiricoh.create_nuclearindustry.explosive.blast.DetonationStyle;
import org.papiricoh.create_nuclearindustry.maintenance.incident.IncidentLog;
import org.papiricoh.create_nuclearindustry.maintenance.incident.IncidentLogDisplaySource;
import org.papiricoh.create_nuclearindustry.maintenance.incident.IncidentType;
import org.papiricoh.create_nuclearindustry.maintenance.physics.InesScale;

import java.util.Locale;

/**
 * Plant operation and maintenance: the Maintenance Kit, natural earthquakes, the incident log and
 * its display source. The wearing parts live in the machines they belong to (see
 * {@link ReactorMaintenance}, {@link PartWear}). Every feature follows its {@code maintenance.*} switch.
 */
public final class MaintenanceRegistry implements NuclearFeature {
    public static final MaintenanceRegistry INSTANCE = new MaintenanceRegistry();

    public static final DeferredHolder<Item, MaintenanceKitItem> MAINTENANCE_KIT = AllNuclearItems.ITEMS.register("maintenance_kit",
            () -> new MaintenanceKitItem(new Item.Properties().durability(32).rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<DisplaySource, IncidentLogDisplaySource> INCIDENT_LOG_DISPLAY =
            AllNuclearDisplaySources.DISPLAY_SOURCES.register("incident_log", IncidentLogDisplaySource::new);

    private MaintenanceRegistry() {}

    @Override
    public void register(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(Earthquakes::onLevelTick);
        NeoForge.EVENT_BUS.addListener(MaintenanceRegistry::onDetonation);
        modEventBus.addListener(MaintenanceRegistry::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            DisplaySource.BY_BLOCK.add(AllNuclearBlocks.REACTOR_CONTROLLER.get(), INCIDENT_LOG_DISPLAY.get());
            DisplaySource.BY_BLOCK.add(CivilDefenseRegistry.SEISMIC_DETECTOR.get(), INCIDENT_LOG_DISPLAY.get());
        });
    }

    /** Nuclear and antimatter blasts go in the log; they are off the INES scale. */
    private static void onDetonation(NuclearDetonationEvent event) {
        DetonationStyle style = event.getStyle();
        String detail = (style == null ? "" : style.name().toLowerCase(Locale.ROOT) + ", ") + "power " + Math.round(event.getPower());
        IncidentLog.record(event.getLevel(), BlockPos.containing(event.getCenter()), IncidentType.DETONATION,
                InesScale.NONE, detail);
    }

    @Override
    public void fillCreativeTab(CreativeModeTab.ItemDisplayParameters parameters, SectionedOutput output) {
        output.section(NuclearTabSection.SAFETY);
        output.accept(MAINTENANCE_KIT.get());
    }
}
