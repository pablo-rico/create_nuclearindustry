package org.papiricoh.create_nuclearindustry.integration.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.processing.KineticProcessingBlock;
import org.papiricoh.create_nuclearindustry.processing.client.ProcessingPartials.MachineParts;

import java.util.function.Supplier;

/**
 * A centrifuge or electrolyzer turning in its JEI page, at the angle Create draws its own machines:
 * the body facing the reader, the rotor spinning behind its slots and the shaft stub at the back.
 */
public class AnimatedProcessingMachine extends AnimatedKinetics {
    /** North or south only, so the partials' yaw and the shaft's spin stay on separate axes. */
    private static final Direction FACING = Direction.SOUTH;
    private static final double SCALE = 23;

    private final Supplier<? extends Block> block;
    private final MachineParts parts;

    public AnimatedProcessingMachine(Supplier<? extends Block> block, MachineParts parts) {
        this.block = block;
        this.parts = parts;
    }

    @Override
    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(xOffset, yOffset, 200);
        pose.mulPose(Axis.XP.rotationDegrees(-15.5f));
        pose.mulPose(Axis.YP.rotationDegrees(22.5f));
        BlockState state = block.get().defaultBlockState().setValue(KineticProcessingBlock.FACING, FACING);
        double yaw = FACING == Direction.NORTH ? 0 : 180;
        float angle = getCurrentAngle();

        blockElement(state).atLocal(0, 0, 0).scale(SCALE).render(graphics);
        blockElement(parts.rotor()).rotateBlock(0, yaw + angle * parts.rotorRatio(), 0).scale(SCALE).render(graphics);
        blockElement(parts.shaftStub()).rotateBlock(0, yaw, angle).scale(SCALE).render(graphics);
        blockElement(parts.needle()).rotateBlock(0, yaw, 0).scale(SCALE).render(graphics);
        pose.popPose();
    }
}
