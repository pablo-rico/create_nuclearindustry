package org.papiricoh.create_nuclearindustry.antimatter.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

/**
 * The lit parts the renderers lay over the antimatter models: a Penning trap's status LED covers,
 * the large trap's charge ring and the medium trap's gauge needle, the annihilation generator's core
 * and mode lamps, the antiproton injector's lamps and nozzle flash. All white or grey, tinted when
 * drawn. They
 * must exist before models load, so {@link #init()} runs from the mod constructor.
 */
public final class AntimatterPartials {
    public static final PartialModel SMALL_STATUS_LED = of("small_status_led");
    public static final PartialModel MEDIUM_STATUS_LED = of("medium_status_led");
    /** Both covers of the large trap, in the space of its top north-west block. */
    public static final PartialModel LARGE_STATUS_LED = of("large_status_led");
    public static final PartialModel GAUGE_NEEDLE = of("penning_gauge_needle");
    /** The large trap's charge ring, clockwise from above starting at the north-west, in its top north-west block's space. */
    public static final PartialModel[] LARGE_RING = new PartialModel[12];
    /** The annihilation generator's core behind its porthole, greyscale, tinted by the burn. */
    public static final PartialModel GENERATOR_CORE = of("generator_core");
    /** The generator's three mode lamps, by {@code AnnihilationModel.Mode} ordinal (rotation, electric, split). */
    public static final PartialModel[] GENERATOR_MODE_LED = {of("generator_mode_led_0"), of("generator_mode_led_1"), of("generator_mode_led_2")};
    public static final PartialModel INJECTOR_ARMED_LED = of("injector_armed_led");
    public static final PartialModel INJECTOR_READY_LED = of("injector_ready_led");
    /** The injector's nozzle, lit for a moment after a shot (on its back face, inside the tokamak). */
    public static final PartialModel INJECTOR_NOZZLE_FLASH = of("injector_nozzle_flash");

    static {
        for (int i = 0; i < LARGE_RING.length; i++) {
            LARGE_RING[i] = of("large_led_" + i);
        }
    }

    private AntimatterPartials() {}

    private static PartialModel of(String path) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "block/antimatter/partial/" + path));
    }

    public static void init() {}
}
