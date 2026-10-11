package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.display.ReactorReadout;
import org.papiricoh.create_nuclearindustry.reactor.physics.ReactorPhysicsSimulator;

/** Xenon-135 poisoning and decay heat in the fission core. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class ReactorPoisonTests {

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void runningBuildsDecayHeatAndXenon(GameTestHelper helper) {
        ReactorPhysicsSimulator physics = fuelled(0.4f);
        for (int i = 0; i < 6000; i++) {
            physics.tick();
            physics.applyExternalCooling(10_000.0, 0.0);
        }
        helper.assertTrue(physics.getDecayHeat() > 0.5, "a core that ran keeps decay heat, got " + physics.getDecayHeat());
        helper.assertTrue(physics.getXenon() > 0.01, "fission makes xenon, got " + physics.getXenon());
        helper.assertTrue(physics.getXenonPenalty() > 0.0, "xenon holds the core back");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void decayHeatKeepsAScrammedCoreHot(GameTestHelper helper) {
        ReactorPhysicsSimulator withDecay = fuelled(1.0f);
        withDecay.setFissionProducts(0.0, 0.0, 4.0);
        ReactorPhysicsSimulator without = fuelled(1.0f);
        for (ReactorPhysicsSimulator physics : new ReactorPhysicsSimulator[]{withDecay, without}) {
            physics.applyExternalHeat(980.0);
            for (int i = 0; i < 600; i++) {
                physics.scramTick();
            }
        }
        // The fuel gives its heat to the core's water, which holds at its boiling point while it boils.
        helper.assertTrue(without.getCoreTemperature() < 400.0, "with no decay heat the core cools to its water, got "
                + without.getCoreTemperature());
        helper.assertTrue(withDecay.getCoreTemperature() > without.getCoreTemperature() + 100.0,
                "decay heat keeps it hot: " + withDecay.getCoreTemperature() + " vs " + without.getCoreTemperature());
        helper.assertTrue(withDecay.getDecayHeat() < 4.0, "and fades while scrammed, got " + withDecay.getDecayHeat());
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void xenonHoldsBackARestart(GameTestHelper helper) {
        ReactorPhysicsSimulator poisoned = fuelled(0.5f);
        poisoned.setFissionProducts(1.0, 0.5, 0.0);
        ReactorPhysicsSimulator clean = fuelled(0.5f);
        for (int i = 0; i < 300; i++) {
            for (ReactorPhysicsSimulator physics : new ReactorPhysicsSimulator[]{poisoned, clean}) {
                physics.tick();
                physics.applyExternalCooling(10_000.0, 0.0);
            }
        }
        helper.assertTrue(poisoned.getNeutronLevel() < clean.getNeutronLevel() * 0.85,
                "the same rods give less power in a poisoned core: " + poisoned.getNeutronLevel() + " vs " + clean.getNeutronLevel());
        helper.assertTrue(poisoned.getReactivity() < clean.getReactivity(), "and its reactivity reads lower");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void xenonPoisoningCanBeTurnedOff(GameTestHelper helper) {
        boolean poisoning = Config.xenonPoisoning;
        try {
            Config.xenonPoisoning = false;
            ReactorPhysicsSimulator physics = fuelled(0.5f);
            physics.setFissionProducts(1.0, 1.0, 0.0);
            helper.assertTrue(physics.getXenonPenalty() == 0.0, "no penalty with poisoning off");
        } finally {
            Config.xenonPoisoning = poisoning;
        }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void fissionProductsSurviveSaving(GameTestHelper helper) {
        ReactorPhysicsSimulator physics = fuelled(0.5f);
        physics.setFissionProducts(0.6, 0.3, 2.5);
        CompoundTag tag = new CompoundTag();
        physics.serializeNBT(tag);
        ReactorPhysicsSimulator loaded = new ReactorPhysicsSimulator(0);
        loaded.deserializeNBT(tag);
        helper.assertTrue(loaded.getIodine() == 0.6 && loaded.getXenon() == 0.3 && loaded.getDecayHeat() == 2.5,
                "iodine, xenon and decay heat are saved");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void idleReactorReadsNoPoison(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.succeedWhen(() -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            helper.assertTrue(ReactorReadout.XENON.read(reactor).getString().equals("0%"), "a fresh core has no xenon");
            helper.assertTrue(ReactorReadout.DECAY_HEAT.read(reactor).getString().equals("0 MW"), "nor decay heat");
            helper.assertTrue(ReactorReadout.REACTIVITY.read(reactor).getString().equals("x1.00"), "a bare LEU core reads x1.00");
        });
    }

    private static ReactorPhysicsSimulator fuelled(float insertion) {
        ReactorPhysicsSimulator physics = new ReactorPhysicsSimulator(4, 4);
        for (int i = 0; i < 4; i++) {
            physics.loadFuelAssembly(5f);
        }
        physics.setControlRodInsertion(insertion);
        return physics;
    }
}
