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
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.AllNuclearTags;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveRegistry;
import org.papiricoh.create_nuclearindustry.explosive.FissileMaterial;
import org.papiricoh.create_nuclearindustry.explosive.ThermonuclearLoad;
import org.papiricoh.create_nuclearindustry.explosive.ThermonuclearStats;

/**
 * A thermonuclear cannon shell: a Create Big Cannons high explosive shell, a thermonuclear bomb for its
 * trigger and radiation case, one weapons-grade core for the primary, one to four lithium deuteride for
 * the secondary and up to two depleted uranium blocks for the tamper, in any slots.
 */
public class ThermonuclearCannonShellRecipe extends CustomRecipe {
    private static final ResourceLocation HE_SHELL = ResourceLocation.fromNamespaceAndPath("createbigcannons", "he_shell");

    public ThermonuclearCannonShellRecipe(CraftingBookCategory category) {
        super(category);
    }

    /** What the shell would carry if the grid holds exactly its parts, else null. */
    @Nullable
    public static ThermonuclearLoad load(CraftingInput input) {
        Item shellItem = BuiltInRegistries.ITEM.get(HE_SHELL);
        int shells = 0;
        int bombs = 0;
        int deuteride = 0;
        int tamper = 0;
        ThermonuclearStats.Primary primary = ThermonuclearStats.Primary.NONE;
        int cores = 0;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(shellItem)) {
                shells++;
            } else if (stack.is(ExplosiveRegistry.THERMONUCLEAR_BOMB_ITEM.get())) {
                bombs++;
            } else if (FissileMaterial.isWarheadCore(stack)) {
                cores++;
                primary = stack.is(AllNuclearItems.WEAPONS_GRADE_PLUTONIUM_CORE.get())
                        ? ThermonuclearStats.Primary.PLUTONIUM : ThermonuclearStats.Primary.URANIUM;
            } else if (stack.is(ExplosiveRegistry.LITHIUM_DEUTERIDE.get())) {
                deuteride++;
            } else if (stack.is(AllNuclearTags.Items.THERMONUCLEAR_TAMPERS)) {
                tamper++;
            } else {
                return null;
            }
        }
        boolean parts = shells == 1 && bombs == 1 && cores == 1
                && deuteride >= 1 && deuteride <= ThermonuclearLoad.SHELL_SECONDARY
                && tamper <= ThermonuclearLoad.SHELL_TAMPER;
        return parts ? new ThermonuclearLoad(primary, deuteride, tamper) : null;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return load(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ThermonuclearLoad load = load(input);
        if (load == null) {
            return ItemStack.EMPTY;
        }
        ItemStack shell = new ItemStack(CBCNuclearContent.THERMONUCLEAR_CANNON_SHELL.get());
        shell.set(ExplosiveRegistry.THERMONUCLEAR_LOAD.get(), load);
        return shell;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 4;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CBCNuclearContent.THERMONUCLEAR_CANNON_SHELL_RECIPE.get();
    }
}
