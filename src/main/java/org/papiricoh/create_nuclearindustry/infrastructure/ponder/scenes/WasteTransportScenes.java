package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.AllNuclearParticles;
import org.papiricoh.create_nuclearindustry.maintenance.MaintenanceRegistry;
import org.papiricoh.create_nuclearindustry.wastetransport.cask.WasteCaskBlockEntity;
import org.papiricoh.create_nuclearindustry.wastetransport.cistern.WasteCisternBlockEntity;

/**
 * Radioactive waste transport: forming a cistern and a cask, what each takes, their shielding,
 * a breach and its repair. Schematic {@code waste_transport} (tools/structgen/ponder_waste_transport.py):
 * a 2x2x2 cistern at (1,1,1)-(2,2,2) fed by a pipe at (0,1,2), and a cask along x at (4,1,1)-(6,2,2).
 */
public final class WasteTransportScenes {

    private WasteTransportScenes() {}

    public static void transport(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("waste_transport", "Moving Radioactive Waste");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.9f);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos cisternBase = util.grid().at(1, 1, 1);
        Selection cistern = util.select().fromTo(1, 1, 1, 2, 2, 2);
        scene.world().showSection(cistern, Direction.DOWN);
        scene.idle(15);
        // Ponder levels are client side, where tanks never form: join the blocks by hand.
        join(scene, util, cistern, cisternBase, WasteCisternBlockEntity.class, "Height", 2);
        scene.idle(10);
        scene.overlay().showText(80)
                .text("Radioactive Waste Cisterns connect like Create's fluid tanks: up to 3x3 across, 8 buckets a block.")
                .pointAt(util.vector().blockSurface(util.grid().at(1, 2, 1), Direction.UP))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        BlockPos pipe = util.grid().at(0, 1, 2);
        scene.world().showSection(util.select().position(pipe), Direction.EAST);
        scene.idle(10);
        scene.overlay().showText(80)
                .text("Fill them with pipes. They take only radioactive fluids: spent fuel solution, nitrates, contaminated water...")
                .pointAt(util.vector().centerOf(pipe))
                .placeNearTarget();
        scene.idle(90);

        Vec3 westFace = util.vector().blockSurface(util.grid().at(1, 2, 1), Direction.WEST);
        scene.overlay().showControls(westFace, Pointing.RIGHT, 50).rightClick().withItem(new ItemStack(Items.WATER_BUCKET));
        scene.overlay().showText(70)
                .text("Anything else is refused, so nothing else can be pumped in, not even into a moving train.")
                .colored(PonderPalette.RED)
                .pointAt(westFace)
                .placeNearTarget();
        scene.idle(80);

        BlockPos caskBase = util.grid().at(4, 1, 1);
        Selection cask = util.select().fromTo(4, 1, 1, 6, 2, 2);
        scene.world().showSection(cask, Direction.DOWN);
        scene.idle(15);
        join(scene, util, cask, caskBase, WasteCaskBlockEntity.class, "Length", 3);
        scene.idle(10);
        scene.overlay().showText(90)
                .text("Shielded Waste Casks join along their axis like Item Vaults. They hold solid waste, packages and cooled spent fuel; Hot fuel is refused.")
                .pointAt(util.vector().blockSurface(util.grid().at(5, 2, 1), Direction.UP))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        scene.overlay().showOutline(PonderPalette.GREEN, "shielded", cistern.copy().add(cask), 90);
        scene.overlay().showText(90)
                .text("Sealed, their shell lets almost no radiation out. Both ride Create trains, where Portable Storage Interfaces and schedule conditions see the cargo.")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().blockSurface(util.grid().at(4, 2, 1), Direction.NORTH))
                .placeNearTarget();
        scene.idle(100);

        Vec3 caskTop = util.vector().topOf(util.grid().at(5, 2, 1));
        scene.effects().emitParticles(caskTop, scene.effects().simpleParticleEmitter(AllNuclearParticles.RADIOACTIVE_DUST.get(),
                new Vec3(0, 0.03, 0)), 4, 120);
        scene.effects().emitParticles(util.vector().topOf(util.grid().at(5, 0, 0)),
                scene.effects().simpleParticleEmitter(AllNuclearParticles.RADIOACTIVE_DUST.get(), new Vec3(0, 0.01, 0)), 2, 120);
        scene.overlay().showOutline(PonderPalette.RED, "breach", cask, 90);
        scene.overlay().showText(90)
                .text("A derailment, a crash or an explosion breaches them: the waste spills into the ground, and they leak and irradiate until mended.")
                .colored(PonderPalette.RED)
                .pointAt(caskTop)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        Vec3 caskFront = util.vector().blockSurface(util.grid().at(4, 2, 1), Direction.WEST);
        scene.overlay().showControls(caskFront, Pointing.RIGHT, 40).rightClick()
                .withItem(new ItemStack(MaintenanceRegistry.MAINTENANCE_KIT.get()));
        scene.idle(45);
        scene.overlay().showControls(caskFront, Pointing.RIGHT, 40).rightClick()
                .withItem(new ItemStack(MaintenanceRegistry.MAINTENANCE_KIT.get()));
        scene.overlay().showText(80)
                .text("Two uses of a Maintenance Kit, by hand or from a Deployer, patch the breach.")
                .colored(PonderPalette.GREEN)
                .pointAt(caskFront)
                .placeNearTarget();
        scene.idle(90);
    }

    /**
     * Joins the blocks of {@code selection} into one multiblock of two blocks across with its controller
     * at {@code base}: the controller learns its size, the rest where their controller is.
     */
    private static void join(CreateSceneBuilder scene, SceneBuildingUtil util, Selection selection, BlockPos base,
                             Class<? extends net.minecraft.world.level.block.entity.BlockEntity> type, String lengthKey, int length) {
        Selection controller = util.select().position(base);
        scene.world().modifyBlockEntityNBT(selection.copy().substract(controller), type,
                tag -> tag.put("Controller", NbtUtils.writeBlockPos(base)), true);
        scene.world().modifyBlockEntityNBT(controller, type, tag -> {
            tag.putInt("Size", 2);
            tag.putInt(lengthKey, length);
        }, true);
    }
}
