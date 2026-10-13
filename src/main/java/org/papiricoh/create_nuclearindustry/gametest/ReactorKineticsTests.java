package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.physics.FissionConfig;
import org.papiricoh.create_nuclearindustry.reactor.physics.FuelKind;
import org.papiricoh.create_nuclearindustry.reactor.physics.ReactorPhysicsSimulator;

/**
 * The fission core's kinetics in a real reactor (see {@link FissionTestBuilder}): pulling a control
 * rod out gives a new, steady power instead of a runaway, but fresh fuel holds far more reactivity
 * than its heat takes back, so every rod out damages it; losing the feed water stops the chain
 * reaction, old saves load cool, and the rated power grows with the fuel.
 *
 * <p>The test reactor has a single channel of three rod blocks, so each block is worth thousands of
 * pcm: with 5% fuel one block out leaves it subcritical and two take it past its rated power. The
 * steady runs load fuel a little richer, which one block out holds at part power.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class ReactorKineticsTests {
    /** The control rod column of the test reactor, top block first. */
    private static BlockPos rod(int fromTop) {
        return FissionTestBuilder.ORIGIN.offset(1, FissionTestBuilder.SIZE - 2 - fromTop, 3);
    }

    /**
     * Opens the lid over the control rod column, as pulling rods out in play does: the rods leave
     * upwards. Under a closed lid a column without rods is an empty column, not a channel.
     */
    private static void openLid(GameTestHelper helper) {
        helper.setBlock(FissionTestBuilder.ORIGIN.offset(1, FissionTestBuilder.SIZE - 1, 3), Blocks.AIR);
    }

    /** Enrichment the test reactor runs at part power on with its top rod block out. */
    private static final float PART_POWER_ENRICHMENT = 9f;

    /** Six assemblies in a reactor fed with water and drained of steam every tick. */
    private static ReactorBlockEntity fedReactor(GameTestHelper helper, float enrichment) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            for (int i = 0; i < 6; i++) {
                reactor.getPhysicsSimulator().loadFuelAssembly(enrichment);
            }
        });
        helper.onEachTick(() -> {
            if (helper.getTick() >= 5) {
                reactor.fillCoolant(new FluidStack(Fluids.WATER, 400), IFluidHandler.FluidAction.EXECUTE);
                reactor.drainSteam(2000, IFluidHandler.FluidAction.EXECUTE);
            }
        });
        return reactor;
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 3200)
    public static void pullingARodSettlesOnANewPower(GameTestHelper helper) {
        ReactorBlockEntity reactor = fedReactor(helper, PART_POWER_ENRICHMENT);
        helper.runAfterDelay(10, () -> {
            openLid(helper);
            helper.setBlock(rod(0), Blocks.AIR);
        });
        double[] earlier = new double[1];
        double[] peak = new double[1];
        helper.onEachTick(() -> peak[0] = Math.max(peak[0], reactor.getPhysicsSimulator().getCoreTemperature()));
        helper.runAfterDelay(2800, () -> earlier[0] = reactor.getPhysicsSimulator().getRelativePower());
        helper.runAfterDelay(3000, () -> {
            ReactorPhysicsSimulator physics = reactor.getPhysicsSimulator();
            double power = physics.getRelativePower();
            helper.assertTrue(power > 0.1 && power < 1.0, "with its top rod block out the core runs at part power: " + power);
            // Xenon still builds up slowly for hours; the chain reaction itself has settled.
            helper.assertTrue(Math.abs(power - earlier[0]) < 0.05 * power, "and has settled: " + earlier[0] + " -> " + power);
            helper.assertTrue(peak[0] < 1500, "without a runaway: peak fuel " + peak[0] + " °C");
            helper.assertTrue(physics.getCoreDamage() == 0, "and without damage");
            helper.assertTrue(reactor.getSteamAmount() >= 0 && physics.getBoiledHeat() > 0, "boiling its water into steam");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 6000)
    public static void everyRodOutOfAFreshCoreDamagesIt(GameTestHelper helper) {
        ReactorBlockEntity reactor = fedReactor(helper, 5f);
        helper.runAfterDelay(10, () -> {
            openLid(helper);
            for (int i = 0; i < 3; i++) {
                helper.setBlock(rod(i), Blocks.AIR);
            }
        });
        boolean[] reinserted = {false};
        helper.onEachTick(() -> {
            // Put the rods back as soon as the fuel is hurt, before it melts down in the arena.
            if (!reinserted[0] && reactor.getPhysicsSimulator().getCoreDamage() > 0) {
                reinserted[0] = true;
                for (int i = 0; i < 3; i++) {
                    helper.setBlock(rod(i), AllNuclearBlocks.CONTROL_ROD.get());
                }
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(reinserted[0], "fresh fuel holds far more reactivity than its heat takes back: the fuel is damaged");
            helper.assertTrue(reactor.getPhysicsSimulator().getRelativePower() < 0.01, "and the rods shut it down again: "
                    + reactor.getPhysicsSimulator().getRelativePower());
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 4000)
    public static void losingTheWaterStopsTheChainReaction(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            for (int i = 0; i < 6; i++) {
                reactor.getPhysicsSimulator().loadFuelAssembly(PART_POWER_ENRICHMENT);
            }
            reactor.fillCoolant(new FluidStack(Fluids.WATER, 6_000), IFluidHandler.FluidAction.EXECUTE);
            openLid(helper);
            helper.setBlock(rod(0), Blocks.AIR);
        });
        helper.onEachTick(() -> reactor.drainSteam(2000, IFluidHandler.FluidAction.EXECUTE));
        boolean[] ran = {false};
        helper.onEachTick(() -> ran[0] |= reactor.getPhysicsSimulator().getRelativePower() > 0.05);
        helper.succeedWhen(() -> {
            helper.assertTrue(ran[0], "it ran on its water first");
            helper.assertTrue(reactor.getCoolantAmount() == 0, "until it boiled it all away");
            helper.assertTrue(reactor.getPhysicsSimulator().getRelativePower() < 0.01,
                    "then the dry core stopped its chain reaction: " + reactor.getPhysicsSimulator().getRelativePower());
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void anOldHotSaveLoadsCool(GameTestHelper helper) {
        CompoundTag old = new CompoundTag();
        old.putInt("uraniumRodCount", 6);
        old.putInt("controlRodCount", 3);
        old.putDouble("coreTemperature", 3500);
        old.putDouble("neutronLevel", 1800);
        old.putDouble("decayHeat", 2.0);
        ReactorPhysicsSimulator physics = new ReactorPhysicsSimulator(6, 3);
        physics.deserializeNBT(old);
        helper.assertTrue(physics.getCoreTemperature() <= 1500, "a core saved hot by the old model loads cooler: "
                + physics.getCoreTemperature());
        helper.assertTrue(physics.getRelativePower() <= 1.0, "and at no more than its rated power: " + physics.getRelativePower());
        CompoundTag saved = new CompoundTag();
        physics.serializeNBT(saved);
        ReactorPhysicsSimulator again = new ReactorPhysicsSimulator(6, 3);
        again.deserializeNBT(saved);
        helper.assertTrue(Math.abs(again.getCoreTemperature() - physics.getCoreTemperature()) < 1e-6, "and saves as it is now");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void ratedPowerGrowsWithTheFuel(GameTestHelper helper) {
        ReactorPhysicsSimulator small = new ReactorPhysicsSimulator(6, 3);
        ReactorPhysicsSimulator large = new ReactorPhysicsSimulator(60, 30);
        ReactorPhysicsSimulator mox = new ReactorPhysicsSimulator(6, 3);
        for (int i = 0; i < 6; i++) {
            small.loadFuelAssembly(5f);
            mox.loadFuelAssembly(FuelKind.MOX, FuelKind.MOX_EQUIVALENT_ENRICHMENT);
        }
        for (int i = 0; i < 60; i++) {
            large.loadFuelAssembly(5f);
        }
        helper.assertTrue(Math.abs(small.getRatedPower() - 6 * FissionConfig.ratedPowerPerRod) < 1e-6,
                "six rods are rated " + small.getRatedPower() + " MW");
        helper.assertTrue(Math.abs(large.getRatedPower() - 10 * small.getRatedPower()) < 1e-6, "ten times the rods, ten times the power");
        helper.assertTrue(mox.getRatedPower() > small.getRatedPower(), "MOX runs hotter");
        helper.succeed();
    }
}
