package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.fuelcycle.FuelCycleRegistry;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.GuidePonderText;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PlantPonderText;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PlantPonderText.Script;
import org.papiricoh.create_nuclearindustry.processing.AllNuclearRecipeTypes;

/**
 * The two fan treatments of the fuel cycle, on the schematics of
 * tools/structgen/ponder_fan_treatments.py: a fan blowing through its catalyst onto a depot. What
 * the item on the depot turns into is looked up in the recipes the game has loaded, so the scene
 * follows whatever recipes the treatment has; without one the item stays as it is. Every text box
 * takes the next line of {@link GuidePonderText}.
 */
public class FanTreatmentScenes {
    private static final int FAN_SPEED = 64;

    public static void acidLeaching(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        PlantPonderText.Scene text = GuidePonderText.ACID_LEACHING;
        Script lines = text.script();
        scene.title(text.id(), text.title().english());
        scene.configureBasePlate(0, 0, 6);
        scene.scaleSceneView(0.9f);

        BlockPos acid = util.grid().at(2, 1, 2);
        BlockPos depot = util.grid().at(3, 1, 2);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(0, 1, 2, 1, 1, 2), Direction.DOWN);
        scene.world().setKineticSpeed(util.select().fromTo(0, 1, 2, 1, 1, 2), FAN_SPEED);
        scene.idle(10);
        scene.world().showSection(util.select().position(depot), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(2, 1, 1, 2, 1, 3), Direction.DOWN);
        scene.idle(20);
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().centerOf(acid))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);

        scene.overlay().showControls(util.vector().topOf(acid), Pointing.DOWN, 60)
                .withItem(new ItemStack(AllNuclearFluids.SULFURIC_ACID_BUCKET.get()));
        scene.overlay().showOutline(PonderPalette.WHITE, "sides", util.select().position(2, 1, 1).add(util.select().position(2, 1, 3)), 80);
        scene.overlay().showText(80)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(util.grid().at(2, 1, 1), Direction.NORTH))
                .placeNearTarget();
        scene.idle(90);

        ItemStack input = new ItemStack(FuelCycleRegistry.HEAVY_MINERAL_SAND.get());
        scene.world().createItemOnBeltLike(depot, Direction.UP, input);
        scene.overlay().showText(80)
                .text(lines.next())
                .pointAt(util.vector().topOf(depot))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(30);
        processed(scene, util, depot, AllNuclearRecipeTypes.ACID_LEACHING, input);
        scene.idle(60);
        scene.overlay().showText(90)
                .text(lines.next())
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().topOf(depot))
                .placeNearTarget();
        scene.idle(100);
    }

    public static void irradiation(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        PlantPonderText.Scene text = GuidePonderText.IRRADIATION;
        Script lines = text.script();
        scene.title(text.id(), text.title().english());
        scene.configureBasePlate(0, 0, 6);
        scene.scaleSceneView(0.9f);

        BlockPos source = util.grid().at(2, 1, 2);
        BlockPos depot = util.grid().at(3, 1, 2);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(0, 1, 2, 1, 1, 2), Direction.DOWN);
        scene.world().setKineticSpeed(util.select().fromTo(0, 1, 2, 1, 1, 2), FAN_SPEED);
        scene.idle(10);
        scene.world().showSection(util.select().position(depot), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().position(source), Direction.DOWN);
        scene.idle(20);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(source, Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showOutline(PonderPalette.GREEN, "source", util.select().position(source), 100);
        scene.overlay().showText(100)
                .text(lines.next())
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().topOf(source))
                .placeNearTarget();
        scene.idle(110);

        ItemStack input = new ItemStack(FuelCycleRegistry.THORIUM_DIOXIDE.get());
        scene.world().createItemOnBeltLike(depot, Direction.UP, input);
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().topOf(depot))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(30);
        scene.effects().emitParticles(util.vector().topOf(depot),
                scene.effects().simpleParticleEmitter(ParticleTypes.HAPPY_VILLAGER, new Vec3(0, 0.05, 0)), 2, 10);
        processed(scene, util, depot, AllNuclearRecipeTypes.IRRADIATION, input);
        scene.idle(80);
        scene.overlay().showText(80)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().centerOf(source))
                .placeNearTarget();
        scene.idle(90);
    }

    /** Swaps the depot's item for what the treatment makes of it. */
    private static void processed(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos depot, AllNuclearRecipeTypes type,
                                  ItemStack input) {
        scene.world().removeItemsFromBelt(depot);
        scene.world().createItemOnBeltLike(depot, Direction.UP, outputOf(type, input));
        scene.effects().indicateSuccess(depot);
    }

    /** The first result of the treatment's recipe for {@code input}, or the input itself when it has none. */
    private static ItemStack outputOf(AllNuclearRecipeTypes type, ItemStack input) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return input.copy();
        }
        RecipeType<Recipe<SingleRecipeInput>> recipeType = type.getType();
        return level.getRecipeManager().getRecipeFor(recipeType, new SingleRecipeInput(input), level)
                .map(holder -> holder.value().getResultItem(level.registryAccess()).copy())
                .filter(stack -> !stack.isEmpty())
                .orElse(input.copy());
    }

    private FanTreatmentScenes() {}
}
