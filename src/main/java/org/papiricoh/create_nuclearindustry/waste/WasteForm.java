package org.papiricoh.create_nuclearindustry.waste;

import java.util.Locale;

/**
 * How a waste package holds its waste, from loose drums to glass. The better the form, the
 * less a repository has to make up for it: {@link #quality()} is the form term of
 * {@link org.papiricoh.create_nuclearindustry.waste.repository.RepositoryQuality}.
 */
public enum WasteForm {
    /** Loose waste in a drum, as the old barrel holds it. */
    RAW(0.3),
    /** Low-level waste crushed into a drum: less volume, little to leach. */
    COMPACTED(0.7),
    /** Intermediate-level waste set in cement inside its drum. */
    CEMENTED(0.7),
    /** High-level waste melted into borosilicate glass in a steel canister. */
    VITRIFIED(1.0);

    private final double quality;

    WasteForm(double quality) {
        this.quality = quality;
    }

    /** 0 to 1, how well this form keeps its waste in by itself. */
    public double quality() {
        return quality;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The form named {@code id}, or {@code null}. */
    public static WasteForm byId(String id) {
        for (WasteForm form : values()) {
            if (form.id().equals(id)) {
                return form;
            }
        }
        return null;
    }
}
