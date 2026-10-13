package org.papiricoh.create_nuclearindustry.fusion.display;

import net.minecraft.network.chat.Component;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakReadings;
import org.papiricoh.create_nuclearindustry.fusion.api.TripCause;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.physics.PlasmaModel;
import org.papiricoh.create_nuclearindustry.panel.PanelCard;

import java.util.List;

import static org.papiricoh.create_nuclearindustry.panel.PanelCard.*;
import static org.papiricoh.create_nuclearindustry.panel.PanelText.*;

/**
 * The tokamak's card on the panel. The summary follows a burn: phase, fusion power and gain, the
 * plasma (temperature, density, confinement, instability), the magnets and the vacuum, and why it
 * tripped. Sneaking shows the details instead: the torus, fuel, cooling and heating.
 */
public final class TokamakPanel {
    private static final String T = "tokamak.";

    private TokamakPanel() {}

    public static PanelCard card(FusionReactorBlockEntity tokamak, boolean detailed) {
        if (!tokamak.isFormed()) {
            PanelCard card = new PanelCard(label(T + "kicker"), label(T + "title"), null, RED)
                    .status(label("status.not_formed"), RED, true);
            card.add(new Alert(tokamak.getValidationText(), RED));
            card.add(new Note(label(T + "hint.structure")));
            return card;
        }
        TokamakReadings r = tokamak.readings();
        boolean tripped = r.tripCause() != TripCause.NONE;
        int accent = tripped ? RED : r.phase() == TokamakPhase.BURN ? AMBER : r.phase().hasPlasma() ? CYAN : DIM;
        PanelCard card = new PanelCard(label(T + "kicker_radius", tokamak.getMajorRadius()), Component.translatable(r.phase().langKey()),
                label(r.dtMode() ? T + "tag.dt" : T + "tag.dd"), accent);
        if (tripped) {
            card.status(label(T + "status.tripped"), RED, true);
        } else if (r.suspended()) {
            card.status(label(T + "status.suspended"), AMBER, true);
        } else if (r.phase() == TokamakPhase.BURN) {
            card.status(label(T + "status.burning"), GREEN, false);
        } else if (r.phase().hasPlasma()) {
            card.status(label(T + "status.plasma"), CYAN, false);
        } else {
            card.status(label(T + "status.standby"), DIM, false);
        }
        if (detailed) {
            details(card, tokamak, r);
            card.footer(label("hint.release"));
        } else {
            summary(card, r, accent);
            card.footer(label("hint.sneak"));
        }
        return card;
    }

    private static void summary(PanelCard card, TokamakReadings r, int accent) {
        card.add(new BigValue(Component.literal(Long.toString(Math.round(r.fusionPower()))), "MW", r.fusionPower() > 0 ? AMBER : DIM));
        card.add(new Grid(List.of(
                new Field(label(T + "gain"), Component.literal(FusionReactorBlockEntity.gainText(r.gain())),
                        Double.isInfinite(r.gain()) ? GREEN : TEXT),
                new Field(label(T + "heating"), unit("mw", number(r.heatingPower(), 1)), r.heatingEnabled() ? AMBER : DIM))));
        double temperature = r.temperature();
        int temperatureColor = temperature >= PlasmaModel.DANGER_TEMP ? RED : temperature >= PlasmaModel.IGNITION_TEMP ? GREEN : CYAN;
        card.add(new Bar(label(T + "temperature"), (float) (temperature / PlasmaModel.DANGER_TEMP),
                unit("degrees", Math.round(temperature)), temperatureColor));
        card.add(new Bar(label(T + "density"), (float) Math.min(1, r.density() / (PlasmaModel.DENSITY_LIMIT * 1.2)),
                percent(r.density()), r.density() > PlasmaModel.DENSITY_LIMIT ? RED : r.density() > 0.9 * PlasmaModel.DENSITY_LIMIT ? AMBER : CYAN));
        card.add(new Bar(label(T + "confinement"), (float) r.confinement(), percent(r.confinement()), CYAN));
        card.add(new Bar(label(T + "instability"), (float) (r.instability() / 100), unit("percent", Math.round(r.instability())),
                r.instability() > PlasmaModel.INSTABILITY_WARNING ? RED : CYAN));

        card.add(new Section(label(T + "magnets")));
        card.add(new Bar(label(T + "field"), (float) r.field(), percent(r.field()), r.field() > 0.01 ? accent : DIM));
        card.add(new Grid(List.of(
                new Field(label(T + "magnet_temperature"), unit("kelvin", number(r.magnetTemperature(), 1)), TEXT),
                new Field(label(T + "vacuum"), unit("pa", vacuum(r.vacuum())), TEXT))));

        if (r.tripCause() != TripCause.NONE) {
            card.add(new Alert(Component.translatable(r.tripCause().langKey()), RED));
        }
        if (r.suspended()) {
            card.add(new Alert(label(T + "suspended"), AMBER));
        }
        if (r.tritiumOverflow()) {
            card.add(new Alert(label(T + "tritium_overflow"), AMBER));
        }
    }

    private static void details(PanelCard card, FusionReactorBlockEntity tokamak, TokamakReadings r) {
        card.add(new Section(label(T + "structure")));
        card.add(new Grid(List.of(
                new Field(label(T + "radius"), Component.literal(Integer.toString(tokamak.getMajorRadius())), TEXT),
                new Field(label(T + "coils"), Component.literal(Integer.toString(tokamak.getCoilCount())), TEXT),
                new Field(label(T + "coverage"), percent(tokamak.getCoilCoverage()), tokamak.getCoilCoverage() < 1 ? AMBER : TEXT),
                new Field(label(T + "blankets"), Component.literal(Integer.toString(tokamak.getBlanketCount())), TEXT))));

        card.add(new Section(label(T + "fuel")));
        card.add(new Grid(List.of(
                new Field(label(T + "deuterium"), unit("mb", r.deuterium()), TEXT),
                new Field(label(T + "tritium"), unit("mb", r.tritium()), TEXT),
                new Field(label(T + "fuel_rate"), unit("mbt", number(r.fuelRate(), 2)), TEXT),
                new Field(label(T + "breeding"), Component.literal(number(r.tritiumBreeding(), 2)), TEXT),
                new Field(label(T + "ash"), unit("percent", number(r.ashFraction() * 100, 1)), r.ashFraction() >= 0.1 ? AMBER : TEXT),
                new Field(label(T + "burn_rate"), unit("mbt", number(r.burnRate(), 3)), TEXT))));

        card.add(new Section(label(T + "cooling")));
        card.add(new Grid(List.of(
                new Field(label(T + "coolant"), unit("mb", r.coolant()), TEXT),
                new Field(label(T + "plasma_steam"), unit("mb", r.plasmaSteam()), TEXT),
                new Field(label(T + "liquid_helium"), unit("mb", r.liquidHelium()), TEXT),
                new Field(label(T + "warm_helium"), unit("mb", r.warmHelium()), TEXT))));

        card.add(new Section(label(T + "heating")));
        card.add(new Grid(List.of(
                new Field(label(T + "heating_switch"), label(r.heatingEnabled() ? "on" : "off"), r.heatingEnabled() ? AMBER : DIM),
                new Field(label(T + "heating_level"), percent(r.heatingLevel()), TEXT),
                new Field(label(T + "heating"), unit("mw", number(r.heatingPower(), 1)), TEXT),
                new Field(label(T + "field_target"), percent(r.fieldTarget()), TEXT))));
    }

    /** Chamber pressure as a mantissa and exponent, such as 3e-4, for "%s Pa". */
    private static String vacuum(double log10Pascals) {
        return String.format(java.util.Locale.ROOT, "%.0e", Math.pow(10, log10Pascals)).replace("e+0", "e").replace("e-0", "e-");
    }
}
