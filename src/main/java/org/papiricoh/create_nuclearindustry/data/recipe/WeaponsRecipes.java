package org.papiricoh.create_nuclearindustry.data.recipe;

import com.simibubi.create.api.data.recipe.MechanicalCraftingRecipeGen;
import com.simibubi.create.api.data.recipe.MixingRecipeGen;
import com.simibubi.create.api.data.recipe.SequencedAssemblyRecipeGen;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveRegistry;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Nuclear weapons are assembled from parts on a Create line, not crafted from a handful of blocks. An
 * explosive lens is TNT and gunpowder pressed into an iron sheet; eight of them round a beryllium
 * reflector make an implosion assembly. A firing set fires them all at once, and a neutron initiator
 * (beryllium and plutonium) starts the chain reaction on time. Casings are riveted on a mechanical
 * crafter. A nuclear bomb is a casing with the three parts deployed into it; a thermonuclear one also
 * takes a tritium cell to boost its primary, so it needs a working tokamak, and its casing is a radiation
 * case lined with depleted uranium. Missiles and launch pads take a firing set too. The warheads' cores
 * still go in through the bomb's or the pad's screen.
 *
 * <p>Like {@link FuelCycleRecipes}, it runs its generators under a name of its own.
 */
public final class WeaponsRecipes implements DataProvider {
    private final List<DataProvider> generators;

    public WeaponsRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        generators = List.of(new Mixing(output, registries), new Assembly(output, registries), new Mechanical(output, registries),
                new Crafting(output, registries));
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(generators.stream().map(generator -> generator.run(cache)).toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Nuclear weapons recipes";
    }

    /** The salts and sources warheads need, mixed hot. */
    @SuppressWarnings("unused")
    static final class Mixing extends MixingRecipeGen {
        GeneratedRecipe LITHIUM_DEUTERIDE = create("lithium_deuteride", b -> b
                .require(AllNuclearItems.LITHIUM_6.get())
                .require(AllNuclearFluids.DEUTERIUM.get(), 250)
                .output(ExplosiveRegistry.LITHIUM_DEUTERIDE.get())
                .requiresHeat(HeatCondition.HEATED));

        // A plutonium-beryllium source: alpha particles knock neutrons out of the beryllium.
        GeneratedRecipe NEUTRON_INITIATOR = create("neutron_initiator", b -> b
                .require(AllNuclearItems.BERYLLIUM.get())
                .require(AllNuclearItems.BERYLLIUM.get())
                .require(AllNuclearItems.PLUTONIUM_DIOXIDE.get())
                .require(CreateItems.get("golden_sheet"))
                .output(ExplosiveRegistry.NEUTRON_INITIATOR.get())
                .requiresHeat(HeatCondition.HEATED));

        Mixing(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, Create_NuclearIndustry.MODID);
        }
    }

    static final class Assembly extends SequencedAssemblyRecipeGen {
        {
            create("explosive_lens", b -> b
                    .require(CreateItems.get("iron_sheet"))
                    .transitionTo(ExplosiveRegistry.INCOMPLETE_EXPLOSIVE_LENS.get())
                    .addOutput(ExplosiveRegistry.EXPLOSIVE_LENS.get(), 1)
                    .loops(2)
                    .addStep(DeployerApplicationRecipe::new, step -> step.require(Items.TNT))
                    .addStep(DeployerApplicationRecipe::new, step -> step.require(Items.GUNPOWDER))
                    .addStep(PressingRecipe::new, step -> step));
            create("firing_set", b -> b
                    .require(CreateItems.get("precision_mechanism"))
                    .transitionTo(ExplosiveRegistry.INCOMPLETE_FIRING_SET.get())
                    .addOutput(ExplosiveRegistry.FIRING_SET.get(), 1)
                    .loops(2)
                    .addStep(DeployerApplicationRecipe::new, step -> step.require(CreateItems.get("electron_tube")))
                    .addStep(DeployerApplicationRecipe::new, step -> step.require(CreateItems.get("copper_sheet")))
                    .addStep(DeployerApplicationRecipe::new, step -> step.require(Items.REDSTONE)));
            // Eight lenses round a beryllium reflector, which tamps the core.
            create("implosion_assembly", b -> b
                    .require(AllNuclearItems.BERYLLIUM_REFLECTOR.get())
                    .transitionTo(ExplosiveRegistry.INCOMPLETE_IMPLOSION_ASSEMBLY.get())
                    .addOutput(ExplosiveRegistry.IMPLOSION_ASSEMBLY.get(), 1)
                    .loops(4)
                    .addStep(DeployerApplicationRecipe::new, step -> step.require(ExplosiveRegistry.EXPLOSIVE_LENS.get()))
                    .addStep(DeployerApplicationRecipe::new, step -> step.require(ExplosiveRegistry.EXPLOSIVE_LENS.get()))
                    .addStep(PressingRecipe::new, step -> step));
            create("nuclear_bomb", b -> b
                    .require(ExplosiveRegistry.BOMB_CASING.get())
                    .transitionTo(ExplosiveRegistry.INCOMPLETE_NUCLEAR_BOMB.get())
                    .addOutput(AllNuclearItems.NUCLEAR_BOMB.get(), 1)
                    .loops(1)
                    .addStep(DeployerApplicationRecipe::new, step -> step.require(ExplosiveRegistry.IMPLOSION_ASSEMBLY.get()))
                    .addStep(DeployerApplicationRecipe::new, step -> step.require(ExplosiveRegistry.NEUTRON_INITIATOR.get()))
                    .addStep(DeployerApplicationRecipe::new, step -> step.require(ExplosiveRegistry.FIRING_SET.get()))
                    .addStep(PressingRecipe::new, step -> step));
            // The primary is boosted with tritium: a thermonuclear bomb needs a tokamak's breeding blanket.
            create("thermonuclear_bomb", b -> b
                    .require(ExplosiveRegistry.THERMONUCLEAR_CASING.get())
                    .transitionTo(ExplosiveRegistry.INCOMPLETE_THERMONUCLEAR_BOMB.get())
                    .addOutput(ExplosiveRegistry.THERMONUCLEAR_BOMB_ITEM.get(), 1)
                    .loops(1)
                    .addStep(DeployerApplicationRecipe::new, step -> step.require(ExplosiveRegistry.IMPLOSION_ASSEMBLY.get()))
                    .addStep(DeployerApplicationRecipe::new, step -> step.require(ExplosiveRegistry.NEUTRON_INITIATOR.get()))
                    .addStep(DeployerApplicationRecipe::new, step -> step.require(ExplosiveRegistry.FIRING_SET.get()))
                    .addStep(DeployerApplicationRecipe::new, step -> step.require(AllNuclearItems.TRITIUM_CELL.get()))
                    .addStep(PressingRecipe::new, step -> step));
        }

        Assembly(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, Create_NuclearIndustry.MODID);
        }
    }

    /** The big pieces, riveted on a mechanical crafter. */
    @SuppressWarnings("unused")
    static final class Mechanical extends MechanicalCraftingRecipeGen {
        GeneratedRecipe BOMB_CASING = create(() -> ExplosiveRegistry.BOMB_CASING.get()).recipe(b -> b
                .key('S', CreateItems.get("sturdy_sheet"))
                .key('I', CreateItems.get("iron_sheet"))
                .key('M', CreateItems.get("precision_mechanism"))
                .patternLine(" SSS ")
                .patternLine("SIIIS")
                .patternLine("SIMIS")
                .patternLine("SIIIS")
                .patternLine(" SSS "));

        // The radiation case: its depleted uranium lining holds the primary's X-rays round the secondary.
        GeneratedRecipe THERMONUCLEAR_CASING = create(() -> ExplosiveRegistry.THERMONUCLEAR_CASING.get()).recipe(b -> b
                .key('S', CreateItems.get("sturdy_sheet"))
                .key('D', AllNuclearItems.DEPLETED_URANIUM_BLOCK.get())
                .key('P', CreateItems.get("precision_mechanism"))
                .key('E', CreateItems.get("electron_tube"))
                .patternLine(" SSSSS ")
                .patternLine("SDDDDDS")
                .patternLine("SDPEPDS")
                .patternLine("SDDDDDS")
                .patternLine(" SSSSS "));

        GeneratedRecipe MISSILE = create(() -> AllNuclearItems.MISSILE.get()).recipe(b -> b
                .key('F', ExplosiveRegistry.FIRING_SET.get())
                .key('I', CreateItems.get("iron_sheet"))
                .key('P', CreateItems.get("precision_mechanism"))
                .key('B', CreateItems.get("blaze_cake"))
                .key('N', CreateItems.get("propeller"))
                .key('K', CreateItems.get("sturdy_sheet"))
                .patternLine(" F ")
                .patternLine("IPI")
                .patternLine("IBI")
                .patternLine("IBI")
                .patternLine("INI")
                .patternLine("K K"));

        GeneratedRecipe LAUNCH_PAD = create(() -> AllNuclearItems.LAUNCH_PAD.get()).recipe(b -> b
                .key('K', CreateItems.get("sturdy_sheet"))
                .key('F', ExplosiveRegistry.FIRING_SET.get())
                .key('C', AllNuclearItems.REACTOR_CASING.get())
                .key('P', CreateItems.get("precision_mechanism"))
                .key('M', CreateItems.get("mechanical_piston"))
                .key('I', Items.IRON_BLOCK)
                .patternLine("KKFKK")
                .patternLine("CPMPC")
                .patternLine("IIIII"));

        Mechanical(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, Create_NuclearIndustry.MODID);
        }
    }

    /** A dirty bomb is only a conventional charge round a waste barrel, but it still needs a firing set. */
    static final class Crafting extends RecipeProvider {
        Crafting(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected void buildRecipes(RecipeOutput output) {
            Item sturdy = CreateItems.get("sturdy_sheet");
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AllNuclearItems.DIRTY_BOMB.get())
                    .pattern("SFS")
                    .pattern("TBT")
                    .pattern("SCS")
                    .define('S', sturdy)
                    .define('F', ExplosiveRegistry.FIRING_SET.get())
                    .define('T', Items.TNT)
                    .define('B', AllNuclearItems.RADIOACTIVE_WASTE_BARREL.get())
                    .define('C', AllNuclearItems.REACTOR_CASING.get())
                    .unlockedBy("has_radioactive_waste_barrel", has(AllNuclearItems.RADIOACTIVE_WASTE_BARREL.get()))
                    .save(output, ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "crafting/dirty_bomb"));
        }
    }
}
