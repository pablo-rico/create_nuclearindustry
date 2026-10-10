package org.papiricoh.create_nuclearindustry.fusion.heating.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

/**
 * The plasma's render type: untextured coloured quads, added onto what is behind them (light, not
 * paint), fullbright, seen from both sides and never writing depth, so the bright core shows through
 * its own halo. It still tests depth, so the shell hides it except through viewports.
 */
public final class PlasmaRenderTypes extends RenderType {
    public static final RenderType PLASMA = create("create_nuclearindustry_plasma", DefaultVertexFormat.POSITION_COLOR,
            // Additive blending does not care about order, so no sorting.
            VertexFormat.Mode.QUADS, 1 << 16, false, false,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));

    private PlasmaRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling,
                              boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
    }
}
