package org.papiricoh.create_nuclearindustry.mixin;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.create_nuclearindustry.AllCreativeTabs;
import org.papiricoh.create_nuclearindustry.creativetab.NuclearTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

/**
 * Lays the mod's tab out in sections once it is built ({@link NuclearTab#withBanners}). The
 * banner rows are empty stacks, which the tab's own builder would refuse, so they go in after it;
 * the search tab's contents are left alone.
 */
@Mixin(CreativeModeTab.class)
abstract class CreativeModeTabMixin {
    @Shadow
    private Collection<ItemStack> displayItems;

    @Inject(method = "buildContents", at = @At("TAIL"))
    private void create_nuclearindustry$sections(CreativeModeTab.ItemDisplayParameters parameters, CallbackInfo ci) {
        if (AllCreativeTabs.NUCLEAR_TAB.isBound() && (Object) this == AllCreativeTabs.NUCLEAR_TAB.get()) {
            displayItems = NuclearTab.withBanners(displayItems);
        }
    }
}
