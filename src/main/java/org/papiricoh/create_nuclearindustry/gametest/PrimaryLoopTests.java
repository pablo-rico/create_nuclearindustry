package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PlantReadings;
import org.papiricoh.create_nuclearindustry.reactor.pwr.steamgen.SteamGeneratorControllerBlockEntity;

/**
 * A whole primary loop moved by real Create pumps and pipes: the hot leg pump takes hot primary from
 * the reactor to a steam generator, the cold leg pump brings it back cooled. Stopping a pump stops
 * the flow, and the core's heat stays in the vessel.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class PrimaryLoopTests {
    private static final BlockPos REACTOR = new BlockPos(3, 1, 3);
    private static final BlockPos STEAM_GENERATOR = new BlockPos(13, 1, 5);
    private static final BlockPos HOT_PUMP = new BlockPos(10, 3, 5);
    private static final BlockPos HOT_MOTOR = HOT_PUMP.above().west();
    private static final BlockPos COLD_PUMP = new BlockPos(8, 2, 8);
    private static final BlockPos COLD_MOTOR = COLD_PUMP.above().east();
    private static final int RPM = 256;

    /**
     * Reactor at x 3-7, steam generator at x 12-14. Hot leg along z=5 at y=3 from the reactor's output
     * port east into the generator's primary inlet; cold leg from its outlet on the south wall, along
     * z=8 at y=2 and back up into the reactor's coolant port on the west wall.
     */
    private static void buildPlant(GameTestHelper helper) {
        FissionTestBuilder.build(helper, REACTOR, true);
        FissionTestBuilder.pressurizer(helper, REACTOR, 4, 2);
        SteamGeneratorTestBuilder.build(helper, STEAM_GENERATOR, 4);

        pump(helper, HOT_PUMP, Direction.EAST, HOT_MOTOR, Direction.EAST);
        pump(helper, COLD_PUMP, Direction.WEST, COLD_MOTOR, Direction.WEST);

        // hot leg
        pipe(helper, new BlockPos(8, 3, 5), Direction.WEST, Direction.EAST);
        pipe(helper, new BlockPos(9, 3, 5), Direction.WEST, Direction.EAST);
        pipe(helper, new BlockPos(11, 3, 5), Direction.WEST, Direction.EAST);
        // cold leg
        pipe(helper, new BlockPos(13, 2, 7), Direction.NORTH, Direction.SOUTH);
        pipe(helper, new BlockPos(13, 2, 8), Direction.NORTH, Direction.WEST);
        for (int x = 12; x >= 9; x--) {
            pipe(helper, new BlockPos(x, 2, 8), Direction.EAST, Direction.WEST);
        }
        for (int x = 7; x >= 3; x--) {
            pipe(helper, new BlockPos(x, 2, 8), Direction.EAST, Direction.WEST);
        }
        pipe(helper, new BlockPos(2, 2, 8), Direction.EAST, Direction.NORTH);
        pipe(helper, new BlockPos(2, 2, 7), Direction.SOUTH, Direction.NORTH);
        pipe(helper, new BlockPos(2, 2, 6), Direction.SOUTH, Direction.NORTH);
        pipe(helper, new BlockPos(2, 2, 5), Direction.SOUTH, Direction.UP);
        pipe(helper, new BlockPos(2, 3, 5), Direction.DOWN, Direction.EAST);
    }

    private static Block create(String id) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", id));
    }

    private static void pipe(GameTestHelper helper, BlockPos pos, Direction... open) {
        BlockState state = create("fluid_pipe").defaultBlockState();
        for (Direction direction : Direction.values()) {
            state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), false);
        }
        for (Direction direction : open) {
            state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), true);
        }
        helper.setBlock(pos, state);
    }

    /** A pump pushing towards {@code facing}, turned through a cogwheel above it by a creative motor beside the cog. */
    private static void pump(GameTestHelper helper, BlockPos pos, Direction facing, BlockPos motor, Direction motorFacing) {
        helper.setBlock(pos, create("mechanical_pump").defaultBlockState().setValue(BlockStateProperties.FACING, facing));
        helper.setBlock(pos.above(), create("cogwheel").defaultBlockState().setValue(BlockStateProperties.AXIS, facing.getAxis()));
        helper.setBlock(motor, create("creative_motor").defaultBlockState().setValue(BlockStateProperties.FACING, motorFacing));
        CreativeMotorBlockEntity motorEntity = helper.getBlockEntity(motor);
        motorEntity.generatedSpeed.setValue(RPM);
    }

    @GameTest(template = NuclearGameTests.PLANT_ARENA, timeoutTicks = 700)
    public static void pumpsCarryTheCoreHeatToTheSteamGenerator(GameTestHelper helper) {
        buildPlant(helper);
        ReactorBlockEntity reactor = helper.getBlockEntity(FissionTestBuilder.controllerAt(REACTOR));
        SteamGeneratorControllerBlockEntity generator = helper.getBlockEntity(SteamGeneratorTestBuilder.controller(STEAM_GENERATOR));
        float[] before = new float[2];
        helper.runAfterDelay(10, () -> {
            int filled = reactor.fillCoolant(new FluidStack(AllNuclearFluids.PRIMARY_COOLANT.get(), 14_000), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(filled == 14_000, "the reactor takes the primary coolant, took " + filled);
            generator.getFeedTank().fill(new FluidStack(Fluids.WATER, 16_000), IFluidHandler.FluidAction.EXECUTE);
            PwrModeTests.heat(reactor, 5.0);
        });
        // A turbine would take the steam; here it is simply drained.
        for (int t = 11; t < 650; t++) {
            helper.runAfterDelay(t, () -> generator.getSteamTank().drain(1_000, IFluidHandler.FluidAction.EXECUTE));
        }
        helper.runAfterDelay(350, () -> {
            PlantReadings plant = reactor.plantReadings();
            helper.assertTrue(plant.pwr(), "the reactor runs pressurized");
            helper.assertTrue(reactor.getPrimaryLoop().flowOut() > 20, "the hot leg pump draws hot primary, "
                    + reactor.getPrimaryLoop().flowOut() + " mB/t");
            helper.assertTrue(reactor.getPrimaryLoop().flowIn() > 20, "the cold leg pump returns it, "
                    + reactor.getPrimaryLoop().flowIn() + " mB/t");
            helper.assertTrue(generator.getTotalCooled() > 1_000, "the steam generator cools it, " + generator.getTotalCooled() + " mB");
            helper.assertTrue(generator.getTotalSteam() > 500, "and boils feedwater, " + generator.getTotalSteam() + " mB of steam");
            helper.assertTrue(plant.subcooling() > 5, "the flowing loop stays subcooled, margin " + plant.subcooling());
            before[0] = plant.hotLeg();
            before[1] = plant.pressure();
            // Stop the hot leg pump.
            helper.setBlock(HOT_MOTOR, Blocks.AIR);
        });
        helper.runAfterDelay(550, () -> {
            PlantReadings plant = reactor.plantReadings();
            helper.assertTrue(reactor.getPrimaryLoop().flowOut() < 1, "no flow leaves the stopped hot leg, "
                    + reactor.getPrimaryLoop().flowOut());
            helper.assertTrue(plant.hotLeg() > before[0] + 10, "the core outlet heats up without flow: "
                    + before[0] + " -> " + plant.hotLeg() + " °C");
            helper.assertTrue(plant.pressure() > before[1] + 1, "and the pressure rises: " + before[1] + " -> " + plant.pressure() + " bar");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.PLANT_ARENA, timeoutTicks = 700)
    public static void aBrokenHotLegDrainsTheVessel(GameTestHelper helper) {
        buildPlant(helper);
        ReactorBlockEntity reactor = helper.getBlockEntity(FissionTestBuilder.controllerAt(REACTOR));
        SteamGeneratorControllerBlockEntity generator = helper.getBlockEntity(SteamGeneratorTestBuilder.controller(STEAM_GENERATOR));
        double[] before = new double[2];
        helper.runAfterDelay(10, () -> {
            reactor.fillCoolant(new FluidStack(AllNuclearFluids.PRIMARY_COOLANT.get(), 14_000), IFluidHandler.FluidAction.EXECUTE);
            generator.getFeedTank().fill(new FluidStack(Fluids.WATER, 16_000), IFluidHandler.FluidAction.EXECUTE);
            PwrModeTests.heat(reactor, 5.0);
        });
        for (int t = 11; t < 650; t++) {
            helper.runAfterDelay(t, () -> generator.getSteamTank().drain(1_000, IFluidHandler.FluidAction.EXECUTE));
        }
        helper.runAfterDelay(350, () -> {
            helper.assertTrue(reactor.plantReadings().vesselLevel() >= 1.0f, "the core starts covered");
            before[0] = reactor.getCoolantAmount() + reactor.getSteamAmount();
            before[1] = reactor.getCoreTemperature();
            // The pipe between the hot leg pump and the steam generator breaks: the pump blows the loop out.
            helper.setBlock(new BlockPos(11, 3, 5), Blocks.AIR);
        });
        helper.runAfterDelay(600, () -> {
            double inventory = reactor.getCoolantAmount() + reactor.getSteamAmount();
            helper.assertTrue(inventory < before[0] - 3_000, "the break drains the vessel: " + before[0] + " -> " + inventory + " mB");
            helper.assertTrue(reactor.plantReadings().vesselLevel() < 1.0f, "until the core uncovers, level "
                    + reactor.plantReadings().vesselLevel());
            helper.assertTrue(reactor.getCoreTemperature() > before[1] + 20, "and the core heats up: " + before[1] + " -> "
                    + reactor.getCoreTemperature() + " °C");
            helper.succeed();
        });
    }
}
