package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorRegistry;
import org.papiricoh.create_nuclearindustry.accelerator.api.BeamPhase;
import org.papiricoh.create_nuclearindustry.accelerator.api.BeamReadings;
import org.papiricoh.create_nuclearindustry.accelerator.api.BeamTripCause;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.AcceleratorMachineBlockEntity;
import org.papiricoh.create_nuclearindustry.energy.CreativeRfGeneratorBlockEntity;
import org.papiricoh.create_nuclearindustry.energy.EnergyRegistry;
import org.papiricoh.create_nuclearindustry.energy.FeSource;

/**
 * The Creative RF Generator pushes its set rate into every block around it and gives cables as much
 * as they pull; it cannot be broken in survival. {@link FeSource}, the store real generators will
 * use, pushes what it holds and never takes energy in.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class CreativeRfGeneratorTests {
    private static final BlockPos GENERATOR = new BlockPos(6, 3, 6);
    private static final BlockPos KLYSTRON = GENERATOR.below();

    /** What a running accelerator tells its machines. */
    private static BeamReadings running() {
        BeamReadings n = BeamReadings.NONE;
        return new BeamReadings(true, false, BeamPhase.RF_ON, BeamTripCause.NONE, 0, 0, 0, 0, 0, 0, 0, -7, 1, 1, 1, 0, 0, 0, 1, 1,
                0, 0, n.magnetTemperature(), true, 0, 10);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void itIsAnEndlessSourceThatTakesNothing(GameTestHelper helper) {
        helper.setBlock(GENERATOR, EnergyRegistry.CREATIVE_RF_GENERATOR.get());
        CreativeRfGeneratorBlockEntity generator = AntimatterTestBuilder.generatorAt(helper, GENERATOR);
        helper.assertTrue(generator != null, "the generator should be there");
        helper.assertTrue(generator.rate() == CreativeRfGeneratorBlockEntity.DEFAULT_RATE, "100,000 FE/t by default");
        IEnergyStorage energy = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(GENERATOR), Direction.WEST);
        helper.assertTrue(energy != null, "cables reach it");
        helper.assertTrue(energy.canExtract() && !energy.canReceive(), "out, never in");
        helper.assertTrue(energy.extractEnergy(1_000_000, false) == 1_000_000 && energy.extractEnergy(1_000_000, false) == 1_000_000,
                "it never runs dry");
        helper.assertTrue(energy.receiveEnergy(500, false) == 0, "and takes nothing");
        helper.assertTrue(helper.getBlockState(GENERATOR).getDestroySpeed(helper.getLevel(), helper.absolutePos(GENERATOR)) < 0,
                "it cannot be broken in survival");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 60)
    public static void itDrivesAKlystronAtItsRateAndStopsAtZero(GameTestHelper helper) {
        helper.setBlock(KLYSTRON, AcceleratorRegistry.KLYSTRON.get());
        helper.setBlock(GENERATOR, EnergyRegistry.CREATIVE_RF_GENERATOR.get());
        AcceleratorMachineBlockEntity klystron = AcceleratorTestBuilder.entity(helper, KLYSTRON, AcceleratorMachineBlockEntity.class);
        helper.assertTrue(klystron != null, "the klystron should be there");
        klystron.observe(running());
        helper.runAfterDelay(5, () -> {
            CreativeRfGeneratorBlockEntity generator = AntimatterTestBuilder.generatorAt(helper, GENERATOR);
            helper.assertTrue(generator != null, "the generator should be there");
            helper.assertTrue(klystron.drive() > 0.99, "full drive on the generator's energy alone: " + klystron.drive());
            helper.assertTrue(generator.lastPushed() > 0, "the generator pushed energy into it");
            generator.setRate(0);
        });
        helper.runAfterDelay(8, () -> {
            CreativeRfGeneratorBlockEntity generator = AntimatterTestBuilder.generatorAt(helper, GENERATOR);
            helper.assertTrue(generator != null && generator.lastPushed() == 0, "at rate zero it pushes nothing");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 40)
    public static void anFeSourcePushesWhatItHolds(GameTestHelper helper) {
        helper.setBlock(KLYSTRON, AcceleratorRegistry.KLYSTRON.get());
        int[] changes = {0};
        FeSource source = new FeSource(1000, 1000, () -> changes[0]++);
        helper.assertTrue(source.receiveEnergy(100, false) == 0 && !source.canReceive(), "nothing pushes into a source");
        helper.assertTrue(source.generate(1500) == 1000, "it fills up to its capacity");
        int sent = source.pushToNeighbours(helper.getLevel(), helper.absolutePos(GENERATOR), 300);
        helper.assertTrue(sent > 0 && sent <= 300, "the klystron below took up to one side's share: " + sent);
        helper.assertTrue(source.getEnergyStored() == 1000 - sent, "and the source holds the rest");
        helper.assertTrue(source.extractEnergy(50, false) == 50 && source.getEnergyStored() == 950 - sent, "cables may pull from it");
        helper.assertTrue(changes[0] >= 3, "every change is reported to its machine");
        helper.succeed();
    }
}
