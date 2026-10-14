package org.papiricoh.create_nuclearindustry.infrastructure.ponder;

import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

/**
 * Keeps a zoomed-in scene behind Ponder's own controls. Ponder draws the scene with depth testing
 * around depth 200 and then its progress bar (400) and buttons (420) with depth testing too; a big
 * scene seen close up ({@code PonderZoom}) reaches far nearer than that, and the floor in front hides
 * them. The view is orthographic, so pushing the whole scene further back changes nothing on screen
 * but the depth it is drawn at.
 */
public final class PonderDepth {
    /** The nearest a scene may come: clear of the progress bar and the buttons. */
    private static final float FRONT_LIMIT = 300;

    private PonderDepth() {}

    /** How far back to push {@code scene} so its nearest corner stays behind the controls; 0 for other mods' scenes. */
    public static float pushBack(PonderScene scene, PonderScene.SceneTransform transform, float partialTicks) {
        if (!Create_NuclearIndustry.MODID.equals(scene.getId().getNamespace())) {
            return 0;
        }
        Matrix4f view = transform.apply(new PoseStack(), partialTicks).last().pose();
        BoundingBox bounds = scene.getBounds();
        float front = Float.NEGATIVE_INFINITY;
        Vector4f corner = new Vector4f();
        for (int i = 0; i < 8; i++) {
            corner.set((i & 1) == 0 ? bounds.minX() : bounds.maxX() + 1,
                    (i & 2) == 0 ? bounds.minY() : bounds.maxY() + 1,
                    (i & 4) == 0 ? bounds.minZ() : bounds.maxZ() + 1, 1);
            view.transform(corner);
            front = Math.max(front, corner.z());
        }
        return Math.max(0, front - FRONT_LIMIT);
    }
}
