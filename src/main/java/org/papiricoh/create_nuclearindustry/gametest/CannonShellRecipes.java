package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearDataComponents;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveRegistry;
import org.papiricoh.create_nuclearindustry.explosive.ThermonuclearLoad;
import org.papiricoh.create_nuclearindustry.explosive.WarheadLoad;

import java.util.ArrayList;
import java.util.List;

/** Crafts nuclear and thermonuclear cannon shells through the recipe manager, without touching Create Big Cannons' classes. */
final class CannonShellRecipes {
    private CannonShellRecipes() {}

    /** The load of the shell a crafting grid of an HE shell, a nuclear bomb and {@code cores} makes, or null if it makes none. */
    @Nullable
    static WarheadLoad load(GameTestHelper helper, ItemStack... cores) {
        List<ItemStack> items = new ArrayList<>();
        items.add(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("createbigcannons", "he_shell"))));
        items.add(new ItemStack(AllNuclearItems.NUCLEAR_BOMB.get()));
        for (ItemStack core : cores) {
            items.add(core.copy());
        }
        while (items.size() < 9) {
            items.add(ItemStack.EMPTY);
        }
        CraftingInput input = CraftingInput.of(3, 3, items);
        return helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .map(recipe -> recipe.value().assemble(input, helper.getLevel().registryAccess()))
                .map(stack -> stack.get(AllNuclearDataComponents.WARHEAD_LOAD.get()))
                .orElse(null);
    }

    /** The load of the shell a grid of an HE shell and {@code parts} makes, or null if it makes none. */
    @Nullable
    static ThermonuclearLoad thermonuclear(GameTestHelper helper, ItemStack... parts) {
        List<ItemStack> items = new ArrayList<>();
        items.add(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("createbigcannons", "he_shell"))));
        for (ItemStack part : parts) {
            items.add(part.copy());
        }
        while (items.size() < 9) {
            items.add(ItemStack.EMPTY);
        }
        CraftingInput input = CraftingInput.of(3, 3, items);
        return helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .map(recipe -> recipe.value().assemble(input, helper.getLevel().registryAccess()))
                .map(stack -> stack.get(ExplosiveRegistry.THERMONUCLEAR_LOAD.get()))
                .orElse(null);
    }
}
