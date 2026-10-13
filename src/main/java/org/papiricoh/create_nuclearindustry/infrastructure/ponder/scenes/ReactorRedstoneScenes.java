package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.transmission.GearshiftBlock;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorRedstonePortBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.RedstonePortMode;

/**
 * The Reactor Redstone Port on the reactor of {@code ponder/reactor_controller.nbt}: its readings,
 * and how its SCRAM signal reaches the piston that drives the control rods. Rods only move
 * physically, so the scene wires the signal through Redstone Links to a Gearshift that
 * reverses the piston.
 */
public class ReactorRedstoneScenes {

    public static void redstonePort(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("reactor_redstone_port", "Reactor Redstone Ports and SCRAM");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.5f);
        scene.setSceneOffsetY(-1.0f);

        BlockPos port = util.grid().at(4, 3, 1);
        BlockPos portLink = util.grid().at(4, 3, 0);
        BlockPos gearshift = util.grid().at(4, 7, 2);
        BlockPos gearshiftLink = util.grid().at(4, 7, 1);
        BlockPos controller = util.grid().at(3, 5, 3);

        // The whole reactor, piston included, appears at once: this scene is about operating it.
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(1, 1, 1, 5, 5, 5).substract(util.select().position(2, 5, 2)), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(2, 5, 2, 2, 7, 2).add(util.select().position(3, 7, 2)), Direction.DOWN);
        scene.idle(20);

        scene.world().replaceBlocks(util.select().position(port), AllNuclearBlocks.REACTOR_REDSTONE_PORT.get().defaultBlockState()
                .setValue(ReactorRedstonePortBlock.FACING, Direction.NORTH)
                .setValue(ReactorRedstonePortBlock.MODE, RedstonePortMode.TEMPERATURE), false);
        scene.idle(10);
        scene.overlay().showText(90)
                .text("A Reactor Redstone Port takes the place of a casing block in the shell and puts out a 0-15 signal on its panel side.")
                .pointAt(util.vector().blockSurface(port, Direction.NORTH))
                .placeNearTarget();
        scene.idle(100);
        scene.overlay().showControls(util.vector().blockSurface(port, Direction.NORTH), Pointing.DOWN, 60)
                .rightClick().withItem(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "wrench"))));
        scene.overlay().showText(90)
                .text("Use a Wrench to pick what it reads: temperature, power, fuel, coolant or steam. Like a comparator, any content gives at least 1.")
                .pointAt(util.vector().blockSurface(port, Direction.NORTH))
                .placeNearTarget();
        scene.idle(100);
        scene.addKeyframe();

        // Rods out: the reactor is running.
        ElementLink<WorldSectionElement> rods = scene.world().makeSectionIndependent(
                util.select().fromTo(2, 2, 2, 2, 4, 2).add(util.select().position(2, 5, 2)));
        scene.world().setKineticSpeed(util.select().fromTo(2, 7, 2, 3, 7, 2), 32);
        scene.world().hideSection(util.select().position(2, 6, 2), Direction.UP);
        scene.world().moveSection(rods, util.vector().of(0, 1, 0), 20);
        scene.idle(25);
        scene.world().setKineticSpeed(util.select().fromTo(2, 7, 2, 3, 7, 2), 0);

        scene.world().modifyBlock(port, state -> state.setValue(ReactorRedstonePortBlock.MODE, RedstonePortMode.SCRAM), false);
        scene.idle(10);
        scene.overlay().showText(110)
                .text("Its last mode is SCRAM. It lights up when SCRAM is pressed on the control panel or the automatic SCRAM trips, and stays on until reset.")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().blockSurface(port, Direction.NORTH))
                .placeNearTarget();
        scene.idle(120);
        scene.addKeyframe();

        // Wire it to the piston: a Redstone Link pair and a Gearshift.
        scene.world().setBlock(portLink, link(Direction.NORTH, false), false);
        scene.world().showSection(util.select().position(portLink), Direction.SOUTH);
        scene.world().setBlock(gearshift, gearshift(false), false);
        scene.world().setBlock(gearshiftLink, link(Direction.NORTH, true), false);
        scene.world().showSection(util.select().position(gearshift).add(util.select().position(gearshiftLink)), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(110)
                .text("Control Rods only move with pistons. Carry the signal to whatever drives them, here a Redstone Link pair and a Gearshift on the piston's shaft.")
                .pointAt(util.vector().blockSurface(gearshift, Direction.NORTH))
                .placeNearTarget();
        scene.idle(120);
        scene.addKeyframe();

        // SCRAM: the signal reverses the piston and the rods drop in.
        scene.overlay().showControls(util.vector().topOf(controller), Pointing.DOWN, 40).rightClick();
        scene.idle(10);
        scene.world().modifyBlock(port, state -> state.setValue(ReactorRedstonePortBlock.POWER, 15), false);
        scene.world().modifyBlock(portLink, state -> state.setValue(RedstoneLinkBlock.POWERED, true), false);
        scene.world().modifyBlock(gearshiftLink, state -> state.setValue(RedstoneLinkBlock.POWERED, true), false);
        scene.world().modifyBlock(gearshift, state -> state.setValue(GearshiftBlock.POWERED, true), false);
        scene.effects().indicateRedstone(portLink);
        scene.world().setKineticSpeed(util.select().fromTo(2, 7, 2, 4, 7, 2), -32);
        scene.world().moveSection(rods, util.vector().of(0, -1, 0), 20);
        scene.idle(22);
        scene.world().showSection(util.select().position(2, 6, 2), Direction.DOWN);
        scene.world().setKineticSpeed(util.select().fromTo(2, 7, 2, 4, 7, 2), 0);
        scene.overlay().showText(110)
                .text("On SCRAM the Gearshift reverses the piston and the rods go all the way in. Without such wiring, SCRAM only lights the port.")
                .colored(PonderPalette.BLUE)
                .pointAt(util.vector().centerOf(util.grid().at(2, 4, 2)))
                .placeNearTarget();
        scene.idle(120);
    }

    private static BlockState link(Direction facing, boolean receiver) {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "redstone_link"));
        return block.defaultBlockState()
                .setValue(BlockStateProperties.FACING, facing)
                .setValue(RedstoneLinkBlock.RECEIVER, receiver)
                .setValue(RedstoneLinkBlock.POWERED, false);
    }

    private static BlockState gearshift(boolean powered) {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "gearshift"));
        return block.defaultBlockState()
                .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X)
                .setValue(GearshiftBlock.POWERED, powered);
    }
}
