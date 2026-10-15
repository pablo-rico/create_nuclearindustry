package org.papiricoh.create_nuclearindustry.mixin.client;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.papiricoh.create_nuclearindustry.creativetab.client.NuclearTabBanners;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tells the creative tab banners which row is at the top of the grid. */
@Mixin(CreativeModeInventoryScreen.ItemPickerMenu.class)
abstract class ItemPickerMenuMixin {
    @Shadow
    protected abstract int getRowIndexForScroll(float scrollOffs);

    @Inject(method = "scrollTo", at = @At("HEAD"))
    private void create_nuclearindustry$topRow(float pos, CallbackInfo ci) {
        NuclearTabBanners.topRow = getRowIndexForScroll(pos);
    }
}
