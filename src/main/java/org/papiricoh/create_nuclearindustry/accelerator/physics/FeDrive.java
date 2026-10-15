package org.papiricoh.create_nuclearindustry.accelerator.physics;

/**
 * Forge Energy as an alternative to rotation for the accelerator's machines. A machine that needs
 * {@code stress} SU per RPM at {@code ratedSpeed} RPM asks for {@code stress * ratedSpeed * fePerSu}
 * FE per tick; the share of that it receives drives it like the same share of its rated speed would.
 * Whichever of rotation and FE drives it more wins. Pure arithmetic.
 */
public final class FeDrive {

    private FeDrive() {}

    /** FE per tick that stands in for full rotation. */
    public static double demand(double stressPerRpm, double ratedSpeed, double fePerSu) {
        return Math.max(0, stressPerRpm) * Math.max(0, ratedSpeed) * Math.max(0, fePerSu);
    }

    /** Share of full drive this much FE gives, 0..1. */
    public static double fromEnergy(double received, double demand) {
        if (demand <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(1, received / demand));
    }

    /** The better of the two drives. */
    public static double drive(double rotationDrive, double energyDrive) {
        return Math.max(0, Math.min(1, Math.max(rotationDrive, energyDrive)));
    }

    /** Buffer a machine keeps: {@code ticks} of its demand, at least one FE. */
    public static int buffer(double demand, int ticks) {
        return (int) Math.max(1, Math.min(Integer.MAX_VALUE, Math.ceil(demand * Math.max(1, ticks))));
    }

    /**
     * Splits {@code available} FE fairly between receivers that would take {@code accepts[i]} each
     * (what a simulated {@code receiveEnergy} said): everyone gets an equal share, and what a full
     * receiver leaves over goes to the others. Whole FE; the sum never exceeds {@code available} nor
     * anyone's acceptance. A generator pushing to its six neighbours uses it so the first side does
     * not starve the rest.
     */
    public static int[] fairShare(int available, int[] accepts) {
        int n = accepts.length;
        int[] given = new int[n];
        long left = Math.max(0, available);
        int open = 0;
        for (int a : accepts) {
            if (a > 0) {
                open++;
            }
        }
        while (left > 0 && open > 0) {
            long share = Math.max(1, left / open);
            int stillOpen = 0;
            for (int i = 0; i < n && left > 0; i++) {
                int room = Math.max(0, accepts[i]) - given[i];
                if (room <= 0) {
                    continue;
                }
                int give = (int) Math.min(Math.min(share, room), left);
                given[i] += give;
                left -= give;
                if (room > give) {
                    stillOpen++;
                }
            }
            open = stillOpen;
        }
        return given;
    }
}
