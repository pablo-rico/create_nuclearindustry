package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.energy.EnergyRegistry;
import org.papiricoh.create_nuclearindustry.integration.sable.SableCompat;
import org.papiricoh.create_nuclearindustry.propulsion.PropulsionRegistry;
import org.papiricoh.create_nuclearindustry.propulsion.block.RocketNozzleBlock;
import org.papiricoh.create_nuclearindustry.propulsion.blockentity.AntimatterEngineBlockEntity;
import org.papiricoh.create_nuclearindustry.propulsion.blockentity.ThermalEngineBlockEntity;

/** The nuclear engines: on a test stand on the ground, and pushing a Create Aeronautics ship. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class PropulsionTests {
    private static final String BATCH = "propulsion";
    private static final BlockPos CONTROLLER = new BlockPos(6, 6, 6);

    /** A thermal engine from {@link #CONTROLLER} towards {@code exhaust}, with {@code segments} exchangers and a redstone block for full throttle. */
    static void thermalEngine(GameTestHelper helper, BlockPos controller, Direction exhaust, int segments) {
        helper.setBlock(controller, PropulsionRegistry.THERMAL_ENGINE.get().defaultBlockState().setValue(BlockStateProperties.FACING, exhaust));
        engineLine(helper, controller, exhaust, segments, PropulsionRegistry.THERMAL_EXCHANGER.get().defaultBlockState(),
                PropulsionRegistry.THERMAL_NOZZLE.get().defaultBlockState());
    }

    static void antimatterEngine(GameTestHelper helper, BlockPos controller, Direction exhaust, int coils) {
        helper.setBlock(controller, PropulsionRegistry.ANTIMATTER_ENGINE.get().defaultBlockState().setValue(BlockStateProperties.FACING, exhaust));
        engineLine(helper, controller, exhaust, coils, PropulsionRegistry.MAGNETIC_COIL.get().defaultBlockState(),
                PropulsionRegistry.MAGNETIC_NOZZLE.get().defaultBlockState());
    }

    private static void engineLine(GameTestHelper helper, BlockPos controller, Direction exhaust, int segments,
                                   net.minecraft.world.level.block.state.BlockState segment,
                                   net.minecraft.world.level.block.state.BlockState nozzle) {
        BlockPos at = controller;
        for (int i = 0; i < segments; i++) {
            at = at.relative(exhaust);
            helper.setBlock(at, segment.setValue(RotatedPillarBlock.AXIS, exhaust.getAxis()));
        }
        helper.setBlock(at.relative(exhaust), nozzle.setValue(BlockStateProperties.FACING, exhaust));
    }

    static void fuel(ThermalEngineBlockEntity engine, int hotPrimary, net.minecraft.world.level.material.Fluid propellant, int amount) {
        engine.fluids().fill(new FluidStack(AllNuclearFluids.HOT_PRIMARY_COOLANT.get(), hotPrimary), IFluidHandler.FluidAction.EXECUTE);
        engine.fluids().fill(new FluidStack(propellant, amount), IFluidHandler.FluidAction.EXECUTE);
    }

    /** The exhaust leaves activity where it lands: tests that fire go in their own contamination batch and clean up. */
    private static final String FIRING = "contamination_propulsion";

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = FIRING + "_stand", timeoutTicks = 100)
    public static void aThermalEngineFiresOnItsTestStand(GameTestHelper helper) {
        thermalEngine(helper, CONTROLLER, Direction.WEST, 2);
        ThermalEngineBlockEntity engine = AcceleratorTestBuilder.entity(helper, CONTROLLER, ThermalEngineBlockEntity.class);
        helper.assertTrue(engine != null, "no engine");
        fuel(engine, 4000, AllNuclearFluids.HYDROGEN.get(), 4000);
        helper.setBlock(CONTROLLER.above(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(engine.isFormed() && engine.segments() == 2, "the engine forms with 2 segments: " + engine.segments());
            helper.assertTrue(engine.thrust() > 0, "it fires at full throttle");
            helper.assertTrue(engine.propellantTank().getFluidAmount() < 4000, "burning its propellant");
            helper.assertTrue(engine.coldTank().getFluidAmount() > 0, "and handing the primary back cold");
            helper.assertTrue(helper.getBlockState(CONTROLLER.west(3)).getValue(RocketNozzleBlock.LIT), "with its nozzle lit");
            MonitorReading reading = engine.plantSample();
            helper.assertTrue(reading.kind() == TargetKind.THERMAL_ENGINE && reading.value(MonitorReading.E_THRUST) > 0,
                    "the control room reads its thrust");
            helper.setBlock(CONTROLLER.above(), Blocks.AIR);
        });
        helper.runAfterDelay(40, () -> {
            ContaminationTests.clean(helper, 3);
            helper.assertTrue(engine.thrust() == 0, "without a signal it stops");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 100)
    public static void aThermalEngineNeedsHotCoolant(GameTestHelper helper) {
        thermalEngine(helper, CONTROLLER, Direction.WEST, 2);
        ThermalEngineBlockEntity engine = AcceleratorTestBuilder.entity(helper, CONTROLLER, ThermalEngineBlockEntity.class);
        helper.assertTrue(engine != null, "no engine");
        fuel(engine, 0, AllNuclearFluids.HYDROGEN.get(), 4000);
        helper.setBlock(CONTROLLER.above(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(engine.isFormed(), "the engine forms");
            helper.assertTrue(engine.thrust() == 0 && engine.propellantTank().getFluidAmount() == 4000,
                    "without hot primary it has no heat and does not fire");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = FIRING + "_burst", timeoutTicks = 100)
    public static void aWornOutNozzleBlowsUp(GameTestHelper helper) {
        org.papiricoh.create_nuclearindustry.radiation.contamination.ContaminationData data =
                org.papiricoh.create_nuclearindustry.radiation.contamination.ContaminationData.get(helper.getLevel());
        int events = data.events().size();
        thermalEngine(helper, CONTROLLER, Direction.WEST, 2);
        ThermalEngineBlockEntity engine = AcceleratorTestBuilder.entity(helper, CONTROLLER, ThermalEngineBlockEntity.class);
        helper.assertTrue(engine != null, "no engine");
        fuel(engine, 4000, net.minecraft.world.level.material.Fluids.WATER, 4000);
        engine.wearForTest(0.9999, 2500);
        helper.setBlock(CONTROLLER.above(), Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> {
            helper.assertTrue(engine.failed() || engine.isRemoved(), "the overheated nozzle has not failed: wear " + engine.wear());
            // Its burst spreads fallout: forget it, so no chunk loaded later catches up on it.
            while (data.events().size() > events) {
                data.events().removeLast();
            }
            ContaminationTests.clean(helper, 3);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = FIRING + "_antimatter", timeoutTicks = 100)
    public static void anAntimatterEngineOutpullsTheThermalOne(GameTestHelper helper) {
        antimatterEngine(helper, CONTROLLER, Direction.WEST, 6);
        helper.setBlock(CONTROLLER.below(), EnergyRegistry.CREATIVE_RF_GENERATOR.get());
        AntimatterEngineBlockEntity engine = AcceleratorTestBuilder.entity(helper, CONTROLLER, AntimatterEngineBlockEntity.class);
        helper.assertTrue(engine != null, "no engine");
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(engine.powered(), "the generator raises the coils' field");
            helper.assertTrue(engine.insert(2, false) == 2, "the chamber takes 2 ng");
            helper.setBlock(CONTROLLER.above(), Blocks.REDSTONE_BLOCK);
        });
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(engine.thrust() > 10_000, "an antimatter engine pushes hard: " + engine.thrust());
            helper.assertTrue(engine.stored() < 2, "burning its antimatter");
            helper.setBlock(CONTROLLER.above(), Blocks.AIR);
            ContaminationTests.clean(helper, 3);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH + "_quench", timeoutTicks = 300)
    public static void aStarvedAntimatterEngineAnnihilates(GameTestHelper helper) {
        AntimatterTestBuilder.listen();
        antimatterEngine(helper, CONTROLLER, Direction.WEST, 2);
        helper.setBlock(CONTROLLER.below(), EnergyRegistry.CREATIVE_RF_GENERATOR.get());
        AntimatterEngineBlockEntity engine = AcceleratorTestBuilder.entity(helper, CONTROLLER, AntimatterEngineBlockEntity.class);
        helper.assertTrue(engine != null, "no engine");
        int before = AntimatterTestBuilder.blastsNear(helper, CONTROLLER, 16);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(engine.insert(0.5, false) > 0, "the chamber takes antimatter");
            // The supply goes: what is left in the buffer runs out and the field fails.
            helper.setBlock(CONTROLLER.below(), Blocks.AIR);
            engine.drainEnergyForTest();
        });
        helper.succeedWhen(() -> helper.assertTrue(AntimatterTestBuilder.blastsNear(helper, CONTROLLER, 16) > before,
                "the chamber's antimatter should annihilate after the quench"));
    }

    @GameTest(template = NuclearGameTests.BLAST_ARENA, batch = FIRING + "_ship", timeoutTicks = 200)
    public static void aThermalEngineLiftsItsShip(GameTestHelper helper) {
        if (!SableCompat.isActive()) {
            helper.succeed();
            return;
        }
        BlockPos controller = new BlockPos(20, 6, 20);
        thermalEngine(helper, controller, Direction.DOWN, 2);
        helper.setBlock(controller.above(), Blocks.REDSTONE_BLOCK);
        ThermalEngineBlockEntity engine = AcceleratorTestBuilder.entity(helper, controller, ThermalEngineBlockEntity.class);
        helper.assertTrue(engine != null, "no engine");
        fuel(engine, 8000, AllNuclearFluids.HYDROGEN.get(), 8000);
        helper.runAfterDelay(5, () -> {
            AeronauticsShips.Spawned ship = AeronauticsShips.assemble(helper, controller.below(3), controller.above());
            helper.assertTrue(ship != null, "no ship assembled");
            // Soon after: at full thrust it is soon high above the arena.
            helper.runAfterDelay(4, () -> {
                double vy = ship.velocity().y;
                double mass = ship.mass();
                ThermalEngineBlockEntity aboard = ship.find(ThermalEngineBlockEntity.class);
                double thrust = aboard == null ? 0 : aboard.thrust();
                ship.remove();
                ContaminationTests.clean(helper, 4);
                helper.assertTrue(thrust > 0, "the engine aboard fires");
                helper.assertTrue(vy > 1, "pointing down, it lifts the ship: vertical speed " + vy + ", mass " + mass + ", thrust " + thrust);
                helper.succeed();
            });
        });
    }

    // ------------------------------------------------------------------ Ponder schematics

    /** The Ponder scenes' engines really form; templates sit one block above the test origin. */
    @GameTest(template = "gametest/nuclear_thermal_engine_ponder", batch = BATCH + "_ponder", timeoutTicks = 60)
    public static void theThermalEngineSceneForms(GameTestHelper helper) {
        helper.succeedWhen(() -> {
            ThermalEngineBlockEntity engine = AcceleratorTestBuilder.entity(helper, new BlockPos(1, 2, 2), ThermalEngineBlockEntity.class);
            helper.assertTrue(engine != null && engine.isFormed() && engine.segments() == 3,
                    "the scene's engine forms with three segments: " + (engine == null ? "none" : engine.segments()));
        });
    }

    @GameTest(template = "gametest/antimatter_engine_ponder", batch = BATCH + "_ponder", timeoutTicks = 60)
    public static void theAntimatterEngineSceneForms(GameTestHelper helper) {
        helper.succeedWhen(() -> {
            AntimatterEngineBlockEntity engine = AcceleratorTestBuilder.entity(helper, new BlockPos(1, 2, 2), AntimatterEngineBlockEntity.class);
            helper.assertTrue(engine != null && engine.isFormed() && engine.segments() == 4 && engine.powered(),
                    "the scene's engine forms with four coils and its field up");
        });
    }
}
