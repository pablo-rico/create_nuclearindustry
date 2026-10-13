package org.papiricoh.create_nuclearindustry.controlroom.link.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import org.papiricoh.create_nuclearindustry.controlroom.link.AdvancedRedstoneLinkBlock;
import org.papiricoh.create_nuclearindustry.controlroom.link.AdvancedRedstoneLinkBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.link.LinkCode;

/**
 * Writes an Advanced Redstone Link's code on the display window of its plate. The model is built
 * facing up with the window at x 3-11, z 5-11 px on the plate's top (y 3 px), read with its top
 * towards +z; the blockstate turns it like an end rod.
 */
public class AdvancedLinkRenderer implements BlockEntityRenderer<AdvancedRedstoneLinkBlockEntity> {
    private static final float PX = 1 / 16f;
    /** Window centre and width on the plate, px. */
    private static final float CENTRE_X = 7, CENTRE_Z = 8, WINDOW = 7.5f, FACE_Y = 3.02f;
    private static final int LIT = 0xFFFFB347;
    private static final int OFF = 0xFF6E4A2A;
    private final Font font;

    public AdvancedLinkRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(AdvancedRedstoneLinkBlockEntity link, float partialTick, PoseStack ms, MultiBufferSource buffers, int light,
                       int overlay) {
        Direction facing = link.getBlockState().getValue(AdvancedRedstoneLinkBlock.FACING);
        String code = LinkCode.complete(link.getCode()) ? link.getCode() : "----";
        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        // As the blockstate turns the model: a y turn, then an x turn, both clockwise.
        ms.mulPose(Axis.YP.rotationDegrees(-yRotation(facing)));
        ms.mulPose(Axis.XP.rotationDegrees(-xRotation(facing)));
        ms.translate(-0.5, -0.5, -0.5);
        ms.translate(CENTRE_X * PX, FACE_Y * PX, CENTRE_Z * PX);
        // Onto the window as read: x to the reader's right, y up, z out of the plate.
        ms.mulPose(Axis.YP.rotationDegrees(180));
        ms.mulPose(Axis.XP.rotationDegrees(-90));
        float scale = WINDOW * PX / Math.max(1, font.width("WWWW"));
        ms.scale(scale, -scale, scale);
        Component text = Component.literal(code);
        int color = link.signal() > 0 ? LIT : OFF;
        font.drawInBatch(text, -font.width(text) / 2f, -4, color, false, ms.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET,
                0, LightTexture.FULL_BRIGHT);
        ms.popPose();
    }

    /** The blockstate's x rotation for a model built facing up, as an end rod's. */
    static int xRotation(Direction facing) {
        return switch (facing) {
            case UP -> 0;
            case DOWN -> 180;
            default -> 90;
        };
    }

    static int yRotation(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180;
            case EAST -> 90;
            case WEST -> 270;
            default -> 0;
        };
    }
}
