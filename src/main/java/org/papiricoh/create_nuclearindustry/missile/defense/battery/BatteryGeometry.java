package org.papiricoh.create_nuclearindustry.missile.defense.battery;

/**
 * Where the battery's moving parts turn and its rounds leave, in blocks from the master's lowest corner, shared by the server
 * (launch points) and the renderer. At rest the turret faces north and the canister is level, its tubes pointing north (-z).
 */
public final class BatteryGeometry {
    /** The turret turns about the vertical through this point. */
    public static final float[] TURRET_PIVOT = {1.0f, 1.0f, 1.0f};
    /** The canister is raised about the east-west axis through this point. */
    public static final float[] CANISTER_PIVOT = {1.0f, 1.4375f, 1.0f};
    /** Each tube's centre across the canister's face, (x, y) in the canister's frame (x east, y up at rest). */
    public static final float[][] TUBE_OFFSETS = {{-0.25f, 0.25f}, {0.25f, 0.25f}, {-0.25f, -0.25f}, {0.25f, -0.25f}};
    /** How far north of the canister's pivot (at rest) a tube's mouth is. */
    public static final float TUBE_MOUTH = 1.1f;

    private BatteryGeometry() {
    }
}
