package org.papiricoh.create_nuclearindustry.antimatter.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.io.IOException;

/**
 * The antimatter cloud's render types. Both draw block-atlas sprites through the cloud shader
 * ({@code shaders/core/antimatter_cloud}), which reads the sprite's brightness as density: the
 * opaque grey sprites then draw soft puffs in the vertex colour. The body blends over what is
 * behind, so the cloud keeps its colour in daylight; the glow is added onto it, light rather than
 * paint. Smoothly filtered, seen from both sides, never writing depth (the trap's heads still hide
 * the cloud, and its glass, drawn later, tints it). Until the shader loads, the vanilla textured
 * shader stands in.
 */
public final class AntimatterRenderTypes extends RenderType {
    @Nullable
    private static ShaderInstance cloudShader;
    private static final ShaderStateShard CLOUD_SHADER = new ShaderStateShard(
            () -> cloudShader != null ? cloudShader : GameRenderer.getPositionTexColorShader());

    public static final RenderType CLOUD_BODY = cloud("create_nuclearindustry_antimatter_cloud_body", TRANSLUCENT_TRANSPARENCY);
    public static final RenderType CLOUD_GLOW = cloud("create_nuclearindustry_antimatter_cloud_glow", LIGHTNING_TRANSPARENCY);

    private AntimatterRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling,
                                  boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
    }

    private static RenderType cloud(String name, TransparencyStateShard transparency) {
        // A handful of layers per trap, drawn back to front by the renderer: no sorting needed.
        return create(name, DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 1 << 12, false, false,
                CompositeState.builder()
                        .setShaderState(CLOUD_SHADER)
                        .setTextureState(new TextureStateShard(TextureAtlas.LOCATION_BLOCKS, true, false))
                        .setTransparencyState(transparency)
                        .setWriteMaskState(COLOR_WRITE)
                        .setCullState(NO_CULL)
                        .createCompositeState(false));
    }

    static void registerShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "antimatter_cloud"),
                    DefaultVertexFormat.POSITION_TEX_COLOR), loaded -> cloudShader = loaded);
        } catch (IOException e) {
            throw new RuntimeException("Could not load the antimatter cloud shader", e);
        }
    }
}
