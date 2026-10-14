package org.papiricoh.create_nuclearindustry.controlroom.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import org.papiricoh.create_nuclearindustry.controlroom.ControlMonitorBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorConfig;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorRow;

import java.util.List;

/**
 * Everything a {@link MonitorStyle} draws one screen from, for one frame. The pose is in screen
 * pixels (80 to a block), origin at the top-left inside the frame, y down; {@code width} and
 * {@code height} are the drawable size.
 *
 * @param lines  the rows the screen shows, in its order
 * @param single a screen of one block, which pages through big readings
 * @param page   the page shown, already within {@code pages}
 * @param detail close enough for fine decoration (scan lines, dotted grids, glow)
 * @param blink  the on half of a once-a-second blink
 */
public record MonitorFrame(ControlMonitorBlockEntity monitor, MonitorReading reading, MonitorConfig config,
                           List<MonitorRow.Line> lines, MonitorPalette palette, Font font, PoseStack ms, MultiBufferSource buffers,
                           float width, float height, boolean single, int page, int pages, boolean detail, boolean blink) {
}
