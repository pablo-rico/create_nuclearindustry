package org.papiricoh.create_nuclearindustry.reactor.display;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorRedstonePortBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorTemperatureSensorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PlantReadings;

import java.util.Optional;
import java.util.function.Function;

/** One value of a fission reactor, as a single Display Link line. */
public enum ReactorReadout {
    TEMPERATURE("reactor_temperature", true, reactor -> Component.literal(Math.round(reactor.getCoreTemperature()) + "°C")),
    POWER("reactor_power", true, reactor -> Component.literal(Math.round(reactor.getPowerOutput()) + " MW")),
    FUEL("reactor_fuel", true, reactor -> Component.literal(percent(reactor.getFuelRemaining(), reactor.getFuelCapacity()) + "%")),
    CONTROL_RODS("reactor_control_rods", true,
            reactor -> Component.literal(Math.round(reactor.getControlRodInsertionRatio() * 100) + "%")),
    COOLANT("reactor_coolant", true, reactor -> Component.literal(reactor.getCoolantAmount() + " mB")),
    STEAM("reactor_steam", true, reactor -> Component.literal(reactor.getSteamAmount() + " mB")),
    XENON("reactor_xenon", true, reactor -> Component.literal(Math.round(reactor.getXenonPenalty() * 100) + "%")),
    DECAY_HEAT("reactor_decay_heat", true, reactor -> Component.literal(Math.round(reactor.getDecayHeatPower()) + " MW")),
    REACTIVITY("reactor_reactivity", true, reactor -> Component.literal(String.format(java.util.Locale.ROOT, "x%.2f", reactor.getReactivity()))),
    // Pressurized water loop; a dash for a reactor that boils its water directly
    PRESSURE("reactor_pressure", true, reactor -> plant(reactor, p -> Math.round(p.pressure()) + " bar")),
    HOT_LEG("reactor_hot_leg", true, reactor -> plant(reactor, p -> Math.round(p.hotLeg()) + "°C")),
    COLD_LEG("reactor_cold_leg", true, reactor -> plant(reactor, p -> Math.round(p.coldLeg()) + "°C")),
    SUBCOOLING("reactor_subcooling", true, reactor -> plant(reactor, p -> Math.round(p.subcooling()) + "°C")),
    PRESSURIZER_LEVEL("reactor_pressurizer_level", true, reactor -> plant(reactor, p -> Math.round(p.pressurizerLevel() * 100) + "%")),
    // Containment around the reactor; a dash without one
    CONTAINMENT_PRESSURE("reactor_containment_pressure", true, reactor -> containment(reactor,
            p -> String.format(java.util.Locale.ROOT, "%.2f bar", p.containmentPressure()))),
    HYDROGEN("reactor_hydrogen", true, reactor -> containment(reactor,
            p -> String.format(java.util.Locale.ROOT, "%.1f%%", p.hydrogen() * 100))),
    // The fission core's physics: power against what its fuel is rated for, reactivity, coolant and damage
    POWER_LEVEL("reactor_power_level", true, reactor -> Component.literal(
            Math.round(reactor.getRatedPower() <= 0 ? 0 : reactor.getPowerOutput() / reactor.getRatedPower() * 100) + "%")),
    NET_REACTIVITY("reactor_net_reactivity", true, reactor -> Component.literal(
            String.format(java.util.Locale.ROOT, "%+d pcm", Math.round(reactor.getNetReactivity())))),
    COOLANT_TEMPERATURE("reactor_coolant_temperature", true, reactor -> Component.literal(Math.round(reactor.getCoolantTemperature()) + "°C")),
    CORE_DAMAGE("reactor_core_damage", true, reactor -> Component.literal(Math.round(reactor.getCoreDamage() * 100) + "%")),
    // The net reactivity split into fuel, rods, temperature, steam and xenon, pcm with their signs
    REACTIVITY_PARTS("reactor_reactivity_parts", false, reactor -> {
        var parts = reactor.getReactivityParts();
        return Component.literal(String.format(java.util.Locale.ROOT, "F%+d R%+d T%+d V%+d Xe%+d B%+d", Math.round(parts.fuel()),
                Math.round(parts.rods()), Math.round(parts.temperature()), Math.round(parts.voids()), Math.round(parts.xenon()),
                Math.round(parts.boron())));
    }),
    STATUS("reactor_status", false, ReactorReadout::status),
    // Safe shutdown: where the core stands, what holds it down and whether its decay heat is carried away
    SHUTDOWN_STATE("reactor_shutdown_state", false, reactor -> ReactorText.shutdown(reactor.getShutdownState())),
    /** What every rod in (and the boron) would hold back once the core is cold, pcm. */
    COLD_MARGIN("reactor_cold_margin", true, reactor -> Component.literal(
            String.format(java.util.Locale.ROOT, "%+d pcm", Math.round(reactor.getColdShutdownMargin(true))))),
    BORON("reactor_boron", true, reactor -> Component.literal(Math.round(reactor.getBoronPpm()) + " ppm")),
    CORE_LEVEL("reactor_core_level", true, reactor -> Component.literal(Math.round(reactor.getCoreCoverage() * 100) + "%")),
    /** Heat the coolant takes away against the decay heat, MW. */
    HEAT_REMOVAL("reactor_heat_removal", false, reactor -> Component.literal(
            Math.round(reactor.getHeatRemovedMw()) + " / " + Math.round(reactor.getDecayHeatPower()) + " MW")),
    /** Rod blocks the inserted rods are worth against the fuel rod blocks: at least as many holds any core down. */
    RODS_INSERTED("reactor_rods_inserted", false, reactor -> Component.literal(
            (long) Math.floor(reactor.getEffectiveRods() + 1.0e-6) + " / " + reactor.getFuelRodCount())),
    /** Hydrogen in the closed room around a reactor with no containment. */
    BUILDING_HYDROGEN("reactor_building_hydrogen", true, reactor -> Component.literal(String.format(java.util.Locale.ROOT, "%.1f%%",
            org.papiricoh.create_nuclearindustry.controlroom.MonitorReading.buildingHydrogen(reactor) * 100)));

    private final String id;
    private final boolean numeric;
    private final Function<ReactorBlockEntity, MutableComponent> reading;

    ReactorReadout(String id, boolean numeric, Function<ReactorBlockEntity, MutableComponent> reading) {
        this.id = id;
        this.numeric = numeric;
        this.reading = reading;
    }

    public String id() {
        return id;
    }

    public boolean numeric() {
        return numeric;
    }

    /** The reading, or a dash while there is no formed reactor. */
    public MutableComponent read(ReactorBlockEntity reactor) {
        if (this != STATUS && !reactor.isFormed()) {
            return Component.literal("-");
        }
        return reading.apply(reactor);
    }

    /** The reactor behind a Display Link source: the controller itself, a linked sensor or redstone port, or a control room device. */
    public static Optional<ReactorBlockEntity> reactorOf(BlockEntity source) {
        if (source instanceof ReactorBlockEntity reactor) {
            return Optional.of(reactor);
        }
        if (source instanceof ReactorTemperatureSensorBlockEntity sensor) {
            return sensor.getLinkedReactor();
        }
        if (source instanceof ReactorRedstonePortBlockEntity port) {
            return port.getLinkedReactor();
        }
        if (source instanceof org.papiricoh.create_nuclearindustry.controlroom.ControlRoomDevice device) {
            return org.papiricoh.create_nuclearindustry.controlroom.LinkedTarget.reactorOf(source, device.getTarget());
        }
        return Optional.empty();
    }

    private static MutableComponent plant(ReactorBlockEntity reactor, Function<PlantReadings, String> format) {
        PlantReadings plant = reactor.plantReadings();
        return Component.literal(plant.pwr() ? format.apply(plant) : "-");
    }

    private static MutableComponent containment(ReactorBlockEntity reactor, Function<PlantReadings, String> format) {
        PlantReadings plant = reactor.plantReadings();
        return Component.literal(plant.containmentPressure() > 0 ? format.apply(plant) : "-");
    }

    private static long percent(double amount, double capacity) {
        return capacity <= 0 ? 0 : Math.round(amount / capacity * 100);
    }

    private static MutableComponent status(ReactorBlockEntity reactor) {
        if (!reactor.isFormed()) {
            return ReactorText.display("unformed");
        }
        if (reactor.getAutoScram().isTripped()) {
            return ReactorText.display("auto_scram");
        }
        if (reactor.isScramRequested()) {
            return ReactorText.display("scram");
        }
        return ReactorText.state(reactor.getReactorState());
    }
}
