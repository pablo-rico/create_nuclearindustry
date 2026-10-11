package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockText;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.multiblock.ReactorStructureValidator;
import org.papiricoh.create_nuclearindustry.reactor.physics.ReactorPhysicsSimulator;

/**
 * The reactor notices its control channels change: rods swapped for a reflector column or pulled out
 * under a closed lid stop counting as a channel, and a rod put back brings it back, while rods hanging
 * from a piston pole keep their channel without ever revalidating. A half-rebuilt column keeps the last
 * structure, and a core changed while unloaded gets a simulator that matches it.
 *
 * <p>The test reactor (see {@link FissionTestBuilder}) has one channel at (1, 3), three blocks deep,
 * capped with casing when it holds rods. Channel changes are checked on the rod scans and need two in a
 * row, so a few dozen ticks are plenty.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class ReactorChannelTests {
    private static final int INNER = FissionTestBuilder.SIZE - 2;
    private static final BlockPos LID = FissionTestBuilder.ORIGIN.offset(1, FissionTestBuilder.SIZE - 1, 3);
    /** Long enough for two rod scans in a row and the revalidation they ask for. */
    private static final int SETTLE = 30;

    /** The control channel's block at height {@code y}, 1 (bottom) to {@link #INNER}. */
    private static BlockPos channel(int y) {
        return FissionTestBuilder.ORIGIN.offset(1, y, 3);
    }

    private static ReactorStructureValidator.ReactorStructure structure(GameTestHelper helper, ReactorBlockEntity reactor) {
        ReactorStructureValidator.ReactorStructure structure = reactor.getStructure().orElse(null);
        helper.assertTrue(structure != null, "the reactor should have a structure");
        return structure;
    }

    private static boolean channelBlocked(ReactorBlockEntity reactor) {
        return reactor.getLastValidation().errors().stream()
                .anyMatch(error -> MultiblockText.key(error).equals(ReactorStructureValidator.CHANNEL_BLOCKED));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void rodsSwappedForReflectorBecomeAReflectorColumn(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        ReactorPhysicsSimulator[] before = new ReactorPhysicsSimulator[1];
        double[] heated = new double[1];
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            ReactorStructureValidator.ReactorStructure structure = structure(helper, reactor);
            helper.assertTrue(structure.controlRodCount == INNER, "one channel of " + INNER + ", got " + structure.controlRodCount);
            helper.assertTrue(structure.reflection == 0.0, "no reflector yet");
            reactor.getPhysicsSimulator().applyExternalHeat(600);
            before[0] = reactor.getPhysicsSimulator();
            heated[0] = before[0].getCoreTemperature();
            // the lid stays casing: the column is closed up as a reflector column
            for (int y = 1; y <= INNER; y++) {
                helper.setBlock(channel(y), AllNuclearBlocks.BERYLLIUM_REFLECTOR.get());
            }
        });
        helper.runAfterDelay(5 + SETTLE, () -> {
            ReactorStructureValidator.ReactorStructure structure = structure(helper, reactor);
            ReactorPhysicsSimulator physics = reactor.getPhysicsSimulator();
            helper.assertTrue(reactor.isFormed(), "the reactor stays formed");
            helper.assertFalse(channelBlocked(reactor), "a reflector column under a closed lid is not a blocked channel");
            helper.assertTrue(structure.controlChannels.isEmpty(), "the channel is gone: " + structure.controlChannels);
            helper.assertTrue(structure.controlRodCount == 0, "rod capacity drops by " + INNER + ", got " + structure.controlRodCount);
            helper.assertTrue(structure.reflection == 1.0 / 8.0, "one of the eight ring columns reflects, got " + structure.reflection);
            helper.assertTrue(physics != before[0], "the simulator was rebuilt for the new core");
            helper.assertTrue(physics.getControlRodCount() == 0 && physics.getMaxAbsorption() == 0.0,
                    "the simulator has no rods left to absorb: " + physics.getControlRodCount() + ", " + physics.getMaxAbsorption());
            helper.assertTrue(physics.getReflection() == structure.reflection, "and gets the reflection: " + physics.getReflection());
            double temperature = physics.getCoreTemperature();
            helper.assertTrue(temperature > 300 && Math.abs(temperature - before[0].getCoreTemperature()) < 0.1 * heated[0],
                    "the core keeps its heat through the rebuild: " + heated[0] + " -> " + temperature);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void rodsPulledUnderAClosedLidLeaveNoChannel(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            for (int y = 1; y <= INNER; y++) {
                helper.setBlock(channel(y), Blocks.AIR);
            }
        });
        helper.runAfterDelay(5 + SETTLE, () -> {
            ReactorStructureValidator.ReactorStructure structure = structure(helper, reactor);
            helper.assertTrue(reactor.isFormed(), "a reactor without rods still forms (only a warning)");
            helper.assertTrue(structure.controlChannels.isEmpty(), "air under a closed lid is an empty column: " + structure.controlChannels);
            helper.assertTrue(structure.controlRodCount == 0 && reactor.getPhysicsSimulator().getControlRodCount() == 0,
                    "no rod capacity left: " + structure.controlRodCount);
            helper.setBlock(channel(2), AllNuclearBlocks.CONTROL_ROD.get());
        });
        helper.runAfterDelay(5 + 2 * SETTLE, () -> {
            ReactorStructureValidator.ReactorStructure structure = structure(helper, reactor);
            helper.assertTrue(structure.controlChannels.size() == 1, "a rod put back makes it a channel again: " + structure.controlChannels);
            helper.assertTrue(structure.controlRodCount == INNER && reactor.getPhysicsSimulator().getControlRodCount() == INNER,
                    "with its whole capacity: " + structure.controlRodCount);
            float insertion = reactor.getControlRodScan().insertionRatio();
            helper.assertTrue(Math.abs(insertion - 1.0f / INNER) < 1e-4, "one rod of three is in: " + insertion);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void anOpenLidWithoutRodsIsAnEmptyChannel(GameTestHelper helper) {
        FissionTestBuilder.build(helper, false);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(10, () -> {
            ReactorStructureValidator.ReactorStructure structure = structure(helper, reactor);
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            helper.assertTrue(structure.controlChannels.size() == 1, "the open lid marks one channel: " + structure.controlChannels);
            helper.assertTrue(structure.controlRodCount == INNER, "of " + INNER + " blocks, got " + structure.controlRodCount);
            helper.assertTrue(reactor.getControlRodScan().insertionRatio() == 0.0f, "with nothing inserted");
            helper.assertTrue(reactor.getPhysicsSimulator().getControlRodInsertion() == 0.0f, "the simulator sees no rods in");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void rodsOnAPistonPoleKeepTheirChannel(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        int[] validations = new int[1];
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            validations[0] = reactor.getValidationCount();
            // A piston above pulled the rods up one block: its pole now fills the lid, air below the rods.
            Block pole = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "piston_extension_pole"));
            helper.assertTrue(pole != Blocks.AIR, "Create's piston extension pole is registered");
            helper.setBlock(LID, pole);
            helper.setBlock(channel(1), Blocks.AIR);
        });
        helper.runAfterDelay(5 + SETTLE, () -> {
            ReactorStructureValidator.ReactorStructure structure = structure(helper, reactor);
            helper.assertTrue(reactor.isFormed(), "the reactor stays formed");
            helper.assertTrue(structure.controlChannels.size() == 1 && structure.controlRodCount == INNER,
                    "the rods keep their channel: " + structure.controlChannels + ", " + structure.controlRodCount);
            float insertion = reactor.getControlRodScan().insertionRatio();
            helper.assertTrue(Math.abs(insertion - 2.0f / INNER) < 1e-4, "two rods of three are in: " + insertion);
            helper.assertTrue(reactor.getValidationCount() == validations[0],
                    "moving rods never revalidate: " + validations[0] + " -> " + reactor.getValidationCount());
            // pulled all the way out: only the pole in the lid marks the channel
            for (int y = 1; y <= INNER; y++) {
                helper.setBlock(channel(y), Blocks.AIR);
            }
        });
        helper.runAfterDelay(5 + 2 * SETTLE, () -> {
            ReactorStructureValidator.ReactorStructure structure = structure(helper, reactor);
            helper.assertTrue(structure.controlChannels.size() == 1 && structure.controlRodCount == INNER,
                    "a piston pole in the lid keeps the channel open: " + structure.controlChannels);
            helper.assertTrue(reactor.getControlRodScan().insertionRatio() == 0.0f, "with the rods all out");
            helper.assertTrue(reactor.getValidationCount() == validations[0],
                    "still without revalidating: " + validations[0] + " -> " + reactor.getValidationCount());
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aHalfRebuiltChannelKeepsTheLastStructure(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        ReactorStructureValidator.ReactorStructure[] before = new ReactorStructureValidator.ReactorStructure[1];
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            before[0] = structure(helper, reactor);
            // mid-edit: one rod swapped for a reflector, two rods still in
            helper.setBlock(channel(1), AllNuclearBlocks.BERYLLIUM_REFLECTOR.get());
        });
        helper.runAfterDelay(5 + SETTLE, () -> {
            helper.assertTrue(structure(helper, reactor) == before[0], "rods still mark the channel: nothing to revalidate");
            // anything else that revalidates finds the mixed column blocked and keeps the structure
            reactor.requestStructureRevalidation(null);
        });
        helper.runAfterDelay(10 + SETTLE, () -> {
            ReactorStructureValidator.ReactorStructure structure = structure(helper, reactor);
            helper.assertTrue(channelBlocked(reactor), "the mixed column is a blocked channel: " + reactor.getLastValidation().errors());
            helper.assertTrue(reactor.isFormed(), "the reactor stays formed");
            helper.assertTrue(structure == before[0] && structure.controlRodCount == INNER,
                    "with its last structure: " + structure.controlRodCount);
            helper.assertTrue(reactor.getPhysicsSimulator().getControlRodCount() == INNER, "and its rod capacity");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aCoreChangedWhileUnloadedGetsAMatchingSimulator(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        ReactorBlockEntity[] loaded = new ReactorBlockEntity[1];
        double[] saved = new double[1];
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            reactor.getPhysicsSimulator().applyExternalHeat(600);
            saved[0] = reactor.getPhysicsSimulator().getCoreTemperature();
            var registries = helper.getLevel().registryAccess();
            CompoundTag tag = reactor.saveWithoutMetadata(registries);
            helper.assertTrue(tag.getCompound("physics").getInt("controlRodCount") == INNER, "the save holds the rod capacity");
            // While unloaded the rods became a reflector column under the casing lid...
            for (int y = 1; y <= INNER; y++) {
                helper.setBlock(channel(y), AllNuclearBlocks.BERYLLIUM_REFLECTOR.get());
            }
            // ...and the controller loads again from its save, with no structure yet.
            helper.setBlock(FissionTestBuilder.CONTROLLER, Blocks.AIR);
            helper.setBlock(FissionTestBuilder.CONTROLLER, AllNuclearBlocks.REACTOR_CONTROLLER.get());
            loaded[0] = FissionTestBuilder.controller(helper);
            helper.assertTrue(loaded[0] != null && loaded[0] != reactor, "a fresh controller");
            loaded[0].loadWithComponents(tag, registries);
            helper.assertTrue(loaded[0].getStructure().isEmpty(), "loaded without a structure");
            helper.assertTrue(loaded[0].getPhysicsSimulator().getControlRodCount() == INNER, "with the saved simulator");
        });
        helper.runAfterDelay(10, () -> {
            ReactorBlockEntity again = loaded[0];
            helper.assertTrue(again != null, "the controller was reloaded");
            ReactorStructureValidator.ReactorStructure structure = structure(helper, again);
            ReactorPhysicsSimulator physics = again.getPhysicsSimulator();
            helper.assertTrue(again.isFormed(), "the reloaded reactor forms");
            helper.assertTrue(structure.controlRodCount == 0, "the core has no channel now: " + structure.controlRodCount);
            helper.assertTrue(physics.getControlRodCount() == structure.controlRodCount,
                    "the simulator matches the core, not the save: " + physics.getControlRodCount());
            helper.assertTrue(physics.getReflection() == structure.reflection && structure.reflection > 0,
                    "and its reflection: " + physics.getReflection());
            helper.assertTrue(Math.abs(physics.getCoreTemperature() - saved[0]) < 0.1 * saved[0],
                    "keeping the saved heat: " + saved[0] + " -> " + physics.getCoreTemperature());
            helper.succeed();
        });
    }
}
