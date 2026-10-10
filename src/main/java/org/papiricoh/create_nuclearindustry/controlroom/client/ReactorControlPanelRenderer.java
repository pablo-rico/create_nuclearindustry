package org.papiricoh.create_nuclearindustry.controlroom.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import org.papiricoh.create_nuclearindustry.controlroom.PanelButton;
import org.papiricoh.create_nuclearindustry.controlroom.ReactorControlPanelBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.ReactorControlPanelBlockEntity.PanelState;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;

import java.util.Locale;

/** The panel's status strip and its six labelled buttons, lit by the state of the linked reactor. */
public class ReactorControlPanelRenderer implements BlockEntityRenderer<ReactorControlPanelBlockEntity> {
    private static final float PIXEL = 1 / 96f;
    private static final String KEY = "create_nuclearindustry.control_panel.";

    private final Font font;

    public ReactorControlPanelRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(ReactorControlPanelBlockEntity panel, float partialTick, PoseStack ms, MultiBufferSource buffers,
                       int light, int overlay) {
        PanelState state = panel.getShown();
        boolean live = state.kind() == TargetKind.REACTOR && state.formed();
        ms.pushPose();
        ScreenDraw.onFrontFace(ms, panel.facing());
        ScreenDraw.rect(ms, buffers, 1 / 16f, PanelButton.STRIP_BOTTOM, 15 / 16f, PanelButton.STRIP_TOP, 0, 0xFF0B1511);
        for (PanelButton button : PanelButton.values()) {
            ScreenDraw.rect(ms, buffers, button.u0(), button.v0(), button.u1(), button.v1(), 0, live ? color(button, state) : 0xFF3A403C);
        }

        // Text in pixels from the top-left of the face, y down.
        ms.translate(0, 1, 0.001);
        ms.scale(PIXEL, -PIXEL, PIXEL);
        float center = 48;
        if (!live) {
            Component message = Component.translatable(state.kind() == TargetKind.NONE ? KEY + "unlinked" : KEY + "no_reactor");
            fit(ms, buffers, message, center, 9, 84, 0xFFB0B8B0);
        } else {
            fit(ms, buffers, Component.literal(state.status()), center, 7.5f, 84, state.scramRequested() ? 0xFFFF5A4A : 0xFF6FDF6E);
            String rods = String.format(Locale.ROOT, "%.0f%%", state.rods() * 100)
                    + (state.rodTarget() >= 0 ? String.format(Locale.ROOT, " > %.0f%%", state.rodTarget() * 100) : "");
            fit(ms, buffers, Component.translatable(KEY + "rods", rods), center, 16.5f, 84, 0xFFB8F0C4);
        }
        for (PanelButton button : PanelButton.values()) {
            float x0 = button.u0() / PIXEL;
            float x1 = button.u1() / PIXEL;
            float y0 = (1 - button.v1()) / PIXEL;
            float y1 = (1 - button.v0()) / PIXEL;
            fit(ms, buffers, label(button, state), (x0 + x1) / 2, (y0 + y1) / 2 - 3.5f, x1 - x0 - 3, live ? 0xFFFFFFFF : 0xFF8A908C);
        }
        ms.popPose();
    }

    /** Centred text, shrunk to fit {@code width}. */
    private void fit(PoseStack ms, MultiBufferSource buffers, Component text, float center, float y, float width, int color) {
        float scale = Math.min(1, width / Math.max(1, font.width(text)));
        ms.pushPose();
        ms.translate(center, y + (1 - scale) * 4, 0);
        ms.scale(scale, scale, 1);
        ScreenDraw.textCentered(ms, buffers, font, text, 0, 0, color);
        ms.popPose();
    }

    private static Component label(PanelButton button, PanelState state) {
        return switch (button) {
            case SCRAM -> Component.translatable(KEY + (state.scramRequested() ? "reset" : "scram"));
            case AUTO_SCRAM -> state.autoEnabled()
                    ? Component.translatable(KEY + "auto", state.threshold())
                    : Component.translatable(KEY + "auto_off");
            case RODS_IN -> Component.translatable(KEY + "rods_in");
            case RODS_OUT -> Component.translatable(KEY + "rods_out");
            case LIMIT_DOWN -> Component.literal("-" + PanelButton.LIMIT_STEP + "°");
            case LIMIT_UP -> Component.literal("+" + PanelButton.LIMIT_STEP + "°");
        };
    }

    private static int color(PanelButton button, PanelState state) {
        return switch (button) {
            case SCRAM -> state.scramRequested() ? 0xFFD03A2E : 0xFF8A2A22;
            case AUTO_SCRAM -> state.tripped() ? 0xFFD03A2E : state.autoEnabled() ? 0xFF3E7A3A : 0xFF4A504C;
            case RODS_IN, RODS_OUT -> 0xFF2E5E8A;
            case LIMIT_DOWN, LIMIT_UP -> state.autoEnabled() ? 0xFF8A6A22 : 0xFF4A504C;
        };
    }
}
