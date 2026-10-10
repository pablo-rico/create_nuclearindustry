package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;

/** Potassium iodide cuts the dose taken; Prussian blue flushes the dose held. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class RadiationMedicineTests {

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void iodideHalvesTheDoseTaken(GameTestHelper helper) {
        Player bare = irradiated(helper);
        Player treated = irradiated(helper);
        treated.addEffect(new MobEffectInstance(AllNuclearRadiation.RADIOPROTECTION, 600));
        RadiationHandler.update(bare);
        RadiationHandler.update(treated);
        float bareRate = bare.getData(AllNuclearRadiation.RATE);
        float treatedRate = treated.getData(AllNuclearRadiation.RATE);
        helper.assertTrue(bareRate > 0, "the waste should irradiate");
        helper.assertTrue(Math.abs(treatedRate - bareRate * 0.5f) < 0.01f, "iodide halves the rate: " + treatedRate + " vs " + bareRate);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void prussianBlueFlushesDoseButNotTheTotal(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Player untreated = helper.makeMockPlayer(GameType.SURVIVAL);
        for (Player p : new Player[]{player, untreated}) {
            p.setData(AllNuclearRadiation.DOSE, 100.0f);
            p.setData(AllNuclearRadiation.TOTAL_DOSE, 100.0f);
        }
        player.addEffect(new MobEffectInstance(AllNuclearRadiation.RADIATION_PURGE, 600));
        RadiationHandler.update(player);
        RadiationHandler.update(untreated);
        float flushed = untreated.getData(AllNuclearRadiation.DOSE) - player.getData(AllNuclearRadiation.DOSE);
        helper.assertTrue(Math.abs(flushed - 15.0f) < 0.01f, "the purge flushes 15 a second on top of the usual change, got " + flushed);
        helper.assertTrue(player.getData(AllNuclearRadiation.TOTAL_DOSE).equals(untreated.getData(AllNuclearRadiation.TOTAL_DOSE)),
                "the lifetime total is untouched");
        for (int i = 0; i < 10; i++) {
            RadiationHandler.update(player);
        }
        helper.assertTrue(player.getData(AllNuclearRadiation.DOSE) == 0.0f, "and never goes below zero");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void untreatedPlayersAreUnchanged(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(RadiationHandler.absorption(player) == 1.0, "no iodide, full uptake");
        helper.assertTrue(RadiationHandler.purge(player) == 0.0f, "no purge");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void eatingAPillGivesItsEffect(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack pill = new ItemStack(AllNuclearItems.POTASSIUM_IODIDE.get());
        player.eat(helper.getLevel(), pill, pill.getFoodProperties(player));
        helper.assertTrue(player.hasEffect(AllNuclearRadiation.RADIOPROTECTION), "the pill gives radioprotection");
        helper.assertTrue(player.getEffect(AllNuclearRadiation.RADIOPROTECTION).getDuration() == 3600, "for three minutes");
        helper.succeed();
    }

    private static Player irradiated(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().add(new ItemStack(AllNuclearItems.RADIOACTIVE_WASTE.get(), 4));
        return player;
    }
}
