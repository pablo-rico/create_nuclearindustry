package org.papiricoh.create_nuclearindustry;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import org.papiricoh.create_nuclearindustry.reactor.display.ReactorReadout;
import org.papiricoh.create_nuclearindustry.reactor.display.ReactorReadoutDisplaySource;
import org.papiricoh.create_nuclearindustry.reactor.display.ReactorSensorDisplaySource;

import java.util.EnumMap;
import java.util.Map;

public class AllNuclearDisplaySources {
    public static final DeferredRegister<DisplaySource> DISPLAY_SOURCES =
            DeferredRegister.create(CreateBuiltInRegistries.DISPLAY_SOURCE, Create_NuclearIndustry.MODID);

    public static final DeferredHolder<DisplaySource, ReactorSensorDisplaySource> REACTOR_TEMPERATURE_SENSOR =
            DISPLAY_SOURCES.register("reactor_temperature_sensor", ReactorSensorDisplaySource::new);

    public static final DeferredHolder<DisplaySource, org.papiricoh.create_nuclearindustry.fusion.display.TokamakDisplaySource> TOKAMAK =
            DISPLAY_SOURCES.register("tokamak", org.papiricoh.create_nuclearindustry.fusion.display.TokamakDisplaySource::new);

    public static final DeferredHolder<DisplaySource, org.papiricoh.create_nuclearindustry.turbine.TurbineDisplaySource> TURBINE =
            DISPLAY_SOURCES.register("turbine", org.papiricoh.create_nuclearindustry.turbine.TurbineDisplaySource::new);

    public static final DeferredHolder<DisplaySource, org.papiricoh.create_nuclearindustry.coolingtower.CoolingTowerDisplaySource> COOLING_TOWER =
            DISPLAY_SOURCES.register("cooling_tower", org.papiricoh.create_nuclearindustry.coolingtower.CoolingTowerDisplaySource::new);

    public static final DeferredHolder<DisplaySource, org.papiricoh.create_nuclearindustry.civildefense.seismic.SeismicDisplaySource> SEISMIC_DETECTOR =
            DISPLAY_SOURCES.register("seismic_detector", org.papiricoh.create_nuclearindustry.civildefense.seismic.SeismicDisplaySource::new);

    public static final DeferredHolder<DisplaySource, org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorDisplaySource> ANNUNCIATOR =
            DISPLAY_SOURCES.register("annunciator", org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorDisplaySource::new);

    /** One source per reactor reading, in {@link ReactorReadout} order. */
    public static final Map<ReactorReadout, DeferredHolder<DisplaySource, SingleLineDisplaySource>> REACTOR_READOUTS = readouts();

    private static Map<ReactorReadout, DeferredHolder<DisplaySource, SingleLineDisplaySource>> readouts() {
        Map<ReactorReadout, DeferredHolder<DisplaySource, SingleLineDisplaySource>> sources = new EnumMap<>(ReactorReadout.class);
        for (ReactorReadout readout : ReactorReadout.values()) {
            sources.put(readout, DISPLAY_SOURCES.register(readout.id(), () -> ReactorReadoutDisplaySource.of(readout)));
        }
        return sources;
    }

    public static void register(IEventBus modEventBus) {
        DISPLAY_SOURCES.register(modEventBus);
        modEventBus.addListener(AllNuclearDisplaySources::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            DisplaySource.BY_BLOCK_ENTITY.add(AllNuclearEntities.REACTOR_TEMPERATURE_SENSOR.get(), REACTOR_TEMPERATURE_SENSOR.get());
            for (DeferredHolder<DisplaySource, SingleLineDisplaySource> source : REACTOR_READOUTS.values()) {
                DisplaySource.BY_BLOCK_ENTITY.add(AllNuclearEntities.REACTOR.get(), source.get());
                DisplaySource.BY_BLOCK_ENTITY.add(AllNuclearEntities.REACTOR_TEMPERATURE_SENSOR.get(), source.get());
                DisplaySource.BY_BLOCK_ENTITY.add(AllNuclearEntities.REACTOR_REDSTONE_PORT.get(), source.get());
                // Control room devices linked to a reactor pass its readings on too.
                DisplaySource.BY_BLOCK_ENTITY.add(AllNuclearEntities.CONTROL_MONITOR.get(), source.get());
                DisplaySource.BY_BLOCK_ENTITY.add(AllNuclearEntities.REACTOR_CONTROL_PANEL.get(), source.get());
            }
            DisplaySource.BY_BLOCK_ENTITY.add(AllNuclearEntities.TURBINE_OUTPUT.get(), TURBINE.get());
            DisplaySource.BY_BLOCK_ENTITY.add(AllNuclearEntities.FUSION_PLASMA_TURBINE.get(), TURBINE.get());
            DisplaySource.BY_BLOCK_ENTITY.add(AllNuclearEntities.FUSION_CONTROLLER.get(), TOKAMAK.get());
            DisplaySource.BY_BLOCK_ENTITY.add(AllNuclearEntities.COOLING_TOWER_CONTROLLER.get(), COOLING_TOWER.get());
            DisplaySource.BY_BLOCK_ENTITY.add(org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry.ANNUNCIATOR_ENTITY.get(),
                    ANNUNCIATOR.get());
            DisplaySource.BY_BLOCK_ENTITY.add(org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry.SEISMIC_DETECTOR_BE.get(),
                    SEISMIC_DETECTOR.get());
        });
    }
}
