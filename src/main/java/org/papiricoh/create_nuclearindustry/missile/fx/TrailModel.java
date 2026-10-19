package org.papiricoh.create_nuclearindustry.missile.fx;

import org.papiricoh.create_nuclearindustry.missile.physics.V3;

/**
 * A rocket's smoke trail: points dropped in the world every {@link #SAMPLE_TICKS} ticks while its engines burn, each spreading
 * wider, drifting with the wind (stronger the higher it is) and thinning out until it is gone after the trail's lifetime. At
 * most {@link #MAX_POINTS} are kept; the oldest go first. Free of game classes, for JUnit.
 */
public final class TrailModel {
    /** Ticks between the points of a trail. */
    public static final int SAMPLE_TICKS = 5;
    /** Most points one trail keeps. */
    public static final int MAX_POINTS = 256;
    /** Widest a trail spreads, blocks. */
    public static final double MAX_WIDTH = 18;

    /**
     * One point of a trail where the rocket passed: world position, game tick, how dense the smoke was (0 to 1) and how wide it
     * started.
     */
    public record Point(double x, double y, double z, long tick, float density, float width) {
    }

    private final Point[] points = new Point[MAX_POINTS];
    /** Index of the oldest point. */
    private int head;
    private int size;
    private long lastSample = Long.MIN_VALUE;

    /** Whether a point is due at {@code tick}. */
    public boolean due(long tick) {
        return lastSample == Long.MIN_VALUE || tick - lastSample >= SAMPLE_TICKS;
    }

    /** Adds a point, dropping the oldest if the trail is full. */
    public void add(Point point) {
        lastSample = point.tick();
        if (size == MAX_POINTS) {
            points[head] = point;
            head = (head + 1) % MAX_POINTS;
        } else {
            points[(head + size) % MAX_POINTS] = point;
            size++;
        }
    }

    /** Forgets the points older than {@code lifeTicks} at {@code now}. */
    public void expire(long now, int lifeTicks) {
        while (size > 0 && now - points[head].tick() > lifeTicks) {
            points[head] = null;
            head = (head + 1) % MAX_POINTS;
            size--;
        }
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /** Point {@code i}, oldest first. */
    public Point get(int i) {
        if (i < 0 || i >= size) throw new IndexOutOfBoundsException(i);
        return points[(head + i) % MAX_POINTS];
    }

    /** The newest point, or null. */
    public Point newest() {
        return size == 0 ? null : get(size - 1);
    }

    /** How wide smoke that left the nozzle {@code start} blocks wide is after {@code ageTicks}: it spreads fast, then slowly. */
    public static double width(double ageTicks, double start) {
        return Math.min(MAX_WIDTH, start + 0.55 * Math.sqrt(Math.max(0, ageTicks)) + 0.012 * Math.max(0, ageTicks));
    }

    /** How opaque smoke is after {@code ageTicks} of a trail lasting {@code lifeTicks}: fully at first, thinning to nothing. */
    public static double alpha(double ageTicks, double lifeTicks) {
        if (lifeTicks <= 0) return 0;
        double t = Math.max(0, Math.min(1, ageTicks / lifeTicks));
        double fade = 1 - t;
        return fade * fade * (0.6 + 0.4 * fade);
    }

    /**
     * The wind at height {@code y} on day-time {@code dayTicks}, blocks per tick: a light breeze near the ground, several times
     * stronger high up, turning slowly over the day.
     */
    public static V3 wind(double y, double dayTicks) {
        double angle = 0.7 + 2 * Math.PI * (dayTicks / 24000.0) * 0.5 + 0.4 * Math.sin(dayTicks / 3100.0);
        double strength = 0.015 + 0.055 * Math.max(0, Math.min(1, (y - 64) / 320.0));
        return new V3(Math.sin(angle) * strength, 0, Math.cos(angle) * strength);
    }

    /** Where a point dropped at {@code (x, y, z)} has drifted to after {@code ageTicks} in that wind. */
    public static V3 drifted(Point point, double ageTicks, V3 wind) {
        // Smoke sinks a touch as it cools.
        return new V3(point.x() + wind.x() * ageTicks, point.y() - 0.004 * ageTicks, point.z() + wind.z() * ageTicks);
    }
}
