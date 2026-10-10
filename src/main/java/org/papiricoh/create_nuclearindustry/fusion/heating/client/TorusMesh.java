package org.papiricoh.create_nuclearindustry.fusion.heating.client;

/**
 * A ring of quads around the tokamak's axis: the plasma torus the renderer draws. The ring lies in
 * the horizontal plane, centred on the origin; its cross-section is an ellipse of half-width
 * {@code halfWidth} (along the major radius) and half-height {@code halfHeight}, so a tall
 * {@code halfHeight} gives the elongated shape of a real tokamak plasma.
 *
 * <p>Quad {@code (i, j)} joins toroidal steps {@code i, i+1} and poloidal steps {@code j, j+1},
 * both wrapping, so the surface is closed and seamless. Pure arithmetic, cached by the renderer.
 */
public final class TorusMesh {
    /** Fewest and most steps around the ring. */
    public static final int MIN_MAJOR_SEGMENTS = 24;
    public static final int MAX_MAJOR_SEGMENTS = 192;
    /** Longest edge along the ring, in blocks, before more segments are used. */
    public static final double MAX_CHORD = 0.75;

    private final double majorRadius;
    private final double halfWidth;
    private final double halfHeight;
    private final int majorSegments;
    private final int minorSegments;
    /** x, y, z of every grid point, {@code (i * minorSegments + j) * 3}. */
    private final float[] grid;

    private TorusMesh(double majorRadius, double halfWidth, double halfHeight, int majorSegments, int minorSegments) {
        this.majorRadius = majorRadius;
        this.halfWidth = halfWidth;
        this.halfHeight = halfHeight;
        this.majorSegments = majorSegments;
        this.minorSegments = minorSegments;
        this.grid = new float[majorSegments * minorSegments * 3];
        for (int i = 0; i < majorSegments; i++) {
            double theta = 2 * Math.PI * i / majorSegments;
            for (int j = 0; j < minorSegments; j++) {
                double phi = 2 * Math.PI * j / minorSegments;
                double[] p = point(majorRadius, halfWidth, halfHeight, theta, phi);
                int k = (i * minorSegments + j) * 3;
                grid[k] = (float) p[0];
                grid[k + 1] = (float) p[1];
                grid[k + 2] = (float) p[2];
            }
        }
    }

    public static TorusMesh build(double majorRadius, double halfWidth, double halfHeight, int majorSegments, int minorSegments) {
        if (majorRadius <= 0 || halfWidth <= 0 || halfHeight <= 0 || halfWidth >= majorRadius) {
            throw new IllegalArgumentException("Bad torus: R=" + majorRadius + " a=" + halfWidth + " b=" + halfHeight);
        }
        if (majorSegments < 3 || minorSegments < 3) {
            throw new IllegalArgumentException("A torus needs at least 3 segments each way");
        }
        return new TorusMesh(majorRadius, halfWidth, halfHeight, majorSegments, minorSegments);
    }

    /** Enough steps around a ring of this radius that no edge is longer than {@link #MAX_CHORD}. */
    public static int majorSegmentsFor(double majorRadius) {
        int needed = (int) Math.ceil(2 * Math.PI * Math.max(0, majorRadius) / MAX_CHORD);
        return Math.max(MIN_MAJOR_SEGMENTS, Math.min(MAX_MAJOR_SEGMENTS, needed));
    }

    /** The surface point at toroidal angle {@code theta} and poloidal angle {@code phi}, as {x, y, z}. */
    public static double[] point(double majorRadius, double halfWidth, double halfHeight, double theta, double phi) {
        double rho = majorRadius + halfWidth * Math.cos(phi);
        return new double[]{rho * Math.cos(theta), halfHeight * Math.sin(phi), rho * Math.sin(theta)};
    }

    public int quadCount() {
        return majorSegments * minorSegments;
    }

    public int majorSegments() {
        return majorSegments;
    }

    public int minorSegments() {
        return minorSegments;
    }

    public double majorRadius() {
        return majorRadius;
    }

    public double halfWidth() {
        return halfWidth;
    }

    public double halfHeight() {
        return halfHeight;
    }

    /**
     * Writes the four corners of quad {@code quad} (counter-clockwise seen from outside) into
     * {@code out} as x, y, z triples: 12 floats.
     */
    public void quad(int quad, float[] out) {
        int i = quad / minorSegments;
        int j = quad % minorSegments;
        int i1 = (i + 1) % majorSegments;
        int j1 = (j + 1) % minorSegments;
        copy(i, j, out, 0);
        copy(i, j1, out, 3);
        copy(i1, j1, out, 6);
        copy(i1, j, out, 9);
    }

    private void copy(int i, int j, float[] out, int offset) {
        int k = (i * minorSegments + j) * 3;
        out[offset] = grid[k];
        out[offset + 1] = grid[k + 1];
        out[offset + 2] = grid[k + 2];
    }

    /** Whether this mesh was built for these dimensions, so the renderer can reuse it. */
    public boolean matches(double majorRadius, double halfWidth, double halfHeight, int majorSegments, int minorSegments) {
        return this.majorRadius == majorRadius && this.halfWidth == halfWidth && this.halfHeight == halfHeight
                && this.majorSegments == majorSegments && this.minorSegments == minorSegments;
    }
}
