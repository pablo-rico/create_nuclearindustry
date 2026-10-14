package org.papiricoh.create_nuclearindustry.integration.cbc;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.AllNuclearDataComponents;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.AllNuclearTags;
import org.papiricoh.create_nuclearindustry.explosive.WarheadLoad;

/**
 * A nuclear cannon shell: a Create Big Cannons high explosive shell, a nuclear bomb for its trigger and
 * one to four weapons-grade cores in any slots. The shell goes off with as many cores as went in (see
 * {@link WarheadLoad}): four make a full warhead, as the old fixed recipe did.
 */
public class NuclearCannonShellRecipe extends CustomRecipe {
    private static final ResourceLocation HE_SHELL = ResourceLocation.fromNamespaceAndPath("createbigcannons", "he_shell");

    public NuclearCannonShellRecipe(CraftingBookCategory category) {
        super(category);
    }

    /** Cores in the grid if it holds exactly a shell, a bomb and one to four cores, else -1. */
    public static int cores(CraftingInput input) {
        Item shellItem = BuiltInRegistries.ITEM.get(HE_SHELL);
        int shells = 0;
        int bombs = 0;
        int cores = 0;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(shellItem)) {
                shells++;
            } else if (stack.is(AllNuclearItems.NUCLEAR_BOMB.get())) {
                bombs++;
            } else if (stack.is(AllNuclearTags.Items.NUCLEAR_WARHEAD_CORES)) {
                cores++;
            } else {
                return -1;
            }
        }
        return shells == 1 && bombs == 1 && cores >= 1 && cores <= WarheadLoad.MAX_CORES ? cores : -1;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return cores(input) > 0;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        int cores = cores(input);
        if (cores <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack shell = new ItemStack(CBCNuclearContent.NUCLEAR_CANNON_SHELL.get());
        shell.set(AllNuclearDataComponents.WARHEAD_LOAD.get(), WarheadLoad.ofCores(cores));
        return shell;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CBCNuclearContent.NUCLEAR_CANNON_SHELL_RECIPE.get();
    }
}
