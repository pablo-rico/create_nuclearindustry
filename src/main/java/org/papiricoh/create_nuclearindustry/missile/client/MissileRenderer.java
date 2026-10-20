package org.papiricoh.create_nuclearindustry.missile.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.create_nuclearindustry.missile.MissileRegistry;
import org.papiricoh.create_nuclearindustry.missile.defense.client.DefensePartials;
import org.papiricoh.create_nuclearindustry.missile.entity.MissileEntity;

public class MissileRenderer extends EntityRenderer<MissileEntity> {
    private final net.minecraft.client.renderer.block.BlockRenderDispatcher blockRenderer;
    private final ItemRenderer itemRenderer;
    private final ItemStack missileStack;

    public MissileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.missileStack = new ItemStack(MissileRegistry.MISSILE_ITEM.get());
    }

    @Override
    public void render(MissileEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        // The model's nose is +Y: tilted forward onto +Z by 90 degrees plus the pitch (-90 when climbing
        // straight up), then turned to the heading, it points along the flight.
        poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot()) + 90.0f));
        if (entity.isInterceptor()) {
            CachedBuffers.partial(DefensePartials.INTERCEPTOR_ROUND, net.minecraft.world.level.block.Blocks.WHITE_CONCRETE.defaultBlockState())
                    .translate(-0.5f, 0, -0.5f).light(packedLight)
                    .renderInto(poseStack, buffer.getBuffer(RenderType.solid()));
            poseStack.popPose();
            super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            return;
        }
        java.util.List<String> sections = entity.visualSections();
        if (!sections.isEmpty()) {
            int first = Math.min(entity.firstVisualSection(), sections.size());
            poseStack.translate(-.5, 0, -.5);
            for (int i = first; i < sections.size(); i++) {
                ResourceLocation id = ResourceLocation.tryParse(sections.get(i));
                if (id != null) blockRenderer.renderSingleBlock(
                        net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id).defaultBlockState(),
                        poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
                poseStack.translate(0, 1, 0);
            }
        } else {
        poseStack.scale(1.35f, 1.35f, 1.35f);
        itemRenderer.renderStatic(
                missileStack,
                ItemDisplayContext.NONE,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId());
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MissileEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
