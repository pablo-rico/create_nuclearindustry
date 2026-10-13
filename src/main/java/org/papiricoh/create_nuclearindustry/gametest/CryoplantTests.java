package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.enrichment.blockentity.CentrifugeBlockEntity;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.cryo.CryoConfig;
import org.papiricoh.create_nuclearindustry.fusion.cryo.CryoRegistry;
import org.papiricoh.create_nuclearindustry.fusion.cryo.plant.CryoplantCompressorBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.cryo.plant.CryoplantControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.cryo.plant.CryoplantPortMode;
import org.papiricoh.create_nuclearindustry.fusion.cryo.tokamak.FusionCryoPortBlock;
import org.papiricoh.create_nuclearindustry.fusion.cryo.tokamak.FusionVacuumPumpBlock;
import org.papiricoh.create_nuclearindustry.fusion.cryo.tokamak.FusionVacuumPumpBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakGeometry;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakShellRole;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakStructure;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;

/**
 * The cryogenic plant (forming, liquefying helium and air, nitrogen pre-cooling), the liquid air
 * centrifuge recipe, and the tokamak's cryogenic ports and vacuum pumps.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class CryoplantTests {
    private static final BlockPos PLANT = new BlockPos(6, 1, 6);
    private static final int RADIUS = 4;

    // ------------------------------------------------------------------ forming

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void formsThreeHigh(GameTestHelper helper) {
        CryoplantTestBuilder.build(helper, PLANT, 3, 2, 128);
        CryoplantControllerBlockEntity plant = helper.getBlockEntity(CryoplantTestBuilder.controller(PLANT));
        helper.succeedWhen(() -> {
            helper.assertTrue(plant.getStructure().isPresent(), "a 3x3x3 plant should form: " + plant.getValidationMessage());
            helper.assertTrue(plant.getStructure().get().height == 3, "it is 3 layers high");
            helper.assertTrue(plant.getStructure().get().compressorPositions().size() == 2, "it counts both compressors");
            helper.assertTrue(!plant.isAirMode(), "with helium ports it liquefies helium");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void formsFiveHighAndSharesTheStress(GameTestHelper helper) {
        CryoplantTestBuilder.build(helper, PLANT, 5, 4, 128);
        CryoplantControllerBlockEntity plant = helper.getBlockEntity(CryoplantTestBuilder.controller(PLANT));
        helper.succeedWhen(() -> {
            helper.assertTrue(plant.getStructure().isPresent(), "a 3x3x5 plant should form: " + plant.getValidationMessage());
            helper.assertTrue(plant.getStructure().get().height == 5, "it is 5 layers high");
            CryoplantCompressorBlockEntity compressor = helper.getBlockEntity(CryoplantTestBuilder.compressor(PLANT, 0));
            float expected = (float) (CryoConfig.compressorStressPerLayer * 5 / 4);
            helper.assertTrue(Math.abs(compressor.calculateStressApplied() - expected) < 1,
                    "each of 4 compressors carries 5/4 of a layer, got " + compressor.calculateStressApplied());
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void rejectsAPlantTooShort(GameTestHelper helper) {
        CryoplantTestBuilder.build(helper, PLANT, 3, 2, 0);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.setBlock(PLANT.offset(dx, 2, dz), Blocks.AIR);
            }
        }
        expectUnformed(helper, "height");
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void rejectsAPlantTooTall(GameTestHelper helper) {
        CryoplantTestBuilder.build(helper, PLANT, 6, 2, 0);
        expectUnformed(helper, "height");
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void rejectsAHoleInTheCasing(GameTestHelper helper) {
        CryoplantTestBuilder.build(helper, PLANT, 3, 2, 0);
        helper.setBlock(PLANT.offset(1, 2, 1), Blocks.IRON_BLOCK);
        expectUnformed(helper, "casing");
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void rejectsAPortInTheColdBox(GameTestHelper helper) {
        CryoplantTestBuilder.build(helper, PLANT, 4, 2, 0);
        CryoplantTestBuilder.port(helper, PLANT.above(), CryoplantPortMode.NITROGEN_IN);
        expectUnformed(helper, "cold_box");
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void needsACompressorAndBothHeliumPorts(GameTestHelper helper) {
        CryoplantTestBuilder.build(helper, PLANT, 3, 0, 0);
        CryoplantControllerBlockEntity plant = helper.getBlockEntity(CryoplantTestBuilder.controller(PLANT));
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(plant.getStructure().isEmpty(), "without a compressor it must not form");
            helper.assertTrue(plant.getValidationMessage().contains("no_compressor"), "it says why: " + plant.getValidationMessage());
            helper.setBlock(CryoplantTestBuilder.compressor(PLANT, 0), CryoRegistry.CRYOPLANT_COMPRESSOR.get().defaultBlockState()
                    .setValue(org.papiricoh.create_nuclearindustry.fusion.cryo.plant.CryoplantCompressorBlock.FACING, Direction.SOUTH));
            helper.setBlock(CryoplantTestBuilder.liquidOut(PLANT), CryoRegistry.CRYOPLANT_CASING.get());
        });
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(plant.getStructure().isEmpty(), "without a liquid helium outlet it must not form");
            helper.assertTrue(plant.getValidationMessage().contains("missing_port"), "it says why: " + plant.getValidationMessage());
            CryoplantTestBuilder.port(helper, CryoplantTestBuilder.liquidOut(PLANT), CryoplantPortMode.LIQUID_HELIUM_OUT);
        });
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(plant.getStructure().isPresent(), "with both ports it forms: " + plant.getValidationMessage());
            helper.succeed();
        });
    }

    private static void expectUnformed(GameTestHelper helper, String error) {
        CryoplantControllerBlockEntity plant = helper.getBlockEntity(CryoplantTestBuilder.controller(PLANT));
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(plant.getStructure().isEmpty(), "the plant must not form");
            helper.assertTrue(plant.getValidationMessage().contains("cryoplant.error." + error),
                    "expected a " + error + " error, got " + plant.getValidationMessage());
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ liquefying

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void liquefiesWarmHeliumIntoANeighbouringTank(GameTestHelper helper) {
        CryoplantTestBuilder.build(helper, PLANT, 3, 2, 128);
        BlockPos tank = CryoplantTestBuilder.liquidOut(PLANT).east();
        helper.setBlock(tank, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "fluid_tank")));
        CryoplantControllerBlockEntity plant = helper.getBlockEntity(CryoplantTestBuilder.controller(PLANT));
        int[] filled = new int[1];
        helper.runAfterDelay(5, () -> filled[0] = fill(helper, CryoplantTestBuilder.heliumIn(PLANT),
                new FluidStack(AllNuclearFluids.HELIUM.get(), 4_000)));
        helper.runAfterDelay(125, () -> {
            helper.assertTrue(filled[0] == 4_000, "the inlet takes warm helium, took " + filled[0]);
            helper.assertTrue(plant.getSpeed() > 0.99, "two compressors at 128 RPM run at rated speed, got " + plant.getSpeed());
            double made = plant.getTotalHelium();
            helper.assertTrue(made > 400 && made <= 6.0 * 125, "a 3-high plant makes about 6 mB/t, made " + made);
            int warmLeft = plant.getWarmHeliumTank().getFluidAmount();
            double taken = 4_000 - warmLeft;
            helper.assertTrue(Math.abs(taken - made / 0.99) <= 2, "1% of the helium leaks: took " + taken + " for " + made);
            IFluidHandler outside = handler(helper, tank);
            helper.assertTrue(outside != null, "the Create tank has a fluid handler");
            FluidStack inTank = outside.getFluidInTank(0);
            helper.assertTrue(NuclearFluidHelper.isLiquidHelium(inTank) && inTank.getAmount() > 0,
                    "the outlet pushes liquid helium into the tank beside it, got " + inTank.getAmount());
            helper.assertTrue(helper.getBlockState(CryoplantTestBuilder.controller(PLANT))
                    .getValue(org.papiricoh.create_nuclearindustry.fusion.cryo.plant.CryoplantControllerBlock.RUNNING), "the lamp is lit");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 150)
    public static void slowCompressorsDoNothing(GameTestHelper helper) {
        CryoplantTestBuilder.build(helper, PLANT, 3, 2, 16);
        CryoplantControllerBlockEntity plant = helper.getBlockEntity(CryoplantTestBuilder.controller(PLANT));
        helper.runAfterDelay(5, () -> plant.getWarmHeliumTank().fill(new FluidStack(AllNuclearFluids.HELIUM.get(), 1_000),
                IFluidHandler.FluidAction.EXECUTE));
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(plant.getStructure().isPresent(), "the plant formed");
            helper.assertTrue(plant.getTotalHelium() == 0, "below 32 RPM nothing is liquefied, made " + plant.getTotalHelium());
            helper.assertTrue(plant.getWarmHeliumTank().getFluidAmount() == 1_000, "and no helium is used");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void nitrogenPrecoolingRaisesTheOutput(GameTestHelper helper) {
        CryoplantTestBuilder.build(helper, PLANT, 3, 2, 128);
        CryoplantTestBuilder.port(helper, CryoplantTestBuilder.spareWall(PLANT), CryoplantPortMode.NITROGEN_IN);
        CryoplantControllerBlockEntity plant = helper.getBlockEntity(CryoplantTestBuilder.controller(PLANT));
        double[] before = new double[2];
        helper.runAfterDelay(5, () -> {
            plant.getWarmHeliumTank().fill(new FluidStack(AllNuclearFluids.HELIUM.get(), 8_000), IFluidHandler.FluidAction.EXECUTE);
            int nitrogen = fill(helper, CryoplantTestBuilder.spareWall(PLANT), new FluidStack(AllNuclearFluids.LIQUID_NITROGEN.get(), 4_000));
            helper.assertTrue(nitrogen == 4_000, "the nitrogen port takes liquid nitrogen, took " + nitrogen);
        });
        helper.runAfterDelay(40, () -> {
            before[0] = plant.getTotalHelium();
            before[1] = plant.getNitrogenTank().getFluidAmount();
        });
        helper.runAfterDelay(100, () -> {
            double rate = (plant.getTotalHelium() - before[0]) / 60.0;
            helper.assertTrue(rate > 8.5 && rate < 9.5, "pre-cooled, a 3-high plant makes 9 mB/t, made " + rate);
            double used = before[1] - plant.getNitrogenTank().getFluidAmount();
            helper.assertTrue(Math.abs(used - rate * 60) <= 2, "1 mB of nitrogen per mB of liquid helium, used " + used);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void airIntakeMakesLiquidAir(GameTestHelper helper) {
        CryoplantTestBuilder.build(helper, PLANT, 3, 2, 128);
        helper.setBlock(CryoplantTestBuilder.heliumIn(PLANT), CryoRegistry.CRYOPLANT_CASING.get());
        CryoplantTestBuilder.port(helper, CryoplantTestBuilder.liquidOut(PLANT), CryoplantPortMode.LIQUID_AIR_OUT);
        CryoplantControllerBlockEntity plant = helper.getBlockEntity(CryoplantTestBuilder.controller(PLANT));
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(plant.getStructure().isPresent(), "a Liquid Air port is enough to form: " + plant.getValidationMessage());
            helper.assertTrue(plant.isAirMode(), "it liquefies air");
            helper.assertTrue(plant.getTotalAir() > 900, "a 3-high plant makes about 15 mB/t of liquid air, made " + plant.getTotalAir());
            helper.assertTrue(plant.getTotalHelium() == 0, "and no helium");
            IFluidHandler port = handler(helper, CryoplantTestBuilder.liquidOut(PLANT));
            helper.assertTrue(port != null && NuclearFluidHelper.isLiquidAir(port.drain(100, IFluidHandler.FluidAction.SIMULATE)),
                    "liquid air comes out of the port");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 400)
    public static void centrifugingLiquidAirGivesNitrogenAndHelium(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 1, 2);
        KineticTestHelper.placePoweredMachine(helper, at, AllNuclearBlocks.CENTRIFUGE.get(), 128);
        CentrifugeBlockEntity centrifuge = helper.getBlockEntity(at);
        helper.runAfterDelay(2, () -> {
            int filled = centrifuge.getFluidHandler(Direction.UP).fill(new FluidStack(AllNuclearFluids.LIQUID_AIR.get(), 1_000),
                    IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(filled == 1_000, "the centrifuge takes liquid air, took " + filled);
        });
        helper.succeedWhen(() -> {
            FluidStack product = centrifuge.getProductFluid();
            FluidStack tails = centrifuge.getTailsFluid();
            helper.assertTrue(NuclearFluidHelper.isLiquidNitrogen(product) && product.getAmount() == 990,
                    "expected 990 mB of liquid nitrogen, got " + product.getAmount());
            helper.assertTrue(NuclearFluidHelper.isHelium(tails) && tails.getAmount() == 10,
                    "expected 10 mB of helium, got " + tails.getAmount());
        });
    }

    // ------------------------------------------------------------------ tokamak parts

    /** North equator of the test tokamak, away from the parts the shared builder places. */
    private static BlockPos northShell(int dy) {
        return TokamakTestBuilder.CENTER.offset(0, dy, -TokamakGeometry.controllerToAxis(RADIUS));
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA, timeoutTicks = 200)
    public static void cryoPortsReachTheTokamaksHeliumTanks(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, RADIUS, true, 0);
        BlockPos in = northShell(1);
        BlockPos out = northShell(-1);
        helper.setBlock(in, CryoRegistry.FUSION_CRYO_PORT.get().defaultBlockState().setValue(FusionCryoPortBlock.MODE, ReactorFluidPortMode.INPUT));
        helper.setBlock(out, CryoRegistry.FUSION_CRYO_PORT.get().defaultBlockState().setValue(FusionCryoPortBlock.MODE, ReactorFluidPortMode.OUTPUT));
        BlockPos tank = out.north();
        helper.setBlock(tank, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "fluid_tank")));
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, RADIUS);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(tokamak.getStructure().isPresent(), "the tokamak forms with cryogenic ports in its shell");
            TokamakStructure structure = tokamak.getStructure().get();
            helper.assertTrue(structure.parts(TokamakShellRole.CRYO_IN).contains(helper.absolutePos(in)), "the inlet counts as CRYO_IN");
            helper.assertTrue(structure.parts(TokamakShellRole.CRYO_OUT).contains(helper.absolutePos(out)), "the outlet counts as CRYO_OUT");

            var liquid = tokamak.cryoTanks().liquidHelium();
            liquid.setFluid(FluidStack.EMPTY);
            IFluidHandler inlet = handler(helper, in);
            helper.assertTrue(inlet != null, "the inlet has a fluid handler");
            helper.assertTrue(inlet.fill(new FluidStack(AllNuclearFluids.HELIUM.get(), 100), IFluidHandler.FluidAction.EXECUTE) == 0,
                    "the inlet refuses warm helium");
            int filled = inlet.fill(new FluidStack(AllNuclearFluids.LIQUID_HELIUM.get(), 1_000), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(filled == 1_000 && liquid.getFluidAmount() == 1_000,
                    "liquid helium goes into the controller's tank, filled " + filled + ", tank " + liquid.getFluidAmount());

            var warm = tokamak.cryoTanks().warmHelium();
            warm.setFluid(new FluidStack(AllNuclearFluids.HELIUM.get(), 500));
            IFluidHandler outlet = handler(helper, out);
            helper.assertTrue(outlet != null, "the outlet has a fluid handler");
            FluidStack drained = outlet.drain(200, IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(NuclearFluidHelper.isHelium(drained) && drained.getAmount() == 200,
                    "warm helium comes out of the outlet, got " + drained.getAmount());
            helper.assertTrue(warm.getFluidAmount() == 300, "it came from the controller's warm tank, left " + warm.getFluidAmount());
        });
        helper.runAfterDelay(60, () -> {
            IFluidHandler outside = handler(helper, tank);
            helper.assertTrue(outside != null, "the Create tank has a fluid handler");
            FluidStack pushed = outside.getFluidInTank(0);
            helper.assertTrue(NuclearFluidHelper.isHelium(pushed) && pushed.getAmount() > 0,
                    "the outlet pushes warm helium into the tank beside it, got " + pushed.getAmount());
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA, timeoutTicks = 200)
    public static void vacuumPumpOffersPumpingSpeedWhileTurning(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, RADIUS, true, 0);
        BlockPos pumpPos = northShell(0);
        helper.setBlock(pumpPos, CryoRegistry.FUSION_VACUUM_PUMP.get().defaultBlockState()
                .setValue(FusionVacuumPumpBlock.FACING, Direction.NORTH));
        CreativeMotorBlockEntity motor = CryoplantTestBuilder.motor(helper, pumpPos.north(), Direction.SOUTH, 128);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, RADIUS);
        FusionVacuumPumpBlockEntity pump = helper.getBlockEntity(pumpPos);
        double[] running = new double[2];
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(tokamak.getStructure().isPresent(), "the tokamak forms with a vacuum pump in its shell");
            helper.assertTrue(tokamak.getStructure().get().parts(TokamakShellRole.VACUUM_PUMP).contains(helper.absolutePos(pumpPos)),
                    "the pump counts as VACUUM_PUMP");
            helper.assertTrue(Math.abs(pump.pumpSpeed() - 1.0) < 1e-6, "at 128 RPM it is one full pump, got " + pump.pumpSpeed());
            running[0] = tokamak.getSupply().pumpSpeed();
            running[1] = tokamak.getSupply().pumpCount();
            motor.generatedSpeed.setValue(0);
        });
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(pump.pumpSpeed() == 0, "a stopped pump does not pump");
            double stopped = tokamak.getSupply().pumpSpeed();
            helper.assertTrue(Math.abs(running[0] - stopped - 1.0) < 1e-6,
                    "the controller's pumping speed drops by one pump, " + running[0] + " -> " + stopped);
            helper.assertTrue(running[1] - tokamak.getSupply().pumpCount() == 1, "and it counts one pump fewer");
            helper.succeed();
        });
    }

    private static int fill(GameTestHelper helper, BlockPos pos, FluidStack stack) {
        IFluidHandler handler = handler(helper, pos);
        return handler == null ? -1 : handler.fill(stack, IFluidHandler.FluidAction.EXECUTE);
    }

    private static IFluidHandler handler(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), Direction.UP);
    }
}
