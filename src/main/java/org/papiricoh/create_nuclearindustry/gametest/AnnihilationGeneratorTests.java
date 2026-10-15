package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorConfig;
import org.papiricoh.create_nuclearindustry.accelerator.physics.AnnihilationModel;
import org.papiricoh.create_nuclearindustry.accelerator.physics.TrapState;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterAnnihilation;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterRegistry;
import org.papiricoh.create_nuclearindustry.antimatter.generator.AnnihilationGeneratorBlockEntity;
import org.papiricoh.create_nuclearindustry.antimatter.trap.PenningTrapBlockEntity;
import org.papiricoh.create_nuclearindustry.antimatter.vial.AntimatterVialItem;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;

/**
 * The annihilation generator: fed from a trap beside it at full redstone it turns its shaft at the
 * rated speed and capacity and burns antimatter at the model's rate; in the electric mode it pushes
 * Forge Energy into its neighbours, enough to keep the very trap that feeds it up with no other
 * source (the closed loop); split gives half of each; without redstone it only keeps the standby
 * burn; a vial in its port is drained into the chamber; and antimatter in a cold chamber with no
 * Forge Energy annihilates, as in a trap.
 *
 * <p>Annihilating tests run in {@link PenningTrapTests#BLASTS}; the chamber holds 1 ng at most, so
 * every blast stays inside the arena. Tests that leave a generator fuelled empty it as they pass.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class AnnihilationGeneratorTests {
    private static final BlockPos GEN = AntimatterTestBuilder.MIDDLE;
    /** The feeding trap, west of the generator, with its creative generator below. */
    private static final BlockPos TRAP = GEN.west();
    /** The shaft side: the generator faces east. */
    private static final BlockPos SHAFT = GEN.east();
    private static final double NEAR = 2.5;

    /** A generator facing east (shaft side), in {@code mode}, with a full field buffer. */
    private static AnnihilationGeneratorBlockEntity generator(GameTestHelper helper, AnnihilationModel.Mode mode) {
        AntimatterTestBuilder.listen();
        helper.setBlock(GEN, AntimatterRegistry.ANNIHILATION_GENERATOR.get().defaultBlockState()
                .setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.EAST));
        AnnihilationGeneratorBlockEntity generator = at(helper);
        if (generator != null) {
            generator.setMode(mode);
            generator.primeField();
        }
        return generator;
    }

    private static AnnihilationGeneratorBlockEntity at(GameTestHelper helper) {
        return AcceleratorTestBuilder.entity(helper, GEN, AnnihilationGeneratorBlockEntity.class);
    }

    /** Full throttle: a redstone block on top. */
    private static void fullThrottle(GameTestHelper helper) {
        helper.setBlock(GEN.above(), Blocks.REDSTONE_BLOCK);
    }

    private static void shaft(GameTestHelper helper) {
        Block shaft = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "shaft"));
        helper.setBlock(SHAFT, shaft.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
    }

    /** Antimatter in the trap and the chamber together, ng. */
    private static double total(PenningTrapBlockEntity trap, AnnihilationGeneratorBlockEntity generator) {
        return trap.stored() + generator.stored();
    }

    private static void leaveEmpty(PenningTrapBlockEntity trap, AnnihilationGeneratorBlockEntity generator) {
        if (trap != null) {
            trap.setStored(0);
        }
        if (generator != null) {
            generator.setStored(0);
        }
    }

    // ------------------------------------------------------------------ the three modes

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void inRotationItTurnsItsShaftAndBurnsAtTheRatedRate(GameTestHelper helper) {
        AntimatterTestBuilder.fedTrap(helper, TRAP);
        generator(helper, AnnihilationModel.Mode.ROTATION);
        fullThrottle(helper);
        shaft(helper);
        AnnihilationModel.Params p = AcceleratorConfig.generator();
        double[] before = {Double.NaN};
        helper.runAfterDelay(5, () -> {
            PenningTrapBlockEntity trap = AntimatterTestBuilder.trap(helper, TRAP);
            helper.assertTrue(trap != null && trap.powered(), "a fed trap beside the generator");
            helper.assertTrue(trap.fill(10) == 10, "with 10 ng in it");
        });
        // A second to fill the chamber, then the burn is measured over 100 ticks.
        helper.runAfterDelay(60, () -> {
            PenningTrapBlockEntity trap = AntimatterTestBuilder.trap(helper, TRAP);
            AnnihilationGeneratorBlockEntity generator = at(helper);
            helper.assertTrue(trap != null && generator != null, "the trap and the generator should be there");
            helper.assertTrue(generator.lit() && generator.burning(), "the chamber is lit: " + generator.statusId());
            before[0] = total(trap, generator);
        });
        helper.runAfterDelay(160, () -> {
            PenningTrapBlockEntity trap = AntimatterTestBuilder.trap(helper, TRAP);
            AnnihilationGeneratorBlockEntity generator = at(helper);
            helper.assertTrue(trap != null && generator != null, "the trap and the generator should still be there");
            double burnt = before[0] - total(trap, generator);
            double expected = 100 * p.ratedBurn();
            helper.assertTrue(Math.abs(burnt - expected) <= expected * 0.02,
                    "100 ticks at full throttle burn " + expected + " ng, burnt " + burnt);
            helper.assertTrue(generator.throttle() == 1.0 && Math.abs(generator.outputFraction() - 1.0) < 1e-9,
                    "a redstone block is full throttle, fully delivered: " + generator.outputFraction());
            helper.assertTrue(Math.abs(generator.getGeneratedSpeed()) == (float) p.ratedSpeed(),
                    "it turns at " + p.ratedSpeed() + " RPM: " + generator.getGeneratedSpeed());
            double su = p.ratedCapacity() * p.ratedSpeed();
            helper.assertTrue(Math.abs(generator.stressCapacity() - su) < 1, "and offers " + su + " SU: " + generator.stressCapacity());
            helper.assertTrue(generator.feMade() == 0, "rotation makes no Forge Energy: " + generator.feMade());
            KineticBlockEntity shaft = AcceleratorTestBuilder.entity(helper, SHAFT, KineticBlockEntity.class);
            helper.assertTrue(shaft != null && Math.abs(shaft.getSpeed()) == (float) p.ratedSpeed() && !shaft.isOverStressed(),
                    "the shaft beside it turns at the rated speed: " + (shaft == null ? "no shaft" : shaft.getSpeed()));
            BlockPos abs = helper.absolutePos(GEN);
            int signal = helper.getLevel().getBlockState(abs).getAnalogOutputSignal(helper.getLevel(), abs);
            helper.assertTrue(signal == 15, "a comparator reads full output: " + signal);
            MonitorReading reading = generator.plantSample();
            helper.assertTrue(reading.kind() == TargetKind.ANNIHILATION_GENERATOR
                            && reading.values()[MonitorReading.G_OUTPUT] == 1f
                            && reading.values()[MonitorReading.G_MODE] == AnnihilationModel.Mode.ROTATION.ordinal(),
                    "the control room reads it");
            leaveEmpty(trap, generator);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 600)
    public static void inElectricItPowersTheTrapThatFeedsIt(GameTestHelper helper) {
        AntimatterTestBuilder.fedTrap(helper, TRAP);
        generator(helper, AnnihilationModel.Mode.ELECTRIC);
        fullThrottle(helper);
        AnnihilationModel.Params p = AcceleratorConfig.generator();
        helper.runAfterDelay(5, () -> {
            PenningTrapBlockEntity trap = AntimatterTestBuilder.trap(helper, TRAP);
            helper.assertTrue(trap != null && trap.powered(), "a fed trap beside the generator");
            trap.fill(10);
        });
        // Once lit, the trap's own creative generator is taken away: from now on only the annihilation
        // generator powers it.
        helper.runAfterDelay(40, () -> {
            AnnihilationGeneratorBlockEntity generator = at(helper);
            helper.assertTrue(generator != null && generator.lit(), "the generator is lit");
            helper.setBlock(TRAP.below(), Blocks.AIR);
        });
        helper.runAfterDelay(60, () -> {
            PenningTrapBlockEntity trap = AntimatterTestBuilder.trap(helper, TRAP);
            AnnihilationGeneratorBlockEntity generator = at(helper);
            helper.assertTrue(trap != null && generator != null, "the trap and the generator should be there");
            helper.assertTrue(generator.getGeneratedSpeed() == 0 && generator.stressCapacity() == 0, "electric turns no shaft");
            helper.assertTrue(generator.feMade() == (int) Math.floor(p.ratedEnergy()), "it makes " + p.ratedEnergy() + " FE/t: " + generator.feMade());
            IEnergyStorage terminals = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(GEN), Direction.NORTH);
            helper.assertTrue(terminals != null && terminals.canExtract() && terminals.canReceive(), "cables can pull from it and feed it");
            // Drain the trap's buffer: only the generator can fill it again.
            trap.buffer().set(0);
        });
        helper.runAfterDelay(62, () -> {
            PenningTrapBlockEntity trap = AntimatterTestBuilder.trap(helper, TRAP);
            helper.assertTrue(trap != null && trap.buffer().getEnergyStored() == trap.buffer().getMaxEnergyStored(),
                    "the generator's Forge Energy refills the trap's buffer at once: " + (trap == null ? 0 : trap.buffer().getEnergyStored()));
        });
        helper.runAfterDelay(400, () -> {
            PenningTrapBlockEntity trap = AntimatterTestBuilder.trap(helper, TRAP);
            AnnihilationGeneratorBlockEntity generator = at(helper);
            helper.assertTrue(trap != null && generator != null, "the closed loop still stands after 18 s");
            helper.assertTrue(trap.state() == TrapState.STABLE && trap.fieldStrength() == 1f, "the trap is held: " + trap.state());
            helper.assertTrue(generator.lit() && generator.chamberState() == TrapState.STABLE, "and the generator still burns");
            helper.assertTrue(generator.fieldBuffer().getEnergyStored() == generator.fieldBuffer().getMaxEnergyStored(),
                    "keeping its own field buffer full");
            helper.assertTrue(trap.stored() < 9.0 && trap.stored() > 7.0, "burning the trap's antimatter: " + trap.stored());
            helper.assertTrue(AntimatterTestBuilder.blastsNear(helper, GEN, NEAR) == 0, "with no annihilation");
            leaveEmpty(trap, generator);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void splitGivesHalfAsRotationAndHalfAsEnergy(GameTestHelper helper) {
        AntimatterTestBuilder.fedTrap(helper, TRAP);
        generator(helper, AnnihilationModel.Mode.SPLIT);
        fullThrottle(helper);
        AnnihilationModel.Params p = AcceleratorConfig.generator();
        helper.runAfterDelay(5, () -> {
            PenningTrapBlockEntity trap = AntimatterTestBuilder.trap(helper, TRAP);
            helper.assertTrue(trap != null && trap.powered(), "a fed trap beside the generator");
            trap.fill(10);
        });
        helper.runAfterDelay(60, () -> {
            PenningTrapBlockEntity trap = AntimatterTestBuilder.trap(helper, TRAP);
            AnnihilationGeneratorBlockEntity generator = at(helper);
            helper.assertTrue(trap != null && generator != null && generator.lit(), "a lit generator");
            helper.assertTrue(Math.abs(generator.getGeneratedSpeed()) == (float) p.ratedSpeed(), "split turns the shaft at the rated speed");
            double su = 0.5 * p.ratedCapacity() * p.ratedSpeed();
            helper.assertTrue(Math.abs(generator.stressCapacity() - su) < 1, "with half the capacity, " + su + " SU: " + generator.stressCapacity());
            int fe = (int) Math.floor(0.5 * p.ratedEnergy());
            helper.assertTrue(generator.feMade() == fe, "and half the energy, " + fe + " FE/t: " + generator.feMade());
            leaveEmpty(trap, generator);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void withoutRedstoneOnlyTheStandbyBurnHoldsTheField(GameTestHelper helper) {
        AntimatterTestBuilder.fedTrap(helper, TRAP);
        generator(helper, AnnihilationModel.Mode.ROTATION);
        AnnihilationModel.Params p = AcceleratorConfig.generator();
        double[] before = {Double.NaN};
        helper.runAfterDelay(5, () -> {
            PenningTrapBlockEntity trap = AntimatterTestBuilder.trap(helper, TRAP);
            helper.assertTrue(trap != null && trap.powered(), "a fed trap beside the generator");
            trap.fill(10);
        });
        helper.runAfterDelay(60, () -> {
            PenningTrapBlockEntity trap = AntimatterTestBuilder.trap(helper, TRAP);
            AnnihilationGeneratorBlockEntity generator = at(helper);
            helper.assertTrue(trap != null && generator != null, "the trap and the generator should be there");
            helper.assertTrue(generator.lit() && generator.standby(), "lit on the standby burn: " + generator.statusId());
            // Its field buffer is no longer needed: the standby burn holds the chamber.
            generator.fieldBuffer().set(0);
            before[0] = total(trap, generator);
        });
        helper.runAfterDelay(160, () -> {
            PenningTrapBlockEntity trap = AntimatterTestBuilder.trap(helper, TRAP);
            AnnihilationGeneratorBlockEntity generator = at(helper);
            helper.assertTrue(trap != null && generator != null, "the trap and the generator should still be there");
            double burnt = before[0] - total(trap, generator);
            double expected = 100 * p.standbyBurn();
            helper.assertTrue(Math.abs(burnt - expected) <= expected * 0.02, "100 ticks of standby burn " + expected + " ng, burnt " + burnt);
            helper.assertTrue(generator.getGeneratedSpeed() == 0 && generator.feMade() == 0 && generator.outputFraction() == 0,
                    "and give nothing");
            helper.assertTrue(generator.chamberState() == TrapState.STABLE && generator.lit(), "the chamber holds without Forge Energy");
            helper.assertTrue("standby".equals(generator.statusId()), "goggles say standby: " + generator.statusId());
            BlockPos abs = helper.absolutePos(GEN);
            int signal = helper.getLevel().getBlockState(abs).getAnalogOutputSignal(helper.getLevel(), abs);
            helper.assertTrue(signal == 1, "a comparator reads 1, burning but giving nothing: " + signal);
            leaveEmpty(trap, generator);
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ the port

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void aVialInThePortIsDrainedIntoTheChamber(GameTestHelper helper) {
        AnnihilationGeneratorBlockEntity placed = generator(helper, AnnihilationModel.Mode.ROTATION);
        AntimatterTestBuilder.generator(helper, GEN.below());
        helper.assertTrue(placed != null, "the generator should be there");
        helper.runAfterDelay(3, () -> {
            AnnihilationGeneratorBlockEntity generator = at(helper);
            helper.assertTrue(generator != null && generator.powered(), "a cold chamber with Forge Energy holds what goes in");
            ItemStack vial = AntimatterRegistry.ANTIMATTER_VIAL.get().filled(0.5);
            helper.assertTrue(generator.port().insertItem(0, vial, false).isEmpty(), "a loaded vial goes into the port");
            helper.assertTrue(generator.port().extractItem(0, 1, true).isEmpty(), "automation cannot take it back loaded");
            helper.assertTrue(generator.port().insertItem(0, new ItemStack(AntimatterRegistry.ANTIMATTER_VIAL.get()), true).getCount() == 1,
                    "and the port takes one vial at a time");
        });
        helper.succeedWhen(() -> {
            AnnihilationGeneratorBlockEntity generator = at(helper);
            helper.assertTrue(generator != null, "the generator should be there");
            helper.assertFalse(AntimatterVialItem.loaded(generator.vial()), "waiting for the vial to empty: "
                    + AntimatterVialItem.antimatter(generator.vial()));
            helper.assertTrue(generator.lit() && generator.stored() > 0.49 && generator.stored() <= 0.5,
                    "its charge is in the chamber, burning on standby: " + generator.stored());
            ItemStack out = generator.port().extractItem(0, 1, false);
            helper.assertTrue(out.is(AntimatterRegistry.ANTIMATTER_VIAL.get()) && !AntimatterVialItem.loaded(out),
                    "the empty vial can be taken out");
            helper.assertTrue(AntimatterAnnihilation.pending(helper.getLevel()) == 0, "nothing annihilated");
            generator.setStored(0);
        });
    }

    // ------------------------------------------------------------------ losing it

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = PenningTrapTests.BLASTS, timeoutTicks = 60)
    public static void antimatterPutIntoAColdUnpoweredChamberAnnihilates(GameTestHelper helper) {
        AntimatterTestBuilder.listen();
        helper.setBlock(GEN, AntimatterRegistry.ANNIHILATION_GENERATOR.get().defaultBlockState()
                .setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.EAST));
        helper.runAfterDelay(3, () -> {
            AnnihilationGeneratorBlockEntity generator = at(helper);
            helper.assertTrue(generator != null, "the generator should be there");
            helper.assertTrue(!generator.powered() && !generator.lit(), "a cold chamber without Forge Energy has no field");
            helper.assertTrue(generator.insert(0.5, false) == 0.5, "it takes the antimatter");
            helper.assertTrue(AntimatterAnnihilation.pending(helper.getLevel()) > 0, "but only to annihilate it");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getTick() > 4, "waiting for the blast");
            helper.assertFalse(helper.getBlockState(GEN).is(AntimatterRegistry.ANNIHILATION_GENERATOR.get()), "the generator is gone");
            helper.assertTrue(AntimatterTestBuilder.blastsNear(helper, GEN, NEAR) == 1, "in one annihilation");
        });
    }

    /**
     * A conduit never delivers into a cold generator without energy (that would only annihilate the
     * delivery and the generator): it waits until the generator gets its energy, then feeds it.
     * In the blasts batch in case that ever breaks.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = PenningTrapTests.BLASTS, timeoutTicks = 200)
    public static void aConduitWaitsForAColdGeneratorsEnergy(GameTestHelper helper) {
        BlockPos from = new BlockPos(3, 2, 6);
        BlockPos gen = new BlockPos(7, 2, 6);
        AntimatterTestBuilder.fedTrap(helper, from);
        for (int x = from.getX() + 1; x < gen.getX(); x++) {
            helper.setBlock(new BlockPos(x, 2, 6), AntimatterRegistry.ANTIMATTER_CONDUIT.get());
        }
        AntimatterTestBuilder.generator(helper, new BlockPos(5, 3, 6));
        helper.setBlock(gen, AntimatterRegistry.ANNIHILATION_GENERATOR.get().defaultBlockState()
                .setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.EAST));
        helper.runAfterDelay(5, () -> {
            PenningTrapBlockEntity trap = AntimatterTestBuilder.trap(helper, from);
            helper.assertTrue(trap != null && trap.powered(), "a fed trap at the far end of a powered conduit");
            trap.fill(10);
        });
        helper.runAfterDelay(45, () -> {
            PenningTrapBlockEntity trap = AntimatterTestBuilder.trap(helper, from);
            AnnihilationGeneratorBlockEntity generator = AcceleratorTestBuilder.entity(helper, gen, AnnihilationGeneratorBlockEntity.class);
            helper.assertTrue(trap != null && generator != null, "the trap and the generator are still there");
            helper.assertTrue(!generator.powered() && !generator.acceptsAntimatter(), "a cold generator without energy takes nothing");
            helper.assertTrue(generator.stored() == 0 && trap.stored() == 10, "so the conduit moved nothing: " + trap.stored());
            helper.assertTrue(AntimatterTestBuilder.blastsNear(helper, gen, NEAR) == 0, "and nothing annihilated");
            AntimatterTestBuilder.generator(helper, gen.below());
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getTick() > 46, "waiting for the generator's energy");
            PenningTrapBlockEntity trap = AntimatterTestBuilder.trap(helper, from);
            AnnihilationGeneratorBlockEntity generator = AcceleratorTestBuilder.entity(helper, gen, AnnihilationGeneratorBlockEntity.class);
            helper.assertTrue(trap != null && generator != null, "the trap and the generator are still there");
            helper.assertTrue(generator.lit() && generator.stored() > 0, "with energy the conduit feeds it and it lights: " + generator.statusId());
            helper.assertTrue(AntimatterTestBuilder.blastsNear(helper, gen, NEAR) == 0, "with nothing annihilated");
            trap.setStored(0);
            generator.setStored(0);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = PenningTrapTests.BLASTS, timeoutTicks = 200)
    public static void aColdLoadedChamberWithoutEnergyQuenchesThenAnnihilates(GameTestHelper helper) {
        AntimatterTestBuilder.listen();
        helper.setBlock(GEN, AntimatterRegistry.ANNIHILATION_GENERATOR.get().defaultBlockState()
                .setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.EAST));
        int[] quenchedAt = {-1};
        helper.runAfterDelay(3, () -> {
            AnnihilationGeneratorBlockEntity generator = at(helper);
            helper.assertTrue(generator != null, "the generator should be there");
            // As if a chunk had been saved with a cold chamber holding antimatter and no energy.
            generator.setStored(0.5);
        });
        helper.onEachTick(() -> {
            AnnihilationGeneratorBlockEntity generator = at(helper);
            if (generator != null && quenchedAt[0] < 0 && generator.chamberState() == TrapState.QUENCHING) {
                quenchedAt[0] = (int) helper.getTick();
                helper.assertTrue(generator.fieldFailing() && "quenching".equals(generator.statusId()), "goggles warn of the quench");
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(quenchedAt[0] > 0, "waiting for the quench");
            helper.assertFalse(helper.getBlockState(GEN).is(AntimatterRegistry.ANNIHILATION_GENERATOR.get()),
                    "waiting for the annihilation");
            helper.assertTrue(helper.getTick() - quenchedAt[0] >= AnnihilationModel.CHAMBER.quenchTicks() - 3,
                    "after the chamber's quench, " + (helper.getTick() - quenchedAt[0]) + " ticks");
            helper.assertTrue(AntimatterTestBuilder.blastsNear(helper, GEN, NEAR) == 1, "in one annihilation");
        });
    }

    // ------------------------------------------------------------------ saving

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 100)
    public static void aSavedGeneratorComesBackLitWithItsModeAndCharge(GameTestHelper helper) {
        AntimatterTestBuilder.fedTrap(helper, TRAP);
        generator(helper, AnnihilationModel.Mode.SPLIT);
        helper.runAfterDelay(5, () -> {
            PenningTrapBlockEntity trap = AntimatterTestBuilder.trap(helper, TRAP);
            helper.assertTrue(trap != null, "a fed trap beside the generator");
            trap.fill(5);
        });
        helper.runAfterDelay(50, () -> {
            PenningTrapBlockEntity trap = AntimatterTestBuilder.trap(helper, TRAP);
            AnnihilationGeneratorBlockEntity generator = at(helper);
            helper.assertTrue(trap != null && generator != null && generator.lit(), "a lit generator");
            BlockPos abs = helper.absolutePos(GEN);
            // What a chunk saved with a running generator reads back, into a block entity not yet in the level.
            AnnihilationGeneratorBlockEntity copy = new AnnihilationGeneratorBlockEntity(AntimatterRegistry.GENERATOR_ENTITY.get(), abs,
                    helper.getLevel().getBlockState(abs));
            copy.loadWithComponents(generator.saveWithoutMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
            helper.assertTrue(copy.mode() == AnnihilationModel.Mode.SPLIT, "the mode comes back: " + copy.mode());
            helper.assertTrue(copy.lit() && Math.abs(copy.stored() - generator.stored()) < 1e-12, "lit, with its charge");
            helper.assertTrue(copy.fieldBuffer().getEnergyStored() == generator.fieldBuffer().getEnergyStored(), "and its field buffer");
            helper.assertTrue(copy.powered(), "so it holds what is put in on its first tick");
            leaveEmpty(trap, generator);
            helper.succeed();
        });
    }
}
