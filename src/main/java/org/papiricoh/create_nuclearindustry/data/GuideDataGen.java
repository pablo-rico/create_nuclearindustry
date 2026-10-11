package org.papiricoh.create_nuclearindustry.data;

/**
 * Lang for the guide: the advancement tabs, the Ponder scenes of civil defense, weapons, the fuel
 * cycle and the fan treatments, and the JEI pages that were missing.
 */
final class GuideDataGen extends AreaDataGen {
    static final GuideDataGen INSTANCE = new GuideDataGen();

    private GuideDataGen() {}

    @Override
    void lang() {
        LangEntries.add("jei.create_nuclearindustry.info.reactor_viewport",
                "A lead glass window for the fission reactor's casing wall.",
                "Una ventana de vidrio plomado para la pared de la carcasa del reactor de fisión.");
    }
}
