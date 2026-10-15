package org.papiricoh.create_nuclearindustry.controlroom.annunciator;

import java.util.Locale;

/**
 * How much an annunciator window matters, which sets its colour and its horn. An ALARM lights red and
 * sounds the urgent horn, a WARNING lights amber with a softer one; both run the full alarm sequence.
 * A STATUS window only says what is so (a pump stopped, a valve open): it lights white while its
 * condition holds and goes dark when it clears, with no horn and nothing to acknowledge. Saved by
 * name. No game classes.
 */
public enum AnnunciatorSeverity {
    ALARM,
    WARNING,
    STATUS;

    /** Whether the window runs the alarm sequence (locks in, sounds, waits for ACK and RESET). */
    public boolean latching() {
        return this != STATUS;
    }

    public String translationKey() {
        return "create_nuclearindustry.annunciator.severity." + name().toLowerCase(Locale.ROOT);
    }

    public AnnunciatorSeverity next(int direction) {
        AnnunciatorSeverity[] all = values();
        return all[Math.floorMod(ordinal() + direction, all.length)];
    }

    /** The severity called {@code name}, or {@code fallback}. */
    public static AnnunciatorSeverity byName(String name, AnnunciatorSeverity fallback) {
        for (AnnunciatorSeverity severity : values()) {
            if (severity.name().equals(name)) {
                return severity;
            }
        }
        return fallback;
    }

    /** The more urgent of two; null counts as nothing. */
    public static AnnunciatorSeverity worst(AnnunciatorSeverity a, AnnunciatorSeverity b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.ordinal() <= b.ordinal() ? a : b;
    }
}
