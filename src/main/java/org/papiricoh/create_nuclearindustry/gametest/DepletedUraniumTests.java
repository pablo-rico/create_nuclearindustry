package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;
import org.papiricoh.create_nuclearindustry.radiation.RadiationTransmission;

/** Depleted uranium is the best radiation shield, as blocks and as heavy armour. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class DepletedUraniumTests {

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void duBlockPassesTenPercent(GameTestHelper helper) {
        double t = RadiationTransmission.of(AllNuclearBlocks.DEPLETED_URANIUM_BLOCK.get().defaultBlockState());
        helper.assertTrue(Math.abs(t - 0.1) < 1e-6, "a depleted uranium block passes 10%, got " + t);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void duWallCutsAPointSourceToATenth(GameTestHelper helper) {
        Vec3 source = Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 3, 6)));
        Vec3 body = Vec3.atCenterOf(helper.absolutePos(new BlockPos(8, 3, 6)));
        double open = RadiationHandler.pointExposure(helper.getLevel(), source, 40, body, null);
        for (int y = 2; y <= 4; y++) {
            for (int z = 5; z <= 7; z++) {
                helper.setBlock(new BlockPos(5, y, z), AllNuclearBlocks.DEPLETED_URANIUM_BLOCK.get());
            }
        }
        double walled = RadiationHandler.pointExposure(helper.getLevel(), source, 40, body, null);
        helper.assertTrue(walled <= 0.12 * open, "a depleted uranium wall should cut it to a tenth, " + open + " -> " + walled);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void duBlockRecipeNeedsDepletedMetal(GameTestHelper helper) {
        ShapedRecipe recipe = (ShapedRecipe) helper.getLevel().getRecipeManager()
                .byKey(id("crafting/depleted_uranium_block")).orElseThrow().value();
        var ingredient = recipe.getIngredients().get(0);
        helper.assertTrue(ingredient.test(UraniumItem.withEnrichment(new ItemStack(AllNuclearItems.URANIUM.get()), 0.3f)),
                "depleted metal (0.3%) should match");
        helper.assertFalse(ingredient.test(new ItemStack(AllNuclearItems.URANIUM.get())),
                "natural uranium (0.7%) must not match");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void crushingKeepsTheMetalDepleted(GameTestHelper helper) {
        ProcessingRecipe<?, ?> crushing = (ProcessingRecipe<?, ?>) helper.getLevel().getRecipeManager()
                .byKey(id("crushing/depleted_uranium_block")).orElseThrow().value();
        ItemStack out = crushing.getRollableResultsAsItemStacks().get(0);
        helper.assertTrue(out.is(AllNuclearItems.URANIUM.get()) && out.getCount() == 9
                        && Math.abs(UraniumItem.getEnrichment(out) - 0.3f) < 1e-4,
                "crushing a block gives back 9 depleted metal, got " + out + " at " + UraniumItem.getEnrichment(out));
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void fullDuArmorBeatsHazmat(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        wearDu(player);
        float shielding = RadiationHandler.shielding(player);
        helper.assertTrue(Math.abs(shielding - 0.98f) < 1e-4, "a full depleted uranium set stops 98%, got " + shielding);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void duArmorSlowsAndProtects(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        double baseSpeed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        wearDu(player);
        // A mock player never ticks, so apply the worn pieces' modifiers as equipping would.
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            player.getItemBySlot(slot).forEachModifier(slot, (attribute, modifier) -> {
                var instance = player.getAttribute(attribute);
                if (instance != null) {
                    instance.addTransientModifier(modifier);
                }
            });
        }
        double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        helper.assertTrue(speed <= 0.85 * baseSpeed && speed >= 0.8 * baseSpeed,
                "a full set slows by about a sixth, " + baseSpeed + " -> " + speed);
        helper.assertTrue(player.getArmorValue() == 20, "3+8+6+3 armour, got " + player.getArmorValue());
        helper.succeed();
    }

    private static void wearDu(Player player) {
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(AllNuclearItems.DEPLETED_URANIUM_HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(AllNuclearItems.DEPLETED_URANIUM_CHESTPLATE.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(AllNuclearItems.DEPLETED_URANIUM_LEGGINGS.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(AllNuclearItems.DEPLETED_URANIUM_BOOTS.get()));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }
}
