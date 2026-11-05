package org.papiricoh.create_nuclearindustry.explosive.fx.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.fx.client.RocketRenderTypes;

import java.io.IOException;

/**
 * How explosions are drawn. Without a shader pack, puffs are textured billboards from the cloud atlas ({@code nuke_cloud.png})
 * through {@code shaders/core/nuke_cloud}: smoke tiles blended over the scene, fire tiles glowing by the temperature the atlas
 * paints into them, lightning added on, all in one far-to-near pass (premultiplied alpha, so fire and smoke sort together).
 * {@link #FIRE} adds light alone ({@code nuke_fire}): a fireball's corona, a conventional fireball's core.
 * <p>
 * A shader pack replaces the game's shaders: textured puffs through a mod shader would come out wrong, and plain position-colour
 * quads go through the pack's basic program, which lights them by a lightmap they lack and leaves them nearly black. Under a
 * pack puffs are soft vertex-coloured fans through the lightning shader, which packs draw unlit ({@link #CLOUD_PACK},
 * {@link #FIRE_PACK}), drawn with the block entities ({@link #stage}) since the weather stage goes through the pack's rain
 * program. None writes depth; all are hidden behind terrain.
 */
public final class NukeRenderTypes extends RenderType {
    public static final ResourceLocation ATLAS = id("textures/fx/nuke_cloud.png");
    public static final ResourceLocation IMPOSTOR_TEXTURE = id("textures/fx/nuke_impostor.png");
    public static final ResourceLocation AFTERIMAGE = id("textures/fx/flash_afterimage.png");
    /** Atlas tiles a row and a column. */
    public static final int TILES = 4;

    @Nullable
    private static ShaderInstance cloudShader;
    @Nullable
    private static ShaderInstance fireShader;

    /** Premultiplied alpha: smoke covers what is behind, fire and lightning add to it, in the same pass. */
    private static final TransparencyStateShard PREMULTIPLIED = new TransparencyStateShard("create_nuclearindustry_premultiplied", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });

    public static final RenderType CLOUD = create("create_nuclearindustry_nuke_cloud", DefaultVertexFormat.POSITION_TEX_COLOR,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(() -> cloudShader))
                    .setTextureState(new TextureStateShard(ATLAS, true, false))
                    .setTransparencyState(PREMULTIPLIED)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));
    /** The far impostor: the same shader on the impostor's texture (its UV moved right by 2 says so). */
    public static final RenderType IMPOSTOR = create("create_nuclearindustry_nuke_impostor", DefaultVertexFormat.POSITION_TEX_COLOR,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(() -> cloudShader))
                    .setTextureState(new TextureStateShard(IMPOSTOR_TEXTURE, true, false))
                    .setTransparencyState(PREMULTIPLIED)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));
    public static final RenderType FIRE = create("create_nuclearindustry_nuke_fire", DefaultVertexFormat.POSITION_TEX_COLOR,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(() -> fireShader))
                    .setTextureState(new TextureStateShard(ATLAS, true, false))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));
    /** Smoke under a shader pack: unlit through the lightning shader, blended. */
    public static final RenderType CLOUD_PACK = create("create_nuclearindustry_nuke_cloud_pack", DefaultVertexFormat.POSITION_COLOR,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));
    /** Fire and light under a shader pack: unlit, added on. */
    public static final RenderType FIRE_PACK = create("create_nuclearindustry_nuke_fire_pack", DefaultVertexFormat.POSITION_COLOR,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));
    /** Until our shaders load (or if they fail to): plain coloured fans, no pack. */
    public static final RenderType CLOUD_PLAIN = create("create_nuclearindustry_nuke_cloud_plain", DefaultVertexFormat.POSITION_COLOR,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));
    public static final RenderType FIRE_PLAIN = create("create_nuclearindustry_nuke_fire_plain", DefaultVertexFormat.POSITION_COLOR,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));
    /** The shader Distant Horizons' far pass draws the cloud with ({@code far_cloud}, textured, hidden by its terrain). */
    public static final ResourceLocation FAR = id("far_cloud");

    private NukeRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling,
                            boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
    }

    private static RenderType create(String name, VertexFormat format, CompositeState state) {
        // Built and sorted by hand.
        return RenderType.create(name, format, VertexFormat.Mode.QUADS, 1 << 12, false, false, state);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }

    static void registerShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), id("nuke_cloud"), DefaultVertexFormat.POSITION_TEX_COLOR),
                    loaded -> cloudShader = loaded);
            event.registerShader(new ShaderInstance(event.getResourceProvider(), id("nuke_fire"), DefaultVertexFormat.POSITION_TEX_COLOR),
                    loaded -> fireShader = loaded);
        } catch (IOException e) {
            throw new RuntimeException("Could not load the explosion shaders", e);
        }
    }

    /** Whether puffs draw textured this frame: our shaders loaded and no shader pack on. */
    public static boolean textured() {
        return cloudShader != null && fireShader != null && !RocketRenderTypes.shaderPackInUse();
    }

    public static boolean pack() {
        return RocketRenderTypes.shaderPackInUse();
    }

    /** The smoke type this frame (with fire and lightning in it when textured). */
    public static RenderType cloud() {
        return textured() ? CLOUD : pack() ? CLOUD_PACK : CLOUD_PLAIN;
    }

    /** The added-light type this frame. */
    public static RenderType fire() {
        return textured() ? FIRE : pack() ? FIRE_PACK : FIRE_PLAIN;
    }

    /** The shader the cloud's own buffers draw with, for its haze uniform, or null. */
    @Nullable
    static ShaderInstance cloudShader() {
        return cloudShader;
    }

    /**
     * The stage explosions draw in: after the weather, or with the block entities under a shader pack, whose weather program may
     * treat whatever is drawn there as rain and hide it on a clear day.
     */
    public static RenderLevelStageEvent.Stage stage() {
        return pack() ? RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES : RenderLevelStageEvent.Stage.AFTER_WEATHER;
    }
}
