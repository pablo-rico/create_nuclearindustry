package org.papiricoh.create_nuclearindustry.dyson.swarm;

/**
 * One of the swarm's orbits round the sun, as it looks from the ground: a ring {@code radius} degrees out from the sun, tipped
 * {@code inclination} degrees out of face-on about an axis turned {@code node} degrees round the sun. Only the ejectors launch into
 * an {@code enabled} orbit; sails already in a disabled one stay until they wear out. The shape changes how the swarm looks, never
 * how much it yields. Free of game classes, for JUnit.
 */
public record OrbitShape(boolean enabled, float radius, float inclination, float node) {
    public static final int COUNT = 4;
    public static final float MIN_RADIUS = 10, MAX_RADIUS = 40;
    public static final float MAX_INCLINATION = 90;

    public OrbitShape {
        radius = clamp(finite(radius, MIN_RADIUS), MIN_RADIUS, MAX_RADIUS);
        inclination = clamp(finite(inclination, 0), 0, MAX_INCLINATION);
        node = (float) (((finite(node, 0) % 360) + 360) % 360);
    }

    /** The orbits of a new world: the first open and face-on, the others closed, each further out and more tipped. */
    public static OrbitShape defaults(int index) {
        return switch (index) {
            case 0 -> new OrbitShape(true, 14, 0, 0);
            case 1 -> new OrbitShape(false, 20, 35, 45);
            case 2 -> new OrbitShape(false, 27, 60, 120);
            default -> new OrbitShape(false, 34, 80, 200);
        };
    }

    /** Whether the values are what a terminal may send: in range and finite (the constructor would clamp them silently). */
    public static boolean valid(float radius, float inclination, float node) {
        return Float.isFinite(radius) && Float.isFinite(inclination) && Float.isFinite(node)
                && radius >= MIN_RADIUS && radius <= MAX_RADIUS && inclination >= 0 && inclination <= MAX_INCLINATION
                && node >= 0 && node <= 360;
    }

    public static boolean validIndex(int index) {
        return index >= 0 && index < COUNT;
    }

    public OrbitShape withEnabled(boolean enabled) {
        return new OrbitShape(enabled, radius, inclination, node);
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static float finite(float v, float fallback) {
        return Float.isFinite(v) ? v : fallback;
    }
}
