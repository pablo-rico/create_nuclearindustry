package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.LinkedTarget;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlDeskBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlModule;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleFunction;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleType;
import org.papiricoh.create_nuclearindustry.controlroom.desk.PidController;
import org.papiricoh.create_nuclearindustry.reactor.block.RedstonePortMode;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorRedstonePortBlockEntity;

/**
 * The rod controller module drives a real reactor's rod target toward its setpoint, in whole rod
 * segments, so the rod ports move the pistons; it lets go in standby or on a SCRAM.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class PidModuleTests {
    private static final BlockPos DESK = new BlockPos(7, 1, 7);
    /** Fast and unlimited, so a few steps show the direction without waiting on the default tuning. */
    private static final PidController.Tuning QUICK = new PidController.Tuning(1.5, 1.0, 0.0, 0.0, 0.0);

    private static ControlDeskBlockEntity desk(GameTestHelper helper) {
        helper.setBlock(DESK, AllNuclearBlocks.REACTOR_CONTROL_PANEL.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(DESK);
    }

    /** Fits a controller in slot 0 set to {@code function} at {@code setting}, linked to the test reactor. */
    private static ControlModule controller(GameTestHelper helper, ControlDeskBlockEntity desk, ModuleFunction function, int setting) {
        desk.insert(0, ModuleType.CONTROLLER);
        CompoundTag config = new ControlModule(ModuleType.CONTROLLER).withFunction(function).withSetting(setting)
                .writeConfig(helper.getLevel().registryAccess());
        desk.configure(0, config);
        desk.setTarget(GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(FissionTestBuilder.CONTROLLER)));
        return desk.module(0);
    }

    private static boolean wholeSegments(ReactorBlockEntity reactor, float target) {
        int height = reactor.getStructure().map(structure -> structure.innerHeight()).orElse(0);
        float segments = target * height;
        return height > 0 && Math.abs(segments - Math.round(segments)) < 1.0e-4f;
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 400)
    public static void autoPowerWithdrawsRodsFromAColdCore(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        ControlDeskBlockEntity desk = desk(helper);
        helper.runAfterDelay(5, () -> {
            ControlModule module = controller(helper, desk, ModuleFunction.AUTO_POWER, 50);
            helper.assertTrue(module != null && module.isController() && module.engaged(), "a controller in automatic");
            helper.assertTrue(Math.abs(module.setpoint() - 50) < 1e-3, "holding 50% of the rated power, got " + module.setpoint());
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            float target = reactor.getRodTarget();
            helper.assertTrue(target >= 0 && target < 1.0f, "a cold core below half its rated power gets its rods out, target " + target);
            helper.assertTrue(wholeSegments(reactor, target), "in whole rod segments, got " + target);
            helper.assertTrue(ReactorRedstonePortBlockEntity.signalFor(RedstonePortMode.ROD_WITHDRAW, reactor) == 15,
                    "so the Withdraw rods ports drive the pistons");
            ControlModule module = desk.module(0);
            helper.assertTrue(module != null && Math.abs(module.demand() - target) < 1e-4, "the module shows its demand");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void autoTemperatureInsertsRodsIntoAHotCore(GameTestHelper helper) {
        FissionTestBuilder.build(helper, false);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        ControlDeskBlockEntity desk = desk(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            ControlModule module = controller(helper, desk, ModuleFunction.AUTO_TEMPERATURE, 0);
            reactor.setRodTarget(0.0f);
            reactor.getPhysicsSimulator().applyExternalHeat(2000);
            LinkedTarget.Resolved target = desk.consoleTarget();
            MonitorReading sample = MonitorReading.sample(target);
            helper.assertTrue(sample.value(MonitorReading.R_TEMPERATURE) > 1000, "a hot core, got " + sample.value(MonitorReading.R_TEMPERATURE));
            module.control(target, sample, 1.0, QUICK);
            helper.assertTrue(reactor.getRodTarget() == 0.0f, "the first step takes over without a bump");
            for (int i = 0; i < 3; i++) {
                module.control(target, sample, 1.0, QUICK);
            }
            float rods = reactor.getRodTarget();
            helper.assertTrue(rods > 0.0f, "too hot: the controller asks for the rods in, got " + rods);
            helper.assertTrue(wholeSegments(reactor, rods), "in whole rod segments, got " + rods);
            helper.assertTrue(ReactorRedstonePortBlockEntity.signalFor(RedstonePortMode.ROD_INSERT, reactor) == 15,
                    "the empty channel is below the target: Insert rods ports light up");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void standbyAndScramLetGoOfTheRods(GameTestHelper helper) {
        FissionTestBuilder.build(helper, false);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        ControlDeskBlockEntity desk = desk(helper);
        helper.runAfterDelay(5, () -> {
            ControlModule module = controller(helper, desk, ModuleFunction.AUTO_TEMPERATURE, 0);
            reactor.setRodTarget(0.0f);
            reactor.getPhysicsSimulator().applyExternalHeat(2000);
            LinkedTarget.Resolved target = desk.consoleTarget();
            MonitorReading sample = MonitorReading.sample(target);

            module.withEngaged(false);
            for (int i = 0; i < 4; i++) {
                module.control(target, sample, 1.0, QUICK);
            }
            helper.assertTrue(reactor.getRodTarget() == 0.0f, "in standby the rods are left alone");
            helper.assertTrue(module.demand() < 0, "and the module asks for nothing");

            module.withEngaged(true);
            reactor.setManualScram(true);
            for (int i = 0; i < 4; i++) {
                module.control(target, sample, 1.0, QUICK);
            }
            helper.assertTrue(reactor.getRodTarget() == 0.0f, "a SCRAM overrides the controller, which lets go");
            reactor.resetScram();
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void theLoopSurvivesSavingAndPressesStepTheSetpoint(GameTestHelper helper) {
        FissionTestBuilder.build(helper, false);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        ControlDeskBlockEntity desk = desk(helper);
        helper.runAfterDelay(5, () -> {
            ControlModule module = controller(helper, desk, ModuleFunction.AUTO_TEMPERATURE, 0);
            reactor.setRodTarget(0.0f);
            reactor.getPhysicsSimulator().applyExternalHeat(2000);
            LinkedTarget.Resolved target = desk.consoleTarget();
            MonitorReading sample = MonitorReading.sample(target);
            for (int i = 0; i < 3; i++) {
                module.control(target, sample, 1.0, QUICK);
            }
            var registries = helper.getLevel().registryAccess();
            ControlModule copy = ControlModule.read(module.write(registries), registries);
            helper.assertTrue(copy != null && copy.isController() && Math.abs(copy.demand() - module.demand()) < 1e-6,
                    "the demand is saved");
            reactor.setRodTarget(module.demand());
            module.control(target, sample, 1.0, QUICK);
            copy.control(target, sample, 1.0, QUICK);
            helper.assertTrue(Math.abs(copy.demand() - module.demand()) < 1e-6, "and the loop carries on exactly where it was");

            CompoundTag old = module.writeConfig(registries);
            old.remove("Engaged");
            ControlModule legacy = ControlModule.read(old, registries);
            helper.assertTrue(legacy != null && legacy.engaged(), "a module saved before standby existed is in automatic");

            int before = module.setting();
            desk.press(0, false);
            helper.assertTrue(desk.module(0).setting() == before + ControlModule.SETPOINT_STEP, "a click raises the setpoint");
            desk.press(0, true);
            helper.assertTrue(desk.module(0).setting() == before, "a sneak-click lowers it");
            helper.succeed();
        });
    }
}
