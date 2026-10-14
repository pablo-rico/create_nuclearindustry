package org.papiricoh.create_nuclearindustry.controlroom.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

/**
 * Flat full-bright colour for the styled screens, painted in the order it was sent: it is not sorted
 * by distance and writes no depth, so a style can lay layer over layer in one plane (a plate over
 * its frame, a pointer over its scale) and the last one always shows, however far the player stands.
 * Layers a hair apart in depth would not: rounding at a distance shuffles them.
 */
public final class MonitorRenderTypes extends RenderType {
    public static final RenderType FLAT = create("create_nuclearindustry_monitor_flat", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
            VertexFormat.Mode.QUADS, 1 << 16, false, false,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_TEXT_BACKGROUND_SHADER)
                    .setTextureState(NO_TEXTURE)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    /**
     * Depth alone, no colour: laid just behind a painted screen once it is done, so what is drawn
     * after it (a Ponder scene draws its blocks after their block entities) stays behind it.
     */
    public static final RenderType SEAL = create("create_nuclearindustry_monitor_seal", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
            VertexFormat.Mode.QUADS, 1536, false, false,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_TEXT_BACKGROUND_SHADER)
                    .setTextureState(NO_TEXTURE)
                    .setLightmapState(LIGHTMAP)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(DEPTH_WRITE)
                    .createCompositeState(false));

    private MonitorRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling,
                               boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
    }
}
