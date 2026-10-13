package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.create_nuclearindustry.fusion.cryo.plant.CryoplantControllerBlock;
import org.papiricoh.create_nuclearindustry.fusion.cryo.plant.CryoplantPortBlock;
import org.papiricoh.create_nuclearindustry.fusion.cryo.plant.CryoplantPortMode;

/**
 * The cryogenic plant of {@code ponder/cryoplant.nbt} (tools/structgen/ponder_cryoplant.py): a 3x3x4
 * box with its controller and two compressors up the north wall and its helium ports on the west wall.
 */
public class CryoplantScenes {

    public static void plant(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("cryoplant", "The Cryogenic Plant");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.9f);

        BlockPos controller = util.grid().at(3, 2, 2);
        BlockPos heliumIn = util.grid().at(2, 2, 3);
        BlockPos liquidOut = util.grid().at(2, 3, 3);
        BlockPos compressor = util.grid().at(3, 3, 2);

        scene.showBasePlate();
        scene.idle(10);
        for (int y = 1; y <= 4; y++) {
            scene.world().showSection(util.select().fromTo(2, y, 2, 4, y, 4), Direction.DOWN);
            scene.idle(6);
        }
        scene.idle(10);
        scene.overlay().showText(100)
                .text("A Cryogenic Plant liquefies helium for a tokamak's magnets. It is a 3x3 box of Cryogenic Plant Casing, 3 to 5 blocks tall; every layer adds capacity.")
                .pointAt(util.vector().topOf(util.grid().at(3, 4, 3)))
                .placeNearTarget();
        scene.idle(110);
        scene.addKeyframe();

        scene.overlay().showControls(util.vector().blockSurface(controller, Direction.NORTH), Pointing.DOWN, 60).rightClick();
        scene.overlay().showText(80)
                .text("The Controller sits in a wall. Right-click it to form the plant.")
                .pointAt(util.vector().blockSurface(controller, Direction.NORTH))
                .placeNearTarget();
        scene.idle(90);
        scene.addKeyframe();

        scene.world().showSection(util.select().fromTo(3, 1, 0, 3, 1, 1).add(util.select().fromTo(3, 3, 0, 3, 3, 1)), Direction.SOUTH);
        scene.idle(10);
        scene.world().setKineticSpeed(util.select().fromTo(3, 1, 0, 3, 1, 2).add(util.select().fromTo(3, 3, 0, 3, 3, 2)), 128);
        scene.world().modifyBlock(controller, state -> state.setValue(CryoplantControllerBlock.RUNNING, true), false);
        scene.overlay().showText(100)
                .text("Compressors in the walls take the rotation, at 128 RPM or more. They are hungry: about 10240 SU per RPM for each layer, shared between them.")
                .pointAt(util.vector().blockSurface(compressor, Direction.NORTH))
                .placeNearTarget();
        scene.idle(110);
        scene.addKeyframe();

        scene.overlay().showOutline(PonderPalette.INPUT, "in", util.select().position(heliumIn), 90);
        scene.overlay().showText(90)
                .text("A port on Warm Helium In takes back the helium the tokamak's coils boil off...")
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().blockSurface(heliumIn, Direction.WEST))
                .placeNearTarget();
        scene.idle(100);
        scene.overlay().showOutline(PonderPalette.OUTPUT, "out", util.select().position(liquidOut), 90);
        scene.overlay().showControls(util.vector().blockSurface(liquidOut, Direction.WEST), Pointing.RIGHT, 60)
                .rightClick()
                .withItem(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "wrench"))));
        scene.overlay().showText(90)
                .text("...and one on Liquid Helium Out sends it, cold, to the tokamak's Cryogenic Ports. A wrench changes a port's mode.")
                .colored(PonderPalette.OUTPUT)
                .pointAt(util.vector().blockSurface(liquidOut, Direction.WEST))
                .placeNearTarget();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("Each pass loses a little helium. The helium ash the plasma makes as it burns tops the loop up again.")
                .pointAt(util.vector().blockSurface(heliumIn, Direction.WEST))
                .placeNearTarget();
        scene.idle(100);
        scene.addKeyframe();

        scene.world().modifyBlock(liquidOut, state -> state.setValue(CryoplantPortBlock.MODE, CryoplantPortMode.LIQUID_AIR_OUT), false);
        scene.overlay().showText(110)
                .text("The first helium comes from the air. With a port on Liquid Air Out the plant liquefies air instead, and a Centrifuge splits liquid air into liquid nitrogen and a trace of helium.")
                .colored(PonderPalette.BLUE)
                .pointAt(util.vector().blockSurface(liquidOut, Direction.WEST))
                .placeNearTarget();
        scene.idle(120);
        scene.world().modifyBlock(liquidOut, state -> state.setValue(CryoplantPortBlock.MODE, CryoplantPortMode.LIQUID_HELIUM_OUT), false);
        scene.addKeyframe();

        scene.world().modifyBlock(heliumIn, state -> state.setValue(CryoplantPortBlock.MODE, CryoplantPortMode.NITROGEN_IN), false);
        scene.overlay().showText(100)
                .text("Liquid nitrogen poured into a Nitrogen In port precools the plant, which then makes half as much liquid helium again.")
                .pointAt(util.vector().blockSurface(heliumIn, Direction.WEST))
                .placeNearTarget();
        scene.idle(110);
        scene.world().modifyBlock(heliumIn, state -> state.setValue(CryoplantPortBlock.MODE, CryoplantPortMode.HELIUM_IN), false);
        scene.idle(10);
    }
}
