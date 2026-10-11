package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.GuidePonderText;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PlantPonderText;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PlantPonderText.Script;

/**
 * The fuel cycle from ore to fuel on one line of machines ({@code ponder/fuel_cycle.nbt},
 * tools/structgen/ponder_fuel_cycle.py): crushing, washing, leaching in a mixer, enrichment in
 * centrifuges, pellets under a press and the fuel assembly, then the electrolyzer for fusion. Each
 * station turns while it is talked about, with what goes in and comes out shown over it. Every text
 * box takes the next line of {@link GuidePonderText}.
 */
public class FuelCycleScenes {
    private static final int SPEED = 64;

    public static void oreToFuel(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        PlantPonderText.Scene text = GuidePonderText.FUEL_CYCLE;
        Script lines = text.script();
        scene.title(text.id(), text.title().english());
        scene.configureBasePlate(0, 0, 14);
        scene.scaleSceneView(0.6f);
        scene.setSceneOffsetY(-1.0f);

        BlockPos gap = util.grid().at(2, 2, 2);
        BlockPos washDepot = util.grid().at(6, 1, 2);
        BlockPos basin = util.grid().at(8, 2, 2);
        BlockPos centrifuge = util.grid().at(10, 1, 2);
        BlockPos pressDepot = util.grid().at(13, 1, 2);
        BlockPos electrolyzer = util.grid().at(10, 1, 4);

        scene.showBasePlate();
        scene.idle(10);

        // 1. Crushing
        scene.world().showSection(util.select().fromTo(0, 1, 0, 4, 5, 5), Direction.DOWN);
        scene.world().setKineticSpeed(util.select().position(1, 2, 2), -SPEED);
        scene.world().setKineticSpeed(util.select().position(3, 2, 2), SPEED);
        scene.idle(15);
        scene.overlay().showControls(util.vector().centerOf(util.grid().at(2, 4, 2)), Pointing.DOWN, 35)
                .withItem(new ItemStack(AllNuclearItems.RAW_URANIUM.get()));
        scene.overlay().showText(110)
                .text(lines.next())
                .pointAt(util.vector().topOf(gap))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(40);
        product(scene, util, gap, AllNuclearItems.CRUSHED_RAW_URANIUM.get(), 70);
        scene.idle(80);

        // 2. Washing
        scene.world().showSection(util.select().fromTo(5, 1, 0, 7, 5, 5), Direction.DOWN);
        scene.world().setKineticSpeed(util.select().position(6, 1, 4), SPEED);
        scene.idle(15);
        scene.world().createItemOnBeltLike(washDepot, Direction.UP, new ItemStack(AllNuclearItems.CRUSHED_RAW_URANIUM.get()));
        scene.overlay().showText(70)
                .text(lines.next())
                .pointAt(util.vector().topOf(washDepot))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(40);
        scene.world().removeItemsFromBelt(washDepot);
        scene.world().createItemOnBeltLike(washDepot, Direction.UP, new ItemStack(AllNuclearItems.URANIUM_CONCENTRATE.get()));
        scene.idle(40);

        // 3. Leaching and conversion in the mixer
        scene.world().showSection(util.select().fromTo(8, 1, 0, 8, 5, 5), Direction.DOWN);
        scene.world().setKineticSpeed(util.select().position(8, 4, 2), SPEED);
        scene.idle(15);
        scene.overlay().showControls(util.vector().blockSurface(basin, Direction.WEST), Pointing.LEFT, 70)
                .withItem(new ItemStack(AllNuclearFluids.SULFURIC_ACID_BUCKET.get()));
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(basin, Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(40);
        product(scene, util, basin, AllNuclearItems.YELLOWCAKE.get(), 50);
        scene.idle(60);
        scene.overlay().showText(80)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().blockSurface(util.grid().at(8, 1, 2), Direction.NORTH))
                .placeNearTarget();
        scene.idle(90);

        // 4. Enrichment
        scene.world().showSection(util.select().fromTo(9, 1, 0, 11, 5, 3), Direction.DOWN);
        scene.world().setKineticSpeed(util.select().fromTo(10, 1, 2, 11, 1, 2), SPEED);
        scene.idle(15);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().topOf(centrifuge))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        // 5. Pellets and the fuel assembly
        scene.world().showSection(util.select().fromTo(12, 1, 0, 13, 5, 5), Direction.DOWN);
        scene.world().setKineticSpeed(util.select().position(13, 3, 2), SPEED);
        scene.idle(15);
        scene.world().createItemOnBeltLike(pressDepot, Direction.UP, new ItemStack(AllNuclearItems.URANIUM_DIOXIDE.get()));
        scene.overlay().showText(80)
                .text(lines.next())
                .pointAt(util.vector().topOf(pressDepot))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(40);
        scene.world().removeItemsFromBelt(pressDepot);
        scene.world().createItemOnBeltLike(pressDepot, Direction.UP, new ItemStack(AllNuclearItems.URANIUM_FUEL_PELLET.get()));
        scene.idle(50);
        scene.world().removeItemsFromBelt(pressDepot);
        scene.world().createItemOnBeltLike(pressDepot, Direction.UP, new ItemStack(AllNuclearItems.INCOMPLETE_URANIUM_REACTOR_FUEL.get()));
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().topOf(pressDepot))
                .placeNearTarget();
        scene.idle(50);
        scene.world().removeItemsFromBelt(pressDepot);
        scene.world().createItemOnBeltLike(pressDepot, Direction.UP, new ItemStack(AllNuclearItems.URANIUM_REACTOR_FUEL.get()));
        scene.effects().indicateSuccess(pressDepot);
        scene.idle(50);

        // 6. The electrolyzer, for fusion
        scene.world().showSection(util.select().fromTo(9, 1, 4, 11, 5, 5), Direction.DOWN);
        scene.world().setKineticSpeed(util.select().position(electrolyzer), SPEED);
        scene.idle(15);
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().topOf(electrolyzer))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(40);
        product(scene, util, electrolyzer, AllNuclearItems.LITHIUM.get(), 60);
        scene.idle(70);
    }

    /** Shows what a station makes, over it, for {@code ticks}. */
    private static void product(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos station, ItemLike item, int ticks) {
        scene.overlay().showControls(util.vector().topOf(station), Pointing.DOWN, ticks).withItem(new ItemStack(item));
    }

    private FuelCycleScenes() {}
}
