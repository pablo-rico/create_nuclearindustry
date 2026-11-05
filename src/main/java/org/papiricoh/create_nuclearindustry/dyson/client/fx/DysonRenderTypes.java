package org.papiricoh.create_nuclearindustry.dyson.client.fx;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import org.papiricoh.create_nuclearindustry.fusion.heating.client.PlasmaRenderTypes;
import org.papiricoh.create_nuclearindustry.missile.fx.client.RocketRenderTypes;

/**
 * How the swarm's effects are drawn. {@link #LIGHT} is added onto the scene (the plasma's type): glow, halos, flashes. {@link #PAINT}
 * is blended over it: the solid cores that must still read against a daylit sky, where added light washes out. Both are plain
 * position–colour quads through the game's own shaders, so a shader pack draws them too; neither writes depth, both test it.
 */
public final class DysonRenderTypes extends RenderType {
    public static final RenderType LIGHT = PlasmaRenderTypes.PLASMA;
    public static final RenderType PAINT = create("create_nuclearindustry_dyson_paint", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 1 << 14, false, false,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));

    /** {@link #PAINT} under a shader pack: through the lightning shader, which packs draw unlit (see {@code RocketRenderTypes.GLOW_PACK}). */
    public static final RenderType PAINT_PACK = create("create_nuclearindustry_dyson_paint_pack", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 1 << 14, false, false,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));

    /** The solid cores' type this frame: {@link #PAINT}, or {@link #PAINT_PACK} with a shader pack on. */
    public static RenderType paint() {
        return RocketRenderTypes.shaderPackInUse() ? PAINT_PACK : PAINT;
    }

    private DysonRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling,
                             boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
    }
}
