package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.accelerator.physics.InjectorModel;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterConfig;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterRegistry;
import org.papiricoh.create_nuclearindustry.antimatter.injector.AntiprotonInjectorBlock;
import org.papiricoh.create_nuclearindustry.antimatter.injector.AntiprotonInjectorBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakGeometry;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakShellRole;
import org.papiricoh.create_nuclearindustry.fusion.physics.PlasmaModel;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The antiproton injector on a tokamak's top shell: in a heating plasma that is held and dense
 * enough it fires one dose ({@code antimatter.ignitionDose}) and the plasma ignites without any
 * neutral beam; without antimatter, or without the Forge Energy for a shot, it does nothing.
 *
 * <p>The tokamak gets no auxiliary heating at all (0 MW of beams), so it would sit in
 * {@link TokamakPhase#HEATING} forever: whatever ignites it is the injector. Each injector has a
 * creative generator on top, its only free face.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class AntiprotonInjectorTests {
    private static final int RADIUS = 4;
    /** Ticks the plasma heats with the injector safe before the test arms it. */
    private static final int HEATING_FIRST = 40;

    /** A top-shell cell above the plasma, {@code dx, dz} from the axis. */
    private static BlockPos topShell(GameTestHelper helper, int dx, int dz) {
        helper.assertTrue(TokamakGeometry.of(RADIUS).classify(dx, TokamakGeometry.HALF_HEIGHT, dz) == TokamakGeometry.Cell.SHELL,
                "(" + dx + ", " + dz + ") is on the top shell");
        return TokamakTestBuilder.CENTER.offset(dx, TokamakGeometry.HALF_HEIGHT, dz);
    }

    /** An injector facing up in place of a blanket, with a creative generator on its free face. */
    private static void injector(GameTestHelper helper, BlockPos pos, boolean fed) {
        AntimatterTestBuilder.listen();
        helper.setBlock(pos, AntimatterRegistry.ANTIPROTON_INJECTOR.get().defaultBlockState()
                .setValue(AntiprotonInjectorBlock.FACING, Direction.UP));
        if (fed) {
            AntimatterTestBuilder.generator(helper, pos.above());
        }
    }

    private static AntiprotonInjectorBlockEntity at(GameTestHelper helper, BlockPos pos) {
        return AcceleratorTestBuilder.entity(helper, pos, AntiprotonInjectorBlockEntity.class);
    }

    /** A tokamak with no auxiliary heating, fuelled and started (pump-down and cool-down skipped). */
    private static FusionReactorBlockEntity unheatedTokamak(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, RADIUS, true, 256);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, RADIUS);
        TokamakTestBuilder.makeStartable(helper, tokamak, RADIUS, 0);
        return tokamak;
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA, timeoutTicks = 1600)
    public static void aDoseIgnitesAHeatingPlasma(GameTestHelper helper) {
        BlockPos pos = topShell(helper, -RADIUS, 0);
        FusionReactorBlockEntity tokamak = unheatedTokamak(helper);
        injector(helper, pos, true);
        InjectorModel.Params p = AntimatterConfig.injector();
        double loaded = 2 * p.dose();
        AtomicInteger heatingTicks = new AtomicInteger();
        AtomicBoolean armed = new AtomicBoolean(false);
        AtomicBoolean burned = new AtomicBoolean(false);
        helper.runAfterDelay(20, () -> {
            AntiprotonInjectorBlockEntity injector = at(helper, pos);
            helper.assertTrue(tokamak.isFormed(), "the tokamak forms with the injector in its shell");
            helper.assertTrue(tokamak.getStructure().map(s -> s.count(TokamakShellRole.ANTIPROTON_INJECTOR)).orElse(0) == 1,
                    "as its antiproton injector");
            helper.assertTrue(injector != null && injector.powered(), "the creative generator raises its field");
            injector.setArming(InjectorModel.Arming.SAFE);
            injector.setStored(loaded);
            TokamakTestBuilder.fuel(tokamak, 8000, 8000, 16000);
            TokamakTestBuilder.primeAndStart(tokamak);
        });
        helper.onEachTick(() -> {
            AntiprotonInjectorBlockEntity injector = at(helper, pos);
            if (injector == null) {
                return;
            }
            if (!armed.get() && tokamak.phase() == TokamakPhase.HEATING && heatingTicks.incrementAndGet() >= HEATING_FIRST) {
                helper.assertTrue(tokamak.getPhysics().model().getTemperature() < PlasmaModel.IGNITION_TEMP,
                        "with no heating the plasma stays below ignition: " + tokamak.getPhysics().model().getTemperature());
                helper.assertTrue(injector.shots() == 0 && injector.verdict() == InjectorModel.Verdict.SAFE,
                        "a safe injector holds its fire: " + injector.verdict());
                injector.setArming(InjectorModel.Arming.AUTOMATIC);
                armed.set(true);
            }
            if (tokamak.phase() == TokamakPhase.BURN) {
                burned.set(true);
            }
        });
        helper.succeedWhen(() -> {
            AntiprotonInjectorBlockEntity injector = at(helper, pos);
            helper.assertTrue(injector != null, "the injector should be there");
            helper.assertTrue(armed.get(), "waiting for the heating plasma, at " + tokamak.phase());
            helper.assertTrue(injector.ignitions() >= 1, "waiting for the shot: " + injector.verdict());
            helper.assertTrue(burned.get(), "the plasma ignites and burns, at " + tokamak.phase());
            helper.assertTrue(injector.shots() == 1, "one shot was enough: " + injector.shots());
            helper.assertTrue(Math.abs(injector.stored() - (loaded - p.dose())) < 1e-6,
                    "and it spent one dose of " + p.dose() + " ng: " + injector.stored());
            helper.assertTrue(injector.coolingDown() || injector.verdict() == InjectorModel.Verdict.IGNITED
                            || injector.verdict() == InjectorModel.Verdict.WAITING,
                    "it does not fire again into a burning plasma: " + injector.verdict());
            injector.setStored(0);
            TokamakTestBuilder.leaveSafe(tokamak);
        });
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA, timeoutTicks = 1600)
    public static void withoutAntimatterOrEnergyItDoesNothing(GameTestHelper helper) {
        BlockPos empty = topShell(helper, -RADIUS, 0);
        BlockPos unpowered = topShell(helper, 0, -RADIUS);
        FusionReactorBlockEntity tokamak = unheatedTokamak(helper);
        injector(helper, empty, true);
        injector(helper, unpowered, true);
        InjectorModel.Params p = AntimatterConfig.injector();
        AtomicInteger heatingTicks = new AtomicInteger();
        AtomicBoolean loaded = new AtomicBoolean(false);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(tokamak.isFormed(), "the tokamak forms with both injectors");
            helper.assertTrue(tokamak.getStructure().map(s -> s.count(TokamakShellRole.ANTIPROTON_INJECTOR)).orElse(0) == 2,
                    "both are its antiproton injectors");
            AntiprotonInjectorBlockEntity second = at(helper, unpowered);
            helper.assertTrue(second != null && second.powered(), "the second has its field");
            second.setStored(2 * p.dose());
            loaded.set(true);
            TokamakTestBuilder.fuel(tokamak, 8000, 8000, 16000);
            TokamakTestBuilder.primeAndStart(tokamak);
        });
        helper.onEachTick(() -> {
            AntiprotonInjectorBlockEntity second = at(helper, unpowered);
            if (loaded.get() && second != null) {
                // Enough for its field, never enough for a shot.
                second.buffer().set(p.shotFe() - 1);
            }
            if (tokamak.phase() == TokamakPhase.HEATING) {
                heatingTicks.incrementAndGet();
            }
        });
        helper.succeedWhen(() -> {
            AntiprotonInjectorBlockEntity first = at(helper, empty);
            AntiprotonInjectorBlockEntity second = at(helper, unpowered);
            helper.assertTrue(first != null && second != null, "both injectors should be there");
            helper.assertTrue(heatingTicks.get() >= 100, "waiting for 5 s of heating, at " + tokamak.phase());
            helper.assertTrue(first.shots() == 0 && first.verdict() == InjectorModel.Verdict.NO_DOSE,
                    "without antimatter it waits for a dose: " + first.verdict());
            helper.assertTrue(second.shots() == 0 && second.verdict() == InjectorModel.Verdict.NO_POWER,
                    "without the energy for a shot it waits for power: " + second.verdict());
            helper.assertTrue(Math.abs(second.stored() - 2 * p.dose()) < 1e-9, "and keeps its doses");
            helper.assertTrue(tokamak.phase() == TokamakPhase.HEATING, "the plasma never ignites: " + tokamak.phase());
            second.setStored(0);
            TokamakTestBuilder.leaveSafe(tokamak);
        });
    }
}
