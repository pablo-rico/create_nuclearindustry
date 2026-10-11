package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DoorBlock;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.door.ContainmentDoorBlock;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.door.ContainmentDoorMotion;

/**
 * The containment door of {@code ponder/containment_door.nbt} (tools/structgen/ponder_containment_door.py)
 * in a stretch of containment wall: it opens and shuts with its own animation, which the door's
 * block entity plays when its state changes.
 */
public class ContainmentDoorScenes {
    private static final int STROKE = ContainmentDoorMotion.STROKE_TICKS + 10;

    public static void door(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("containment_door", "The Containment Door");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.9f);

        BlockPos lower = util.grid().at(3, 1, 3);
        BlockPos upper = util.grid().at(3, 2, 3);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(0, 1, 4, 6, 5, 6), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(1, 1, 3, 5, 4, 3), Direction.DOWN);
        scene.idle(20);
        scene.overlay().showOutline(PonderPalette.WHITE, "door", util.select().fromTo(3, 1, 3, 3, 2, 3), 90);
        scene.overlay().showText(90)
                .text("A Containment Door is the way into a containment. Shut, it seals like the wall around it and holds 8 bar.")
                .pointAt(util.vector().blockSurface(upper, Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        scene.overlay().showControls(util.vector().blockSurface(lower, Direction.NORTH), Pointing.RIGHT, 40).rightClick();
        scene.idle(10);
        open(scene, lower, upper, true);
        scene.overlay().showText(90)
                .text("Click it or give it a redstone signal: it pulls out of its frame and slides sideways along the wall.")
                .pointAt(util.vector().blockSurface(util.grid().at(4, 2, 3), Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showOutline(PonderPalette.RED, "doorway", util.select().fromTo(3, 1, 3, 3, 2, 3), 80);
        scene.overlay().showText(80)
                .text("Open, the doorway is a hole: the containment is no longer sealed until the door shuts again.")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().centerOf(lower))
                .placeNearTarget();
        scene.idle(90);

        scene.overlay().showControls(util.vector().blockSurface(lower, Direction.NORTH), Pointing.RIGHT, 40)
                .withItem(new ItemStack(Items.REDSTONE_TORCH));
        scene.idle(10);
        open(scene, lower, upper, false);
        scene.idle(STROKE);
        scene.overlay().showText(100)
                .text("While the containment behind it is more than 0.2 bar above the outside, the door is pressure-locked: neither a click nor redstone opens it.")
                .colored(PonderPalette.BLUE)
                .pointAt(util.vector().topOf(util.grid().at(3, 1, 5)))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(80)
                .text("Two doors side by side, hinged on opposite sides, open together and slide apart.")
                .pointAt(util.vector().blockSurface(upper, Direction.NORTH))
                .placeNearTarget();
        scene.idle(90);
    }

    /** Sets both halves; the door's block entity animates the stroke. */
    private static void open(CreateSceneBuilder scene, BlockPos lower, BlockPos upper, boolean open) {
        for (BlockPos half : new BlockPos[]{lower, upper}) {
            scene.world().modifyBlock(half, state -> state.setValue(DoorBlock.OPEN, open)
                    .setValue(ContainmentDoorBlock.VISIBLE, !open && state.getValue(ContainmentDoorBlock.VISIBLE)), false);
        }
    }
}
