package org.papiricoh.create_nuclearindustry.controlroom.desk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Clicks land on the slot whose module is drawn there, whichever way a surface is turned. */
class SurfaceGeometryTest {
    private static final int[] YAWS = {0, 90, 180, 270};

    @Test
    void turningAndUnturningCancel() {
        for (int yaw : YAWS) {
            double[] world = SurfaceGeometry.fromNative(0.8, 0.1, yaw);
            assertArrayEquals(new double[]{0.8, 0.1}, SurfaceGeometry.toNative(world[0], world[1], yaw), 1e-9, "yaw " + yaw);
        }
        // A quarter turn clockwise seen from above takes the north-east corner to the south-east.
        assertArrayEquals(new double[]{1, 1}, SurfaceGeometry.fromNative(1, 0, 90), 1e-9);
    }

    @Test
    void tableSlotsAreWhereTheirModulesAre() {
        for (int yaw : YAWS) {
            for (int slot = 0; slot < 4; slot++) {
                float[] centre = SurfaceGeometry.TABLE_SLOTS[slot];
                double[] world = SurfaceGeometry.fromNative(centre[0] / 16, centre[2] / 16, yaw);
                double[] back = SurfaceGeometry.toNative(world[0], world[1], yaw);
                assertEquals(slot, SurfaceGeometry.tableSlot(back[0], centre[1] / 16, back[1]), "yaw " + yaw);
            }
        }
        assertEquals(-1, SurfaceGeometry.tableSlot(0.75, 0.5, 0.25), "the side of the table is no slot");
    }

    @Test
    void wallSlotsAreWhereTheirModulesAre() {
        for (int yaw : YAWS) {
            for (int slot = 0; slot < 4; slot++) {
                float[] centre = SurfaceGeometry.WALL_SLOTS[slot];
                double[] world = SurfaceGeometry.fromNative(centre[0] / 16, centre[2] / 16, yaw);
                double[] back = SurfaceGeometry.toNative(world[0], world[1], yaw);
                assertEquals(slot, SurfaceGeometry.wallSlot(back[0], centre[1] / 16, back[1]), "yaw " + yaw);
            }
        }
        assertEquals(0, SurfaceGeometry.wallSlot(0.9, 0.1, 13 / 16.0), "lower left as the operator sees it");
        assertEquals(3, SurfaceGeometry.wallSlot(0.1, 0.9, 13 / 16.0), "upper right");
        assertEquals(-1, SurfaceGeometry.wallSlot(1.0, 0.5, 14.5 / 16), "the plate's side is no slot");
    }

    @Test
    void deskSlotsAreWhereTheirModulesAre() {
        for (int yaw : YAWS) {
            for (int slot = 0; slot < 4; slot++) {
                float[] centre = SurfaceGeometry.DESK_SLOTS[slot];
                double[] world = SurfaceGeometry.fromNative(centre[0] / 16, centre[2] / 16, yaw);
                double[] back = SurfaceGeometry.toNative(world[0], world[1], yaw);
                assertEquals(slot, SurfaceGeometry.topSlot(back[0], back[1]), "yaw " + yaw);
            }
        }
    }
}
