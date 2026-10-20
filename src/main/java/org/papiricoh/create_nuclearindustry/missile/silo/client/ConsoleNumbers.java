package org.papiricoh.create_nuclearindustry.missile.silo.client;

import org.papiricoh.create_nuclearindustry.missile.gui.SiloLayout;

import java.util.Locale;

/** Pure screen arithmetic; cell zero stands immediately above the base. */
public final class ConsoleNumbers {
    private ConsoleNumbers() {}

    public static int cellAt(double x, double y, int count) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || x < SiloLayout.cellX()
                || x >= SiloLayout.cellX() + SiloLayout.CELL_W || y >= SiloLayout.baseY()) return -1;
        int index = (int) Math.ceil((SiloLayout.baseY() - y) / SiloLayout.CELL_H) - 1;
        return index >= 0 && index < Math.min(count, SiloLayout.CELLS) ? index : -1;
    }

    public static String number(double value) {
        if (!Double.isFinite(value)) return "—";
        double magnitude = Math.abs(value);
        if (magnitude >= 1_000_000) return String.format(Locale.ROOT, "%.1fM", value / 1_000_000);
        if (magnitude >= 10_000) return String.format(Locale.ROOT, "%.1fk", value / 1_000);
        return String.format(Locale.ROOT, magnitude < 100 ? "%.1f" : "%.0f", value);
    }

    public static int countdownSeconds(int ticks) {
        return ticks <= 0 ? 0 : (int) ((ticks + 19L) / 20);
    }

    public static Integer coordinate(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
