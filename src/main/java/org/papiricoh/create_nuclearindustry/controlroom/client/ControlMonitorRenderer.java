package org.papiricoh.create_nuclearindustry.controlroom.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.controlroom.ControlMonitorBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorConfig;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorRow;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorTheme;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;

/**
 * Draws a whole control room screen from its origin monitor in the {@link MonitorStyle} it was set up
 * with: a list of readings with gauges, a two-minute history graph, or (on a single block) one big
 * reading per page. What it shows and in which order comes from the screen's {@link MonitorConfig};
 * the rows themselves from {@link MonitorRow}.
 */
public class ControlMonitorRenderer implements BlockEntityRenderer<ControlMonitorBlockEntity> {
    private static final float INSET = 1 / 16f;
    /** Size of one text pixel, in blocks. */
    private static final float PIXEL = 1 / 80f;
    /** Fine decoration is left out beyond this many blocks: too small to see, and it shimmers. */
    private static final double DETAIL_DISTANCE = 24;

    private final Font font;

    public ControlMonitorRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(ControlMonitorBlockEntity monitor, float partialTick, PoseStack ms, MultiBufferSource buffers,
                       int light, int overlay) {
        if (!monitor.isOrigin()) {
            return;
        }
        int blocksWide = monitor.screenWidth();
        int blocksHigh = monitor.screenHeight();
        MonitorConfig config = monitor.getConfig();
        MonitorPalette palette = MonitorPalette.of(config.theme());
        TargetKind kind = monitor.getReading().kind();
        boolean styled = config.theme() != MonitorTheme.STANDARD && kind != TargetKind.NONE && kind != TargetKind.NO_SIGNAL;
        ms.pushPose();
        ScreenDraw.onFrontFace(ms, monitor.facing());
        if (!styled) {
            // A styled screen paints its own background, in the same plane as the rest of it.
            ScreenDraw.rect(ms, buffers, INSET, INSET, blocksWide - INSET, blocksHigh - INSET, 0, palette.backdrop());
        }
        ms.translate(INSET, blocksHigh - INSET, 0.001);
        ms.scale(PIXEL, -PIXEL, PIXEL);
        float width = (blocksWide - 2 * INSET) / PIXEL;
        float height = (blocksHigh - 2 * INSET) / PIXEL;
        draw(monitor, config, palette, ms, buffers, width, height);
        ms.popPose();
    }

    private void draw(ControlMonitorBlockEntity monitor, MonitorConfig config, MonitorPalette palette, PoseStack ms,
                      MultiBufferSource buffers, float width, float height) {
        MonitorReading reading = monitor.getReading();
        if (reading.kind() == TargetKind.NONE) {
            ScreenDraw.textCentered(ms, buffers, font, MonitorDraw.label("unlinked"), width / 2, height / 2 - 10, palette.dim());
            ScreenDraw.textCentered(ms, buffers, font, MonitorDraw.label("unlinked.hint"), width / 2, height / 2 + 2, palette.dim());
            return;
        }
        if (reading.kind() == TargetKind.NO_SIGNAL) {
            ScreenDraw.textCentered(ms, buffers, font, MonitorDraw.label("no_signal"), width / 2, height / 2 - 10, palette.bad());
            ScreenDraw.textCentered(ms, buffers, font, Component.translatable(MonitorDraw.KEY + "no_signal." + reading.status()),
                    width / 2, height / 2 + 2, palette.dim());
            return;
        }
        boolean single = monitor.screenWidth() == 1 && monitor.screenHeight() == 1;
        int pages = monitor.pageCount();
        boolean blink = Util.getMillis() / 500 % 2 == 0;
        MonitorFrame frame = new MonitorFrame(monitor, reading, config, MonitorRow.visible(reading, config), palette, font, ms, buffers,
                width, height, single, Math.floorMod(monitor.getPage(), pages), pages, detail(monitor), blink);
        MonitorStyle.of(config.theme()).draw(frame);
    }

    /** Close enough for fine decoration; always in a Ponder scene, which shows screens up close in a level of its own. */
    private static boolean detail(ControlMonitorBlockEntity monitor) {
        Minecraft mc = Minecraft.getInstance();
        if (monitor.getLevel() != mc.level) {
            return true;
        }
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        BlockPos pos = monitor.getBlockPos();
        Direction right = monitor.right();
        Vec3 centre = Vec3.atCenterOf(pos).add(right.getStepX() * (monitor.screenWidth() - 1) / 2.0,
                (monitor.screenHeight() - 1) / 2.0, right.getStepZ() * (monitor.screenWidth() - 1) / 2.0);
        return camera.distanceToSqr(centre) < DETAIL_DISTANCE * DETAIL_DISTANCE;
    }

    @Override
    public AABB getRenderBoundingBox(ControlMonitorBlockEntity monitor) {
        BlockPos pos = monitor.getBlockPos();
        Direction right = monitor.right();
        BlockPos far = pos.relative(right, monitor.screenWidth() - 1).above(monitor.screenHeight() - 1);
        return new AABB(pos).minmax(new AABB(far));
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
