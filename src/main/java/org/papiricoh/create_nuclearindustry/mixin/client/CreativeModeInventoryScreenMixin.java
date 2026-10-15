package org.papiricoh.create_nuclearindustry.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.create_nuclearindustry.AllCreativeTabs;
import org.papiricoh.create_nuclearindustry.creativetab.client.NuclearTabBanners;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Draws the mod tab's section banners and names an item's section in its tooltip. */
@Mixin(CreativeModeInventoryScreen.class)
abstract class CreativeModeInventoryScreenMixin {
    @Shadow
    private static CreativeModeTab selectedTab;

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen;renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V"))
    private void create_nuclearindustry$banners(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (AllCreativeTabs.NUCLEAR_TAB.isBound() && selectedTab == AllCreativeTabs.NUCLEAR_TAB.get()) {
            NuclearTabBanners.render((CreativeModeInventoryScreen) (Object) this, graphics, mouseX, mouseY);
        }
    }

    @Inject(method = "getTooltipFromContainerItem", at = @At("RETURN"))
    private void create_nuclearindustry$sectionLine(ItemStack stack, CallbackInfoReturnable<List<Component>> cir) {
        NuclearTabBanners.addSectionLine(stack, cir.getReturnValue());
    }
}
