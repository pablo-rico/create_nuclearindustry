package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.reactor.block.TurbineRotorBlock;
import org.papiricoh.create_nuclearindustry.turbine.AbstractTurbineControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.turbine.TurbineDisplay;
import org.papiricoh.create_nuclearindustry.turbine.TurbineTier;

/**
 * Builds the 5x5x7 turbine of {@code ponder/turbine.nbt} / {@code ponder/fusion_turbine.nbt}
 * (tools/structgen/ponder_turbine.py). The controller and the glass wall face the camera, so
 * the rotor is seen spinning through the glass once the turbine runs; the camera turns to
 * show the output port on the far side.
 */
public class TurbineScenes {

    public static void steam(SceneBuilder builder, SceneBuildingUtil util) {
        assembly(builder, util, TurbineTier.STEAM, "turbine", "Building a Steam Turbine", "Turbine Output", "steam", "water");
    }

    public static void plasma(SceneBuilder builder, SceneBuildingUtil util) {
        assembly(builder, util, TurbineTier.PLASMA, "fusion_plasma_turbine", "Building a Plasma Turbine", "Plasma Turbine",
                "plasma steam", "water");
    }

    private static void assembly(SceneBuilder builder, SceneBuildingUtil util, TurbineTier tier, String id, String title,
                                 String controllerName, String fluidName, String condensateName) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title(id, title);
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.75f);
        scene.setSceneOffsetY(-1.0f);

        BlockPos controller = util.grid().at(4, 3, 1);
        BlockPos input = util.grid().at(4, 5, 4);
        BlockPos output = util.grid().at(6, 3, 4);
        Selection rotors = util.select().fromTo(4, 3, 2, 4, 3, 6);
        Selection glass = util.select().fromTo(2, 2, 2, 2, 4, 6);

        scene.showBasePlate();
        scene.idle(10);

        // Floor and back wall first: the inside stays open to the camera while it is explained.
        scene.world().showSection(util.select().fromTo(2, 1, 1, 6, 1, 7), Direction.UP);
        scene.idle(6);
        scene.world().showSection(util.select().fromTo(2, 2, 7, 6, 5, 7), Direction.NORTH);
        scene.idle(15);
        scene.overlay().showText(90)
                .text("A turbine is a box of casing: 3 to 7 blocks wide and high, and 3 to 16 blocks long.")
                .pointAt(util.vector().topOf(util.grid().at(4, 1, 4)))
                .placeNearTarget();
        scene.idle(100);
        scene.addKeyframe();

        scene.world().showSection(rotors, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showOutline(PonderPalette.WHITE, "rotors", rotors, 90);
        scene.overlay().showText(90)
                .text("A column of rotors runs down its axis. Every rotor stage expands " + tier.perRotor + " mB/t of " + fluidName + ".")
                .pointAt(util.vector().centerOf(util.grid().at(4, 3, 4)))
                .placeNearTarget();
        scene.idle(100);
        scene.addKeyframe();

        scene.world().showSection(util.select().fromTo(6, 2, 2, 6, 4, 6).substract(util.select().position(output)), Direction.WEST);
        scene.world().showSection(util.select().fromTo(2, 5, 2, 6, 5, 6).substract(util.select().position(input)), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(glass, Direction.EAST);
        scene.idle(15);
        scene.overlay().showOutline(PonderPalette.GREEN, "glass", glass, 100);
        scene.overlay().showOutline(PonderPalette.RED, "edge", util.select().fromTo(2, 1, 7, 2, 5, 7), 100);
        scene.overlay().showText(90)
                .text("Reinforced Turbine Glass can replace casing anywhere on a face, but not on edges or corners.")
                .colored(PonderPalette.BLUE)
                .pointAt(util.vector().blockSurface(util.grid().at(2, 3, 4), Direction.WEST))
                .placeNearTarget();
        scene.idle(100);
        scene.addKeyframe();

        scene.world().showSection(util.select().position(input), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80)
                .text("Fluid Ports go on the faces. One set to INPUT takes the " + fluidName + "...")
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().topOf(input))
                .placeNearTarget();
        scene.idle(90);

        // The output port sits on the far (east) wall.
        PonderCamera.showEast(scene);
        scene.world().showSection(util.select().position(output), Direction.WEST);
        scene.idle(10);
        scene.overlay().showText(80)
                .text("...and one set to OUTPUT returns the condensed " + condensateName + ". Each pair passes " + tier.perPort + " mB/t.")
                .colored(PonderPalette.OUTPUT)
                .pointAt(util.vector().blockSurface(output, Direction.EAST))
                .placeNearTarget();
        scene.idle(90);
        PonderCamera.backFromEast(scene);
        scene.addKeyframe();

        scene.world().showSection(util.select().fromTo(2, 2, 1, 6, 5, 1).add(util.select().position(4, 3, 0)), Direction.SOUTH);
        scene.idle(15);
        scene.overlay().showControls(util.vector().blockSurface(controller, Direction.NORTH), Pointing.DOWN, 60).rightClick();
        scene.overlay().showText(90)
                .text("The " + controllerName + " sits at the centre of one end, its shaft pointing out. Right-click it to form the turbine.")
                .pointAt(util.vector().blockSurface(controller, Direction.NORTH))
                .placeNearTarget();
        scene.idle(100);
        scene.addKeyframe();

        scene.world().modifyBlocks(rotors, state -> state.hasProperty(TurbineRotorBlock.FORMED)
                ? state.setValue(TurbineRotorBlock.FORMED, true) : state, false);
        scene.world().modifyBlockEntityNBT(util.select().position(controller), AbstractTurbineControllerBlockEntity.class,
                nbt -> formedState(nbt, tier), false);
        scene.world().setKineticSpeed(util.select().fromTo(4, 3, 0, 4, 3, 1), 64);
        Vec3 inlet = util.vector().topOf(input);
        scene.effects().emitParticles(inlet, scene.effects().simpleParticleEmitter(ParticleTypes.CLOUD, new Vec3(0, -0.05, 0)), 1, 30);
        scene.idle(20);
        scene.overlay().showText(100)
                .text("Once formed, the rotor spins up while " + fluidName + " flows, and the glass shows how full the turbine is.")
                .pointAt(util.vector().blockSurface(util.grid().at(2, 3, 4), Direction.WEST))
                .placeNearTarget();
        scene.idle(110);
        scene.overlay().showText(100)
                .text("Rotors and port pairs both cap the flow; the smaller limit wins. Longer turbines with more ports make more power.")
                .pointAt(util.vector().blockSurface(util.grid().at(2, 3, 3), Direction.WEST))
                .placeNearTarget();
        scene.idle(110);
    }

    /** Makes the controller render as a formed turbine with its tank half full. */
    private static void formedState(CompoundTag nbt, TurbineTier tier) {
        int rotors = 5;
        int limit = Math.min(rotors * tier.perRotor, tier.perPort);
        int capacity = tier.baseTank + 3 * 3 * 5 * tier.tankPerInteriorBlock;
        FluidTank steam = new FluidTank(capacity);
        steam.fill(new FluidStack(tier == TurbineTier.STEAM ? AllNuclearFluids.STEAM.get() : AllNuclearFluids.PLASMA_STEAM.get(),
                capacity / 2), IFluidHandler.FluidAction.EXECUTE);
        var registries = Minecraft.getInstance().level.registryAccess();
        nbt.putInt("TurbineVersion", 2);
        nbt.putInt("TankCapacity", capacity);
        nbt.put("Steam", steam.writeToNBT(registries, new CompoundTag()));
        nbt.put("Turbine", new TurbineDisplay(true, 2, 2, 7, rotors, 1, 1, limit, limit, capacity, "").write());
    }
}
