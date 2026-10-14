package org.papiricoh.create_nuclearindustry.controlroom.desk.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.papiricoh.create_nuclearindustry.controlroom.desk.BlockScreen;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlDeskBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.desk.SurfaceGeometry;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlModule;
import org.papiricoh.create_nuclearindustry.controlroom.desk.DeskGeometry;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleSurface;
import org.papiricoh.create_nuclearindustry.controlroom.desk.LedGroups;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleFunction;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleType;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the modules on a desk, table or wall panel, each as a base and a moving part animated from
 * its shown state, the digits of the LED displays headed there and the pictures of its screen modules.
 */
public class ControlDeskRenderer implements BlockEntityRenderer<ControlDeskBlockEntity> {
    private static final float PX = 1 / 16f;
    private final Font font;

    public ControlDeskRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(ControlDeskBlockEntity desk, float partialTick, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = desk.getBlockState();
        ModuleSurface surface = ModuleSurface.of(state);
        if (surface == null) {
            return;
        }
        float time = desk.getLevel() == null ? 0 : desk.getLevel().getGameTime() + partialTick;
        for (int slot = 0; slot < ControlDeskBlockEntity.SLOTS; slot++) {
            ControlModule module = desk.module(slot);
            if (module == null || !surface.hasSlot(state, slot)) {
                continue;
            }
            ms.pushPose();
            DeskGeometry.toSurfaceSlot(ms, state, slot);
            ms.translate(-0.5, 0, -0.5);
            renderModule(desk, slot, module, state, surface.vertical(), time, ms, buffers, light);
            ms.popPose();
        }
        List<LedGroups.Group> displays = desk.getLevel() == null ? List.of() : LedGroups.headedAt(desk.getLevel(), desk.getBlockPos());
        for (LedGroups.Group display : displays) {
            ms.pushPose();
            DeskGeometry.toSurfaceSlot(ms, state, display.head().slot());
            LedDisplayDraw.segments(desk.module(display.head().slot()), display.length(), ms, buffers);
            ms.popPose();
        }
        List<Screen> screens = screens(desk, state, surface);
        for (Screen screen : screens) {
            ms.pushPose();
            DeskGeometry.toSurfaceSlot(ms, state, screen.slot());
            ModuleScreenDraw.picture(screen.module(), screen.window(), screen.merged(), time, ms, buffers);
            ms.popPose();
        }
        // Text goes last: the font takes its own buffer, which closes the shared one the modules draw into.
        for (LedGroups.Group display : displays) {
            ms.pushPose();
            DeskGeometry.toSurfaceSlot(ms, state, display.head().slot());
            LedDisplayDraw.text(desk.module(display.head().slot()), display.length(), font, ms, buffers);
            ms.popPose();
        }
        for (Screen screen : screens) {
            ms.pushPose();
            DeskGeometry.toSurfaceSlot(ms, state, screen.slot());
            ModuleScreenDraw.text(screen.module(), screen.window(), font, ms, buffers);
            ms.popPose();
        }
    }

    /** A screen module to draw, from its slot, or a merged screen from slot 0 over the whole block. */
    private record Screen(int slot, ControlModule module, ModuleScreenDraw.Window window, boolean merged) {}

    private static List<Screen> screens(ControlDeskBlockEntity desk, BlockState state, ModuleSurface surface) {
        List<Screen> screens = new ArrayList<>();
        if (desk.mergedScreen()) {
            float pitch = surface == ModuleSurface.DESK ? SurfaceGeometry.DESK_ROW_PITCH : SurfaceGeometry.FLAT_ROW_PITCH;
            screens.add(new Screen(BlockScreen.HEAD, desk.module(BlockScreen.HEAD), ModuleScreenDraw.Window.merged(pitch), true));
            return screens;
        }
        for (int slot = 0; slot < ControlDeskBlockEntity.SLOTS; slot++) {
            ControlModule module = desk.module(slot);
            if (module != null && surface.hasSlot(state, slot) && isScreen(module.type())) {
                screens.add(new Screen(slot, module, ModuleScreenDraw.Window.single(module.type()), false));
            }
        }
        return screens;
    }

    private static boolean isScreen(ModuleType type) {
        return type == ModuleType.BAR_GRAPH || type == ModuleType.TREND_RECORDER || type == ModuleType.CORE_MAP
                || type == ModuleType.PLASMA_SECTION;
    }

    /** Taller than the block for what stands on a table, and as wide as the LED displays headed here. */
    @Override
    public AABB getRenderBoundingBox(ControlDeskBlockEntity desk) {
        BlockPos pos = desk.getBlockPos();
        AABB box = new AABB(pos).expandTowards(0, 0.5, 0);
        if (desk.getLevel() == null || ModuleSurface.of(desk.getBlockState()) == null) {
            return box;
        }
        for (LedGroups.Group display : LedGroups.headedAt(desk.getLevel(), pos)) {
            int beyond = (display.length() - display.cellsInHeadBlock() + 1) / 2;
            if (beyond > 0) {
                box = box.minmax(new AABB(pos.relative(ModuleSurface.rightOf(desk.getBlockState()), beyond)));
            }
        }
        return box;
    }

    /**
     * Draws a module's base and moving part, asking for the buffer right before each draw. {@code wall}
     * for a module standing on a wall panel, where a gauge's dial lies flat to be read straight on.
     */
    private static void renderModule(ControlDeskBlockEntity desk, int slot, ControlModule module, BlockState state, boolean wall,
                                     float time, PoseStack ms, MultiBufferSource buffers, int light) {
        CachedBuffers.partial(DeskPartials.base(module.type(), wall), state).light(light)
                .renderInto(ms, buffers.getBuffer(RenderType.solid()));
        if (module.type() == ModuleType.LED_DISPLAY || isScreen(module.type())) {
            // Digits and pictures are drawn per display or screen, from its head's slot.
            return;
        }
        if (module.type() == ModuleType.GUARDED_BUTTON) {
            boolean pressed = module.shown() > 0.5f;
            CachedBuffers.partial(DeskPartials.BUTTON_CAP, state).translate(0, -0.75f * PX * (pressed ? 1 : 0), 0).light(light)
                    .renderInto(ms, buffers.getBuffer(RenderType.solid()));
            float angle = CoverSwing.angle(desk, slot, module.coverOpen(), time);
            pivot(CachedBuffers.partial(DeskPartials.GUARDED_BUTTON_COVER, state), 8, 4.75f, 11.25f)
                    .rotateXDegrees(angle).translate(-8 * PX, -4.75f * PX, -11.25f * PX).light(light)
                    .renderInto(ms, buffers.getBuffer(RenderType.solid()));
            return;
        }
        float value = module.shown();
        boolean on = value > 0.5f;
        SuperByteBuffer part = switch (module.type()) {
            case PUSH_BUTTON -> CachedBuffers.partial(DeskPartials.BUTTON_CAP, state)
                    .translate(0, -0.75f * PX * (on ? 1 : 0), 0);
            case LEVER -> pivot(CachedBuffers.partial(DeskPartials.LEVER_HANDLE, state), 8, 1, 8)
                    .rotateXDegrees(on ? 30 : -30).translate(-8 * PX, -PX, -8 * PX);
            case DIAL -> pivot(CachedBuffers.partial(DeskPartials.DIAL_KNOB, state), 8, 0, 8)
                    .rotateYDegrees(-(value - 0.5f) * 270).translate(-8 * PX, 0, -8 * PX);
            // On a wall the needle turns in the dial's plane, clockwise as seen by the operator as the value rises.
            case GAUGE -> wall
                    ? pivot(CachedBuffers.partial(DeskPartials.GAUGE_WALL_NEEDLE, state), 8, 1.05f, 6.5f)
                    .rotateYDegrees(-(value - 0.5f) * 90).translate(-8 * PX, -1.05f * PX, -6.5f * PX)
                    : pivot(CachedBuffers.partial(DeskPartials.GAUGE_NEEDLE, state), 8, 1.5f, 5.85f)
                    .rotateZDegrees((value - 0.5f) * 90).translate(-8 * PX, -1.5f * PX, -5.85f * PX);
            case KEY_SWITCH -> pivot(CachedBuffers.partial(DeskPartials.KEY_SWITCH_BAT, state), 8, 1.5f, 8)
                    .rotateXDegrees(on ? 25 : -25).translate(-8 * PX, -1.5f * PX, -8 * PX);
            case LAMP -> CachedBuffers.partial(on ? DeskPartials.LAMP_LENS_LIT : DeskPartials.LAMP_LENS, state)
                    .color(lampColor(module.function()));
            // The setpoint knob turns like a dial's.
            case CONTROLLER -> pivot(CachedBuffers.partial(DeskPartials.CONTROLLER_KNOB, state), 8, 0, 6.75f)
                    .rotateYDegrees(-(value - 0.5f) * 270).translate(-8 * PX, 0, -6.75f * PX);
            case LED_DISPLAY, GUARDED_BUTTON, BAR_GRAPH, TREND_RECORDER, CORE_MAP, PLASMA_SECTION ->
                    throw new IllegalStateException("drawn above");
        };
        part.light(module.type() == ModuleType.LAMP && on ? LightTexture.FULL_BRIGHT : light)
                .renderInto(ms, buffers.getBuffer(RenderType.solid()));
        if (module.type() == ModuleType.CONTROLLER) {
            // Green while the loop drives the rods, dark in standby or with nothing to drive.
            boolean driving = module.active() && module.engaged() && module.demand() >= 0;
            CachedBuffers.partial(driving ? DeskPartials.CONTROLLER_LAMP_LIT : DeskPartials.CONTROLLER_LAMP, state)
                    .color(0xFF5BE05B)
                    .light(driving ? LightTexture.FULL_BRIGHT : light)
                    .renderInto(ms, buffers.getBuffer(RenderType.solid()));
        }
    }

    private static SuperByteBuffer pivot(SuperByteBuffer buffer, float x, float y, float z) {
        return buffer.translate(x * PX, y * PX, z * PX);
    }

    /** Red for SCRAM and alarms, green for running, amber for no signal. */
    private static int lampColor(ModuleFunction function) {
        return switch (function) {
            case LAMP_RUNNING -> 0xFF5BE05B;
            case LAMP_NO_SIGNAL -> 0xFFF0B030;
            default -> 0xFFFF4A3A;
        };
    }
}
