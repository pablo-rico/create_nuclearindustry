package org.papiricoh.create_nuclearindustry.fusion.heating.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakReadings;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.heating.FusionViewportBlock;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakFormedLook;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakGeometry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Draws a formed tokamak's skin ({@link TokamakSkinMesh}) and the glow of its energised solenoid,
 * from the controller's renderer. It finds the windows and the parts poking out by looking at the
 * shell blocks on the client, so no extra sync is needed, and rechecks them and the light now and then.
 */
final class TokamakSkinRenderer {
    private static final int RESCAN_TICKS = 40;
    private static final int RELIGHT_TICKS = 20;
    /** The liner is the dark inside of the vessel. */
    private static final float LINER_SHADE = 0.35f;

    private static final Map<TokamakSkinMesh.Kind, ResourceLocation> SPRITES = new EnumMap<>(TokamakSkinMesh.Kind.class);

    static {
        SPRITES.put(TokamakSkinMesh.Kind.SKIN, sprite("skin"));
        SPRITES.put(TokamakSkinMesh.Kind.LINER, sprite("skin"));
        SPRITES.put(TokamakSkinMesh.Kind.CHAMFER, sprite("skin_chamfer"));
        SPRITES.put(TokamakSkinMesh.Kind.BAND, sprite("skin_band"));
        SPRITES.put(TokamakSkinMesh.Kind.WINDOW, sprite("window"));
        SPRITES.put(TokamakSkinMesh.Kind.GLASS, sprite("window_glass"));
    }

    private static final ResourceLocation SOLENOID_GLOW = sprite("solenoid_glow");

    private TokamakSkinMesh mesh;
    private BlockPos meshCenter;
    private List<int[]> viewports = List.of();
    private List<int[]> features = List.of();
    private long scannedAt = Long.MIN_VALUE;
    private int[] light = new int[0];
    private long litAt = Long.MIN_VALUE;

    private static ResourceLocation sprite(String name) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "block/tokamak/" + name);
    }

    /** {@code ms} is already centred on the middle solenoid block. */
    void render(FusionReactorBlockEntity controller, PoseStack ms, MultiBufferSource buffers) {
        Level level = controller.getLevel();
        TokamakReadings readings = controller.readings();
        int radius = readings.majorRadius();
        if (level == null || !readings.formed() || !controller.showsSkin() || radius < TokamakGeometry.MIN_RADIUS) {
            mesh = null;
            return;
        }
        BlockPos center = controller.getCenter();
        long time = level.getGameTime();
        if (mesh == null || mesh.radius() != radius || !center.equals(meshCenter) || time - scannedAt >= RESCAN_TICKS || time < scannedAt) {
            rescan(level, center, radius);
            scannedAt = time;
        }
        if (time - litAt >= RELIGHT_TICKS || time < litAt || light.length != mesh.quads().size()) {
            relight(level, center);
            litAt = time;
        }

        PoseStack.Pose pose = ms.last();
        TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
        Map<TokamakSkinMesh.Kind, TextureAtlasSprite> sprites = new EnumMap<>(TokamakSkinMesh.Kind.class);
        SPRITES.forEach((kind, id) -> sprites.put(kind, atlas.getSprite(id)));
        // Each layer asks for its own buffer right before drawing into it.
        draw(buffers.getBuffer(RenderType.solid()), pose, sprites, TokamakSkinMesh.Kind.SKIN, TokamakSkinMesh.Kind.CHAMFER,
                TokamakSkinMesh.Kind.BAND, TokamakSkinMesh.Kind.LINER);
        draw(buffers.getBuffer(RenderType.cutout()), pose, sprites, TokamakSkinMesh.Kind.WINDOW);
        draw(buffers.getBuffer(RenderType.translucent()), pose, sprites, TokamakSkinMesh.Kind.GLASS);
        solenoidGlow(readings, ms, buffers, atlas.getSprite(SOLENOID_GLOW));
    }

    private void rescan(Level level, BlockPos center, int radius) {
        TokamakGeometry geometry = TokamakGeometry.of(radius);
        List<int[]> windows = new ArrayList<>();
        List<int[]> parts = new ArrayList<>();
        for (int i = 0; i < geometry.shellCount(); i++) {
            int[] cell = {geometry.shellX(i), geometry.shellY(i), geometry.shellZ(i)};
            BlockState state = level.getBlockState(center.offset(cell[0], cell[1], cell[2]));
            boolean hidden = state.hasProperty(TokamakFormedLook.HIDDEN) && state.getValue(TokamakFormedLook.HIDDEN);
            if (hidden && state.getBlock() instanceof FusionViewportBlock) {
                windows.add(cell);
            } else if (!hidden && !state.isAir()) {
                parts.add(cell);
            }
        }
        if (mesh == null || mesh.radius() != radius || !center.equals(meshCenter) || !same(windows, viewports) || !same(parts, features)) {
            mesh = TokamakSkinMesh.build(radius, windows, parts);
            meshCenter = center;
            viewports = windows;
            features = parts;
            light = new int[0];
        }
    }

    private static boolean same(List<int[]> a, List<int[]> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!java.util.Arrays.equals(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    private void relight(Level level, BlockPos center) {
        List<TokamakSkinMesh.Quad> quads = mesh.quads();
        light = new int[quads.size()];
        for (int q = 0; q < quads.size(); q++) {
            int[] cell = quads.get(q).light();
            light[q] = LevelRenderer.getLightColor(level, center.offset(cell[0], cell[1], cell[2]));
        }
    }

    private void draw(VertexConsumer buffer, PoseStack.Pose pose, Map<TokamakSkinMesh.Kind, TextureAtlasSprite> sprites,
                      TokamakSkinMesh.Kind... kinds) {
        List<TokamakSkinMesh.Quad> quads = mesh.quads();
        for (int q = 0; q < quads.size(); q++) {
            TokamakSkinMesh.Quad quad = quads.get(q);
            if (!matches(quad.kind(), kinds)) {
                continue;
            }
            TextureAtlasSprite sprite = sprites.get(quad.kind());
            float shade = shade(quad.nx(), quad.ny(), quad.nz()) * (quad.kind() == TokamakSkinMesh.Kind.LINER ? LINER_SHADE : 1);
            float[] xyz = quad.xyz();
            float[] uv = quad.uv();
            for (int c = 0; c < 4; c++) {
                buffer.addVertex(pose, xyz[c * 3], xyz[c * 3 + 1], xyz[c * 3 + 2])
                        .setColor(shade, shade, shade, 1)
                        .setUv(sprite.getU(uv[c * 2]), sprite.getV(uv[c * 2 + 1]))
                        .setLight(light[q])
                        .setNormal(pose, quad.nx(), quad.ny(), quad.nz());
            }
        }
    }

    private static boolean matches(TokamakSkinMesh.Kind kind, TokamakSkinMesh.Kind[] kinds) {
        for (TokamakSkinMesh.Kind k : kinds) {
            if (k == kind) {
                return true;
            }
        }
        return false;
    }

    /** Vanilla's face shading (top 1, bottom 0.5, north/south 0.8, east/west 0.6), blended by the normal. */
    static float shade(float nx, float ny, float nz) {
        float vertical = ny > 0 ? 1.0f : 0.5f;
        return Math.min(1, nx * nx * 0.6f + ny * ny * vertical + nz * nz * 0.8f);
    }

    /** The ribs of the solenoid light up with the toroidal field the magnets hold. */
    private static void solenoidGlow(TokamakReadings readings, PoseStack ms, MultiBufferSource buffers, TextureAtlasSprite sprite) {
        double target = readings.fieldTarget();
        float glow = target > 0 ? (float) Math.min(1, Math.max(0, readings.field() / target)) : 0;
        if (glow <= 0.02f) {
            return;
        }
        // Just proud of the winding core, behind the rib rings: only the gaps between ribs show.
        float h = (float) (6.05 / 16);
        float bottom = -0.5f - TokamakGeometry.HALF_HEIGHT;
        PoseStack.Pose pose = ms.last();
        VertexConsumer buffer = buffers.getBuffer(RenderType.translucent());
        float[][] sides = {{-h, -h, h, -h, 0, -1}, {h, h, -h, h, 0, 1}, {h, -h, h, h, 1, 0}, {-h, h, -h, -h, -1, 0}};
        for (float[] side : sides) {
            for (int block = 0; block < 2 * TokamakGeometry.HALF_HEIGHT + 1; block++) {
                float y0 = bottom + block;
                float y1 = y0 + 1;
                // Both windings: the texture is the same from either side.
                for (int pass = 0; pass < 2; pass++) {
                    float a = pass == 0 ? 0 : 1;
                    float xa = pass == 0 ? side[0] : side[2], za = pass == 0 ? side[1] : side[3];
                    float xb = pass == 0 ? side[2] : side[0], zb = pass == 0 ? side[3] : side[1];
                    glowVertex(buffer, pose, xa, y0, za, sprite.getU(a), sprite.getV(1), glow, side);
                    glowVertex(buffer, pose, xb, y0, zb, sprite.getU(1 - a), sprite.getV(1), glow, side);
                    glowVertex(buffer, pose, xb, y1, zb, sprite.getU(1 - a), sprite.getV(0), glow, side);
                    glowVertex(buffer, pose, xa, y1, za, sprite.getU(a), sprite.getV(0), glow, side);
                }
            }
        }
    }

    private static void glowVertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float u, float v, float glow, float[] side) {
        buffer.addVertex(pose, x, y, z).setColor(1, 1, 1, glow).setUv(u, v).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, side[4], 0, side[5]);
    }
}
