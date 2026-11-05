package org.papiricoh.create_nuclearindustry.explosive.fx.model;

import org.papiricoh.create_nuclearindustry.missile.fx.FxBudget;

/**
 * How a mushroom cloud past the far plane is drawn the size it should look. Whatever lies beyond the plane is pulled in along
 * its line of sight, its size scaled by the same factor as its distance ({@link FxBudget#pullIn}), so it covers the same angle;
 * with no Distant Horizons, a cloud wholly past the plane is drawn as an impostor whose painted cap and stem are sized to cover
 * the cloud's own puffs ({@link CloudEvolution#visibleCapRadius} and the rest), allowing for the margins round the paint in the
 * impostor texture. No game classes.
 */
public final class FarView {
    /** The share of its sheet's width and height the painted cap fills in the impostor texture, and how far its middle sits up. */
    public static final double CAP_FILL_WIDTH = 0.93, CAP_FILL_HEIGHT = 0.6, CAP_RAISED = 0.144;
    /** The same for the painted stem. */
    public static final double STEM_FILL_WIDTH = 0.5, STEM_FILL_HEIGHT = 0.86;

    private FarView() {}

    /** One upright sheet of the impostor: half its width and height, and the height of its middle above ground zero. */
    public record Sheet(double halfWidth, double halfHeight, double centreY) {}

    /** The angle, radians, something of {@code radius} subtends from {@code distance} blocks. */
    public static double angularDiameter(double radius, double distance) {
        return 2 * Math.atan(radius / Math.max(1e-6, distance));
    }

    /** How much something {@code distance} blocks off is pulled in when the far plane is at {@code limit}: 1 if not at all. */
    public static double pullIn(double distance, double limit) {
        return FxBudget.pullIn(distance, limit);
    }

    /** The cap's sheet {@code t} seconds in: its painted cap covers the cloud's cap as its puffs do. */
    public static Sheet capSheet(CloudEvolution cloud, double t) {
        double top = cloud.visibleCapTop(t), bottom = cloud.visibleCapBottom(t);
        double halfHeight = (top - bottom) / 2 / CAP_FILL_HEIGHT;
        return new Sheet(cloud.visibleCapRadius(t) / CAP_FILL_WIDTH, halfHeight, (top + bottom) / 2 - CAP_RAISED * halfHeight);
    }

    /** The stem's sheet, from the ground up into the cap's underside. */
    public static Sheet stemSheet(CloudEvolution cloud, double t) {
        double top = cloud.visibleCapBottom(t) + 0.1 * (cloud.visibleCapTop(t) - cloud.visibleCapBottom(t));
        return new Sheet(cloud.visibleStemRadius(t) / STEM_FILL_WIDTH, top / 2 / STEM_FILL_HEIGHT, top / 2);
    }
}
