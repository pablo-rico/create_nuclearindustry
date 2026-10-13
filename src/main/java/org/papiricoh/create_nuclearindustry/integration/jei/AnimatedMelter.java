package org.papiricoh.create_nuclearindustry.integration.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import net.minecraft.client.gui.GuiGraphics;
import org.papiricoh.create_nuclearindustry.waste.WasteRegistry;

/**
 * The Vitrification Melter as Create draws its basin in the mixing category: same angle, scale and
 * height, so Create's animated Blaze Burner fits right under it.
 */
public class AnimatedMelter extends AnimatedKinetics {

    @Override
    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(xOffset, yOffset, 200);
        pose.mulPose(Axis.XP.rotationDegrees(-15.5f));
        pose.mulPose(Axis.YP.rotationDegrees(22.5f));
        blockElement(WasteRegistry.VITRIFICATION_MELTER.get().defaultBlockState())
                .atLocal(0, 1.65, 0)
                .scale(23)
                .render(graphics);
        pose.popPose();
    }
}
