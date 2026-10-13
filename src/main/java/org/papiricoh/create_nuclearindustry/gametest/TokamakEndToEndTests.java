package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlDeskBlock;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlDeskBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlModule;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleFunction;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleType;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakReadings;
import org.papiricoh.create_nuclearindustry.fusion.api.TripCause;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.cryo.CryoRegistry;
import org.papiricoh.create_nuclearindustry.fusion.cryo.tokamak.FusionCryoPortBlock;
import org.papiricoh.create_nuclearindustry.fusion.cryo.tokamak.FusionVacuumPumpBlock;
import org.papiricoh.create_nuclearindustry.fusion.heating.FusionViewportBlock;
import org.papiricoh.create_nuclearindustry.fusion.heating.PlasmaHeatingRegistry;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakGeometry;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;

/**
 * A tokamak started with the real parts of every tokamak area: a vacuum pump and a pair of
 * cryogenic ports in the shell (fed through the inlet's own fluid handler, the warm helium pushed
 * into a Create tank), radio-frequency antennas for heating and a viewport that lights up. Kept in
 * its own batch: a burning tokamak should not share the arena's timing with other tests.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class TokamakEndToEndTests {
    private static final int RADIUS = 4;
    private static final String BATCH = "tokamak_end_to_end";
    private static final BlockPos DESK = new BlockPos(1, 1, 1);

    /** A shell cell on the outer equator (dy -1..1) of the given side, offset sideways. */
    private static BlockPos outerShell(Direction side, int sideways, int dy) {
        int axis = TokamakGeometry.controllerToAxis(RADIUS);
        Direction across = side.getClockWise();
        return TokamakTestBuilder.CENTER.offset(side.getStepX() * axis + across.getStepX() * sideways, dy,
                side.getStepZ() * axis + across.getStepZ() * sideways);
    }

    private static CreativeMotorBlockEntity motor(GameTestHelper helper, BlockPos pos, Direction facing, int rpm) {
        Block motor = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "creative_motor"));
        helper.setBlock(pos, motor.defaultBlockState().setValue(BlockStateProperties.FACING, facing));
        CreativeMotorBlockEntity entity = helper.getBlockEntity(pos);
        entity.generatedSpeed.setValue(rpm);
        return entity;
    }

    /** An antenna on the outer wall of {@code side}, its shaft out through the wall to its own motor. */
    private static void antenna(GameTestHelper helper, Direction side, int sideways) {
        BlockPos pos = outerShell(side, sideways, 0);
        helper.setBlock(pos, PlasmaHeatingRegistry.RF_ANTENNA.get().defaultBlockState().setValue(DirectionalKineticBlock.FACING, side));
        motor(helper, pos.relative(side), side.getOpposite(), 64);
    }

    private static IFluidHandler handler(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), null);
    }

    /**
     * Builds the tokamak with its real auxiliaries. The cryogenic inlet is kept topped up with
     * liquid helium through its own handler, and the coolant tank full: a burning R=4 boils its
     * water in seconds and there is no turbine here.
     */
    private static FusionReactorBlockEntity buildWithRealParts(GameTestHelper helper, BlockPos viewport) {
        TokamakTestBuilder.build(helper, RADIUS, true, 256);

        BlockPos pump = outerShell(Direction.NORTH, 0, 0);
        helper.setBlock(pump, CryoRegistry.FUSION_VACUUM_PUMP.get().defaultBlockState().setValue(FusionVacuumPumpBlock.FACING, Direction.NORTH));
        motor(helper, pump.north(), Direction.SOUTH, 128);

        BlockPos inlet = outerShell(Direction.NORTH, 0, 1);
        BlockPos outlet = outerShell(Direction.NORTH, 0, -1);
        helper.setBlock(inlet, CryoRegistry.FUSION_CRYO_PORT.get().defaultBlockState().setValue(FusionCryoPortBlock.MODE, ReactorFluidPortMode.INPUT));
        helper.setBlock(outlet, CryoRegistry.FUSION_CRYO_PORT.get().defaultBlockState().setValue(FusionCryoPortBlock.MODE, ReactorFluidPortMode.OUTPUT));
        helper.setBlock(outlet.north(), BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "fluid_tank")));

        antenna(helper, Direction.WEST, 2);
        antenna(helper, Direction.WEST, -2);
        antenna(helper, Direction.SOUTH, 2);
        antenna(helper, Direction.SOUTH, -2);

        helper.setBlock(viewport, PlasmaHeatingRegistry.VIEWPORT.get());

        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, RADIUS);
        TokamakTestBuilder.fuel(tokamak, 8_000, 8_000, 16_000);
        helper.onEachTick(() -> {
            IFluidHandler cryo = handler(helper, inlet);
            if (cryo != null) {
                cryo.fill(new FluidStack(AllNuclearFluids.LIQUID_HELIUM.get(), 160), IFluidHandler.FluidAction.EXECUTE);
            }
            tokamak.getCoolantTank().fill(new FluidStack(Fluids.WATER, tokamak.getCoolantTank().getSpace()), IFluidHandler.FluidAction.EXECUTE);
            tokamak.getPlasmaSteamTank().setFluid(FluidStack.EMPTY);
        });
        return tokamak;
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA, batch = BATCH, timeoutTicks = 3600)
    public static void realPartsTakeATokamakFromColdToBurn(GameTestHelper helper) {
        BlockPos viewport = TokamakTestBuilder.CENTER.offset(0, TokamakGeometry.HALF_HEIGHT, -TokamakGeometry.controllerToAxis(RADIUS) + 1);
        FusionReactorBlockEntity tokamak = buildWithRealParts(helper, viewport);
        TripCause[] start = {null};
        boolean[] cooled = {false};
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(tokamak.isFormed(), "the tokamak forms with pumps, cryogenic ports, antennas and a viewport in its shell: "
                    + tokamak.getValidationMessage());
            start[0] = tokamak.requestStart();
            helper.assertTrue(start[0] == TripCause.NONE, "the real parts are enough to start, got " + start[0]);
        });
        helper.onEachTick(() -> {
            if (tokamak.readings().magnetTemperature() < 20) {
                cooled[0] = true;
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(start[0] == TripCause.NONE, "started");
            TokamakReadings readings = tokamak.readings();
            helper.assertTrue(tokamak.getSupply().pumpCount() == 1, "the pump runs, got " + tokamak.getSupply().pumpCount());
            helper.assertTrue(tokamak.getSupply().rfCount() == 4, "all four antennas run, got " + tokamak.getSupply().rfCount());
            helper.assertTrue(cooled[0], "the liquid helium from the cryogenic port cooled the coils");
            helper.assertTrue(readings.phase() == TokamakPhase.BURN && readings.fusionPower() > 0,
                    "reaches a burn, now " + readings.phase() + " at " + Math.round(readings.temperature()) + "° (trip " + readings.tripCause() + ")");
            helper.assertTrue(readings.vacuum() <= -3.0, "under plasma vacuum, got " + readings.vacuum());
            BlockState window = helper.getBlockState(viewport);
            helper.assertTrue(window.hasProperty(FusionViewportBlock.GLOW) && window.getValue(FusionViewportBlock.GLOW) > 0,
                    "the viewport lights up while the plasma burns");
            IFluidHandler tank = handler(helper, outerShell(Direction.NORTH, 0, -1).north());
            helper.assertTrue(tank != null && NuclearFluidHelper.isHelium(tank.getFluidInTank(0)) && tank.getFluidInTank(0).getAmount() > 0,
                    "the boiled-off helium comes back out through the outlet port");
            TokamakTestBuilder.leaveSafe(tokamak);
        });
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA, batch = BATCH, timeoutTicks = 3600)
    public static void autoFusionPowerTakesOverABurningTokamak(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, RADIUS, true, 256);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, RADIUS);
        TokamakTestBuilder.fuel(tokamak, 8_000, 8_000, 16_000);
        TokamakTestBuilder.makeStartable(helper, tokamak, RADIUS, 40.0);
        BlockState deskState = AllNuclearBlocks.REACTOR_CONTROL_PANEL.get().defaultBlockState().setValue(ControlDeskBlock.FACING, Direction.NORTH);
        helper.setBlock(DESK, deskState);
        helper.getLevel().setBlock(helper.absolutePos(DESK),
                ControlDeskBlock.withConnections(helper.getBlockState(DESK), helper.getLevel(), helper.absolutePos(DESK)), 3);
        double[] fuelBefore = {-1};
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(tokamak.isFormed(), "the tokamak forms");
            helper.assertTrue(TokamakTestBuilder.primeAndStart(tokamak) == TripCause.NONE, "it starts");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(tokamak.phase() == TokamakPhase.BURN, "waiting for a burn, now " + tokamak.phase());
            if (!(helper.getLevel().getBlockEntity(helper.absolutePos(DESK)) instanceof ControlDeskBlockEntity desk)) {
                helper.fail("no control desk");
                return;
            }
            if (fuelBefore[0] < 0) {
                desk.setTarget(GlobalPos.of(helper.getLevel().dimension(), tokamak.getBlockPos()));
                desk.insert(0, ModuleType.CONTROLLER);
                ControlModule module = desk.module(0);
                helper.assertTrue(module != null, "a controller module fits the desk");
                desk.configure(0, module.withFunction(ModuleFunction.AUTO_FUSION_POWER).withSetting(10)
                        .writeConfig(helper.getLevel().registryAccess()));
                fuelBefore[0] = tokamak.readings().fuelRate();
            }
            ControlModule controller = desk.module(0);
            helper.assertTrue(controller != null && controller.active(), "the controller drives the burning tokamak");
            helper.assertTrue(tokamak.readings().fuelRate() < fuelBefore[0] - 1.0e-4,
                    "asked for far less power than it makes, it cuts the fuelling from " + fuelBefore[0]
                            + " (now " + tokamak.readings().fuelRate() + ", " + Math.round(tokamak.readings().fusionPower()) + " MW)");
            TokamakTestBuilder.leaveSafe(tokamak);
        });
    }
}
