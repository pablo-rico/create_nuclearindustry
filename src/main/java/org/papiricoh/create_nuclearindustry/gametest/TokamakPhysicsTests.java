package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.fusion.FusionConfig;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;
import org.papiricoh.create_nuclearindustry.fusion.api.TripCause;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.physics.PlasmaModel;

/**
 * The plasma inside a running tokamak. Since the startup sequence, a tokamak only burns after a
 * start with vacuum pumps, cold coils and heating, so these tests stand in for the pumps, cryogenic
 * plant and heaters ({@link TokamakTestBuilder#makeStartable}) and skip the slow pump-down and
 * cool-down ({@link TokamakTestBuilder#primeAndStart}).
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class TokamakPhysicsTests {

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA, timeoutTicks = 900)
    public static void deuteriumTritiumIgnitesAndMakesPlasmaSteam(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, 4, true, 256);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, 4);
        TokamakTestBuilder.makeStartable(helper, tokamak, 4, 40);
        helper.runAfterDelay(20, () -> {
            TokamakTestBuilder.fuel(tokamak, 4000, 4000, 16000);
            helper.assertTrue(TokamakTestBuilder.primeAndStart(tokamak) == TripCause.NONE, "the start should be accepted");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(tokamak.phase() == TokamakPhase.BURN && tokamak.getPhysics().getMode() == PlasmaModel.Mode.DT,
                    "should burn D-T, phase " + tokamak.phase() + ", plasma at " + Math.round(tokamak.getPhysics().getPlasmaTemperature()));
            helper.assertTrue(tokamak.getPlasmaSteamTank().getFluidAmount() > 0, "plasma steam should be produced");
            helper.assertTrue(tokamak.readings().gain() > 1, "Q should be above 1, got " + tokamak.readings().gain());
            TokamakTestBuilder.leaveSafe(tokamak);
        });
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA, timeoutTicks = 900)
    public static void deuteriumAloneBreedsTritium(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, 4, true, 256);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, 4);
        TokamakTestBuilder.makeStartable(helper, tokamak, 4, 40);
        helper.runAfterDelay(20, () -> {
            TokamakTestBuilder.fuel(tokamak, 8000, 0, 16000);
            // Deuterium-only fuelling, so the bred tritium stays in its tank.
            tokamak.setDtRatio(1.0);
            TokamakTestBuilder.primeAndStart(tokamak);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(tokamak.getTritiumTank().getFluidAmount() > 0,
                    "D-D burning should breed tritium, plasma at " + Math.round(tokamak.getPhysics().getPlasmaTemperature())
                            + " mode " + tokamak.getPhysics().getMode() + " phase " + tokamak.phase());
            TokamakTestBuilder.leaveSafe(tokamak);
        });
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void noBlanketsMeansNoBreeding(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, 4, false, 256);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, 4);
        helper.succeedWhen(() -> {
            helper.assertTrue(tokamak.getStructure().isPresent(), "tokamak should form");
            helper.assertTrue(tokamak.getBlanketCount() == 0, "no blankets were placed");
            helper.assertTrue(tokamak.getPhysics().getTritiumBreedingRatio() == 0, "TBR must be zero without blankets");
        });
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA, timeoutTicks = 1200)
    public static void dryTokamakOverheatsAndDisrupts(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, 4, true, 256);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, 4);
        TokamakTestBuilder.makeStartable(helper, tokamak, 4, 40, new java.util.concurrent.atomic.AtomicBoolean(true), false);
        boolean explosions = FusionConfig.disruptionExplosions;
        FusionConfig.disruptionExplosions = false;
        helper.runAfterDelay(20, () -> {
            TokamakTestBuilder.fuel(tokamak, 8000, 8000, 0);
            TokamakTestBuilder.primeAndStart(tokamak);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(tokamak.tripCause() == TripCause.DISRUPTION, "a dry tokamak should disrupt, plasma at "
                    + Math.round(tokamak.getPhysics().getPlasmaTemperature()) + " phase " + tokamak.phase());
            FusionConfig.disruptionExplosions = explosions;
            helper.assertTrue(tokamak.getTritiumTank().isEmpty(), "a disruption should lose the tritium inventory");
        });
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void legacyPelletsBecomeFuel(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, 4, true, 0);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, 4);
        var registries = helper.getLevel().registryAccess();
        ItemStackHandler legacy = new ItemStackHandler(2);
        legacy.setStackInSlot(0, new ItemStack(AllNuclearItems.DT_FUEL_PELLET.get(), 2));
        CompoundTag tag = tokamak.saveWithoutMetadata(registries);
        tag.put("fuelInventory", legacy.serializeNBT(registries));
        tokamak.loadCustomOnly(tag, registries);
        helper.assertTrue(tokamak.getDeuteriumTank().getFluidAmount() == 500 && tokamak.getTritiumTank().getFluidAmount() == 500,
                "two pellets should give 500 mB of D and T");
        helper.succeed();
    }

    /** A controller saved by an older version with a burning plasma comes back offline, saying why. */
    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void burningPlasmaFromAnOldWorldShutsDown(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, 4, true, 0);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, 4);
        var registries = helper.getLevel().registryAccess();
        CompoundTag tag = tokamak.saveWithoutMetadata(registries);
        tag.remove("phase");
        CompoundTag physics = new CompoundTag();
        physics.putDouble("plasmaTemperature", 3000);
        physics.putDouble("fusionPower", 200);
        physics.putString("mode", "DT");
        tag.put("physics", physics);
        tokamak.loadCustomOnly(tag, registries);
        helper.assertTrue(tokamak.phase() == TokamakPhase.OFFLINE, "an old burning plasma should be offline, got " + tokamak.phase());
        helper.assertTrue(tokamak.tripCause() == TripCause.STRUCTURE, "and say why, got " + tokamak.tripCause());
        helper.assertTrue(tokamak.getPhysics().getPlasmaTemperature() <= PlasmaModel.AMBIENT_TEMP, "with its plasma gone");
        helper.succeed();
    }
}
