package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.attachment.AttachmentInternals;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.DoseReading;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;

@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class DoseHudTests {

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void lifetimeTotalNeverFalls(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().add(new ItemStack(AllNuclearItems.RADIOACTIVE_WASTE.get(), 10));
        RadiationHandler.update(player);
        float total = player.getData(AllNuclearRadiation.TOTAL_DOSE);
        helper.assertTrue(Math.abs(total - 60f) < 0.5f, "a second next to 60/s should total ~60, got " + total);
        player.getInventory().clearContent();
        RadiationHandler.update(player);
        RadiationHandler.update(player);
        helper.assertTrue(player.getData(AllNuclearRadiation.DOSE) < 60f, "the current dose should decay");
        helper.assertTrue(player.getData(AllNuclearRadiation.TOTAL_DOSE) == total, "the lifetime total must not decay");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void lifetimeTotalSurvivesDeath(GameTestHelper helper) {
        Player before = helper.makeMockPlayer(GameType.SURVIVAL);
        before.setData(AllNuclearRadiation.DOSE, 50f);
        before.setData(AllNuclearRadiation.TOTAL_DOSE, 123f);
        Player after = helper.makeMockPlayer(GameType.SURVIVAL);
        AttachmentInternals.copyEntityAttachments(before, after, true);
        helper.assertTrue(after.getData(AllNuclearRadiation.TOTAL_DOSE) == 123f, "the total should carry over death");
        helper.assertTrue(after.getData(AllNuclearRadiation.DOSE) == 0f, "the current dose should still clear on death");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void hudOnlyChangesWhenItsRoundedValuesDo(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        RadiationHandler.publish(player, 0.5f, 10.2f, 20f);
        DoseReading first = player.getExistingDataOrNull(AllNuclearRadiation.DOSE_HUD);
        helper.assertTrue(first != null && first.visible(), "an exposed player should get a reading");
        RadiationHandler.publish(player, 0.501f, 10.4f, 20.3f);
        helper.assertTrue(player.getExistingDataOrNull(AllNuclearRadiation.DOSE_HUD) == first,
                "a change too small to show must not replace (and resend) the reading");
        RadiationHandler.publish(player, 0.52f, 10.4f, 20.3f);
        helper.assertTrue(player.getExistingDataOrNull(AllNuclearRadiation.DOSE_HUD) != first, "a visible change should update it");
        RadiationHandler.publish(player, 0f, 0f, 20.3f);
        helper.assertTrue(player.getExistingDataOrNull(AllNuclearRadiation.DOSE_HUD) == null,
                "with no rate and no dose the HUD should go, even with a lifetime total");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void stageAndBarFollowTheThresholds(GameTestHelper helper) {
        DoseReading mild = DoseReading.of(0, 150, 0);
        helper.assertTrue(mild.stage() == 1 && mild.bar() == 25, "150 rad should be nausea at a quarter bar, got " + mild);
        DoseReading sick = DoseReading.of(0, 650, 0);
        helper.assertTrue(sick.stage() == 3 && sick.bar() == 100, "650 rad should be sickness at a full bar, got " + sick);
        helper.succeed();
    }
}
