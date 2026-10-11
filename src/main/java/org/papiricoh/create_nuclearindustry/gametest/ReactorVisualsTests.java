package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.radiation.RadiationTransmission;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.multiblock.ReactorStructureValidator;
import org.papiricoh.create_nuclearindustry.reactor.physics.FuelKind;
import org.papiricoh.create_nuclearindustry.reactor.physics.ReactorPhysicsSimulator;
import org.papiricoh.create_nuclearindustry.reactor.visual.ReactorVisualsRegistry;
import org.papiricoh.create_nuclearindustry.reactor.visual.RodVisualSync;
import org.papiricoh.create_nuclearindustry.reactor.visual.RodVisuals;

import java.util.List;

/**
 * The fuel channels show what the reactor holds: loaded assemblies in fill order, burnup, glow and
 * coolant; they go dark when the reactor unforms or its controller is removed, and rewriting them
 * never makes the reactor revalidate. A lead glass viewport can stand in the casing wall.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class ReactorVisualsTests {
    /** The test reactor's six channels in fill order: column (1, 1) bottom up, then column (3, 3). */
    private static final List<BlockPos> CHANNELS = List.of(
            rod(1, 1, 1), rod(1, 2, 1), rod(1, 3, 1), rod(3, 1, 3), rod(3, 2, 3), rod(3, 3, 3));

    private static BlockPos rod(int x, int y, int z) {
        return FissionTestBuilder.ORIGIN.offset(x, y, z);
    }

    private static RodVisuals.Look look(GameTestHelper helper, BlockPos channel) {
        BlockState state = helper.getBlockState(channel);
        helper.assertTrue(state.is(AllNuclearBlocks.URANIUM_ROD.get()), "no fuel channel at " + channel);
        return RodVisualSync.lookOf(state);
    }

    private static ReactorPhysicsSimulator.AssemblyView burnt(double fraction) {
        return new ReactorPhysicsSimulator.AssemblyView(FuelKind.URANIUM, fraction);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void channelsShowLoadedFuelAndCoolant(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            ItemStack rest = reactor.getFuelInventory().insertItem(0, new ItemStack(AllNuclearItems.URANIUM_REACTOR_FUEL.get(), 2), false);
            helper.assertTrue(rest.isEmpty(), "two assemblies fit in the input slot");
            // A full tank: the small test core runs and boils its water off, so give it plenty.
            int filled = reactor.fillCoolant(new FluidStack(Fluids.WATER, reactor.getTankCapacity()), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(filled > 0, "the reactor takes water");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(reactor.getPhysicsSimulator().getLoadedAssemblyCount() == 2, "both assemblies are loaded");
            for (int i = 0; i < CHANNELS.size(); i++) {
                RodVisuals.Look look = look(helper, CHANNELS.get(i));
                int expected = i < 2 ? RodVisuals.FRESH : RodVisuals.EMPTY;
                helper.assertTrue(look.fuel() == expected, "channel " + i + " should show fuel " + expected + ", shows " + look);
                helper.assertTrue(look.coolant(), "channel " + i + " should show coolant (" + reactor.getCoolantAmount() + " mB)");
            }
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void channelsDarkenWithBurnupAndGlowWithPower(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.succeedWhen(() -> {
            ReactorStructureValidator.ReactorStructure structure = reactor.getStructure().orElse(null);
            helper.assertTrue(structure != null, "reactor should form");
            reactor.getRodVisuals().applyNow(helper.getLevel(), structure, List.of(burnt(0.9), burnt(0.5), burnt(0.1)), 1.0, false);
            int[] expected = {RodVisuals.SPENT, RodVisuals.HALF, RodVisuals.FRESH, RodVisuals.EMPTY, RodVisuals.EMPTY, RodVisuals.EMPTY};
            for (int i = 0; i < CHANNELS.size(); i++) {
                RodVisuals.Look look = look(helper, CHANNELS.get(i));
                helper.assertTrue(look.fuel() == expected[i], "channel " + i + " should show fuel " + expected[i] + ", shows " + look);
                helper.assertTrue(look.glow() == RodVisuals.MAX_GLOW, "full power glows brightest, got " + look.glow());
                helper.assertFalse(look.coolant(), "no coolant, no water in the channels");
            }
            helper.assertTrue(helper.getLevel().getLightEmission(helper.absolutePos(CHANNELS.get(0))) == 15, "a bright channel lights up");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void channelsGoDarkWhenTheReactorUnforms(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            ReactorStructureValidator.ReactorStructure structure = reactor.getStructure().orElse(null);
            helper.assertTrue(structure != null, "reactor should form");
            reactor.getRodVisuals().applyNow(helper.getLevel(), structure, List.of(burnt(0.0), burnt(0.0)), 0.5, true);
            helper.assertTrue(look(helper, CHANNELS.get(0)).fuel() == RodVisuals.FRESH, "the channels were lit");
            helper.setBlock(FissionTestBuilder.WALL_CASING, Blocks.AIR);
        });
        helper.runAfterDelay(10, () -> {
            helper.assertFalse(reactor.isFormed(), "a hole in the wall unforms the reactor");
            for (BlockPos channel : CHANNELS) {
                helper.assertTrue(look(helper, channel).equals(RodVisuals.Look.DARK), "channel " + channel + " should go dark");
            }
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void channelsGoDarkWhenTheControllerIsRemoved(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            ReactorStructureValidator.ReactorStructure structure = reactor.getStructure().orElse(null);
            helper.assertTrue(structure != null, "reactor should form");
            reactor.getRodVisuals().applyNow(helper.getLevel(), structure, List.of(burnt(0.5), burnt(0.9)), 0.8, true);
            helper.setBlock(FissionTestBuilder.CONTROLLER, Blocks.AIR);
        });
        helper.runAfterDelay(8, () -> {
            for (BlockPos channel : CHANNELS) {
                helper.assertTrue(look(helper, channel).equals(RodVisuals.Look.DARK), "channel " + channel + " should go dark");
            }
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void channelUpdatesNeverRevalidate(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        int[] before = new int[1];
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            before[0] = reactor.getValidationCount();
            helper.assertTrue(before[0] > 0, "forming took a validation");
        });
        for (int tick = 6; tick < 16; tick++) {
            int step = tick;
            helper.runAfterDelay(step, () -> {
                ReactorStructureValidator.ReactorStructure structure = reactor.getStructure().orElse(null);
                helper.assertTrue(structure != null, "the reactor stays formed");
                List<ReactorPhysicsSimulator.AssemblyView> views = step % 2 == 0 ? List.of(burnt(0.9), burnt(0.9), burnt(0.9)) : List.of(burnt(0.1));
                reactor.getRodVisuals().applyNow(helper.getLevel(), structure, views, step % 3 / 2.0, step % 2 == 0);
                RodVisuals.Look first = look(helper, CHANNELS.get(0));
                helper.assertTrue(first.fuel() == (step % 2 == 0 ? RodVisuals.SPENT : RodVisuals.FRESH), "the channel was rewritten: " + first);
            });
        }
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(reactor.isFormed(), "the reactor stays formed");
            helper.assertTrue(reactor.getValidationCount() == before[0],
                    "rewriting channels must not revalidate: " + before[0] + " -> " + reactor.getValidationCount());
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void scramOfAHotCoreEndsInAColdShutdown(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            reactor.getPhysicsSimulator().applyExternalHeat(600);
            reactor.setManualScram(true);
        });
        helper.runAfterDelay(7, () -> {
            helper.assertTrue(reactor.isCoolingAfterScram(), "a hot core scrammed: waiting for the cold shutdown");
            reactor.getPhysicsSimulator().applyExternalCooling(2000, 0);
        });
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(reactor.getCoreTemperature() < 100, "the core was cooled, got " + reactor.getCoreTemperature());
            helper.assertFalse(reactor.isCoolingAfterScram(), "once cold, the shutdown is complete");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void scramOfAColdCoreIsNotAShutdown(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> reactor.setManualScram(true));
        helper.runAfterDelay(7, () -> {
            helper.assertFalse(reactor.isCoolingAfterScram(), "a cold core has nothing to cool down from");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void viewportInTheWallForms(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        helper.setBlock(FissionTestBuilder.WALL_CASING, ReactorVisualsRegistry.REACTOR_VIEWPORT.get());
        helper.setBlock(FissionTestBuilder.ORIGIN.offset(2, 3, 4), ReactorVisualsRegistry.REACTOR_VIEWPORT.get());
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.succeedWhen(() -> helper.assertTrue(reactor.isFormed(), "viewports in the walls count as casing"));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void viewportCannotMarkOutTheFloor(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        // The floor cross under the controller is how the reactor finds its size: it must be casing.
        helper.setBlock(FissionTestBuilder.ORIGIN.offset(4, 0, 2), ReactorVisualsRegistry.REACTOR_VIEWPORT.get());
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(reactor.getValidationCount() > 0, "the reactor was checked");
            helper.assertFalse(reactor.isFormed(), "a viewport in the floor cross must not form");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void viewportShieldsLikeLeadGlass(GameTestHelper helper) {
        double viewport = RadiationTransmission.of(ReactorVisualsRegistry.REACTOR_VIEWPORT.get().defaultBlockState());
        double casing = RadiationTransmission.of(AllNuclearBlocks.REACTOR_CASING.get().defaultBlockState());
        helper.assertTrue(Math.abs(viewport - 0.3) < 1e-6, "lead glass lets 30% through, got " + viewport);
        helper.assertTrue(viewport <= casing, "no weaker than the casing it replaces");
        helper.assertTrue(viewport < Config.radiationSolidTransmission, "better than a plain solid block");
        helper.assertTrue(RadiationTransmission.of(Blocks.GLASS.defaultBlockState()) > viewport, "better than plain glass");
        helper.succeed();
    }
}
