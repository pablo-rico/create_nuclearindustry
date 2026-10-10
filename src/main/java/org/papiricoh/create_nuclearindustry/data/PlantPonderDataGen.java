package org.papiricoh.create_nuclearindustry.data;

import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PlantPonderText;

/**
 * Lang for the nuclear power plant Ponder series. The lines come from {@link PlantPonderText},
 * the same lists the scenes read in order, so the {@code text_N} keys always match their boxes.
 */
final class PlantPonderDataGen extends AreaDataGen {
    static final PlantPonderDataGen INSTANCE = new PlantPonderDataGen();

    private PlantPonderDataGen() {}

    @Override
    void lang() {
        LangEntries.add(PlantPonderText.TAG_KEY, PlantPonderText.TAG_TITLE.english(), PlantPonderText.TAG_TITLE.spanish());
        LangEntries.add(PlantPonderText.TAG_KEY + ".description",
                PlantPonderText.TAG_DESCRIPTION.english(), PlantPonderText.TAG_DESCRIPTION.spanish());
        for (PlantPonderText.Scene scene : PlantPonderText.SCENES) {
            LangEntries.add(scene.headerKey(), scene.title().english(), scene.title().spanish());
            for (int i = 0; i < scene.lines().size(); i++) {
                PlantPonderText.Line line = scene.lines().get(i);
                LangEntries.add(scene.textKey(i + 1), line.english(), line.spanish());
            }
        }
    }
}
