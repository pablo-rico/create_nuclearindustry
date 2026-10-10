package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;

/**
 * A control room on the control_room schematic (tools/structgen/ponder_control_room.py): a 3x2
 * monitor wall, the reactor panel and an alarm, next to a block of reactor casing standing in for
 * a reactor, with a Redstone Port in a rod mode on its west face.
 */
public class ControlRoomScenes {

    public static void controlRoom(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("control_room", "Building a Control Room");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.7f);
        scene.setSceneOffsetY(-1.0f);

        BlockPos controller = util.grid().at(7, 3, 6);
        BlockPos port = util.grid().at(6, 1, 6);
        BlockPos panel = util.grid().at(2, 1, 3);
        BlockPos alarm = util.grid().at(4, 1, 3);
        Selection reactor = util.select().fromTo(6, 1, 5, 8, 3, 7);
        Selection wall = util.select().fromTo(1, 1, 6, 3, 2, 6);
        ItemStack linker = new ItemStack(AllNuclearItems.CONTROL_ROOM_LINKER.get());

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(reactor, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showControls(util.vector().topOf(controller), Pointing.DOWN, 60).rightClick().withItem(linker);
        scene.overlay().showText(90)
                .text("A control room watches and drives machines from afar. Click a reactor, turbine or cooling tower (any of its blocks) with the Control Room Linker.")
                .pointAt(util.vector().topOf(controller))
                .placeNearTarget();
        scene.idle(100);
        scene.addKeyframe();

        scene.world().showSection(wall, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showControls(util.vector().blockSurface(util.grid().at(2, 2, 6), Direction.NORTH), Pointing.DOWN, 60)
                .rightClick().withItem(linker);
        scene.overlay().showText(100)
                .text("Monitors side by side join into one screen up to 8x8. Click one with the linker to show the machine: readings and gauges, and on a big screen a two-minute graph.")
                .pointAt(util.vector().blockSurface(util.grid().at(2, 2, 6), Direction.NORTH))
                .placeNearTarget();
        scene.idle(110);
        scene.overlay().showText(80)
                .text("Click a screen to change page. A comparator reads its main value; Display Links read a linked reactor too.")
                .pointAt(util.vector().blockSurface(util.grid().at(1, 1, 6), Direction.NORTH))
                .placeNearTarget();
        scene.idle(90);
        scene.addKeyframe();

        scene.world().showSection(util.select().position(panel), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(100)
                .text("The Reactor Control Panel SCRAMs and resets the reactor, switches and tunes its automatic SCRAM, and sets a target for the control rods.")
                .pointAt(util.vector().blockSurface(panel, Direction.NORTH))
                .placeNearTarget();
        scene.idle(110);
        scene.overlay().showOutline(PonderPalette.RED, "port", util.select().position(port), 100);
        scene.overlay().showText(110)
                .text("Rods still only move with pistons: Redstone Ports set to Insert rods or Withdraw rods light up until the rods reach the target. Wire them to the clutch and gearshift of the rod pistons.")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().blockSurface(port, Direction.WEST))
                .placeNearTarget();
        scene.idle(120);
        scene.addKeyframe();

        scene.world().showSection(util.select().position(alarm), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(100)
                .text("A Nuclear Alarm rings and powers every side once a reading passes its threshold. Pick the reading on its top and the threshold on its sides.")
                .pointAt(util.vector().topOf(alarm))
                .placeNearTarget();
        scene.idle(110);
    }
}
