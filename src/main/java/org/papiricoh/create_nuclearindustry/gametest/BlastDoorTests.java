package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry;
import org.papiricoh.create_nuclearindustry.civildefense.blastdoor.BlastDoorBlock;
import org.papiricoh.create_nuclearindustry.civildefense.blastdoor.BlastDoorControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.civildefense.blastdoor.LeafState;
import org.papiricoh.create_nuclearindustry.radiation.RadiationRay;
import org.papiricoh.create_nuclearindustry.radiation.RadiationTransmission;

/**
 * Blast doors: they open and shut by redstone and by rotation (faster with speed), slam shut when
 * their frame breaks, and shut leaves stop blasts and radiation while open ones let them through.
 * Doors are built across X, in the plane z = {@code z}, with the controller on the west side.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class BlastDoorTests {

    /**
     * Builds a door {@code width} x {@code height} with its bottom-left leaf at {@code (x0, y0, z)} and
     * its frame around it; returns the controller, at the bottom of the west side.
     */
    static BlockPos buildDoor(GameTestHelper helper, Block leaf, int x0, int y0, int z, int width, int height) {
        for (int x = x0 - 1; x <= x0 + width; x++) {
            for (int y = y0 - 1; y <= y0 + height; y++) {
                boolean inside = x >= x0 && x < x0 + width && y >= y0 && y < y0 + height;
                helper.setBlock(new BlockPos(x, y, z), inside ? leaf : CivilDefenseRegistry.BLAST_DOOR_FRAME.get());
            }
        }
        BlockPos controller = new BlockPos(x0 - 1, y0, z);
        helper.setBlock(controller, CivilDefenseRegistry.BLAST_DOOR_CONTROLLER.get().defaultBlockState()
                .setValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS, Direction.Axis.Z));
        return controller;
    }

    private static void assertLeaves(GameTestHelper helper, int x0, int y0, int z, int width, int height, LeafState expected) {
        for (int x = x0; x < x0 + width; x++) {
            for (int y = y0; y < y0 + height; y++) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockState state = helper.getBlockState(pos);
                helper.assertTrue(state.getBlock() instanceof BlastDoorBlock && state.getValue(BlastDoorBlock.LEAF) == expected,
                        "leaf at " + pos + " should be " + expected + ", is " + state);
                boolean solid = !state.getCollisionShape(helper.getLevel(), helper.absolutePos(pos)).isEmpty();
                helper.assertTrue(solid == (expected != LeafState.OPEN), "leaf at " + pos + (solid ? " still blocks" : " lets you through"));
            }
        }
    }

    private static CreativeMotorBlockEntity motor(GameTestHelper helper, BlockPos pos, Direction facing, int rpm) {
        Block motor = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "creative_motor"));
        helper.setBlock(pos, motor.defaultBlockState().setValue(BlockStateProperties.FACING, facing));
        CreativeMotorBlockEntity entity = helper.getBlockEntity(pos);
        entity.generatedSpeed.setValue(rpm);
        return entity;
    }

    private static int comparator(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(pos));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void redstoneOpensAndShutsTheDoor(GameTestHelper helper) {
        BlockPos controllerPos = buildDoor(helper, CivilDefenseRegistry.STEEL_BLAST_DOOR.get(), 5, 3, 6, 2, 2);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    BlastDoorControllerBlockEntity controller = helper.getBlockEntity(controllerPos);
                    helper.assertTrue(controller != null && controller.getLayout() != null, "a framed 2x2 door forms");
                })
                .thenExecute(() -> {
                    assertLeaves(helper, 5, 3, 6, 2, 2, LeafState.CLOSED);
                    helper.assertTrue(comparator(helper, controllerPos) == 0, "shut reads 0");
                    helper.setBlock(controllerPos.north(), Blocks.REDSTONE_BLOCK);
                })
                .thenWaitUntil(() -> {
                    assertLeaves(helper, 5, 3, 6, 2, 2, LeafState.OPEN);
                    helper.assertTrue(comparator(helper, controllerPos) == 15, "fully open reads 15");
                })
                .thenExecute(() -> helper.setBlock(controllerPos.north(), Blocks.AIR))
                .thenWaitUntil(() -> {
                    assertLeaves(helper, 5, 3, 6, 2, 2, LeafState.CLOSED);
                    helper.assertTrue(comparator(helper, controllerPos) == 0, "shut again reads 0");
                })
                .thenSucceed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void rotationOpensOneWayAndShutsTheOther(GameTestHelper helper) {
        BlockPos controllerPos = buildDoor(helper, CivilDefenseRegistry.LEAD_BLAST_DOOR.get(), 5, 3, 6, 3, 3);
        CreativeMotorBlockEntity drive = motor(helper, controllerPos.north(), Direction.SOUTH, 64);
        int[] rpm = {64};
        helper.startSequence()
                .thenWaitUntil(() -> {
                    BlastDoorControllerBlockEntity controller = helper.getBlockEntity(controllerPos);
                    helper.assertTrue(controller != null && controller.getSpeed() != 0, "the motor drives the controller");
                })
                .thenExecute(() -> {
                    BlastDoorControllerBlockEntity controller = helper.getBlockEntity(controllerPos);
                    if (controller.getSpeed() < 0) {
                        // Whichever way the motor turns it, find the opening direction.
                        rpm[0] = -rpm[0];
                        drive.generatedSpeed.setValue(rpm[0]);
                    }
                })
                .thenWaitUntil(() -> {
                    BlastDoorControllerBlockEntity controller = helper.getBlockEntity(controllerPos);
                    helper.assertTrue(controller.getSpeed() > 0 && controller.isFullyOpen(), "positive speed opens it");
                    assertLeaves(helper, 5, 3, 6, 3, 3, LeafState.OPEN);
                })
                .thenExecute(() -> drive.generatedSpeed.setValue(-rpm[0]))
                .thenWaitUntil(() -> {
                    BlastDoorControllerBlockEntity controller = helper.getBlockEntity(controllerPos);
                    helper.assertTrue(controller.isShut(), "reversing the shaft shuts it");
                    assertLeaves(helper, 5, 3, 6, 3, 3, LeafState.CLOSED);
                })
                .thenSucceed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void fasterShaftsOpenTheDoorSooner(GameTestHelper helper) {
        BlockPos fastPos = buildDoor(helper, CivilDefenseRegistry.STEEL_BLAST_DOOR.get(), 3, 3, 3, 2, 2);
        BlockPos slowPos = buildDoor(helper, CivilDefenseRegistry.STEEL_BLAST_DOOR.get(), 3, 3, 9, 2, 2);
        CreativeMotorBlockEntity fast = motor(helper, fastPos.north(), Direction.SOUTH, 128);
        CreativeMotorBlockEntity slow = motor(helper, slowPos.north(), Direction.SOUTH, 16);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    BlastDoorControllerBlockEntity a = helper.getBlockEntity(fastPos);
                    BlastDoorControllerBlockEntity b = helper.getBlockEntity(slowPos);
                    helper.assertTrue(a.getSpeed() != 0 && b.getSpeed() != 0, "both doors are driven");
                })
                .thenExecute(() -> {
                    // Both doors are built the same way round, so both turn the same way.
                    BlastDoorControllerBlockEntity a = helper.getBlockEntity(fastPos);
                    if (a.getSpeed() < 0) {
                        fast.generatedSpeed.setValue(-128);
                        slow.generatedSpeed.setValue(-16);
                    }
                })
                .thenWaitUntil(() -> {
                    BlastDoorControllerBlockEntity a = helper.getBlockEntity(fastPos);
                    helper.assertTrue(a.isFullyOpen(), "the fast door opens");
                })
                .thenExecute(() -> {
                    BlastDoorControllerBlockEntity b = helper.getBlockEntity(slowPos);
                    helper.assertTrue(b.getOffset() > 0 && !b.isFullyOpen(),
                            "the slow door is still on its way, at " + b.getOffset() + " blocks");
                })
                .thenSucceed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void brokenFrameSlamsTheDoorShut(GameTestHelper helper) {
        BlockPos controllerPos = buildDoor(helper, CivilDefenseRegistry.STEEL_BLAST_DOOR.get(), 5, 3, 6, 2, 2);
        helper.setBlock(controllerPos.north(), Blocks.REDSTONE_BLOCK);
        helper.startSequence()
                .thenWaitUntil(() -> assertLeaves(helper, 5, 3, 6, 2, 2, LeafState.OPEN))
                .thenExecute(() -> helper.setBlock(new BlockPos(7, 5, 6), Blocks.AIR))
                .thenWaitUntil(() -> {
                    assertLeaves(helper, 5, 3, 6, 2, 2, LeafState.CLOSED);
                    BlastDoorControllerBlockEntity controller = helper.getBlockEntity(controllerPos);
                    helper.assertTrue(controller.getLayout() == null, "an incomplete frame is no door");
                })
                .thenIdle(30)
                .thenExecute(() -> assertLeaves(helper, 5, 3, 6, 2, 2, LeafState.CLOSED))
                .thenExecute(() -> {
                    helper.setBlock(new BlockPos(7, 5, 6), CivilDefenseRegistry.BLAST_DOOR_FRAME.get());
                    helper.setBlock(controllerPos.north(), Blocks.AIR);
                })
                .thenIdle(5)
                .thenExecute(() -> helper.setBlock(controllerPos.north(), Blocks.REDSTONE_BLOCK))
                .thenWaitUntil(() -> assertLeaves(helper, 5, 3, 6, 2, 2, LeafState.OPEN))
                .thenExecute(() -> helper.destroyBlock(controllerPos))
                .thenWaitUntil(() -> assertLeaves(helper, 5, 3, 6, 2, 2, LeafState.CLOSED))
                .thenSucceed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void onlyTwoByTwoToFourByFourDoorsForm(GameTestHelper helper) {
        BlockPos wide = buildDoor(helper, CivilDefenseRegistry.STEEL_BLAST_DOOR.get(), 2, 3, 3, 5, 2);
        BlockPos single = buildDoor(helper, CivilDefenseRegistry.STEEL_BLAST_DOOR.get(), 2, 3, 9, 1, 2);
        helper.runAfterDelay(15, () -> {
            BlastDoorControllerBlockEntity a = helper.getBlockEntity(wide);
            BlastDoorControllerBlockEntity b = helper.getBlockEntity(single);
            helper.assertTrue(a != null && a.getLayout() == null, "five wide is too wide");
            helper.assertTrue(b != null && b.getLayout() == null, "one wide is too narrow");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void shutLeavesStopBlastsAndRadiation(GameTestHelper helper) {
        for (Block part : new Block[]{CivilDefenseRegistry.STEEL_BLAST_DOOR.get(), CivilDefenseRegistry.LEAD_BLAST_DOOR.get(),
                CivilDefenseRegistry.BLAST_DOOR_FRAME.get(), CivilDefenseRegistry.BLAST_DOOR_CONTROLLER.get()}) {
            helper.assertTrue(part.getExplosionResistance() > 1200.0f, part + " is spared by nuclear craters");
        }
        BlockState lead = CivilDefenseRegistry.LEAD_BLAST_DOOR.get().defaultBlockState();
        BlockState steel = CivilDefenseRegistry.STEEL_BLAST_DOOR.get().defaultBlockState();
        helper.assertTrue(Math.abs(RadiationTransmission.of(lead) - 0.02) < 1e-6, "lead lets 2% through, got " + RadiationTransmission.of(lead));
        helper.assertTrue(Math.abs(RadiationTransmission.of(steel) - 0.15) < 1e-6, "steel lets 15% through, got " + RadiationTransmission.of(steel));

        // High in the arena, so the test blast stays inside it.
        buildDoor(helper, CivilDefenseRegistry.LEAD_BLAST_DOOR.get(), 5, 9, 6, 2, 2);
        Vec3 front = helper.absoluteVec(new Vec3(5.5, 9.5, 3.5));
        Vec3 behind = helper.absoluteVec(new Vec3(5.5, 9.5, 9.5));
        double shut = RadiationRay.transmission(helper.getLevel(), front, behind);
        helper.assertTrue(Math.abs(shut - 0.02) < 1e-6, "a shut lead door lets 2% through, got " + shut);

        BlockPos shielded = new BlockPos(5, 9, 7);
        BlockPos exposed = new BlockPos(6, 9, 3);
        helper.setBlock(shielded, Blocks.DIRT);
        helper.setBlock(exposed, Blocks.DIRT);
        Vec3 blast = helper.absoluteVec(new Vec3(6.0, 9.5, 4.5));
        helper.getLevel().explode(null, blast.x, blast.y, blast.z, 2.5f, Level.ExplosionInteraction.TNT);
        helper.assertTrue(helper.getBlockState(exposed).isAir(), "the blast was real: the dirt in front is gone");
        helper.assertTrue(helper.getBlockState(shielded).is(Blocks.DIRT), "the dirt behind the shut door survives");
        assertLeaves(helper, 5, 9, 6, 2, 2, LeafState.CLOSED);

        // Open leaves: no shielding, no resistance, and a blast through the doorway leaves them be.
        helper.setBlock(shielded, Blocks.AIR);
        for (int x = 5; x <= 6; x++) {
            for (int y = 9; y <= 10; y++) {
                helper.setBlock(new BlockPos(x, y, 6), lead.setValue(BlastDoorBlock.LEAF, LeafState.OPEN));
            }
        }
        double open = RadiationRay.transmission(helper.getLevel(), front, behind);
        helper.assertTrue(Math.abs(open - 1.0) < 1e-6, "an open doorway lets everything through, got " + open);
        BlockPos leafPos = new BlockPos(5, 9, 6);
        helper.assertTrue(helper.getBlockState(leafPos).isSolid(), "water cannot wash away an open leaf");
        helper.assertTrue(helper.getBlockState(leafPos).getExplosionResistance(helper.getLevel(), helper.absolutePos(leafPos), null) == 0,
                "an open doorway does not hold back a blast");
        helper.getLevel().explode(null, blast.x, blast.y, blast.z, 2.5f, Level.ExplosionInteraction.TNT);
        helper.assertTrue(BlastDoorBlock.isOpen(helper.getBlockState(leafPos)), "the retracted panel survives the blast");
        helper.succeed();
    }
}
