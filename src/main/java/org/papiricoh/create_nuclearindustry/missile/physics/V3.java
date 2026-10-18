package org.papiricoh.create_nuclearindustry.missile.physics;

/** A plain 3D vector for the flight models, which keep clear of game classes so JUnit can run them. */
public record V3(double x, double y, double z) {
    public static final V3 ZERO = new V3(0, 0, 0);
    public static final V3 UP = new V3(0, 1, 0);

    public V3 add(V3 o) {
        return new V3(x + o.x, y + o.y, z + o.z);
    }

    public V3 sub(V3 o) {
        return new V3(x - o.x, y - o.y, z - o.z);
    }

    public V3 scale(double s) {
        return new V3(x * s, y * s, z * s);
    }

    public double dot(V3 o) {
        return x * o.x + y * o.y + z * o.z;
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double horizontalLength() {
        return Math.hypot(x, z);
    }

    public V3 normalize() {
        double l = length();
        return l < 1.0e-12 ? ZERO : scale(1.0 / l);
    }

    public double distance(V3 o) {
        return sub(o).length();
    }
}
