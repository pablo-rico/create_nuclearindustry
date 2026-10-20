package org.papiricoh.create_nuclearindustry.missile.defense.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.defense.battery.BatteryGeometry;

/**
 * The interceptor battery's moving parts and the interceptor round, drawn by their renderers. Must be created before models load
 * (mod constructor).
 *
 * <p>The battery's partials are modelled in the master block's frame (pixels, 16 a block, from its lowest corner) at rest: turret
 * facing north and canister level, its tubes pointing north (-z). The renderer turns the turret and canister by the battery's yaw
 * (degrees clockwise from north) about the vertical through {@link #TURRET_PIVOT}, and raises the canister by its pitch about the
 * east-west axis through {@link #CANISTER_PIVOT}. {@link #TUBE_ROUND} and {@link #TUBE_COVER} are modelled for a tube on the
 * canister's axis, at its mouth; the renderer shifts them by {@link #TUBE_OFFSETS}. The base under the turret is the master's
 * block model, the same whichever way the battery faces. The round in flight is modelled two blocks long up +y, centred on x = z = 8.
 */
public final class DefensePartials {
    /** The turret: turns about the vertical axis through {@link #TURRET_PIVOT}, modelled facing north. */
    public static final PartialModel TURRET = of("block/interceptor_battery/turret");
    /** The four-tube canister: pitches about its local x axis through {@link #CANISTER_PIVOT}. */
    public static final PartialModel CANISTER = of("block/interceptor_battery/canister");
    /** A round's nose showing in a loaded tube. */
    public static final PartialModel TUBE_ROUND = of("block/interceptor_battery/tube_round");
    /** A tube's frangible cover, shown while its round is loading. */
    public static final PartialModel TUBE_COVER = of("block/interceptor_battery/tube_cover");
    /** An interceptor in flight, for its puppet entity. */
    public static final PartialModel INTERCEPTOR_ROUND = of("entity/interceptor_round");

    public static final float[] TURRET_PIVOT = BatteryGeometry.TURRET_PIVOT;
    public static final float[] CANISTER_PIVOT = BatteryGeometry.CANISTER_PIVOT;
    /** Each tube's centre across the canister's face, (x, y) in the canister's frame (x east, y up at rest), blocks. */
    public static final float[][] TUBE_OFFSETS = BatteryGeometry.TUBE_OFFSETS;
    /** How far north of its pivot (at rest) a tube's mouth is, blocks. */
    public static final float TUBE_MOUTH = BatteryGeometry.TUBE_MOUTH;

    private DefensePartials() {
    }

    private static PartialModel of(String path) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path));
    }

    public static void init() {
    }
}
