package org.papiricoh.create_nuclearindustry.integration.dh;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.config.EDhApiDepthDirection;
import com.seibel.distanthorizons.api.enums.config.EDhApiDepthRange;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiRenderProxy;
import com.seibel.distanthorizons.api.methods.events.DhApiEventRegister;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiAfterRenderEvent;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeRenderEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiCancelableEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.distanthorizons.api.objects.DhApiResult;
import com.seibel.distanthorizons.api.objects.math.DhApiMat4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.explosive.client.NuclearExplosionEffects;
import org.slf4j.Logger;

import java.io.IOException;

/**
 * Draws the far part of a nuclear cloud in Distant Horizons' pass ({@link DhApiAfterRenderEvent}), which
 * comes right after its distant terrain and before the vanilla terrain: the puffs past the vanilla
 * terrain's edge, with Distant Horizons' projection so they are not cut off at the vanilla far plane, and
 * hidden behind its terrain by a look at its depth (the {@code far_cloud} shader), forward or reversed. The vanilla terrain,
 * all nearer than these puffs, then draws over them where it should; the nearer puffs stay in the vanilla
 * pass. The matrices come from {@link DhApiBeforeRenderEvent} of the same frame.
 * <p>
 * When Distant Horizons defers its transparent pass until after the vanilla terrain (with shader packs),
 * the cloud stays all in the vanilla pass. Costs nothing with no detonation in sight: the pass returns at once.
 */
final class DhFarCloudPass {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Matrix4f MODEL_VIEW = new Matrix4f();
    private static final Matrix4f PROJECTION = new Matrix4f();
    private static final Matrix4f INVERSE_PROJECTION = new Matrix4f();
    private static boolean captured;
    private static ShaderInstance shader;
    /** Set when Distant Horizons' API failed us mid-frame: the cloud then stays in the vanilla pass. */
    private static boolean broken;

    private DhFarCloudPass() {}

    static void register(IEventBus modEventBus) {
        modEventBus.addListener(DhFarCloudPass::onRegisterShaders);
        DhApiEventRegister.on(DhApiBeforeRenderEvent.class, new DhApiBeforeRenderEvent() {
            @Override
            public void beforeRender(DhApiCancelableEventParam<DhApiRenderParam> event) {
                if (!broken) {
                    capture(event.value);
                }
            }
        });
        DhApiEventRegister.on(DhApiAfterRenderEvent.class, new DhApiAfterRenderEvent() {
            @Override
            public void afterRender(DhApiEventParam<Void> event) {
                if (broken) {
                    return;
                }
                try {
                    draw();
                } catch (LinkageError | RuntimeException e) {
                    broken = true;
                    LOGGER.warn("Drawing the nuclear cloud over Distant Horizons failed; it stays within the vanilla view", e);
                }
            }
        });
    }

    private static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "far_cloud"), DefaultVertexFormat.POSITION_COLOR),
                    loaded -> shader = loaded);
        } catch (IOException e) {
            throw new RuntimeException("Could not load the far nuclear cloud shader", e);
        }
    }

    private static void capture(DhApiRenderParam param) {
        if (param == null) {
            captured = false;
            return;
        }
        // The vanilla view (camera-relative, like the cloud's quads) seen through Distant Horizons' lens.
        set(MODEL_VIEW, param.mcModelViewMatrix);
        set(PROJECTION, param.dhProjectionMatrix);
        captured = true;
    }

    /** Distant Horizons keeps its matrices by rows; JOML takes them by columns. */
    private static void set(Matrix4f into, DhApiMat4f m) {
        into.set(m.m00, m.m10, m.m20, m.m30,
                m.m01, m.m11, m.m21, m.m31,
                m.m02, m.m12, m.m22, m.m32,
                m.m03, m.m13, m.m23, m.m33);
    }

    private static void draw() {
        Minecraft mc = Minecraft.getInstance();
        if (!captured || shader == null || mc.level == null) {
            return;
        }
        captured = false;
        IDhApiRenderProxy proxy = DhApi.Delayed.renderProxy;
        if (proxy == null || deferred(proxy)) {
            return;
        }
        float partial = mc.getTimer().getGameTimeDeltaPartialTick(false);
        MeshData mesh = NuclearExplosionEffects.farMesh(partial, mc.gameRenderer.getMainCamera(), mc.gameRenderer.getRenderDistance());
        if (mesh == null) {
            return;
        }
        int depth = depthTexture(proxy);

        RenderTarget target = mc.getMainRenderTarget();
        target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(PROJECTION, VertexSorting.DISTANCE_TO_ORIGIN);
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.set(MODEL_VIEW);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        // Distant Horizons binds its own textures and arrays behind the state cache's back: bind ours for real.
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, Math.max(0, depth));
        RenderSystem.setShaderTexture(0, Math.max(0, depth));
        shader.safeGetUniform("Occlude").set(depth > 0 ? 1.0f : 0.0f);
        shader.safeGetUniform("InverseProjMat").set(PROJECTION.invert(INVERSE_PROJECTION));
        shader.safeGetUniform("ScreenSize").set((float) target.width, (float) target.height);
        shader.safeGetUniform("EmptyDepth").set(proxy.getDepthDirection() == EDhApiDepthDirection.REVERSE_Z ? 0.0f : 1.0f);
        shader.safeGetUniform("DepthZeroToOne").set(proxy.getDepthRange() == EDhApiDepthRange.ZERO_TO_POS_ONE ? 1.0f : 0.0f);
        RenderSystem.setShader(() -> shader);
        BufferUploader.invalidate();
        BufferUploader.drawWithShader(mesh);

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();
    }

    /** Whether Distant Horizons leaves its transparent pass until after the vanilla terrain, or is not ready. */
    private static boolean deferred(IDhApiRenderProxy proxy) {
        try {
            proxy.getDepthDirection();
            return proxy.getDeferTransparentRendering();
        } catch (IllegalStateException notReady) {
            return true;
        }
    }

    /** Distant Horizons' depth texture, or -1 to draw without hiding behind its terrain when it cannot be had. */
    private static int depthTexture(IDhApiRenderProxy proxy) {
        DhApiResult<Integer> id = proxy.getDhDepthTextureGlId();
        return id.success && id.payload != null ? id.payload : -1;
    }
}
