package org.papiricoh.create_nuclearindustry.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.ui.PonderUI;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PonderDepth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Draws the mod's zoomed-in scenes behind Ponder's progress bar and buttons ({@link PonderDepth}). */
@Mixin(value = PonderUI.class, remap = false)
abstract class PonderUIMixin {
    @WrapOperation(method = "renderScene", at = @At(value = "INVOKE",
            target = "Lnet/createmod/ponder/foundation/PonderScene$SceneTransform;apply(Lcom/mojang/blaze3d/vertex/PoseStack;F)Lcom/mojang/blaze3d/vertex/PoseStack;"))
    private PoseStack create_nuclearindustry$behindTheControls(PonderScene.SceneTransform transform, PoseStack poseStack, float partialTicks,
                                                              Operation<PoseStack> original, @Local PonderScene scene) {
        poseStack.translate(0, 0, -PonderDepth.pushBack(scene, transform, partialTicks));
        return original.call(transform, poseStack, partialTicks);
    }
}
