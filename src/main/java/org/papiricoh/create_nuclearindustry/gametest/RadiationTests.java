package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.FalloutData;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;

@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class RadiationTests {

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void spentFuelAccumulatesDose(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().add(new ItemStack(AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL.get(), 4));
        RadiationHandler.update(player);
        RadiationHandler.update(player);
        float dose = player.getData(AllNuclearRadiation.DOSE);
        helper.assertTrue(Math.abs(dose - 32f) < 0.5f, "four spent assemblies for two seconds should give ~32, got " + dose);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void enrichmentScalesTheHazard(GameTestHelper helper) {
        double natural = RadiationHandler.stackRate(UraniumItem.withEnrichment(new ItemStack(AllNuclearItems.URANIUM.get()), 0.7f));
        double weapons = RadiationHandler.stackRate(UraniumItem.withEnrichment(new ItemStack(AllNuclearItems.URANIUM.get()), 90f));
        helper.assertTrue(natural < 0.02, "natural uranium should barely register, got " + natural);
        helper.assertTrue(weapons > 50 * natural, "weapons-grade should be far hotter, got " + weapons);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void fullHazmatSuitBlocksMostRadiation(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().add(new ItemStack(AllNuclearItems.RADIOACTIVE_WASTE.get(), 10));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(AllNuclearItems.HAZMAT_HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(AllNuclearItems.HAZMAT_CHESTPLATE.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(AllNuclearItems.HAZMAT_LEGGINGS.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(AllNuclearItems.HAZMAT_BOOTS.get()));
        helper.assertTrue(Math.abs(RadiationHandler.shielding(player) - 0.95f) < 1e-4, "a full suit should stop 95%");
        RadiationHandler.update(player);
        float rate = player.getData(AllNuclearRadiation.RATE);
        helper.assertTrue(rate < 3.5f, "60/s of waste should drop to ~3/s in a suit, got " + rate);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void disabledRadiationDoesNothing(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().add(new ItemStack(AllNuclearItems.RADIOACTIVE_WASTE.get(), 64));
        boolean enabled = Config.radiationEnabled;
        try {
            Config.radiationEnabled = false;
            RadiationHandler.update(player);
        } finally {
            Config.radiationEnabled = enabled;
        }
        helper.assertTrue(player.getData(AllNuclearRadiation.DOSE) == 0f, "no dose when radiation is disabled");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void falloutIrradiatesNearbyPlayers(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(3, 2, 3)));
        double clean = RadiationHandler.exposureRate(player);
        FalloutData.contaminate(helper.getLevel(), player.blockPosition().offset(2, 0, 0), 40, 1200);
        double contaminated = RadiationHandler.exposureRate(player);
        // A strength-40 hot spot is halved by contamination.hotSpotScale; two blocks off it still makes you sick within a minute.
        helper.assertTrue(contaminated > clean + 10, "standing next to fallout should be dangerous, got " + contaminated);
        FalloutData.get(helper.getLevel()).sources().clear();
        helper.succeed();
    }
}
