package org.papiricoh.create_nuclearindustry.processing.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

/**
 * The moving parts of the centrifuge and the electrolyzer, all modelled facing north. Must be
 * created before models load (mod constructor).
 */
public final class ProcessingPartials {
    public static final MachineParts CENTRIFUGE = parts("centrifuge", 4);
    public static final MachineParts ELECTROLYZER = parts("electrolyzer", 2);

    private ProcessingPartials() {}

    /**
     * One machine's partials. The rotor turns about the vertical axis through the block's centre,
     * {@code rotorRatio} times as fast as the shaft; the shaft stub about the front-back axis; the
     * needle about the same axis through its own pivot.
     */
    public record MachineParts(PartialModel rotor, PartialModel shaftStub, PartialModel needle, float rotorRatio) {}

    private static MachineParts parts(String machine, float rotorRatio) {
        return new MachineParts(of(machine + "/rotor"), of(machine + "/shaft_stub"), of(machine + "/needle"), rotorRatio);
    }

    private static PartialModel of(String path) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "block/" + path));
    }

    public static void init() {}
}
