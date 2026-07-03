package org.papiricoh.create_nuclearindustry.integration.jei;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public record CentrifugingDisplay(ItemStack input, ItemStack output, Component description) {}
