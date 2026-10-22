package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.enrichment.block.CentrifugeBlock;

/**
 * The centrifuge and the electrolyzer on their schematics (tools/structgen/ponder_centrifuge.py):
 * each machine faces north, one block up, so the feed pipe on top, the product pipe in front and
 * the tails pipe underneath all show, with the drive coming in from behind through a cogwheel.
 * {@code centrifuge_cascade.nbt} holds three centrifuges side by side; the single-machine scene
 * shows only the middle one, its headers closed.
 */
public class CentrifugeScenes {
    private static final int SPEED = 64;

    public static void processing(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("centrifuge", "Enriching uranium with Centrifuges");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.9f);
        BlockPos machine = util.grid().at(3, 2, 2);
        scene.world().modifyBlock(machine, state -> state.setValue(CentrifugeBlock.LEFT, false).setValue(CentrifugeBlock.RIGHT, false), false);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().position(machine), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(3, 2, 3, 3, 2, 6), Direction.NORTH);
        scene.world().setKineticSpeed(util.select().fromTo(3, 2, 2, 3, 2, 6), SPEED);
        scene.idle(15);
        scene.overlay().showText(90)
                .text("The Centrifuge separates uranium hexafluoride by enrichment. Its rotor is driven through the back, at 32 RPM or more.")
                .pointAt(util.vector().topOf(util.grid().at(3, 2, 3)))
                .placeNearTarget();
        scene.idle(100);
        scene.addKeyframe();

        BlockPos feed = machine.above();
        scene.world().showSection(util.select().position(feed), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(70)
                .text("Pipe UF6 into the top or the sides.")
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().topOf(feed))
                .placeNearTarget();
        scene.idle(80);

        BlockPos product = machine.north();
        scene.world().showSection(util.select().position(product), Direction.SOUTH);
        scene.idle(10);
        scene.overlay().showText(70)
                .text("The enriched product leaves through the front...")
                .colored(PonderPalette.OUTPUT)
                .pointAt(util.vector().blockSurface(product, Direction.WEST))
                .placeNearTarget();
        scene.idle(80);

        BlockPos tails = machine.below();
        scene.world().showSection(util.select().position(tails), Direction.UP);
        scene.idle(10);
        scene.overlay().showText(70)
                .text("...and the depleted tails through the bottom.")
                .colored(PonderPalette.OUTPUT)
                .pointAt(util.vector().blockSurface(tails, Direction.WEST))
                .placeNearTarget();
        scene.idle(80);
        // Out of the way of the dial.
        scene.world().hideSection(util.select().position(feed).add(util.select().position(product))
                .add(util.select().position(tails)), Direction.UP);
        scene.idle(15);
        scene.addKeyframe();

        scene.overlay().showText(90)
                .text("The dial on its front shows how far the current batch has got. Engineer's Goggles show its tanks.")
                .pointAt(util.vector().blockSurface(machine, Direction.NORTH).add(0, 0.15, 0))
                .placeNearTarget();
        scene.idle(100);

        scene.world().setKineticSpeed(util.select().fromTo(3, 2, 2, 3, 2, 6), 16);
        scene.overlay().showText(70)
                .text("Below 32 RPM it stalls.")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().topOf(util.grid().at(3, 2, 3)))
                .placeNearTarget();
        scene.idle(80);
        scene.world().setKineticSpeed(util.select().fromTo(3, 2, 2, 3, 2, 6), SPEED);
        scene.addKeyframe();

        scene.overlay().showControls(util.vector().topOf(feed), Pointing.DOWN, 60)
                .withItem(new ItemStack(AllNuclearItems.LITHIUM.get()));
        scene.overlay().showText(80)
                .text("It also separates Lithium-6 from lithium, and liquid air into nitrogen and helium.")
                .pointAt(util.vector().centerOf(machine))
                .placeNearTarget();
        scene.idle(90);
    }

    public static void cascade(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("centrifuge_cascade", "Centrifuge cascades");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.9f);
        BlockPos middle = util.grid().at(3, 2, 2);
        BlockPos west = util.grid().at(2, 2, 2);
        scene.world().modifyBlock(middle, state -> state.setValue(CentrifugeBlock.LEFT, false).setValue(CentrifugeBlock.RIGHT, false), false);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(3, 2, 2, 3, 2, 6), Direction.DOWN);
        scene.world().setKineticSpeed(util.select().fromTo(3, 2, 2, 3, 2, 6), SPEED);
        scene.idle(20);

        scene.world().showSection(util.select().fromTo(2, 2, 2, 2, 2, 3), Direction.EAST);
        scene.world().showSection(util.select().fromTo(4, 2, 2, 4, 2, 3), Direction.WEST);
        // Neighbouring cogwheels mesh, so the outer stages turn the other way.
        scene.world().setKineticSpeed(util.select().fromTo(2, 2, 2, 2, 2, 3).add(util.select().fromTo(4, 2, 2, 4, 2, 3)), -SPEED);
        scene.idle(10);
        scene.world().modifyBlock(middle, state -> state.setValue(CentrifugeBlock.LEFT, true).setValue(CentrifugeBlock.RIGHT, true), false);
        scene.idle(10);
        scene.overlay().showText(90)
                .text("Centrifuges standing side by side and facing the same way join into a cascade, a header running between them.")
                .pointAt(util.vector().topOf(middle).add(-0.5, -0.2, 0))
                .placeNearTarget();
        scene.idle(100);
        scene.addKeyframe();

        scene.world().showSection(util.select().fromTo(2, 2, 1, 4, 2, 1), Direction.SOUTH);
        scene.idle(15);
        scene.overlay().showOutline(PonderPalette.OUTPUT, "product", util.select().position(west.north()), 100);
        scene.overlay().showText(100)
                .text("Each stage enriches a little more: natural to LEU, LEU to HEU, HEU to weapons grade. Pipe each stage's product into the next one...")
                .colored(PonderPalette.OUTPUT)
                .pointAt(util.vector().blockSurface(west.north(), Direction.WEST))
                .placeNearTarget();
        scene.idle(110);
        scene.world().hideSection(util.select().fromTo(2, 2, 1, 4, 2, 1), Direction.NORTH);
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(2, 1, 2, 4, 1, 2), Direction.UP);
        scene.idle(15);
        scene.overlay().showOutline(PonderPalette.INPUT, "tails", util.select().position(middle.below()), 90);
        scene.overlay().showText(90)
                .text("...and its depleted tails back into the stage before it.")
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().blockSurface(middle.below(), Direction.WEST))
                .placeNearTarget();
        scene.idle(100);
        scene.world().hideSection(util.select().fromTo(2, 1, 2, 4, 1, 2), Direction.DOWN);
        scene.idle(15);
        scene.addKeyframe();

        scene.overlay().showText(80)
                .text("The header is only for show: the stages trade product and tails through pipes.")
                .pointAt(util.vector().topOf(middle))
                .placeNearTarget();
        scene.idle(90);
    }

    public static void electrolysis(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("electrolyzer", "Electrolysis");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.9f);
        BlockPos machine = util.grid().at(2, 2, 2);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().position(machine), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(2, 2, 3, 2, 2, 4), Direction.NORTH);
        scene.world().setKineticSpeed(util.select().fromTo(2, 2, 2, 2, 2, 4), SPEED);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("The Electrolyzer uses rotation in place of electric current, at 32 RPM or more.")
                .pointAt(util.vector().topOf(util.grid().at(2, 2, 3)))
                .placeNearTarget();
        scene.idle(90);
        scene.addKeyframe();

        BlockPos feed = machine.above();
        scene.world().showSection(util.select().position(feed), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(100)
                .text("Fed water, it slowly concentrates heavy water. Heavy water then splits into deuterium gas.")
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().topOf(feed))
                .placeNearTarget();
        scene.idle(110);

        BlockPos product = machine.north();
        scene.world().showSection(util.select().position(product), Direction.SOUTH);
        scene.idle(10);
        scene.overlay().showText(70)
                .text("Whichever it makes, heavy water or deuterium, leaves through the front.")
                .colored(PonderPalette.OUTPUT)
                .pointAt(util.vector().blockSurface(product, Direction.WEST))
                .placeNearTarget();
        scene.idle(80);
        scene.addKeyframe();

        scene.overlay().showControls(util.vector().topOf(feed), Pointing.DOWN, 70)
                .withItem(new ItemStack(AllNuclearItems.LITHIUM_CARBONATE.get()));
        scene.overlay().showText(80)
                .text("It also reduces lithium carbonate to lithium metal. Put the carbonate in through the top or the sides...")
                .pointAt(util.vector().centerOf(machine))
                .placeNearTarget();
        scene.idle(90);

        BlockPos bottom = machine.below();
        scene.world().showSection(util.select().position(bottom), Direction.UP);
        scene.idle(10);
        scene.overlay().showControls(util.vector().blockSurface(bottom, Direction.WEST), Pointing.LEFT, 70)
                .withItem(new ItemStack(AllNuclearItems.LITHIUM.get()));
        scene.overlay().showText(80)
                .text("...and take the lithium out of the bottom with a funnel or a chute.")
                .colored(PonderPalette.OUTPUT)
                .pointAt(util.vector().blockSurface(bottom, Direction.WEST))
                .placeNearTarget();
        scene.idle(90);
    }
}
