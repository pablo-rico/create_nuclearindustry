package org.papiricoh.create_nuclearindustry.reactor.display;

import net.minecraft.network.chat.Component;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockText;
import org.papiricoh.create_nuclearindustry.panel.PanelCard;
import org.papiricoh.create_nuclearindustry.reactor.pwr.accident.ReactorAccidents;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.physics.FissionConfig;
import org.papiricoh.create_nuclearindustry.reactor.physics.kinetics.ReactivityModel;
import org.papiricoh.create_nuclearindustry.reactor.physics.kinetics.ShutdownState;
import org.papiricoh.create_nuclearindustry.reactor.physics.ReactorPhysicsSimulator;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PlantReadings;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PrimaryLoop;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrConfig;

import java.util.ArrayList;
import java.util.List;

import static org.papiricoh.create_nuclearindustry.panel.PanelCard.*;
import static org.papiricoh.create_nuclearindustry.panel.PanelText.*;

/**
 * The fission reactor's card on the panel. The summary is what an operator watches: state, core
 * temperature, power, rods, fuel and stress, the tanks, the pressurized loop and anything going
 * wrong. Sneaking shows the details instead: the structure, its ports, reactivity term by term and
 * the fuel inventory.
 */
public final class FissionPanel {
    private static final String F = "fission.";

    private FissionPanel() {}

    public static PanelCard card(ReactorBlockEntity reactor, boolean detailed) {
        ReactorBlockEntity.ReactorDisplaySnapshot snapshot = reactor.getDisplaySnapshot();
        PlantReadings plant = reactor.plantReadings();
        Component kicker = snapshot.dimensions().isEmpty() ? label(F + "kicker") : label(F + "kicker_size", snapshot.dimensions());
        if (!snapshot.formed()) {
            PanelCard card = new PanelCard(kicker, label(F + "title"), null, RED)
                    .status(label("status.not_formed"), RED, true);
            card.add(new Alert(MultiblockText.text(snapshot.validationMessage()), RED));
            card.add(new Note(label(F + "hint.structure")));
            return card;
        }
        int accent = accent(reactor, snapshot);
        PanelCard card = new PanelCard(kicker, ReactorText.state(snapshot.state()),
                label(plant.pwr() ? F + "tag.pwr" : F + "tag.bwr"), accent);
        status(card, reactor, snapshot);
        if (detailed) {
            details(card, reactor, snapshot);
            card.footer(label("hint.release"));
        } else {
            summary(card, reactor, snapshot, plant);
            card.footer(label("hint.sneak"));
        }
        return card;
    }

    private static int accent(ReactorBlockEntity reactor, ReactorBlockEntity.ReactorDisplaySnapshot snapshot) {
        if (danger(reactor, snapshot)) {
            return RED;
        }
        return switch (snapshot.state()) {
            case RUNNING -> CYAN;
            case IDLE -> DIM;
            default -> AMBER;
        };
    }

    private static boolean danger(ReactorBlockEntity reactor, ReactorBlockEntity.ReactorDisplaySnapshot snapshot) {
        return snapshot.coreDamage() > 0 || snapshot.thermalExcursionActive() || reactor.getAccidents().vessel().breached()
                || snapshot.state() == ReactorPhysicsSimulator.ReactorState.CRITICAL
                || snapshot.state() == ReactorPhysicsSimulator.ReactorState.MELTING;
    }

    private static void status(PanelCard card, ReactorBlockEntity reactor, ReactorBlockEntity.ReactorDisplaySnapshot snapshot) {
        if (reactor.getAutoScram().isTripped() || snapshot.manualScram()) {
            card.status(label(F + "status.scram"), RED, true);
        } else if (danger(reactor, snapshot)) {
            card.status(label(F + "status.danger"), RED, true);
        } else if (snapshot.state() == ReactorPhysicsSimulator.ReactorState.WARNING) {
            card.status(label(F + "status.warning"), AMBER, true);
        } else if (snapshot.state() == ReactorPhysicsSimulator.ReactorState.RUNNING) {
            card.status(label(F + "status.nominal"), GREEN, false);
        } else {
            card.status(label(F + "status.standby"), DIM, false);
        }
    }

    // ------------------------------------------------------------------ summary

    private static void summary(PanelCard card, ReactorBlockEntity reactor, ReactorBlockEntity.ReactorDisplaySnapshot snapshot,
                                PlantReadings plant) {
        double temperature = snapshot.coreTemperature();
        card.add(new BigValue(Component.literal(Long.toString(Math.round(temperature))), "°C", temperatureColor(temperature)));
        double power = snapshot.powerFraction();
        card.add(new Bar(label(F + "power"), (float) Math.min(1, power),
                label(F + "power_value", Math.round(power * 100), Math.round(snapshot.powerOutput())), power > 1.05 ? AMBER : CYAN));
        float rodTarget = reactor.getRodTarget();
        float rods = snapshot.controlRodInsertionRatio();
        card.add(new Bar(label(F + "rods"), rods, rodTarget >= 0
                ? label(F + "rods_to", Math.round(rods * 100), Math.round(rodTarget * 100)) : percent(rods), CYAN));
        double fuel = snapshot.fuelCapacity() > 0 ? snapshot.fuelRemaining() / snapshot.fuelCapacity() : 0;
        card.add(new Bar(label(F + "fuel"), (float) fuel, of(number(snapshot.fuelRemaining(), 1), number(snapshot.fuelCapacity(), 1)),
                fuel < 0.1 ? AMBER : CYAN));
        double stress = snapshot.thermalStress();
        card.add(new Bar(label(F + "stress"), (float) (stress / 100), unit("percent", Math.round(stress)), stressColor(stress)));

        List<Field> readings = new ArrayList<>();
        readings.add(new Field(label(F + "reactivity_short"), unit("pcm", signed(snapshot.netReactivity())), TEXT));
        readings.add(new Field(label(F + "coolant_temperature"), unit("celsius", Math.round(snapshot.coolantTemperature())), TEXT));
        if (snapshot.xenonPenalty() >= 0.005) {
            readings.add(new Field(label(F + "xenon"), unit("percent", Math.round(snapshot.xenonPenalty() * 100)),
                    snapshot.xenonPenalty() >= 0.15 ? AMBER : TEXT));
        }
        if (snapshot.decayHeatPower() >= 0.5) {
            readings.add(new Field(label(F + "decay_heat"), unit("mw", Math.round(snapshot.decayHeatPower())), TEXT));
        }
        card.add(new Grid(readings));

        card.add(new Section(label(F + "tanks")));
        int capacity = Math.max(1, snapshot.tankCapacity());
        card.add(new Bar(label(F + "coolant"), snapshot.coolantAmount() / (float) capacity,
                of(snapshot.coolantAmount(), snapshot.tankCapacity()), snapshot.coolantAmount() < capacity * 0.2f ? RED : CYAN));
        card.add(new Bar(label(F + "steam"), snapshot.steamAmount() / (float) capacity,
                of(snapshot.steamAmount(), snapshot.tankCapacity()), snapshot.steamAmount() > capacity * 0.9f ? AMBER : CYAN));

        shutdown(card, snapshot);
        primaryLoop(card, reactor, plant);
        alerts(card, reactor, snapshot);
    }

    /**
     * Where a core that is not running stands on its way to a cold shutdown (or any core with boron in
     * its water): its state, the margin the rods and boron hold once cold, the boron, the rods against
     * the fuel, and whether the coolant carries the decay heat away.
     */
    private static void shutdown(PanelCard card, ReactorBlockEntity.ReactorDisplaySnapshot snapshot) {
        ShutdownState state = snapshot.shutdownState();
        if (state == ShutdownState.OPERATING && snapshot.boronPpm() < 0.5) {
            return;
        }
        card.add(new Section(label(F + "shutdown")));
        double margin = snapshot.coldShutdownMargin();
        double required = FissionConfig.requiredShutdownMargin;
        long rods = (long) Math.floor(snapshot.effectiveRods() + 1.0e-6);
        int fuel = snapshot.uraniumRodCount();
        double decay = snapshot.decayHeatPower();
        double removed = snapshot.heatRemovedMw();
        // The long values get a line each; boron and rods share one.
        card.add(new Field(label(F + "shutdown.state"), ReactorText.shutdown(state), shutdownColor(state)));
        card.add(new Field(label(F + "shutdown.margin"), unit("pcm", signed(margin)),
                margin < required ? RED : margin < 2 * required ? AMBER : GREEN));
        card.add(new Grid(List.of(
                new Field(label(F + "shutdown.boron"), unit("ppm", Math.round(snapshot.boronPpm())), TEXT),
                new Field(label(F + "shutdown.rods"), of(rods, fuel), rods >= fuel ? GREEN : AMBER))));
        card.add(new Field(label(F + "shutdown.heat"), label(F + "shutdown.heat_value", Math.round(removed), Math.round(decay)),
                decayHeatUnremoved(snapshot) ? RED : TEXT));
    }

    /** The chain reaction is off, the decay heat worth watching, and the coolant takes away less than nine tenths of it. */
    private static boolean decayHeatUnremoved(ReactorBlockEntity.ReactorDisplaySnapshot snapshot) {
        return snapshot.shutdownState() != ShutdownState.OPERATING && snapshot.decayHeatPower() >= 0.5
                && snapshot.heatRemovedMw() < 0.9 * snapshot.decayHeatPower();
    }

    private static int shutdownColor(ShutdownState state) {
        return switch (state) {
            case COLD_SHUTDOWN -> GREEN;
            case HOT_SHUTDOWN -> CYAN;
            case SUBCRITICAL -> AMBER;
            case OPERATING -> TEXT;
        };
    }

    /** The pressurized loop when there is one, or the missing pressurizer of a reactor holding primary coolant. */
    private static void primaryLoop(PanelCard card, ReactorBlockEntity reactor, PlantReadings plant) {
        if (!plant.pwr()) {
            if (reactor.holdsPrimaryCoolant() && !reactor.hasPressurizer()) {
                card.add(new Alert(label(F + "no_pressurizer"), RED));
            }
            return;
        }
        PrimaryLoop loop = reactor.getPrimaryLoop();
        card.add(new Section(label(F + "primary")));
        int pressureColor = plant.pressure() >= PwrConfig.reliefOpenPressure ? RED : plant.subcooling() < 5 ? AMBER : TEXT;
        card.add(new Grid(List.of(
                new Field(label(F + "pressure"), unit("bar", Math.round(plant.pressure())), pressureColor),
                new Field(label(F + "subcooling"), unit("celsius", Math.round(plant.subcooling())), plant.subcooling() < 5 ? AMBER : TEXT),
                new Field(label(F + "hot_leg"), unit("celsius", Math.round(plant.hotLeg())), TEXT),
                new Field(label(F + "cold_leg"), unit("celsius", Math.round(plant.coldLeg())), TEXT),
                new Field(label(F + "flow"), unit("mbt", Math.round(plant.flow())), plant.flow() <= 0 ? RED : TEXT),
                new Field(label(F + "heaters"), label(loop.heatersRunning() ? "on" : "off"), loop.heatersRunning() ? AMBER : DIM))));
        card.add(new Bar(label(F + "pressurizer"), plant.pressurizerLevel(), percent(plant.pressurizerLevel()), CYAN));
        card.add(new Bar(label(F + "vessel"), plant.vesselLevel(), percent(plant.vesselLevel()), plant.vesselLevel() < 1 ? RED : CYAN));
        if (loop.reliefStuck()) {
            card.add(new Alert(label(F + "relief_stuck"), RED));
        } else if (loop.reliefOpen()) {
            card.add(new Alert(label(F + "relief_open"), RED));
        }
    }

    /** Containment, cladding, vessel, the core and its SCRAMs, and fuel that cannot get out. */
    private static void alerts(PanelCard card, ReactorBlockEntity reactor, ReactorBlockEntity.ReactorDisplaySnapshot snapshot) {
        ReactorAccidents accidents = reactor.getAccidents();
        if (accidents.containmentPressure() > 0) {
            card.add(new Field(label(F + "containment"), label(F + "containment_value", number(accidents.containmentPressure(), 2),
                    number(accidents.containmentHydrogen() * 100, 1)), accidents.containmentHydrogen() >= 0.04f ? AMBER : TEXT));
        }
        double building = accidents.buildingHydrogen();
        if (building >= 0.001) {
            card.add(new Alert(label(F + "building_hydrogen", number(building * 100, 1)), building >= 0.04 ? RED : AMBER));
        }
        double cladding = accidents.cladding().damage(snapshot.uraniumRodCount(), PwrConfig.zirconiumPerRod);
        if (cladding >= 0.005) {
            card.add(new Alert(label(F + "cladding", Math.round(cladding * 100)), RED));
        }
        if (accidents.vessel().breached()) {
            card.add(new Alert(label(F + "breached"), RED));
        }
        if (snapshot.coreDamage() > 0) {
            card.add(new Alert(label(F + "core_damage", Math.max(1, Math.round(snapshot.coreDamage() * 100))), RED));
        }
        if (snapshot.thermalExcursionActive()) {
            card.add(new Alert(label(F + "excursion"), RED));
        }
        if (snapshot.fuelRemaining() > 0 && snapshot.coreCoverage() < ShutdownState.COVERED) {
            card.add(new Alert(label(F + "core_uncovered", Math.round(snapshot.coreCoverage() * 100)), RED));
        }
        if (decayHeatUnremoved(snapshot)) {
            card.add(new Alert(label(F + "decay_unremoved"), RED));
        }
        if (snapshot.bleeding()) {
            boolean contaminated = FissionConfig.contaminatedDamage > 0 && snapshot.coreDamage() >= FissionConfig.contaminatedDamage;
            card.add(contaminated ? new Alert(label(F + "bleeding_contaminated"), AMBER) : new Note(label(F + "bleeding")));
        }
        if (reactor.getAutoScram().isTripped()) {
            card.add(new Alert(label(F + "auto_scram", reactor.getAutoScram().getThreshold()), RED));
        } else if (snapshot.manualScram()) {
            card.add(new Alert(label(F + "manual_scram"), RED));
        }
        if (snapshot.pendingDepletedFuel() > 0) {
            card.add(new Alert(label(F + "output_blocked", snapshot.pendingDepletedFuel()), AMBER));
        }
        if (snapshot.fuelRemaining() > 0 && snapshot.neutronLevel() <= 1 && snapshot.controlRodInsertionRatio() >= 0.99f) {
            card.add(new Note(label(F + "suppressed")));
        }
    }

    // ------------------------------------------------------------------ details

    private static void details(PanelCard card, ReactorBlockEntity reactor, ReactorBlockEntity.ReactorDisplaySnapshot snapshot) {
        card.add(new Section(label(F + "structure")));
        List<Field> structure = new ArrayList<>(List.of(
                new Field(label(F + "fuel_columns"), Component.literal(Integer.toString(snapshot.uraniumColumnCount())), TEXT),
                new Field(label(F + "exchanger_columns"), Component.literal(Integer.toString(snapshot.heatExchangerColumnCount())), TEXT),
                new Field(label(F + "control_columns"), Component.literal(Integer.toString(snapshot.controlRodColumnCount())), TEXT),
                new Field(label(F + "rod_segments"), of(snapshot.insertedControlRodSegments(), snapshot.expectedControlRodSegments()), TEXT),
                new Field(label(F + "static_rods"), Component.literal(Integer.toString(snapshot.staticControlRodSegments())), TEXT),
                new Field(label(F + "moving_rods"), Component.literal(Integer.toString(snapshot.movingControlRodSegments())), TEXT)));
        if (snapshot.moderatorColumnCount() > 0) {
            structure.add(new Field(label(F + "graphite_columns"),
                    label(F + "count_share", snapshot.moderatorColumnCount(), Math.round(snapshot.moderation() * 100)), TEXT));
        }
        if (snapshot.reflectorColumnCount() > 0) {
            structure.add(new Field(label(F + "reflector_columns"),
                    label(F + "count_share", snapshot.reflectorColumnCount(), Math.round(snapshot.reflection() * 100)), TEXT));
        }
        card.add(new Grid(structure));

        card.add(new Section(label(F + "ports")));
        card.add(new Grid(List.of(
                new Field(label(F + "coolant_ports"), Component.literal(Integer.toString(snapshot.coolantInputPortCount())), TEXT),
                new Field(label(F + "steam_ports"), Component.literal(Integer.toString(snapshot.steamOutputPortCount())), TEXT),
                new Field(label(F + "fuel_ports"), Component.literal(Integer.toString(snapshot.fuelPortCount())), TEXT),
                new Field(label(F + "rated_power"), unit("mw", Math.round(snapshot.ratedPower())), TEXT))));

        card.add(new Section(label(F + "reactivity")));
        ReactivityModel.Parts parts = snapshot.reactivityParts();
        card.add(new Grid(List.of(
                new Field(label(F + "net"), unit("pcm", signed(snapshot.netReactivity())), TEXT),
                new Field(label(F + "k"), Component.literal(number(snapshot.reactivity(), 3)), TEXT),
                new Field(label(F + "term.fuel"), unit("pcm", signed(parts.fuel())), TEXT),
                new Field(label(F + "term.rods"), unit("pcm", signed(parts.rods())), TEXT),
                new Field(label(F + "term.temperature"), unit("pcm", signed(parts.temperature())), TEXT),
                new Field(label(F + "term.voids"), unit("pcm", signed(parts.voids())), TEXT),
                new Field(label(F + "term.xenon"), unit("pcm", signed(parts.xenon())), TEXT),
                new Field(label(F + "term.boron"), unit("pcm", signed(parts.boron())), TEXT),
                new Field(label(F + "neutrons"), Component.literal(Long.toString(Math.round(snapshot.neutronLevel()))), TEXT))));

        card.add(new Section(label(F + "fuel")));
        List<Field> fuel = new ArrayList<>(List.of(
                new Field(label(F + "fuel_left"), of(number(snapshot.fuelRemaining(), 1), number(snapshot.fuelCapacity(), 1)), TEXT),
                new Field(label(F + "waiting"), Component.literal(Integer.toString(snapshot.inputFuelCount())), TEXT),
                new Field(label(F + "depleted"), Component.literal(Integer.toString(snapshot.outputFuelCount())), TEXT)));
        if (snapshot.fuelEnrichment() > 0) {
            fuel.add(new Field(label(F + "enrichment"), unit("percent", number(snapshot.fuelEnrichment(), 1)), TEXT));
        }
        card.add(new Grid(fuel));
    }

    // ------------------------------------------------------------------ colours

    private static int temperatureColor(double temperature) {
        if (temperature >= FissionConfig.dangerTemperature()) {
            return RED;
        }
        return temperature >= FissionConfig.warningTemperature() ? AMBER : CYAN;
    }

    private static int stressColor(double stress) {
        return stress >= 90 ? RED : stress >= 65 ? AMBER : CYAN;
    }
}
