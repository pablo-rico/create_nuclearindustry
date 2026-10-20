package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearDisplaySources;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.silo.LaunchControllerDisplaySource;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloControllerEntity;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry;

/** The launch controller exposes a live Display Link readout. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class LaunchControllerDisplayTests {
    private static final BlockPos CONTROLLER = new BlockPos(5, 2, 5);

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void displaySourceReadsController(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, SiloRegistry.CONTROLLER.get());
        SiloControllerEntity controller = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(controller != null, "launch controller block entity exists");
        var registered = DisplaySource.BY_BLOCK_ENTITY.get(SiloRegistry.CONTROLLER_ENTITY.get());
        helper.assertTrue(registered != null && registered.contains(AllNuclearDisplaySources.LAUNCH_CONTROLLER.get()),
                "launch controller has its Display Link source");
        var lines = new LaunchControllerDisplaySource().read(controller);
        helper.assertTrue(lines.size() == 4, "state, countdown, reach and target are displayed");
        helper.assertTrue(lines.get(0).getString().toLowerCase(java.util.Locale.ROOT)
                        .contains(controller.state().name().toLowerCase(java.util.Locale.ROOT)),
                "the state line reads the controller");
        helper.succeed();
    }
}
