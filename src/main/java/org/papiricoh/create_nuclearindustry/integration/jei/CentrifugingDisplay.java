package org.papiricoh.create_nuclearindustry.integration.jei;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record CentrifugingDisplay(List<ItemStack> inputs, ItemStack output, Component description) {
    public CentrifugingDisplay(ItemStack input, ItemStack output, Component description) {
        this(List.of(input), output, description);
    }
}
