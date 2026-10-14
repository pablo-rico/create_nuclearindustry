package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.contraptions.piston.LinearActuatorBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlDeskBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleFunction;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleSurfaceBlock;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleType;
import org.papiricoh.create_nuclearindustry.controlroom.desk.OutputMode;
import org.papiricoh.create_nuclearindustry.controlroom.servo.ServoGearboxBlock;
import org.papiricoh.create_nuclearindustry.controlroom.servo.ServoGearboxBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.servo.ServoMode;
import org.papiricoh.create_nuclearindustry.controlroom.servo.ServoPlan;

/**
 * The Servo Gearbox with real Create pistons and pulleys: a creative motor turns the servo, whose far
 * end drives a piston standing up on its poles with a stone on its head. The signal comes from a
 * redstone block, or from a comparator reading a composter for an analog level (a composter at
 * level n gives n).
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class ServoGearboxTests {
    private static final int RPM = 128;
    private static final int POLES = 4;
    /** The piston's foot, above its poles, on every rig. */
    private static final int Y = 1 + POLES;

    /** Motor at x 2, servo at x 3 and a piston at x 4 (on poles below, a stone on its head), along z. */
    private record Rig(BlockPos motor, BlockPos servo, BlockPos piston, BlockPos load) {}

    private static Rig rig(GameTestHelper helper, int z, boolean sticky) {
        BlockPos motor = new BlockPos(2, Y, z);
        BlockPos servo = new BlockPos(3, Y, z);
        BlockPos piston = new BlockPos(4, Y, z);
        piston(helper, piston, sticky);
        helper.setBlock(servo, ControlRoom2Registry.SERVO_GEARBOX.get().defaultBlockState().setValue(ServoGearboxBlock.AXIS, Direction.Axis.X));
        motor(helper, motor, Direction.EAST);
        return new Rig(motor, servo, piston, piston.above());
    }

    /** A piston facing up, its shaft along x, with {@link #POLES} poles under it and a stone on top. */
    private static void piston(GameTestHelper helper, BlockPos pos, boolean sticky) {
        for (int y = pos.getY() - POLES; y < pos.getY(); y++) {
            helper.setBlock(new BlockPos(pos.getX(), y, pos.getZ()), with(create("piston_extension_pole").defaultBlockState(), "facing", "up"));
        }
        BlockState piston = create(sticky ? "sticky_mechanical_piston" : "mechanical_piston").defaultBlockState();
        helper.setBlock(pos, with(with(piston, "facing", "up"), "axis_along_first", "true"));
        helper.setBlock(pos.above(), Blocks.STONE);
    }

    private static void motor(GameTestHelper helper, BlockPos pos, Direction facing) {
        helper.setBlock(pos, create("creative_motor").defaultBlockState().setValue(BlockStateProperties.FACING, facing));
        CreativeMotorBlockEntity motor = helper.getBlockEntity(pos);
        motor.generatedSpeed.setValue(RPM);
    }

    /**
     * A comparator reading a composter at {@code level} into the servo from the north, on a stone so it
     * stays. The composter goes in after the comparator: a comparator only looks again when a
     * neighbour changes.
     */
    private static void analogSignal(GameTestHelper helper, BlockPos servo, int level) {
        BlockPos comparator = servo.north();
        helper.setBlock(comparator.below(), Blocks.STONE);
        helper.setBlock(comparator.north().below(), Blocks.STONE);
        helper.setBlock(comparator, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.NORTH));
        setComposter(helper, servo, level);
    }

    private static void setComposter(GameTestHelper helper, BlockPos servo, int level) {
        helper.setBlock(servo.north().north(), Blocks.COMPOSTER.defaultBlockState().setValue(ComposterBlock.LEVEL, level));
    }

    private static ServoGearboxBlockEntity servo(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos);
    }

    private static LinearActuatorBlockEntity actuator(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos);
    }

    /** The servo has stopped with the actuator set down at {@code offset}: blocks placed, output still. */
    private static void assertSettled(GameTestHelper helper, Rig rig, int offset) {
        ServoGearboxBlockEntity servo = servo(helper, rig.servo());
        LinearActuatorBlockEntity piston = actuator(helper, rig.piston());
        helper.assertTrue(servo != null && piston != null, "the servo and the piston are there");
        helper.assertTrue(Math.abs(piston.offset - offset) < ServoPlan.AT_POSITION,
                "the piston stands at " + offset + ", got " + piston.offset + " (servo " + servo.status() + ", target " + servo.target() + ")");
        helper.assertTrue(!piston.running, "the piston set its blocks down");
        helper.assertTrue(servo.output() == 0, "the servo's output is still");
        helper.assertBlockPresent(Blocks.STONE, rig.load().above(offset));
        helper.assertTrue(servo.status() == ServoGearboxBlockEntity.Status.HOLDING, "the servo holds, got " + servo.status());
    }

    // ------------------------------------------------------------------ pistons

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void aFullSignalRunsTheWholeStroke(GameTestHelper helper) {
        Rig rig = rig(helper, 6, false);
        servo(helper, rig.servo()).configure(POLES, ServoMode.EXTEND_WITH_SIGNAL);
        helper.setBlock(rig.servo().south(), Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> assertSettled(helper, rig, POLES));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void anAnalogSignalSetsAShareOfTheStroke(GameTestHelper helper) {
        Rig rig = rig(helper, 6, false);
        servo(helper, rig.servo()).configure(POLES, ServoMode.EXTEND_WITH_SIGNAL);
        // 8/15 of 4 blocks is 2.13: two blocks.
        analogSignal(helper, rig.servo(), 8);
        helper.succeedWhen(() -> {
            helper.assertTrue(servo(helper, rig.servo()).signal() == 8, "the comparator gives 8, got " + servo(helper, rig.servo()).signal());
            assertSettled(helper, rig, 2);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 500)
    public static void aWeakerSignalPullsItBack(GameTestHelper helper) {
        Rig rig = rig(helper, 6, true);
        servo(helper, rig.servo()).configure(POLES, ServoMode.EXTEND_WITH_SIGNAL);
        analogSignal(helper, rig.servo(), 8);
        helper.startSequence()
                .thenWaitUntil(() -> assertSettled(helper, rig, 2))
                // 4/15 of 4 blocks is 1.07: one block, and the sticky piston takes the stone with it.
                .thenExecute(() -> setComposter(helper, rig.servo(), 4))
                .thenWaitUntil(() -> assertSettled(helper, rig, 1))
                .thenExecute(() -> setComposter(helper, rig.servo(), 0))
                .thenWaitUntil(() -> assertSettled(helper, rig, 0))
                .thenSucceed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void retractingWithTheSignalRunsOutWithoutOne(GameTestHelper helper) {
        Rig rig = rig(helper, 6, false);
        servo(helper, rig.servo()).configure(3, ServoMode.RETRACT_WITH_SIGNAL);
        helper.succeedWhen(() -> assertSettled(helper, rig, 3));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void theStrokeCutsTheTravelShort(GameTestHelper helper) {
        Rig rig = rig(helper, 6, false);
        servo(helper, rig.servo()).configure(2, ServoMode.EXTEND_WITH_SIGNAL);
        helper.setBlock(rig.servo().south(), Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> assertSettled(helper, rig, 2));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void aLongerStrokeStopsAtThePoles(GameTestHelper helper) {
        Rig rig = rig(helper, 6, false);
        servo(helper, rig.servo()).configure(10, ServoMode.EXTEND_WITH_SIGNAL);
        helper.setBlock(rig.servo().south(), Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> {
            assertSettled(helper, rig, POLES);
            helper.assertTrue(servo(helper, rig.servo()).range() == POLES, "the servo sees the piston reach " + POLES);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void aComparatorReadsWhereItStands(GameTestHelper helper) {
        Rig rig = rig(helper, 6, false);
        servo(helper, rig.servo()).configure(POLES, ServoMode.EXTEND_WITH_SIGNAL);
        analogSignal(helper, rig.servo(), 8);
        helper.succeedWhen(() -> {
            assertSettled(helper, rig, 2);
            BlockPos abs = helper.absolutePos(rig.servo());
            int read = helper.getBlockState(rig.servo()).getAnalogOutputSignal(helper.getLevel(), abs);
            // Two of four blocks out reads 7.5, rounded to 8: the signal that put it there.
            helper.assertTrue(read == 8, "a comparator reads 8, got " + read);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void twoPistonsOnOneShaftMoveTogether(GameTestHelper helper) {
        Rig rig = rig(helper, 6, false);
        BlockPos second = rig.piston().east();
        piston(helper, second, false);
        servo(helper, rig.servo()).configure(POLES, ServoMode.EXTEND_WITH_SIGNAL);
        analogSignal(helper, rig.servo(), 8);
        helper.succeedWhen(() -> {
            assertSettled(helper, rig, 2);
            helper.assertTrue(servo(helper, rig.servo()).actuators() == 2, "the servo turns both pistons");
            LinearActuatorBlockEntity other = actuator(helper, second);
            helper.assertTrue(Math.abs(other.offset - 2) < ServoPlan.AT_POSITION, "the second piston follows, at " + other.offset);
            helper.assertBlockPresent(Blocks.STONE, second.above(3));
        });
    }

    /** A powered gearshift between servo and piston turns the piston the other way: the servo must still get there. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void throughAReversingGearshiftItStillArrives(GameTestHelper helper) {
        BlockPos motor = new BlockPos(1, Y, 6);
        BlockPos servoPos = new BlockPos(2, Y, 6);
        BlockPos gearshift = new BlockPos(3, Y, 6);
        BlockPos piston = new BlockPos(4, Y, 6);
        piston(helper, piston, false);
        helper.setBlock(gearshift.below(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(gearshift, with(with(create("gearshift").defaultBlockState(), "axis", "x"), "powered", "true"));
        helper.setBlock(servoPos, ControlRoom2Registry.SERVO_GEARBOX.get().defaultBlockState().setValue(ServoGearboxBlock.AXIS, Direction.Axis.X));
        motor(helper, motor, Direction.EAST);
        servo(helper, servoPos).configure(POLES, ServoMode.EXTEND_WITH_SIGNAL);
        analogSignal(helper, servoPos, 8);
        helper.succeedWhen(() -> assertSettled(helper, new Rig(motor, servoPos, piston, piston.above()), 2));
    }

    /** A new signal while the piston is on its way: it stops, then goes to the new block. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 400)
    public static void aSignalChangingOnTheWayIsFollowed(GameTestHelper helper) {
        Rig rig = rig(helper, 6, true);
        servo(helper, rig.servo()).configure(POLES, ServoMode.EXTEND_WITH_SIGNAL);
        BlockPos redstone = rig.servo().south();
        helper.setBlock(redstone, Blocks.REDSTONE_BLOCK);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(actuator(helper, rig.piston()).offset > 0.5f, "the piston sets off"))
                .thenExecute(() -> {
                    helper.setBlock(redstone, Blocks.AIR);
                    analogSignal(helper, rig.servo(), 4);
                })
                .thenWaitUntil(() -> assertSettled(helper, rig, 1))
                .thenSucceed();
    }

    /** The motor slowing down mid-move must not change where the piston ends up. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 500)
    public static void aChangeOfSpeedOnTheWayStillEndsOnTheBlock(GameTestHelper helper) {
        Rig rig = rig(helper, 6, false);
        servo(helper, rig.servo()).configure(POLES, ServoMode.EXTEND_WITH_SIGNAL);
        helper.setBlock(rig.servo().south(), Blocks.REDSTONE_BLOCK);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(actuator(helper, rig.piston()).offset > 0.5f, "the piston sets off"))
                .thenExecute(() -> {
                    CreativeMotorBlockEntity motor = helper.getBlockEntity(rig.motor());
                    motor.generatedSpeed.setValue(48);
                })
                .thenWaitUntil(() -> assertSettled(helper, rig, POLES))
                .thenSucceed();
    }

    /** At Create's top speed a piston moves almost half a block a tick: it must still stop on the block. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void atTopSpeedItStillStopsOnTheBlock(GameTestHelper helper) {
        Rig rig = rig(helper, 6, false);
        CreativeMotorBlockEntity motor = helper.getBlockEntity(rig.motor());
        motor.generatedSpeed.setValue(256);
        servo(helper, rig.servo()).configure(POLES, ServoMode.EXTEND_WITH_SIGNAL);
        analogSignal(helper, rig.servo(), 8);
        helper.succeedWhen(() -> assertSettled(helper, rig, 2));
    }

    /** Something unbreakable in the way: the piston stops against it and the servo says it is blocked. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void anObstacleLeavesItBlocked(GameTestHelper helper) {
        Rig rig = rig(helper, 6, false);
        helper.setBlock(rig.load().above(2), Blocks.OBSIDIAN);
        servo(helper, rig.servo()).configure(POLES, ServoMode.EXTEND_WITH_SIGNAL);
        helper.setBlock(rig.servo().south(), Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> {
            ServoGearboxBlockEntity servo = servo(helper, rig.servo());
            helper.assertTrue(servo.status() == ServoGearboxBlockEntity.Status.STALLED, "the servo is blocked, got " + servo.status());
            helper.assertTrue(servo.output() == 0, "and holds its output still");
            helper.assertBlockPresent(Blocks.OBSIDIAN, rig.load().above(2));
            helper.assertBlockPresent(Blocks.STONE, rig.load().above(1));
        });
    }

    /**
     * A desk dial set to the control rod target puts the linked reactor's target out as 0-15; through
     * the servo a piston stands at that share of its stroke.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 400)
    public static void aRodTargetDialPutsThePistonAtTheTarget(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        Rig rig = rig(helper, 10, false);
        servo(helper, rig.servo()).configure(POLES, ServoMode.EXTEND_WITH_SIGNAL);
        BlockPos desk = rig.servo().above();
        helper.setBlock(desk, AllNuclearBlocks.REACTOR_CONTROL_PANEL.get().defaultBlockState().setValue(ModuleSurfaceBlock.FACING, Direction.NORTH));
        helper.runAfterDelay(5, () -> {
            ControlDeskBlockEntity entity = helper.getBlockEntity(desk);
            helper.assertTrue(entity.insert(0, ModuleType.DIAL), "the dial fits");
            entity.configure(0, entity.module(0).withFunction(ModuleFunction.ROD_TARGET).withOutput(OutputMode.BELOW)
                    .writeConfig(helper.getLevel().registryAccess()));
            entity.setTarget(net.minecraft.core.GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(FissionTestBuilder.CONTROLLER)));
            FissionTestBuilder.controller(helper).setRodTarget(0.5f);
        });
        // Half the rods in puts out 7.5, so 8; 8/15 of 4 blocks is 2.
        helper.succeedWhen(() -> {
            helper.assertTrue(servo(helper, rig.servo()).signal() == 8, "the dial gives 8, got " + servo(helper, rig.servo()).signal());
            assertSettled(helper, rig, 2);
        });
    }

    // ------------------------------------------------------------------ pulleys

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 400)
    public static void aRopePulleyLowersByTheStroke(GameTestHelper helper) {
        int y = 12;
        BlockPos motor = new BlockPos(2, y, 6);
        BlockPos servoPos = new BlockPos(3, y, 6);
        BlockPos pulley = new BlockPos(4, y, 6);
        helper.setBlock(pulley.below(), Blocks.STONE);
        helper.setBlock(pulley, with(create("rope_pulley").defaultBlockState(), "axis", "x"));
        helper.setBlock(servoPos, ControlRoom2Registry.SERVO_GEARBOX.get().defaultBlockState().setValue(ServoGearboxBlock.AXIS, Direction.Axis.X));
        motor(helper, motor, Direction.EAST);
        servo(helper, servoPos).configure(3, ServoMode.EXTEND_WITH_SIGNAL);
        helper.setBlock(servoPos.north(), Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> {
            ServoGearboxBlockEntity servo = servo(helper, servoPos);
            LinearActuatorBlockEntity rope = actuator(helper, pulley);
            helper.assertTrue(Math.abs(rope.offset - 3) < ServoPlan.AT_POSITION,
                    "the pulley paid out 3 blocks of rope, got " + rope.offset + " (servo " + servo.status() + ")");
            helper.assertTrue(!rope.running && servo.output() == 0, "and stopped");
            helper.assertBlockPresent(Blocks.STONE, pulley.below(4));
        });
    }

    // ------------------------------------------------------------------ what it tells

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 100)
    public static void withoutRotationNothingMoves(GameTestHelper helper) {
        BlockPos servoPos = new BlockPos(3, Y, 6);
        BlockPos piston = new BlockPos(4, Y, 6);
        piston(helper, piston, false);
        helper.setBlock(servoPos, ControlRoom2Registry.SERVO_GEARBOX.get().defaultBlockState().setValue(ServoGearboxBlock.AXIS, Direction.Axis.X));
        helper.setBlock(servoPos.south(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(60, () -> {
            ServoGearboxBlockEntity servo = servo(helper, servoPos);
            helper.assertTrue(servo.status() == ServoGearboxBlockEntity.Status.NO_ROTATION, "no rotation, got " + servo.status());
            helper.assertTrue(actuator(helper, piston).offset == 0, "the piston stays in");
            helper.assertBlockPresent(Blocks.STONE, piston.above());
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 100)
    public static void withNothingToDriveItSaysSo(GameTestHelper helper) {
        BlockPos motor = new BlockPos(2, 2, 6);
        BlockPos servoPos = new BlockPos(3, 2, 6);
        helper.setBlock(servoPos.east(), with(create("shaft").defaultBlockState(), "axis", "x"));
        helper.setBlock(servoPos, ControlRoom2Registry.SERVO_GEARBOX.get().defaultBlockState().setValue(ServoGearboxBlock.AXIS, Direction.Axis.X));
        motor(helper, motor, Direction.EAST);
        helper.setBlock(servoPos.south(), Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> {
            ServoGearboxBlockEntity servo = servo(helper, servoPos);
            helper.assertTrue(servo.status() == ServoGearboxBlockEntity.Status.NO_ACTUATOR, "no actuator, got " + servo.status());
            helper.assertTrue(servo.output() == 0 && servo.comparatorSignal() == 0, "still, and the comparator reads nothing");
        });
    }

    /** A Control Desk dial set to a redstone level, putting it out below, drives the servo under the desk. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void aDeskDialMovesThePiston(GameTestHelper helper) {
        Rig rig = rig(helper, 6, false);
        servo(helper, rig.servo()).configure(POLES, ServoMode.EXTEND_WITH_SIGNAL);
        BlockPos desk = rig.servo().above();
        helper.setBlock(desk, AllNuclearBlocks.REACTOR_CONTROL_PANEL.get().defaultBlockState().setValue(ModuleSurfaceBlock.FACING, Direction.NORTH));
        ControlDeskBlockEntity entity = helper.getBlockEntity(desk);
        entity.insert(0, ModuleType.DIAL);
        entity.configure(0, entity.module(0).withFunction(ModuleFunction.SIGNAL_LEVEL).withSetting(12).withOutput(OutputMode.BELOW)
                .writeConfig(helper.getLevel().registryAccess()));
        // 12/15 of 4 blocks is 3.2: three blocks.
        helper.succeedWhen(() -> {
            helper.assertTrue(servo(helper, rig.servo()).signal() == 12, "the desk gives 12, got " + servo(helper, rig.servo()).signal());
            assertSettled(helper, rig, 3);
        });
    }

    // ------------------------------------------------------------------ settings

    /**
     * The mode and the stroke are two settings on the block: both are there to click (Create keeps one
     * behaviour per type, so sharing one would hide the other), each keeps its own value, and both
     * survive a save.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 40)
    public static void theModeAndTheStrokeAreBothSetOnTheBlock(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 6);
        helper.setBlock(pos, ControlRoom2Registry.SERVO_GEARBOX.get().defaultBlockState().setValue(ServoGearboxBlock.AXIS, Direction.Axis.X));
        ServoGearboxBlockEntity servo = servo(helper, pos);
        ScrollValueBehaviour stroke = servo.getBehaviour(ScrollValueBehaviour.TYPE);
        ScrollOptionBehaviour<?> mode = servo.getBehaviour(ServoGearboxBlockEntity.MODE);
        helper.assertTrue(stroke != null && mode != null && stroke != mode, "the stroke and the mode are two behaviours");
        long settings = servo.getAllBehaviours().stream().filter(b -> b instanceof ValueSettingsBehaviour).count();
        helper.assertTrue(settings == 2, "two settings to click on, got " + settings);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        mode.setValueSettings(player, new ValueSettingsBehaviour.ValueSettings(0, ServoMode.RETRACT_WITH_SIGNAL.ordinal()), false);
        stroke.setValueSettings(player, new ValueSettingsBehaviour.ValueSettings(0, 5), false);
        helper.assertTrue(servo.mode() == ServoMode.RETRACT_WITH_SIGNAL && servo.stroke() == 5,
                "both set from their sides, got " + servo.mode() + " / " + servo.stroke());

        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        CompoundTag saved = servo.saveWithoutMetadata(registries);
        servo.configure(ServoGearboxBlockEntity.DEFAULT_STROKE, ServoMode.EXTEND_WITH_SIGNAL);
        servo.loadWithComponents(saved, registries);
        helper.assertTrue(servo.mode() == ServoMode.RETRACT_WITH_SIGNAL && servo.stroke() == 5,
                "both come back from a save, got " + servo.mode() + " / " + servo.stroke());
        helper.succeed();
    }

    /** Flipping the mode on its side turns the motion over: full signal out becomes full signal in. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 500)
    public static void flippingTheModeOnItsSideSendsItTheOtherWay(GameTestHelper helper) {
        Rig rig = rig(helper, 6, true);
        servo(helper, rig.servo()).configure(POLES, ServoMode.EXTEND_WITH_SIGNAL);
        helper.setBlock(rig.servo().south(), Blocks.REDSTONE_BLOCK);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.startSequence()
                .thenWaitUntil(() -> assertSettled(helper, rig, POLES))
                .thenExecute(() -> servo(helper, rig.servo()).getBehaviour(ServoGearboxBlockEntity.MODE).setValueSettings(player,
                        new ValueSettingsBehaviour.ValueSettings(0, ServoMode.RETRACT_WITH_SIGNAL.ordinal()), false))
                .thenWaitUntil(() -> assertSettled(helper, rig, 0))
                .thenExecute(() -> servo(helper, rig.servo()).getBehaviour(ServoGearboxBlockEntity.MODE).setValueSettings(player,
                        new ValueSettingsBehaviour.ValueSettings(0, ServoMode.EXTEND_WITH_SIGNAL.ordinal()), false))
                .thenWaitUntil(() -> assertSettled(helper, rig, POLES))
                .thenSucceed();
    }

    // ------------------------------------------------------------------ helpers

    private static Block create(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", path));
    }

    /** The state with the property called {@code name} set to {@code value}, as a blockstate file writes it. */
    private static BlockState with(BlockState state, String name, String value) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
        if (property == null) {
            throw new IllegalArgumentException(state.getBlock() + " has no property " + name);
        }
        return set(state, property, value);
    }

    private static <T extends Comparable<T>> BlockState set(BlockState state, Property<T> property, String value) {
        return state.setValue(property, property.getValue(value).orElseThrow());
    }
}
