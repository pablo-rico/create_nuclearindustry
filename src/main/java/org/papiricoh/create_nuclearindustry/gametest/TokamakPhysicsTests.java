package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.physics.TokamakPhysicsSimulator;

@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class TokamakPhysicsTests {

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA, timeoutTicks = 900)
    public static void deuteriumTritiumIgnitesAndMakesPlasmaSteam(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, 4, true, 256);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, 4);
        helper.runAfterDelay(5, () -> fuel(tokamak, 4000, 4000, 16000));
        helper.succeedWhen(() -> {
            helper.assertTrue(tokamak.getPhysics().getMode() == TokamakPhysicsSimulator.Mode.DT,
                    "should burn D-T, plasma at " + Math.round(tokamak.getPhysics().getPlasmaTemperature())
                            + " confinement " + tokamak.getPhysics().getConfinement());
            helper.assertTrue(tokamak.getPlasmaSteamTank().getFluidAmount() > 0, "plasma steam should be produced");
        });
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA, timeoutTicks = 900)
    public static void deuteriumAloneBreedsTritium(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, 4, true, 256);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, 4);
        helper.runAfterDelay(5, () -> fuel(tokamak, 8000, 0, 16000));
        helper.succeedWhen(() -> helper.assertTrue(tokamak.getTritiumTank().getFluidAmount() > 0,
                "D-D burning should breed tritium, plasma at " + Math.round(tokamak.getPhysics().getPlasmaTemperature())
                        + " mode " + tokamak.getPhysics().getMode()));
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
        boolean explosions = Config.fusionDisruptionExplosions;
        Config.fusionDisruptionExplosions = false;
        helper.runAfterDelay(5, () -> fuel(tokamak, 8000, 8000, 0));
        helper.onEachTick(() -> {
            if (tokamak.getPhysics().getPlasmaTemperature() > TokamakPhysicsSimulator.DANGER_TEMP) {
                helper.runAfterDelay(80, () -> {
                    Config.fusionDisruptionExplosions = explosions;
                    helper.assertTrue(tokamak.getTritiumTank().isEmpty(), "a disruption should lose the tritium inventory");
                    helper.succeed();
                });
            }
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

    private static void fuel(FusionReactorBlockEntity tokamak, int deuterium, int tritium, int water) {
        if (deuterium > 0) {
            tokamak.fillFuel(new FluidStack(AllNuclearFluids.DEUTERIUM.get(), deuterium), IFluidHandler.FluidAction.EXECUTE);
        }
        if (tritium > 0) {
            tokamak.fillFuel(new FluidStack(AllNuclearFluids.TRITIUM.get(), tritium), IFluidHandler.FluidAction.EXECUTE);
        }
        if (water > 0) {
            tokamak.getCoolantTank().fill(new FluidStack(Fluids.WATER, water), IFluidHandler.FluidAction.EXECUTE);
        }
    }
}
