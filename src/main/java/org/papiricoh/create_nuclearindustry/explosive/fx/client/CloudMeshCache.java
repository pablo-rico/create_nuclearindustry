package org.papiricoh.create_nuclearindustry.explosive.fx.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.papiricoh.create_nuclearindustry.explosive.fx.model.CloudEvolution;
import org.papiricoh.create_nuclearindustry.explosive.fx.model.CloudLighting;

/**
 * One mushroom cloud's mesh, kept on the GPU and rebuilt only a few times a second ({@code QualityPreset.rebuildHz}; every frame
 * while the fireball still changes fast): the cloud moves slowly, so redrawing the same buffer in between costs next to nothing.
 * Each rebuild asks {@link CloudEvolution} for the puffs, lights them, sorts them far to near from where the camera is and writes
 * them as billboards facing it, relative to the cloud's ground zero, into one shared byte buffer that is reused. A camera moving
 * between rebuilds sees the puffs turned a little off it, which spheres of smoke hide.
 * <p>
 * Without a shader pack it is one textured buffer (smoke, fire and lightning sorted together); with one, two buffers of plain
 * fans: the smoke blended, then the fire added on.
 */
public final class CloudMeshCache implements AutoCloseable {
    /** Bytes every cloud's rebuild writes through, reused. */
    @Nullable
    private static ByteBufferBuilder bytes;

    private final PuffList puffs = new PuffList();
    @Nullable
    private VertexBuffer smoke;
    @Nullable
    private VertexBuffer light;
    private boolean hasSmoke, hasLight;
    private double builtAt = Double.NEGATIVE_INFINITY;
    private boolean builtTextured;
    private int quads;

    /** The puffs of the last rebuild, lit and sorted, in the cloud's frame (for Distant Horizons' far pass). */
    PuffList puffs() {
        return puffs;
    }

    /** Quads in the buffers now. */
    int quads() {
        return quads;
    }

    /** Forgets the last rebuild, so the next {@link #update} rebuilds whatever the time. */
    void invalidate() {
        builtAt = Double.NEGATIVE_INFINITY;
    }

    /**
     * Rebuilds the mesh if {@code interval} seconds have passed since the last rebuild (or the look changed): the cloud
     * {@code age} seconds in with {@code counts} puffs, each {@code growth} times bigger, lit by {@code sky}, facing a camera at
     * {@code (cx, cy, cz)} from ground zero.
     */
    void update(CloudEvolution cloud, double age, CloudEvolution.Counts counts, float growth, CloudLighting.Sky sky, double cx, double cy,
                double cz, double interval, boolean textured) {
        if (age - builtAt < interval && age >= builtAt && textured == builtTextured) return;
        builtAt = age;
        builtTextured = textured;
        puffs.clear();
        puffs.scale(growth, 1);
        cloud.emit(age, counts, puffs);
        puffs.light(sky, !textured && NukeRenderTypes.pack() ? (float) (0.75 * sky.night()) : 0);
        puffs.sort(cx, cy, cz);
        if (bytes == null) bytes = new ByteBufferBuilder(1 << 20);
        quads = 0;
        if (textured) {
            hasSmoke = upload(smoke = buffer(smoke), PuffList.Filter.ALL, true, cx, cy, cz);
            hasLight = false;
        } else {
            hasSmoke = upload(smoke = buffer(smoke), PuffList.Filter.SMOKE, false, cx, cy, cz);
            hasLight = upload(light = buffer(light), PuffList.Filter.LIGHT, false, cx, cy, cz);
        }
    }

    private static VertexBuffer buffer(@Nullable VertexBuffer existing) {
        return existing != null ? existing : new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
    }

    private boolean upload(VertexBuffer into, PuffList.Filter filter, boolean textured, double cx, double cy, double cz) {
        BufferBuilder builder = new BufferBuilder(bytes, VertexFormat.Mode.QUADS,
                textured ? DefaultVertexFormat.POSITION_TEX_COLOR : DefaultVertexFormat.POSITION_COLOR);
        int n = textured ? puffs.writeTextured(builder, 0, 0, 0, cx, cy, cz, 0, filter) : puffs.writeFans(builder, 0, 0, 0, cx, cy, cz, 0, filter);
        MeshData mesh = builder.build();
        if (mesh == null) return false;
        quads += n;
        into.bind();
        into.upload(mesh);
        VertexBuffer.unbind();
        return true;
    }

    /**
     * Draws the cloud with {@code modelView} (already moved to its ground zero and pulled in if far) and {@code projection}; the
     * haze over it is {@code haze} (r, g, b, amount).
     */
    void draw(Matrix4f modelView, Matrix4f projection, float[] haze) {
        if (builtTextured) {
            if (hasSmoke && smoke != null) drawBuffer(smoke, NukeRenderTypes.CLOUD, modelView, projection, haze);
        } else {
            boolean pack = NukeRenderTypes.pack();
            if (hasSmoke && smoke != null) drawBuffer(smoke, pack ? NukeRenderTypes.CLOUD_PACK : NukeRenderTypes.CLOUD_PLAIN, modelView, projection, null);
            if (hasLight && light != null) drawBuffer(light, pack ? NukeRenderTypes.FIRE_PACK : NukeRenderTypes.FIRE_PLAIN, modelView, projection, null);
        }
    }

    private static void drawBuffer(VertexBuffer buffer, RenderType type, Matrix4f modelView, Matrix4f projection, @Nullable float[] haze) {
        type.setupRenderState();
        ShaderInstance shader = RenderSystem.getShader();
        if (shader != null) {
            if (haze != null) shader.safeGetUniform("HazeColor").set(haze[0], haze[1], haze[2], haze[3]);
            buffer.bind();
            buffer.drawWithShader(modelView, projection, shader);
            VertexBuffer.unbind();
        }
        type.clearRenderState();
    }

    @Override
    public void close() {
        if (smoke != null) smoke.close();
        if (light != null) light.close();
        smoke = null;
        light = null;
        hasSmoke = hasLight = false;
    }
}
