package org.papiricoh.create_nuclearindustry.missile.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.missile.blockentity.LaunchPadBlockEntity;

public class LaunchPadRenderer implements BlockEntityRenderer<LaunchPadBlockEntity> {
    private final ItemRenderer itemRenderer;

    public LaunchPadRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(LaunchPadBlockEntity launchPad, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ItemStack missile = launchPad.getInventory().getStackInSlot(LaunchPadBlockEntity.MISSILE_SLOT);
        if (!missile.is(AllNuclearItems.MISSILE.get())) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.69F, 0.5F);
        itemRenderer.renderStatic(
                missile,
                ItemDisplayContext.NONE,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                launchPad.getLevel(),
                (int) launchPad.getBlockPos().asLong());
        poseStack.popPose();
    }
}
