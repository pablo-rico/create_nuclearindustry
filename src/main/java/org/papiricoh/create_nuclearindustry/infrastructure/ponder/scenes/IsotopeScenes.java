package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.GuidePonderText;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PlantPonderText;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PlantPonderText.Script;
import org.papiricoh.create_nuclearindustry.isotope.IsotopeRegistry;
import org.papiricoh.create_nuclearindustry.isotope.SterileFood;
import org.papiricoh.create_nuclearindustry.isotope.channel.ChannelStatus;
import org.papiricoh.create_nuclearindustry.isotope.channel.IrradiationChannelBlock;
import org.papiricoh.create_nuclearindustry.isotope.irradiator.GammaIrradiatorBlockEntity;
import org.papiricoh.create_nuclearindustry.isotope.smoke.SmokeDetectorBlock;

/** Isotopes and nuclear medicine: making cobalt-60, decay, the Gamma Irradiator and the Smoke Detector. */
public final class IsotopeScenes {

    private IsotopeScenes() {}

    /**
     * A piece of reactor wall with the irradiation channel in it (1,2,2), facing west; a depot (4,1,4)
     * under the gamma irradiator (4,3,4); a smoke detector on the floor (7,1,2) with a redstone lamp
     * west of it (6,1,2) and a campfire two blocks south (7,1,4).
     */
    public static void isotopes(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        PlantPonderText.Scene text = GuidePonderText.ISOTOPES;
        Script lines = text.script();
        scene.title(text.id(), text.title().english());
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.9f);

        BlockPos channel = util.grid().at(1, 2, 2);
        BlockPos depot = util.grid().at(4, 1, 4);
        BlockPos irradiator = util.grid().at(4, 3, 4);
        BlockPos detector = util.grid().at(7, 1, 2);
        BlockPos lamp = util.grid().at(6, 1, 2);
        BlockPos campfire = util.grid().at(7, 1, 4);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(1, 1, 1, 1, 3, 3), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(channel, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        scene.overlay().showControls(util.vector().blockSurface(channel, Direction.WEST), Pointing.RIGHT, 40).rightClick()
                .withItem(new ItemStack(IsotopeRegistry.COBALT_INGOT.get()));
        scene.idle(10);
        scene.world().modifyBlock(channel, state -> state.setValue(IrradiationChannelBlock.STATUS, ChannelStatus.IRRADIATING), false);
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(channel, Direction.WEST))
                .placeNearTarget();
        scene.idle(70);
        scene.world().modifyBlock(channel, state -> state.setValue(IrradiationChannelBlock.STATUS, ChannelStatus.READY), false);
        scene.idle(10);
        scene.overlay().showControls(util.vector().blockSurface(channel, Direction.WEST), Pointing.RIGHT, 40)
                .withItem(new ItemStack(IsotopeRegistry.COBALT_60.get()));
        scene.idle(40);

        scene.overlay().showText(100)
                .text(lines.next())
                .colored(PonderPalette.BLUE)
                .pointAt(util.vector().blockSurface(channel, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);

        scene.world().showSection(util.select().position(depot), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().position(irradiator), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showControls(util.vector().topOf(irradiator), Pointing.DOWN, 40).rightClick()
                .withItem(new ItemStack(IsotopeRegistry.COBALT_60.get()));
        scene.world().modifyBlockEntity(irradiator, GammaIrradiatorBlockEntity.class, be -> be.preview(2, false));
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(irradiator, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        ItemStack flesh = new ItemStack(Items.ROTTEN_FLESH, 4);
        scene.world().createItemOnBeltLike(depot, Direction.NORTH, flesh);
        scene.idle(10);
        scene.world().modifyBlockEntity(irradiator, GammaIrradiatorBlockEntity.class, be -> be.preview(2, true));
        scene.overlay().showText(100)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().topOf(depot))
                .placeNearTarget();
        scene.idle(60);
        scene.world().removeItemsFromBelt(depot);
        scene.world().createItemOnBeltLike(depot, Direction.UP, SterileFood.sterilise(flesh));
        scene.world().modifyBlockEntity(irradiator, GammaIrradiatorBlockEntity.class, be -> be.preview(2, false));
        scene.idle(50);

        scene.world().showSection(util.select().position(detector).add(util.select().position(lamp)), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().position(campfire), Direction.DOWN);
        scene.idle(15);
        scene.world().modifyBlock(campfire, state -> state.setValue(CampfireBlock.LIT, true), false);
        scene.idle(10);
        scene.world().modifyBlock(detector, state -> state.setValue(SmokeDetectorBlock.POWER, 12), false);
        scene.world().modifyBlock(lamp, state -> state.setValue(RedstoneLampBlock.LIT, true), false);
        Vec3 smoke = util.vector().topOf(campfire);
        scene.effects().emitParticles(smoke, scene.effects().simpleParticleEmitter(ParticleTypes.CAMPFIRE_COSY_SMOKE, new Vec3(0, 0.05, 0)), 1, 30);
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().topOf(detector))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);
    }
}
