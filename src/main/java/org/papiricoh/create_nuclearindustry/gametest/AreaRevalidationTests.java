package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseConfig;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry;
import org.papiricoh.create_nuclearindustry.civildefense.shelter.ShelterCertification.Status;
import org.papiricoh.create_nuclearindustry.civildefense.shelter.ShelterFiltrationUnitBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentMonitorBlockEntity;

/**
 * Changes no neighbour update reports (a nuclear crater through a building reaches controllers
 * through {@code requestStructureRevalidation(null)}) still get the building measured again
 * promptly, although routine measurements of an untouched building are skipped.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class AreaRevalidationTests {
    private static final BlockPos UNIT = new BlockPos(6, 2, 6);

    /** A sealed containment asked to revalidate is measured again once the change settles. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "perf_revalidate_containment", timeoutTicks = 300)
    public static void revalidatedContainmentIsMeasuredAgain(GameTestHelper helper) {
        ContainmentTestBuilder.concreteRoom(helper);
        int[] surveys = new int[1];
        helper.startSequence()
                .thenWaitUntil(() -> {
                    ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
                    helper.assertTrue(monitor != null && monitor.sealed() && !monitor.isSurveying(), "the room seals");
                })
                .thenExecute(() -> {
                    ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
                    helper.assertTrue(monitor != null, "the monitor is there");
                    surveys[0] = monitor.getSurveysStarted();
                    monitor.requestStructureRevalidation(null);
                })
                .thenWaitUntil(() -> {
                    ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
                    helper.assertTrue(monitor != null && monitor.getSurveysStarted() > surveys[0],
                            "a revalidation request measures the building again");
                })
                .thenWaitUntil(() -> {
                    ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
                    helper.assertTrue(monitor != null && monitor.sealed() && !monitor.isSurveying(), "and finds it still sealed");
                })
                .thenSucceed();
    }

    /**
     * An open shelter unit whose wait has grown to four rescan intervals surveys again within one
     * interval after a revalidation request, and starts its back-off over.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "perf_revalidate_shelter", timeoutTicks = 300)
    public static void revalidatedShelterIsSurveyedWithinOneInterval(GameTestHelper helper) {
        helper.setBlock(UNIT.below(), Blocks.STONE);
        helper.setBlock(UNIT, CivilDefenseRegistry.SHELTER_FILTRATION_UNIT.get().defaultBlockState()
                .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Y));
        int rescan = CivilDefenseConfig.shelterRescanTicks;
        int[] surveys = new int[1];
        helper.startSequence()
                .thenWaitUntil(() -> {
                    ShelterFiltrationUnitBlockEntity unit = unit(helper);
                    helper.assertTrue(unit != null && unit.getSurveysStarted() >= 1
                            && (unit.getStatus() == Status.OPEN || unit.getStatus() == Status.TOO_BIG), "the first survey finds no room");
                })
                .thenExecute(() -> expire(helper))
                .thenIdle(12)
                .thenExecute(() -> expire(helper))
                .thenIdle(12)
                .thenExecute(() -> {
                    ShelterFiltrationUnitBlockEntity unit = unit(helper);
                    helper.assertTrue(unit != null && unit.getOpenInterval() > rescan,
                            "the open room's wait has grown, got " + (unit == null ? -1 : unit.getOpenInterval()));
                    surveys[0] = unit.getSurveysStarted();
                    unit.requestStructureRevalidation(null);
                })
                .thenIdle(rescan + 5)
                .thenExecute(() -> {
                    ShelterFiltrationUnitBlockEntity unit = unit(helper);
                    helper.assertTrue(unit != null && unit.getSurveysStarted() > surveys[0],
                            "surveyed again within one rescan interval");
                    helper.assertTrue(unit.getOpenInterval() == rescan,
                            "and the back-off starts over, got " + unit.getOpenInterval());
                })
                .thenSucceed();
    }

    private static ShelterFiltrationUnitBlockEntity unit(GameTestHelper helper) {
        return helper.getLevel().getBlockEntity(helper.absolutePos(UNIT)) instanceof ShelterFiltrationUnitBlockEntity unit ? unit : null;
    }

    private static void expire(GameTestHelper helper) {
        ShelterFiltrationUnitBlockEntity unit = unit(helper);
        helper.assertTrue(unit != null, "the unit is there");
        unit.expireSurveyTimer();
    }
}
