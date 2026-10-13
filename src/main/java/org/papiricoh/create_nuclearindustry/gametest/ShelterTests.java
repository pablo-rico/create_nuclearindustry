package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseConfig;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry;
import org.papiricoh.create_nuclearindustry.civildefense.shelter.ShelterCertification.Status;
import org.papiricoh.create_nuclearindustry.civildefense.shelter.ShelterFiltrationUnitBlockEntity;
import org.papiricoh.create_nuclearindustry.civildefense.shelter.ShelterRegistry;
import org.papiricoh.create_nuclearindustry.radiation.ExposureModifiers;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;

/**
 * Shelters: a stone brick room with 5x3x5 blocks of air inside and a filtration unit on its floor,
 * driven by a creative motor set into the floor under it.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class ShelterTests {
    private static final String BATCH = "civildefense_shelter";
    private static final BlockPos UNIT = new BlockPos(6, 2, 6);
    /** A body standing inside the room, next to the unit. */
    private static final Vec3 INSIDE = new Vec3(4.5, 2.9, 6.5);
    private static final Vec3 OUTSIDE = new Vec3(11.5, 2.9, 6.5);

    /** Builds the room and its unit; with {@code rpm} > 0, a creative motor under the unit turns it. */
    private static void shelter(GameTestHelper helper, int rpm) {
        for (int x = 3; x <= 9; x++) {
            for (int y = 1; y <= 5; y++) {
                for (int z = 3; z <= 9; z++) {
                    boolean shell = x == 3 || x == 9 || y == 1 || y == 5 || z == 3 || z == 9;
                    helper.setBlock(new BlockPos(x, y, z), shell ? Blocks.STONE_BRICKS : Blocks.AIR);
                }
            }
        }
        if (rpm > 0) {
            Block motor = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "creative_motor"));
            helper.setBlock(UNIT.below(), motor.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
            CreativeMotorBlockEntity motorEntity = helper.getBlockEntity(UNIT.below());
            motorEntity.generatedSpeed.setValue(rpm);
        }
        helper.setBlock(UNIT, CivilDefenseRegistry.SHELTER_FILTRATION_UNIT.get().defaultBlockState()
                .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Y));
    }

    private static Status status(GameTestHelper helper) {
        ShelterFiltrationUnitBlockEntity unit = helper.getBlockEntity(UNIT);
        return unit == null ? null : unit.getStatus();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_shelterFiltersGroundDose", timeoutTicks = 100)
    public static void runningShelterFiltersGroundDose(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 inside = helper.absoluteVec(INSIDE);
        Vec3 outside = helper.absoluteVec(OUTSIDE);
        shelter(helper, 64);
        ContaminationTests.contaminate(helper, 1, 4);
        // Before the unit has surveyed the room, only the roof shields the body.
        double before = RadiationHandler.ambientRate(level, inside);
        helper.runAfterDelay(20, () -> {
            try {
                helper.assertTrue(before > 1, "the roof alone lets most of the ground dose in, got " + before);
                helper.assertTrue(status(helper) == Status.CERTIFIED, "a sealed stone room is certified, got " + status(helper));
                ShelterFiltrationUnitBlockEntity unit = helper.getBlockEntity(UNIT);
                helper.assertTrue(unit != null && unit.isRunning(), "the motor runs the unit");
                helper.assertTrue(unit.getVolume() == 5 * 3 * 5 - 1, "the room holds 74 blocks of air, got " + unit.getVolume());
                double after = RadiationHandler.ambientRate(level, inside);
                helper.assertTrue(Math.abs(after - before * CivilDefenseConfig.shelterFilterFactor) < before * 1e-3,
                        "the filter lets " + CivilDefenseConfig.shelterFilterFactor + " of " + before + " in, got " + after);
                helper.assertTrue(ExposureModifiers.shelter(level, outside) == 1.0, "outside the room nothing is filtered");
            } finally {
                ContaminationTests.clean(helper, 1);
            }
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 100)
    public static void roomWithAHoleIsNotCertified(GameTestHelper helper) {
        shelter(helper, 64);
        helper.setBlock(new BlockPos(9, 3, 6), Blocks.AIR);
        Vec3 inside = helper.absoluteVec(INSIDE);
        helper.runAfterDelay(20, () -> {
            Status status = status(helper);
            helper.assertTrue(status == Status.OPEN || status == Status.TOO_BIG, "air gets out through the hole, got " + status);
            helper.assertTrue(ShelterRegistry.at(helper.getLevel(), net.minecraft.core.BlockPos.containing(inside)) == null,
                    "no shelter is registered");
            helper.assertTrue(ExposureModifiers.shelter(helper.getLevel(), inside) == 1.0, "and nothing is filtered");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 100)
    public static void glassWallIsTooWeak(GameTestHelper helper) {
        shelter(helper, 64);
        helper.setBlock(new BlockPos(9, 3, 6), Blocks.GLASS);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(status(helper) == Status.WEAK_WALL, "glass gives way to blasts, got " + status(helper));
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 100)
    public static void stoppedShelterStillCutsBlastsButDoesNotFilter(GameTestHelper helper) {
        shelter(helper, 0);
        Vec3 inside = helper.absoluteVec(INSIDE);
        Vec3 outside = helper.absoluteVec(OUTSIDE);
        helper.runAfterDelay(20, () -> {
            ServerLevel level = helper.getLevel();
            helper.assertTrue(status(helper) == Status.CERTIFIED, "the room is certified without power, got " + status(helper));
            helper.assertTrue(ExposureModifiers.shelter(level, inside) == 1.0, "but a stopped unit filters nothing");
            helper.assertTrue(ShelterRegistry.blastFactor(level, inside) == CivilDefenseConfig.shelterBlastFactor,
                    "blast waves are cut inside");
            helper.assertTrue(ShelterRegistry.blastFactor(level, outside) == 1.0, "and not outside");
            helper.setBlock(UNIT, Blocks.AIR);
            helper.assertTrue(ShelterRegistry.blastFactor(level, inside) == 1.0, "breaking the unit ends the shelter");
            helper.succeed();
        });
    }
}
