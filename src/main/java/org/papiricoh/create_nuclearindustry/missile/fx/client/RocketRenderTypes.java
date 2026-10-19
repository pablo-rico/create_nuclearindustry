package org.papiricoh.create_nuclearindustry.missile.fx.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * How the rocket effects are drawn. The light (flames, flares, flashes, plasma) is added onto the scene; the smoke, steam and
 * dust are blended over it. Both go through shaders of their own ({@code shaders/core/rocket_glow} and {@code rocket_smoke})
 * that soften each quad into its shape and make it flicker or billow. Neither writes depth; both are hidden behind terrain.
 * <p>
 * A shader pack (Iris or Oculus) replaces the game's own shaders and may not draw ours: then, and until ours load, the plain
 * coloured types stand in, and the quads carry their soft edges in their vertex colours instead ({@link #shaded()}).
 */
public final class RocketRenderTypes extends RenderType {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Nullable
    private static ShaderInstance glowShader;
    @Nullable
    private static ShaderInstance smokeShader;

    public static final RenderType GLOW = create("create_nuclearindustry_rocket_glow", DefaultVertexFormat.POSITION_TEX_COLOR,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(() -> glowShader))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));
    public static final RenderType SMOKE = create("create_nuclearindustry_rocket_smoke", DefaultVertexFormat.POSITION_TEX_COLOR,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(() -> smokeShader))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));
    public static final RenderType GLOW_PLAIN = create("create_nuclearindustry_rocket_glow_plain", DefaultVertexFormat.POSITION_COLOR,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));
    public static final RenderType SMOKE_PLAIN = create("create_nuclearindustry_rocket_smoke_plain", DefaultVertexFormat.POSITION_COLOR,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));

    @Nullable
    private static MethodHandle shaderPackInUse;
    private static boolean irisChecked;

    private RocketRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling,
                              boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
    }

    private static RenderType create(String name, VertexFormat format, CompositeState state) {
        // Built and drawn by hand, already sorted where it matters.
        return RenderType.create(name, format, VertexFormat.Mode.QUADS, 1 << 12, false, false, state);
    }

    static void registerShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "rocket_glow"),
                    DefaultVertexFormat.POSITION_TEX_COLOR), loaded -> glowShader = loaded);
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "rocket_smoke"),
                    DefaultVertexFormat.POSITION_TEX_COLOR), loaded -> smokeShader = loaded);
        } catch (IOException e) {
            throw new RuntimeException("Could not load the rocket effect shaders", e);
        }
    }

    /** Whether the effects draw through their own shaders this frame: they are loaded and no shader pack is in use. */
    public static boolean shaded() {
        return glowShader != null && smokeShader != null && !shaderPackInUse();
    }

    public static RenderType glow(boolean shaded) {
        return shaded ? GLOW : GLOW_PLAIN;
    }

    public static RenderType smoke(boolean shaded) {
        return shaded ? SMOKE : SMOKE_PLAIN;
    }

    /** Whether Iris (or Oculus, which shares its API) has a shader pack on. */
    private static boolean shaderPackInUse() {
        if (!irisChecked) {
            irisChecked = true;
            if (ModList.get() != null && (ModList.get().isLoaded("iris") || ModList.get().isLoaded("oculus"))) {
                try {
                    Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                    Object instance = api.getMethod("getInstance").invoke(null);
                    shaderPackInUse = MethodHandles.publicLookup().findVirtual(api, "isShaderPackInUse", MethodType.methodType(boolean.class))
                            .bindTo(instance);
                } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
                    LOGGER.warn("Could not ask Iris whether a shader pack is on; rocket effects use their plain look", e);
                    shaderPackInUse = MethodHandles.constant(boolean.class, true);
                }
            }
        }
        if (shaderPackInUse == null) return false;
        try {
            return (boolean) shaderPackInUse.invoke();
        } catch (Throwable e) {
            return true;
        }
    }
}
