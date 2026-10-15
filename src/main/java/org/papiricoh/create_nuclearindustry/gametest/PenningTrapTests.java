package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.accelerator.physics.PenningTrapModel;
import org.papiricoh.create_nuclearindustry.accelerator.physics.TrapState;
import org.papiricoh.create_nuclearindustry.accelerator.physics.TrapTier;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterAnnihilation;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterConfig;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterHolder;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterHolders;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterRegistry;
import org.papiricoh.create_nuclearindustry.antimatter.trap.LargePenningTrapBlock;
import org.papiricoh.create_nuclearindustry.antimatter.trap.MediumPenningTrapBlock;
import org.papiricoh.create_nuclearindustry.antimatter.trap.PenningTrapBlockEntity;
import org.papiricoh.create_nuclearindustry.antimatter.trap.VialPortMode;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.energy.CreativeRfGeneratorBlockEntity;

import java.util.List;

/**
 * Penning traps: fed, a trap holds its antimatter without loss; antimatter put into a trap without
 * its field annihilates at once; a loaded trap that loses its energy quenches for
 * {@code antimatter.trap.quenchTicks} and then annihilates; a loaded trap broken annihilates (and
 * the blast may set its neighbours off), an empty one drops itself; the medium and large traps are
 * placed and broken as one.
 *
 * <p>Annihilating tests share the batch {@link #BLASTS}, so no other area's test stands beside a
 * blast; every charge is small enough to stay inside the arena.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class PenningTrapTests {
    public static final String BLASTS = "antimatter_blasts";
    private static final BlockPos TRAP = AntimatterTestBuilder.MIDDLE;
    /** Near the blast: what counts as this test's own annihilation. */
    private static final double NEAR = 2.5;

    /** Breaks a block with its drops, as a player mining it would ({@code destroyBlock} of the helper drops nothing). */
    private static void breakByHand(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
    }

    // ------------------------------------------------------------------ holding

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void aFedTrapHoldsItsChargeWithoutLoss(GameTestHelper helper) {
        AntimatterTestBuilder.fedTrap(helper, TRAP);
        helper.runAfterDelay(5, () -> {
            PenningTrapBlockEntity trap = AntimatterTestBuilder.trap(helper, TRAP);
            helper.assertTrue(trap != null, "the trap should be there");
            helper.assertTrue(trap.powered() && trap.state() == TrapState.STABLE, "the generator raises its field: " + trap.state());
            helper.assertTrue(trap.fill(10) == 10, "it takes 10 ng");
        });
        helper.runAfterDelay(105, () -> {
            PenningTrapBlockEntity trap = AntimatterTestBuilder.trap(helper, TRAP);
            helper.assertTrue(trap != null, "the trap should still be there");
            helper.assertTrue(trap.stored() == 10, "and hold every nanogram after 100 ticks: " + trap.stored());
            helper.assertTrue(trap.state() == TrapState.STABLE && trap.fieldStrength() == 1f, "stable at full field: " + trap.state());
            helper.assertTrue(trap.buffer().getEnergyStored() == trap.buffer().getMaxEnergyStored(), "with its buffer kept full");
            BlockPos abs = helper.absolutePos(TRAP);
            BlockState state = helper.getLevel().getBlockState(abs);
            int signal = state.getAnalogOutputSignal(helper.getLevel(), abs);
            helper.assertTrue(signal == PenningTrapModel.comparator(trap.params(), 10) && signal > 0, "a comparator reads its fill: " + signal);
            MonitorReading reading = trap.plantSample();
            helper.assertTrue(reading.kind() == TargetKind.ANTIMATTER_TRAP
                            && reading.values()[MonitorReading.P_STORED] == 10f && reading.values()[MonitorReading.P_STATE] == TrapState.STABLE.ordinal(),
                    "the control room reads it");
            helper.assertTrue(trap.ticksToAnnihilation() == Integer.MAX_VALUE, "fed, it never runs out");
            trap.setStored(0);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 40)
    public static void aSavedTrapComesBackWithItsChargeFieldAndPort(GameTestHelper helper) {
        BlockPos copy = TRAP.east(2);
        AntimatterTestBuilder.fedTrap(helper, TRAP);
        AntimatterTestBuilder.smallTrap(helper, copy);
        helper.runAfterDelay(5, () -> {
            PenningTrapBlockEntity trap = AntimatterTestBuilder.trap(helper, TRAP);
            PenningTrapBlockEntity loaded = AntimatterTestBuilder.trap(helper, copy);
            helper.assertTrue(trap != null && loaded != null && trap.powered(), "a fed trap and an unfed one");
            trap.fill(5);
            trap.setMode(VialPortMode.DRAIN);
            trap.port().insertItem(0, new ItemStack(AntimatterRegistry.ANTIMATTER_VIAL.get()), false);
            CompoundTag saved = trap.saveWithoutMetadata(helper.getLevel().registryAccess());
            // What a chunk saved with a loaded trap reads back: the field must still hold on the first tick.
            loaded.loadWithComponents(saved, helper.getLevel().registryAccess());
            helper.assertTrue(loaded.stored() == 5, "the charge comes back: " + loaded.stored());
            helper.assertTrue(loaded.powered() && loaded.state() == TrapState.STABLE, "with its field up, not as a dead trap");
            helper.assertTrue(loaded.buffer().getEnergyStored() == trap.buffer().getEnergyStored(), "and its buffer");
            helper.assertTrue(loaded.mode() == VialPortMode.DRAIN && loaded.vial().is(AntimatterRegistry.ANTIMATTER_VIAL.get()),
                    "and its port");
            helper.assertTrue(loaded.insert(1, false) == 1 && AntimatterAnnihilation.pending(helper.getLevel()) == 0,
                    "so antimatter put in on its first tick is held");
            trap.setStored(0);
            loaded.setStored(0);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void trapsTakeEnergyButNeverGiveIt(GameTestHelper helper) {
        AntimatterTestBuilder.smallTrap(helper, TRAP);
        IEnergyStorage energy = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(TRAP), Direction.EAST);
        helper.assertTrue(energy != null, "a trap takes Forge Energy");
        helper.assertTrue(energy.canReceive() && !energy.canExtract(), "in, never out");
        int capacity = PenningTrapModel.bufferCapacity(AntimatterConfig.trap(TrapTier.SMALL));
        helper.assertTrue(energy.getMaxEnergyStored() == capacity, "its buffer holds 200 ticks of its rated demand: " + capacity);
        helper.assertTrue(energy.extractEnergy(100, false) == 0, "nothing can be pulled out");
        helper.succeed();
    }

    // ------------------------------------------------------------------ losing it

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BLASTS, timeoutTicks = 60)
    public static void antimatterPutIntoAnUnpoweredTrapAnnihilates(GameTestHelper helper) {
        AntimatterTestBuilder.smallTrap(helper, TRAP);
        helper.runAfterDelay(3, () -> {
            PenningTrapBlockEntity trap = AntimatterTestBuilder.trap(helper, TRAP);
            helper.assertTrue(trap != null, "the trap should be there");
            helper.assertTrue(!trap.powered() && trap.state() == TrapState.OFFLINE, "without energy its field is down: " + trap.state());
            helper.assertTrue(trap.insert(1.0, true) == 1.0, "it would take the antimatter");
            helper.assertTrue(trap.insert(1.0, false) == 1.0, "and does");
            helper.assertTrue(AntimatterAnnihilation.pending(helper.getLevel()) > 0, "but only to annihilate it");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getTick() > 4, "waiting for the blast");
            helper.assertFalse(helper.getBlockState(TRAP).is(AntimatterRegistry.SMALL_PENNING_TRAP.get()), "the trap is gone");
            helper.assertTrue(AntimatterTestBuilder.blastsNear(helper, TRAP, NEAR) == 1, "in one annihilation");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BLASTS, timeoutTicks = 200)
    public static void aLoadedTrapThatLosesItsEnergyQuenchesThenAnnihilates(GameTestHelper helper) {
        AntimatterTestBuilder.fedTrap(helper, TRAP);
        int quenchTicks = AntimatterConfig.trap(TrapTier.SMALL).quenchTicks();
        int[] quenchedAt = {-1};
        int[] goneAt = {-1};
        int[] lowestCountdown = {Integer.MAX_VALUE};
        helper.runAfterDelay(5, () -> {
            PenningTrapBlockEntity trap = AntimatterTestBuilder.trap(helper, TRAP);
            helper.assertTrue(trap != null && trap.powered(), "a fed trap");
            trap.fill(1.0);
        });
        helper.runAfterDelay(10, () -> {
            PenningTrapBlockEntity trap = AntimatterTestBuilder.trap(helper, TRAP);
            CreativeRfGeneratorBlockEntity generator = AntimatterTestBuilder.generatorAt(helper, TRAP.below());
            helper.assertTrue(trap != null && generator != null, "the trap and its generator should be there");
            // The cable is cut and the buffer has run dry.
            generator.setRate(0);
            trap.buffer().set(0);
        });
        helper.onEachTick(() -> {
            PenningTrapBlockEntity trap = AntimatterTestBuilder.trap(helper, TRAP);
            if (trap != null && trap.state() == TrapState.QUENCHING) {
                if (quenchedAt[0] < 0) {
                    quenchedAt[0] = (int) helper.getTick();
                }
                lowestCountdown[0] = Math.min(lowestCountdown[0], trap.quenchTicksLeft());
            }
            if (quenchedAt[0] >= 0 && goneAt[0] < 0 && !helper.getBlockState(TRAP).is(AntimatterRegistry.SMALL_PENNING_TRAP.get())) {
                goneAt[0] = (int) helper.getTick();
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(quenchedAt[0] > 0, "waiting for the quench");
            helper.assertTrue(quenchedAt[0] <= 12, "it quenches as soon as the energy is gone, at " + quenchedAt[0]);
            helper.assertTrue(goneAt[0] > 0, "waiting for the annihilation; countdown at " + lowestCountdown[0]);
            int lasted = goneAt[0] - quenchedAt[0];
            helper.assertTrue(Math.abs(lasted - quenchTicks) <= 3, "the quench lasts about " + quenchTicks + " ticks, lasted " + lasted);
            helper.assertTrue(AntimatterTestBuilder.blastsNear(helper, TRAP, NEAR) == 1, "and ends in an annihilation");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BLASTS, timeoutTicks = 60)
    public static void breakingALoadedTrapSetsItsNeighbourOff(GameTestHelper helper) {
        BlockPos neighbour = TRAP.east();
        AntimatterTestBuilder.fedTrap(helper, TRAP);
        AntimatterTestBuilder.fedTrap(helper, neighbour);
        helper.runAfterDelay(5, () -> {
            PenningTrapBlockEntity first = AntimatterTestBuilder.trap(helper, TRAP);
            PenningTrapBlockEntity second = AntimatterTestBuilder.trap(helper, neighbour);
            helper.assertTrue(first != null && second != null && first.powered() && second.powered(), "two fed traps");
            // Enough in the first for a blast that surely breaks the trap beside it.
            first.fill(8);
            second.fill(1);
            helper.destroyBlock(TRAP);
            helper.assertTrue(AntimatterAnnihilation.pending(helper.getLevel()) > 0, "breaking a loaded trap annihilates it");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getTick() > 7, "waiting for the blasts");
            helper.assertFalse(helper.getBlockState(neighbour).is(AntimatterRegistry.SMALL_PENNING_TRAP.get()),
                    "the blast broke the loaded trap beside it");
            helper.assertTrue(AntimatterTestBuilder.blastsNear(helper, TRAP, 3) == 2, "which went off in turn: "
                    + AntimatterTestBuilder.blastsNear(helper, TRAP, 3));
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 40)
    public static void breakingAnEmptyTrapDropsItAndItsVial(GameTestHelper helper) {
        AntimatterTestBuilder.smallTrap(helper, TRAP);
        helper.runAfterDelay(3, () -> {
            PenningTrapBlockEntity trap = AntimatterTestBuilder.trap(helper, TRAP);
            helper.assertTrue(trap != null, "the trap should be there");
            ItemStack left = trap.port().insertItem(0, new ItemStack(AntimatterRegistry.ANTIMATTER_VIAL.get()), false);
            helper.assertTrue(left.isEmpty() && !trap.vial().isEmpty(), "an empty vial sits in its port");
            breakByHand(helper, TRAP);
            helper.assertTrue(AntimatterAnnihilation.pending(helper.getLevel()) == 0, "an empty trap does not annihilate");
        });
        helper.runAfterDelay(6, () -> {
            helper.assertItemEntityPresent(AntimatterRegistry.SMALL_PENNING_TRAP_ITEM.get(), TRAP, 2.0);
            helper.assertItemEntityPresent(AntimatterRegistry.ANTIMATTER_VIAL.get(), TRAP, 2.0);
            helper.assertTrue(AntimatterTestBuilder.blastsNear(helper, TRAP, NEAR) == 0, "no blast");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ multi-block traps

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 40)
    public static void theMediumTrapIsPlacedAndBrokenAsOne(GameTestHelper helper) {
        BlockPos lower = TRAP;
        BlockPos upper = lower.above();
        AntimatterRegistry.PENNING_TRAP.get().place(helper.getLevel(), helper.absolutePos(lower), Direction.NORTH);
        helper.assertTrue(helper.getBlockState(upper).is(AntimatterRegistry.PENNING_TRAP.get()), "both halves stand");
        helper.assertTrue(helper.getBlockState(upper).getValue(MediumPenningTrapBlock.HALF)
                == DoubleBlockHalf.UPPER, "the upper half on top");
        PenningTrapBlockEntity master = AntimatterTestBuilder.trap(helper, lower);
        helper.assertTrue(master != null && master.tier() == TrapTier.MEDIUM, "the lower half holds the trap");
        helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(upper)) == null, "the upper half has no block entity of its own");
        helper.assertTrue(AntimatterHolders.at(helper.getLevel(), helper.absolutePos(upper)) == master, "it answers through the lower one");
        IEnergyStorage energy = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(upper), Direction.UP);
        helper.assertTrue(energy == master.energy(), "a cable on the upper half feeds the trap");
        helper.assertTrue(master.capacity() == AntimatterConfig.trap(TrapTier.MEDIUM).capacity(), "256 ng of room");
        helper.runAfterDelay(2, () -> breakByHand(helper, upper));
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(helper.getBlockState(lower).isAir() && helper.getBlockState(upper).isAir(), "breaking one half takes both");
            helper.assertItemEntityCountIs(AntimatterRegistry.PENNING_TRAP_ITEM.get(), lower, 3.0, 1);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 40)
    public static void theLargeTrapIsPlacedAndBrokenAsOne(GameTestHelper helper) {
        BlockPos origin = new BlockPos(5, 2, 5);
        AntimatterRegistry.LARGE_PENNING_TRAP.get().place(helper.getLevel(), helper.absolutePos(origin));
        List<BlockPos> parts = LargePenningTrapBlock.partsFrom(origin);
        helper.assertTrue(parts.size() == 12, "two by two, three high");
        PenningTrapBlockEntity master = AntimatterTestBuilder.trap(helper, origin);
        helper.assertTrue(master != null && master.tier() == TrapTier.LARGE, "the bottom north-west block holds the trap");
        for (BlockPos part : parts) {
            helper.assertTrue(helper.getBlockState(part).is(AntimatterRegistry.LARGE_PENNING_TRAP.get()), "every part stands: " + part);
            helper.assertTrue(AntimatterHolders.at(helper.getLevel(), helper.absolutePos(part)) == master, "and answers through the master: " + part);
            if (!part.equals(origin)) {
                helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(part)) == null, "only the master has a block entity");
            }
        }
        BlockPos top = origin.offset(1, 2, 1);
        IEnergyStorage energy = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(top), Direction.EAST);
        helper.assertTrue(energy == master.energy(), "a cable on any part feeds the trap");
        helper.assertTrue(master.parts().size() == 12, "the master knows its parts");
        helper.runAfterDelay(2, () -> breakByHand(helper, top));
        helper.runAfterDelay(5, () -> {
            for (BlockPos part : parts) {
                helper.assertTrue(helper.getBlockState(part).isAir(), "breaking one part takes the whole trap: " + part);
            }
            helper.assertItemEntityCountIs(AntimatterRegistry.LARGE_PENNING_TRAP_ITEM.get(), origin, 5.0, 1);
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ the Ponder scene

    @GameTest(template = "gametest/antimatter_containment_ponder", timeoutTicks = 60)
    public static void theContainmentPonderSchematicHoldsWholeTraps(GameTestHelper helper) {
        BlockPos small = new BlockPos(1, 2, 3);
        BlockPos medium = new BlockPos(3, 2, 3);
        BlockPos large = new BlockPos(5, 2, 3);
        helper.succeedWhen(() -> {
            PenningTrapBlockEntity s = AntimatterTestBuilder.trap(helper, small);
            PenningTrapBlockEntity m = AntimatterTestBuilder.trap(helper, medium);
            PenningTrapBlockEntity l = AntimatterTestBuilder.trap(helper, large);
            helper.assertTrue(s != null && m != null && l != null, "the three traps of the scene stand");
            helper.assertTrue(s.tier() == TrapTier.SMALL && m.tier() == TrapTier.MEDIUM && l.tier() == TrapTier.LARGE, "one of each size");
            helper.assertTrue(AntimatterHolders.at(helper.getLevel(), helper.absolutePos(medium.above())) == m, "the medium trap is whole");
            for (BlockPos part : LargePenningTrapBlock.partsFrom(large)) {
                helper.assertTrue(AntimatterHolders.at(helper.getLevel(), helper.absolutePos(part)) == l, "the large trap is whole: " + part);
            }
            helper.assertTrue(s.powered() && m.powered() && l.powered(), "and the scene's generators keep them all powered");
            AntimatterHolder none = AntimatterHolders.at(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 3)));
            helper.assertTrue(none == null, "the conduit between them holds nothing");
        });
    }
}
