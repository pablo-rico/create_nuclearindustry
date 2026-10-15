package org.papiricoh.create_nuclearindustry.wastetransport.client;

import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

/**
 * Connected-texture sheets of the waste containers, registered straight with Create's CT system
 * (Create's own route goes through Registrate). Each is a 4x4 RECTANGLE sheet beside its lone tile.
 */
public final class WasteTransportSpriteShifts {
    public static final CTSpriteShiftEntry CISTERN_SIDE = rectangle("radioactive_waste_cistern_side");
    public static final CTSpriteShiftEntry CISTERN_TOP = rectangle("radioactive_waste_cistern_top");
    public static final CTSpriteShiftEntry CASK_FRONT = rectangle("shielded_waste_cask_front");
    public static final CTSpriteShiftEntry CASK_SIDE = rectangle("shielded_waste_cask_side");
    public static final CTSpriteShiftEntry CASK_TOP = rectangle("shielded_waste_cask_top");
    public static final CTSpriteShiftEntry CASK_BOTTOM = rectangle("shielded_waste_cask_bottom");

    private WasteTransportSpriteShifts() {}

    private static CTSpriteShiftEntry rectangle(String name) {
        return CTSpriteShifter.getCT(AllCTTypes.RECTANGLE, block(name), block(name + "_connected"));
    }

    private static ResourceLocation block(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "block/" + path);
    }

    /** Loads the class, so the shifts are registered before textures stitch. */
    static void init() {}
}
