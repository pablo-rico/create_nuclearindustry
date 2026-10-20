package org.papiricoh.create_nuclearindustry.missile.silo;

/** Integer geometry shared by base discovery, clearance checks and fuel ports. */
public final class LaunchSiteGeometry {
    private LaunchSiteGeometry() {}

    /** Candidate centres beside a controller, with the legacy facing-derived centre first. */
    public static int[][] candidates(int facingStepX, int facingStepZ) {
        int[][] centres = new int[12][2];
        int sx = -facingStepX, sz = -facingStepZ;
        int index = 0;
        for (int side = 0; side < 4; side++) {
            for (int lateral : new int[]{0, -1, 1}) {
                centres[index][0] = 2 * sx - lateral * sz;
                centres[index++][1] = 2 * sz + lateral * sx;
            }
            int nextX = -sz;
            sz = sx;
            sx = nextX;
        }
        return centres;
    }

    public static boolean portOffset(int dx, int dy, int dz, int maxRocket) {
        int radius = Math.max(Math.abs(dx), Math.abs(dz));
        return dy == 0 && radius <= 2
                || dy == -1 && radius <= 1
                || dy >= 1 && dy <= maxRocket + 1 && radius == 2;
    }

    public static boolean clearanceOffset(int dx, int dy, int dz, int height) {
        return dy >= 1 && dy <= height && Math.abs(dx) <= 1 && Math.abs(dz) <= 1 && (dx != 0 || dz != 0);
    }
}
