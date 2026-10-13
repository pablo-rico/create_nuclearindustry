package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.integration.cbc.CBCNuclearIntegration;

/** The depleted uranium AP shot, only with Create Big Cannons installed. CBC classes stay in a nested holder. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class CBCDepletedUraniumTests {

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void duShotIsHeavierThanTheApShot(GameTestHelper helper) {
        if (!CBCNuclearIntegration.isLoaded()) {
            helper.succeed();
            return;
        }
        CBCChecks.heavierThanApShot(helper);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void duShotBlockFiresItsOwnProjectile(GameTestHelper helper) {
        if (!CBCNuclearIntegration.isLoaded()) {
            helper.succeed();
            return;
        }
        CBCChecks.firesOwnProjectile(helper);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void duShotRecipeNeedsDepletedMetal(GameTestHelper helper) {
        if (!CBCNuclearIntegration.isLoaded()) {
            helper.succeed();
            return;
        }
        var recipe = helper.getLevel().getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "crafting/depleted_uranium_ap_shot"));
        helper.assertTrue(recipe.isPresent(), "the AP shot recipe should load with CBC");
        var ingredient = recipe.get().value().getIngredients().stream().filter(i -> !i.isEmpty()).findFirst().orElseThrow();
        helper.assertTrue(ingredient.test(UraniumItem.withEnrichment(new ItemStack(AllNuclearItems.URANIUM.get()), 0.3f)),
                "depleted metal should match");
        helper.assertFalse(ingredient.test(new ItemStack(AllNuclearItems.URANIUM.get())), "natural uranium must not");
        helper.succeed();
    }

    private static final class CBCChecks {
        static void heavierThanApShot(GameTestHelper helper) {
            net.minecraft.world.entity.EntityType<?> ours =
                    org.papiricoh.create_nuclearindustry.integration.cbc.CBCNuclearContent.DEPLETED_URANIUM_AP_SHOT_PROJECTILE.get();
            net.minecraft.world.entity.EntityType<?> theirs = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                    .get(ResourceLocation.fromNamespaceAndPath("createbigcannons", "ap_shot"));
            var handler = rbasamoyai.createbigcannons.index.CBCMunitionPropertiesHandlers.INERT_BIG_CANNON_PROJECTILE;
            var ourProps = handler.getPropertiesOf(ours);
            var theirProps = handler.getPropertiesOf(theirs);
            helper.assertTrue(ourProps.ballistics().durabilityMass() > theirProps.ballistics().durabilityMass(),
                    "a uranium penetrator is heavier: " + ourProps.ballistics().durabilityMass() + " vs " + theirProps.ballistics().durabilityMass());
            helper.assertTrue(ourProps.ballistics().penetration() > theirProps.ballistics().penetration(),
                    "and penetrates more: " + ourProps.ballistics().penetration() + " vs " + theirProps.ballistics().penetration());
        }

        static void firesOwnProjectile(GameTestHelper helper) {
            var block = org.papiricoh.create_nuclearindustry.integration.cbc.CBCNuclearContent.DEPLETED_URANIUM_AP_SHOT_BLOCK.get();
            var projectile = block.getProjectile(helper.getLevel(),
                    new ItemStack(org.papiricoh.create_nuclearindustry.integration.cbc.CBCNuclearContent.DEPLETED_URANIUM_AP_SHOT.get()));
            helper.assertTrue(projectile instanceof org.papiricoh.create_nuclearindustry.integration.cbc.DepletedUraniumAPShotProjectile,
                    "the shot block should fire a depleted uranium projectile, got " + projectile);
        }
    }
}
