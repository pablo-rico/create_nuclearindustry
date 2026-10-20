package org.papiricoh.create_nuclearindustry.missile.fx.client;

import net.minecraft.world.phys.Vec3;

/**
 * A moment's light or dust that plays out on its own once started: a flash, a ring thrown out at a separation, the glow up a
 * silo's shaft at ignition, the dust ring racing out from a launch or an impact.
 *
 * @param at     where, in the world
 * @param axis   the rocket's axis there (rings lie across it), or null
 * @param start  game tick it began
 * @param ticks  how long it lasts
 * @param size   its full size, blocks
 * @param colour its colour, RGB
 * @param height for a shaft, how tall; for dust, the ground's height
 */
record Burst(Kind kind, Vec3 at, Vec3 axis, long start, int ticks, float size, int colour, double height) {
    enum Kind {
        /** A ball of light that blooms and dies. */
        FLASH,
        /** A thin ring of light spreading across the rocket's axis. */
        RING,
        /** Light pouring up a silo's shaft and out of its mouth. */
        SHAFT,
        /** A curtain of dust racing out along the ground. */
        DUST_RING,
        /**
         * A cloud of vapour or debris swelling out in the high air and thinning away, lit by the sun up there long after dusk
         * below: a vented engine, a shattered satellite.
         */
        CLOUD
    }

    double seconds(long now, float partial) {
        return (now - start + partial) / 20.0;
    }

    boolean over(long now) {
        return now - start > ticks;
    }
}
