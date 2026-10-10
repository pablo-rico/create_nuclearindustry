package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlDeskBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlModule;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleFunction;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleType;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PlantReadings;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PrimaryLoop;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrConfig;

/**
 * The pressurizer run from a control desk: the backup heaters lever raises the pressure, the spray
 * lever lowers it while the cold leg flows, the automatic pressure controller settles it at its
 * setpoint, and a reactor without a pressurizer leaves all of them dark.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class PressurizerDeskTests {
    private static final BlockPos ORIGIN = FissionTestBuilder.ORIGIN;
    private static final BlockPos DESK = new BlockPos(8, 1, 8);

    /** A reactor with a four-block pressurizer (two automatic heater ports), full of cold primary coolant. */
    private static ReactorBlockEntity pressurizedReactor(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        FissionTestBuilder.pressurizer(helper, ORIGIN, 4, 2);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            int filled = reactor.fillCoolant(new FluidStack(AllNuclearFluids.PRIMARY_COOLANT.get(), 14_000), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(filled == 14_000, "the reactor takes the primary coolant, took " + filled);
            PwrModeTests.heat(reactor, 0.0);
        });
        return reactor;
    }

    /** A desk linked to the test reactor, with {@code type} modules set to {@code functions} from slot 0 on. */
    private static ControlDeskBlockEntity desk(GameTestHelper helper, ModuleType[] types, ModuleFunction[] functions) {
        helper.setBlock(DESK, AllNuclearBlocks.REACTOR_CONTROL_PANEL.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        ControlDeskBlockEntity desk = helper.getBlockEntity(DESK);
        for (int slot = 0; slot < types.length; slot++) {
            desk.insert(slot, types[slot]);
            CompoundTag config = new ControlModule(types[slot]).withFunction(functions[slot])
                    .writeConfig(helper.getLevel().registryAccess());
            desk.configure(slot, config);
        }
        desk.setTarget(GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(FissionTestBuilder.CONTROLLER)));
        return desk;
    }

    private static ControlDeskBlockEntity lever(GameTestHelper helper, ModuleFunction function) {
        return desk(helper, new ModuleType[]{ModuleType.LEVER}, new ModuleFunction[]{function});
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void theHeatersLeverRaisesThePressure(GameTestHelper helper) {
        ReactorBlockEntity reactor = pressurizedReactor(helper);
        ControlDeskBlockEntity[] desk = new ControlDeskBlockEntity[1];
        helper.runAfterDelay(10, () -> {
            desk[0] = lever(helper, ModuleFunction.PZR_HEATERS);
            helper.assertTrue(desk[0].module(0).active(), "the lever works on a reactor with a pressurizer");
            helper.assertTrue(desk[0].press(0, false), "a click throws it");
            helper.assertTrue(reactor.getPrimaryLoop().backupHeatersHeld(), "the desk holds the backup heaters on");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(desk[0] != null && desk[0].module(0).shown() > 0.5f, "the lever shows on");
            helper.assertTrue(reactor.getPrimaryLoop().heatersRunning(), "every heater runs");
            float pressure = reactor.plantReadings().pressure();
            helper.assertTrue(pressure > PwrConfig.pressureSetpoint + 8,
                    "past where the automatic heaters would stop, reading " + pressure);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void theSprayLeverLowersThePressure(GameTestHelper helper) {
        ReactorBlockEntity reactor = pressurizedReactor(helper);
        // The spray takes cold leg water: a little primary coolant pumped in every tick stands in for the loop.
        for (int t = 6; t < 300; t++) {
            helper.runAfterDelay(t, () -> reactor.fillCoolant(new FluidStack(AllNuclearFluids.PRIMARY_COOLANT.get(), 4),
                    IFluidHandler.FluidAction.EXECUTE));
        }
        ControlDeskBlockEntity[] desk = new ControlDeskBlockEntity[1];
        helper.runAfterDelay(30, () -> {
            helper.assertFalse(reactor.getPrimaryLoop().spraying(), "no spray at the setpoint on its own");
            desk[0] = lever(helper, ModuleFunction.PZR_SPRAY);
            helper.assertTrue(desk[0].press(0, false), "a click throws the spray lever");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(desk[0] != null && reactor.getPrimaryLoop().sprayHeld(), "the desk holds the spray open");
            helper.assertTrue(reactor.getPrimaryLoop().spraying(), "the spray runs with the cold leg flowing");
            float pressure = reactor.plantReadings().pressure();
            helper.assertTrue(pressure < PwrConfig.pressureSetpoint - 20,
                    "it condenses the steam bubble, even against the automatic heaters, reading " + pressure);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 1100)
    public static void automaticPressureSettlesAtItsSetpoint(GameTestHelper helper) {
        ReactorBlockEntity reactor = pressurizedReactor(helper);
        // 56 of 100 is 140 bar, well below the 155 the pressurizer holds by itself.
        int setting = 56;
        float setpoint = setting * ControlModule.PRESSURE_SCALE / 100.0f;
        ControlDeskBlockEntity[] desk = new ControlDeskBlockEntity[1];
        helper.runAfterDelay(10, () -> {
            desk[0] = desk(helper, new ModuleType[]{ModuleType.CONTROLLER}, new ModuleFunction[]{ModuleFunction.AUTO_PRESSURE});
            ControlModule module = desk[0].module(0);
            helper.assertTrue(module.isController() && module.engaged(), "a pressure controller in automatic");
            helper.assertTrue(Math.abs(module.setpoint() - PwrConfig.pressureSetpoint) < 1.5,
                    "it starts at the pressurizer's own setpoint, " + module.formatSetpoint());
            desk[0].configure(0, new ControlModule(ModuleType.CONTROLLER).withFunction(ModuleFunction.AUTO_PRESSURE)
                    .withSetting(setting).writeConfig(helper.getLevel().registryAccess()));
            helper.assertTrue(Math.abs(desk[0].module(0).setpoint() - 140) < 1e-3, "now set to 140 bar, " + desk[0].module(0).formatSetpoint());
        });
        for (int t = 900; t <= 1050; t += 10) {
            helper.runAfterDelay(t, () -> {
                PrimaryLoop loop = reactor.getPrimaryLoop();
                helper.assertTrue(loop.pressureControlled(), "the controller drives the heaters and spray");
                float pressure = reactor.plantReadings().pressure();
                helper.assertTrue(Math.abs(pressure - setpoint) < 3.0f, "the pressure holds near " + setpoint + " bar, reading " + pressure);
            });
        }
        helper.runAfterDelay(1060, () -> {
            ControlModule module = desk[0].module(0);
            helper.assertTrue(module.demand() >= 0 && module.demand() < 0.5f,
                    "holding it takes some heat against the pressurizer's losses, output " + module.demand());
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 100)
    public static void aReactorWithoutAPressurizerIgnoresThem(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(reactor.isFormed() && !reactor.hasPressurizer(), "a formed reactor without a pressurizer");
            ControlDeskBlockEntity desk = desk(helper,
                    new ModuleType[]{ModuleType.LEVER, ModuleType.LEVER, ModuleType.CONTROLLER, ModuleType.LAMP},
                    new ModuleFunction[]{ModuleFunction.PZR_HEATERS, ModuleFunction.PZR_SPRAY, ModuleFunction.AUTO_PRESSURE,
                            ModuleFunction.LAMP_RELIEF_OPEN});
            for (int slot = 0; slot < 4; slot++) {
                ControlModule module = desk.module(slot);
                helper.assertTrue(module != null && !module.active(), module.function() + " shows dark");
                helper.assertFalse(desk.press(slot, false), module.function() + " does nothing when clicked");
            }
        });
        helper.runAfterDelay(50, () -> {
            PrimaryLoop loop = reactor.getPrimaryLoop();
            helper.assertFalse(loop.backupHeatersHeld() || loop.sprayHeld() || loop.pressureControlled(), "the loop got no orders");
            helper.assertTrue(reactor.plantReadings() == PlantReadings.NONE, "and the reactor still boils its water directly");
            helper.succeed();
        });
    }
}
