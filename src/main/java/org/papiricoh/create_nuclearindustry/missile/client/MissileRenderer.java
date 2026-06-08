package org.papiricoh.create_nuclearindustry.missile.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.missile.entity.MissileEntity;

/**
 * Renderer placeholder del misil: dibuja el modelo de bloque de la bomba nuclear estirado y
 * orientado segun la trayectoria. Se sustituira por un modelo propio cuando este disponible.
 */
public class MissileRenderer extends EntityRenderer<MissileEntity> {
    private static final ResourceLocation PLACEHOLDER_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    private final BlockState placeholderState = AllNuclearBlocks.NUCLEAR_BOMB.get().defaultBlockState();

    public MissileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MissileEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        // Orientar el cuerpo del misil a lo largo del vector de vuelo.
        poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
        // Estirar el bloque para que parezca un cohete y centrarlo.
        poseStack.scale(0.4f, 1.6f, 0.4f);
        poseStack.translate(-0.5, -0.5, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                placeholderState, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MissileEntity entity) {
        return PLACEHOLDER_TEXTURE;
    }
}
