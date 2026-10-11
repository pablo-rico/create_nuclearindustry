package org.papiricoh.create_nuclearindustry.accelerator.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.accelerator.api.BeamReadings;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.AcceleratorControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.heating.client.PlasmaRenderTypes;
import org.papiricoh.create_nuclearindustry.fusion.heating.client.TorusMesh;

import java.util.List;

/**
 * Draws the beam while it runs: a thin glowing core in a faint halo along the injector line, round
 * the ring and down its extraction line, coloured by energy ({@link BeamLook}). It is light, not
 * paint (the plasma's additive render type), so the beamline blocks hide it except through the beam
 * pipe's windows.
 */
public class AcceleratorBeamRenderer implements BlockEntityRenderer<AcceleratorControllerBlockEntity> {
    static final float CORE = 0.05f;
    static final float HALO = 0.14f;
    private static final int VIEW_DISTANCE = 128;

    private TorusMesh ringCore;
    private TorusMesh ringHalo;
    private final float[] corners = new float[12];

    public AcceleratorBeamRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(AcceleratorControllerBlockEntity controller, float partialTicks, PoseStack ms, MultiBufferSource buffers,
                       int light, int overlay) {
        if (controller.getLevel() == null) {
            return;
        }
        BeamReadings r = controller.readings();
        if (!r.formed() || r.suspended() || !r.phase().hasBeam()) {
            return;
        }
        double time = controller.getLevel().getGameTime() + partialTicks;
        BeamLook look = BeamLook.of(r.energy(), Math.max(r.current(), r.sourceCurrent() * 0.5), r.lossFraction(), time);
        if (look.alpha() <= 0.004f) {
            return;
        }
        BlockPos origin = controller.getBlockPos();
        VertexConsumer buffer = buffers.getBuffer(PlasmaRenderTypes.PLASMA);
        PoseStack.Pose pose = ms.last();
        drawLine(buffer, pose, controller.injectorPath(), origin, look);
        drawLine(buffer, pose, controller.extractionPath(), origin, look);
        int radius = controller.ringRadius();
        if (radius > 0) {
            int segments = TorusMesh.majorSegmentsFor(radius);
            if (ringCore == null || !ringCore.matches(radius, CORE, CORE, segments, 6)) {
                ringCore = TorusMesh.build(radius, CORE, CORE, segments, 6);
                ringHalo = TorusMesh.build(radius, HALO, HALO, segments, 6);
            }
            BlockPos center = controller.ringCenter();
            ms.pushPose();
            ms.translate(center.getX() - origin.getX() + 0.5, center.getY() - origin.getY() + 0.5, center.getZ() - origin.getZ() + 0.5);
            PoseStack.Pose ringPose = ms.last();
            drawMesh(buffer, ringPose, ringHalo, look, look.alpha() * 0.3f);
            drawMesh(buffer, ringPose, ringCore, look, look.alpha());
            ms.popPose();
        }
    }

    private void drawLine(VertexConsumer buffer, PoseStack.Pose pose, List<BlockPos> points, BlockPos origin, BeamLook look) {
        for (int i = 0; i + 1 < points.size(); i++) {
            Vec3 a = Vec3.atCenterOf(points.get(i)).subtract(origin.getX(), origin.getY(), origin.getZ());
            Vec3 b = Vec3.atCenterOf(points.get(i + 1)).subtract(origin.getX(), origin.getY(), origin.getZ());
            box(buffer, pose, a, b, HALO, look, look.alpha() * 0.3f);
            box(buffer, pose, a, b, CORE, look, look.alpha());
        }
    }

    /** A square tube of half-width {@code w} from {@code a} to {@code b}, which differ along one axis. */
    private static void box(VertexConsumer buffer, PoseStack.Pose pose, Vec3 a, Vec3 b, float w, BeamLook look, float alpha) {
        float x0 = (float) Math.min(a.x, b.x) - w, x1 = (float) Math.max(a.x, b.x) + w;
        float y0 = (float) Math.min(a.y, b.y) - w, y1 = (float) Math.max(a.y, b.y) + w;
        float z0 = (float) Math.min(a.z, b.z) - w, z1 = (float) Math.max(a.z, b.z) + w;
        quad(buffer, pose, look, alpha, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0);
        quad(buffer, pose, look, alpha, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        quad(buffer, pose, look, alpha, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        quad(buffer, pose, look, alpha, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0);
        quad(buffer, pose, look, alpha, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        quad(buffer, pose, look, alpha, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
    }

    private static void quad(VertexConsumer buffer, PoseStack.Pose pose, BeamLook look, float alpha, float... v) {
        for (int c = 0; c < 4; c++) {
            buffer.addVertex(pose, v[c * 3], v[c * 3 + 1], v[c * 3 + 2]).setColor(look.red(), look.green(), look.blue(), alpha);
        }
    }

    private void drawMesh(VertexConsumer buffer, PoseStack.Pose pose, TorusMesh mesh, BeamLook look, float alpha) {
        for (int q = 0; q < mesh.quadCount(); q++) {
            mesh.quad(q, corners);
            for (int c = 0; c < 4; c++) {
                buffer.addVertex(pose, corners[c * 3], corners[c * 3 + 1], corners[c * 3 + 2])
                        .setColor(look.red(), look.green(), look.blue(), alpha);
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(AcceleratorControllerBlockEntity controller) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return VIEW_DISTANCE;
    }

    @Override
    public boolean shouldRender(AcceleratorControllerBlockEntity controller, Vec3 camera) {
        return bounds(controller).inflate(VIEW_DISTANCE).contains(camera);
    }

    /** The whole machine, so the beam is not culled when the controller itself is off screen. */
    @Override
    public AABB getRenderBoundingBox(AcceleratorControllerBlockEntity controller) {
        return bounds(controller);
    }

    private static AABB bounds(AcceleratorControllerBlockEntity controller) {
        AABB box = new AABB(controller.getBlockPos());
        for (BlockPos p : controller.injectorPath()) {
            box = box.minmax(new AABB(p));
        }
        for (BlockPos p : controller.extractionPath()) {
            box = box.minmax(new AABB(p));
        }
        int radius = controller.ringRadius();
        if (radius > 0) {
            BlockPos c = controller.ringCenter();
            box = box.minmax(new AABB(c.getX() - radius, c.getY(), c.getZ() - radius, c.getX() + radius + 1, c.getY() + 1,
                    c.getZ() + radius + 1));
        }
        return box;
    }
}
