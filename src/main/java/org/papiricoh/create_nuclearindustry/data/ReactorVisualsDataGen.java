package org.papiricoh.create_nuclearindustry.data;

import net.neoforged.neoforge.client.model.generators.ModelFile;
import org.papiricoh.create_nuclearindustry.reactor.visual.ReactorVisualsRegistry;

/** Lang, models and tags for the visible and audible fission reactor: the viewport, the fuel channels' looks and the sounds. */
final class ReactorVisualsDataGen extends AreaDataGen {
    static final ReactorVisualsDataGen INSTANCE = new ReactorVisualsDataGen();

    private ReactorVisualsDataGen() {}

    @Override
    void lang() {
        LangEntries.block("reactor_viewport", "Reactor Viewport", "Mirilla del reactor");

        String s = "subtitle.create_nuclearindustry.";
        LangEntries.add(s + "reactor_hum", "Reactor hums", "Zumba el reactor");
        LangEntries.add(s + "reactor_scram", "Control rods drop", "Caen las barras de control");
        LangEntries.add(s + "reactor_klaxon", "Reactor klaxon sounds", "Suena la bocina del reactor");
        LangEntries.add(s + "reactor_overtemp_alarm", "Over-temperature alarm", "Alarma de sobretemperatura");
        LangEntries.add(s + "steam_hiss", "Steam hisses", "Silba el vapor");
        LangEntries.add(s + "coolant_bubbling", "Coolant bubbles", "Burbujea el refrigerante");
        LangEntries.add(s + "relief_valve_lift", "Relief valve lifts", "Salta la válvula de alivio");
        LangEntries.add(s + "centrifuge_whine", "Centrifuge whines", "Silba la centrifugadora");
        LangEntries.add(s + "electrolyzer_buzz", "Electrolyzer buzzes", "Zumba el electrolizador");
        LangEntries.add(s + "melter_roar", "Melter roars", "Ruge el fundidor");
        LangEntries.add(s + "rtg_hum", "Radioisotope generator hums", "Zumba el generador de radioisótopos");
    }

    @Override
    void blockStates(NuclearBlockStateProvider provider) {
        ModelFile viewport = provider.models().cubeAll("reactor_viewport", provider.modLoc("block/reactor_viewport"))
                .renderType("cutout");
        provider.simpleBlockWithItem(ReactorVisualsRegistry.REACTOR_VIEWPORT.get(), viewport);
    }
}
