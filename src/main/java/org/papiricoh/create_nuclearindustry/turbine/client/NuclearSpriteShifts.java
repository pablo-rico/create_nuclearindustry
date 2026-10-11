package org.papiricoh.create_nuclearindustry.turbine.client;

import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

/**
 * Connected-texture sheets, registered directly with Create's CT system (the usual route
 * goes through Registrate, which this project does not compile against).
 */
public final class NuclearSpriteShifts {
    public static final CTSpriteShiftEntry TURBINE_GLASS = CTSpriteShifter.getCT(AllCTTypes.RECTANGLE,
            block("reinforced_turbine_glass"), block("reinforced_turbine_glass_connected"));
    /** Reactor viewports side by side frame one big window. */
    public static final CTSpriteShiftEntry REACTOR_VIEWPORT = CTSpriteShifter.getCT(AllCTTypes.RECTANGLE,
            block("reactor_viewport"), block("reactor_viewport_connected"));

    private NuclearSpriteShifts() {}

    private static ResourceLocation block(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "block/" + path);
    }

    static void init() {}
}
