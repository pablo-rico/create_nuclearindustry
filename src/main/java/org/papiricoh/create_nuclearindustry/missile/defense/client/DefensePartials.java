package org.papiricoh.create_nuclearindustry.missile.defense.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

/**
 * The interceptor battery's moving parts and the interceptor round, drawn by their renderers. Must be created before models load
 * (mod constructor). Pivots are in blocks from the battery master's lowest corner; the canister's frame has the tubes along its
 * local +z, pointing up as its pitch rises.
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

    public static final float[] TURRET_PIVOT = {1.0f, 1.0f, 1.0f};
    public static final float[] CANISTER_PIVOT = {1.0f, 1.4375f, 1.0f};
    /** Each tube's centre across the canister's face, (x, y) in the canister's frame, blocks. */
    public static final float[][] TUBE_OFFSETS = {{-0.25f, 0.25f}, {0.25f, 0.25f}, {-0.25f, -0.25f}, {0.25f, -0.25f}};
    /** How far along the canister from its pivot a tube's mouth is, blocks. */
    public static final float TUBE_MOUTH = 1.1f;

    private DefensePartials() {
    }

    private static PartialModel of(String path) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path));
    }

    public static void init() {
    }
}
