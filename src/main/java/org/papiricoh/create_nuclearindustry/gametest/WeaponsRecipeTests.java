package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveRegistry;
import org.papiricoh.create_nuclearindustry.missile.MissileRegistry;

import java.util.List;
import java.util.Optional;

/** Warheads are assembled from parts on a Create line, no longer crafted from a handful of blocks. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class WeaponsRecipeTests {

    private static Optional<Recipe<?>> recipe(GameTestHelper helper, String path) {
        return helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path))
                .map(RecipeHolder::value);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void everyWeaponHasItsRecipe(GameTestHelper helper) {
        for (String path : List.of("mixing/lithium_deuteride", "mixing/neutron_initiator", "sequenced_assembly/explosive_lens",
                "sequenced_assembly/firing_set", "sequenced_assembly/implosion_assembly", "sequenced_assembly/nuclear_bomb",
                "sequenced_assembly/thermonuclear_bomb", "mechanical_crafting/bomb_casing", "mechanical_crafting/thermonuclear_casing",
                "silo_controller", "silo_wall", "rocket_engine", "rocket_tank", "rocket_guidance", "rocket_warhead",
                "propellant_cartridge", "crafting/dirty_bomb")) {
            helper.assertTrue(recipe(helper, path).isPresent(), "no recipe " + path);
        }
        for (String path : List.of("mixing/lithium_deuteride", "mixing/neutron_initiator")) {
            Recipe<?> mixing = recipe(helper, path).orElse(null);
            helper.assertTrue(mixing instanceof MixingRecipe mix && mix.getRequiredHeat() == HeatCondition.HEATED, path + " is mixed hot");
        }
        helper.succeed();
    }

    /** No crafting table makes a bomb, a missile or a launch pad any more. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void noBenchMakesAWarhead(GameTestHelper helper) {
        List<Item> weapons = List.of(AllNuclearItems.NUCLEAR_BOMB.get(), MissileRegistry.MISSILE_ITEM.get(), MissileRegistry.LAUNCH_PAD_ITEM.get(),
                ExplosiveRegistry.THERMONUCLEAR_BOMB_ITEM.get());
        for (RecipeHolder<?> holder : helper.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            ItemStack result = holder.value().getResultItem(helper.getLevel().registryAccess());
            helper.assertFalse(weapons.stream().anyMatch(result::is), holder.id() + " crafts " + result + " on a bench");
        }
        helper.assertTrue(recipe(helper, "crafting/nuclear_bomb").isEmpty(), "the old bomb recipe is gone");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void bombsAreAssembledFromTheirParts(GameTestHelper helper) {
        SequencedAssemblyRecipe atomic = (SequencedAssemblyRecipe) recipe(helper, "sequenced_assembly/nuclear_bomb").orElse(null);
        SequencedAssemblyRecipe hydrogen = (SequencedAssemblyRecipe) recipe(helper, "sequenced_assembly/thermonuclear_bomb").orElse(null);
        helper.assertTrue(atomic != null && hydrogen != null, "both bombs are sequenced assemblies");
        helper.assertTrue(atomic.getIngredient().test(new ItemStack(ExplosiveRegistry.BOMB_CASING.get()))
                && hydrogen.getIngredient().test(new ItemStack(ExplosiveRegistry.THERMONUCLEAR_CASING.get())), "each on its own casing");
        for (Item part : List.of(ExplosiveRegistry.IMPLOSION_ASSEMBLY.get(), ExplosiveRegistry.NEUTRON_INITIATOR.get(),
                ExplosiveRegistry.FIRING_SET.get())) {
            helper.assertTrue(deploys(atomic, part) && deploys(hydrogen, part), "both take a " + part);
        }
        helper.assertTrue(deploys(hydrogen, AllNuclearItems.TRITIUM_CELL.get()) && !deploys(atomic, AllNuclearItems.TRITIUM_CELL.get()),
                "only the thermonuclear bomb is boosted with tritium");
        helper.succeed();
    }

    private static boolean deploys(SequencedAssemblyRecipe recipe, Item item) {
        ItemStack stack = new ItemStack(item);
        return recipe.getSequence().stream().anyMatch(step -> step.getRecipe().getIngredients().stream()
                .anyMatch(ingredient -> ingredient.test(stack)));
    }
}
