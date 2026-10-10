package org.papiricoh.create_nuclearindustry.controlroom.desk.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.joml.Vector3f;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlDeskBlock;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlDeskBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlModule;
import org.papiricoh.create_nuclearindustry.controlroom.desk.DeskGeometry;
import org.papiricoh.create_nuclearindustry.controlroom.desk.DeskShape;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleFunction;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleType;

/**
 * Draws the modules on a desk's slope, each as a base and a moving part animated from its shown
 * state, and floats the label and reading of the module under the crosshair above it.
 */
public class ControlDeskRenderer implements BlockEntityRenderer<ControlDeskBlockEntity> {
    private static final float PX = 1 / 16f;
    /** Name-tag text is 0.025; module labels are read from arm's length, so smaller. */
    private static final float LABEL_SCALE = 0.01f;
    private final Font font;

    public ControlDeskRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(ControlDeskBlockEntity desk, float partialTick, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = desk.getBlockState();
        DeskShape shape = state.getValue(ControlDeskBlock.SHAPE);
        for (int slot = 0; slot < ControlDeskBlockEntity.SLOTS; slot++) {
            ControlModule module = desk.module(slot);
            if (module == null || !shape.hasSlot(slot)) {
                continue;
            }
            ms.pushPose();
            DeskGeometry.toModel(ms, state);
            DeskGeometry.toSlot(ms, shape, slot);
            ms.translate(-0.5, 0, -0.5);
            renderModule(module, state, ms, buffers, light);
            ms.popPose();
        }
        // Text goes last: the font takes its own buffer, which closes the shared one the modules draw into.
        int hovered = Minecraft.getInstance().options.hideGui ? -1 : hoveredSlot(desk);
        if (hovered >= 0 && shape.hasSlot(hovered) && desk.module(hovered) != null) {
            renderLabel(desk, state, hovered, desk.module(hovered), ms, buffers);
        }
    }

    /** Draws a module's base and moving part, asking for the buffer right before each draw. */
    private static void renderModule(ControlModule module, BlockState state, PoseStack ms, MultiBufferSource buffers, int light) {
        CachedBuffers.partial(DeskPartials.base(module.type()), state).light(light).renderInto(ms, buffers.getBuffer(RenderType.solid()));
        float value = module.shown();
        boolean on = value > 0.5f;
        SuperByteBuffer part = switch (module.type()) {
            case PUSH_BUTTON -> CachedBuffers.partial(DeskPartials.BUTTON_CAP, state)
                    .translate(0, -0.75f * PX * (on ? 1 : 0), 0);
            case LEVER -> pivot(CachedBuffers.partial(DeskPartials.LEVER_HANDLE, state), 8, 1, 8)
                    .rotateXDegrees(on ? 30 : -30).translate(-8 * PX, -PX, -8 * PX);
            case DIAL -> pivot(CachedBuffers.partial(DeskPartials.DIAL_KNOB, state), 8, 0, 8)
                    .rotateYDegrees(-(value - 0.5f) * 270).translate(-8 * PX, 0, -8 * PX);
            case GAUGE -> pivot(CachedBuffers.partial(DeskPartials.GAUGE_NEEDLE, state), 8, 1.5f, 5.85f)
                    .rotateZDegrees((value - 0.5f) * 90).translate(-8 * PX, -1.5f * PX, -5.85f * PX);
            case KEY_SWITCH -> pivot(CachedBuffers.partial(DeskPartials.KEY_SWITCH_BAT, state), 8, 1.5f, 8)
                    .rotateXDegrees(on ? 25 : -25).translate(-8 * PX, -1.5f * PX, -8 * PX);
            case LAMP -> CachedBuffers.partial(on ? DeskPartials.LAMP_LENS_LIT : DeskPartials.LAMP_LENS, state)
                    .color(lampColor(module.function()));
            // The setpoint knob turns like a dial's.
            case CONTROLLER -> pivot(CachedBuffers.partial(DeskPartials.CONTROLLER_KNOB, state), 8, 0, 6.75f)
                    .rotateYDegrees(-(value - 0.5f) * 270).translate(-8 * PX, 0, -6.75f * PX);
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

    /** The slot under the crosshair, or -1. */
    private static int hoveredSlot(ControlDeskBlockEntity desk) {
        if (!(Minecraft.getInstance().hitResult instanceof BlockHitResult hit) || !hit.getBlockPos().equals(desk.getBlockPos())) {
            return -1;
        }
        return ControlDeskBlock.slotAt(desk.getBlockState(), desk.getBlockPos(), hit.getLocation());
    }

    /** Label and current reading of a module, floating above it and turned to the camera like a name tag. */
    private void renderLabel(ControlDeskBlockEntity desk, BlockState state, int slot, ControlModule module, PoseStack ms,
                             MultiBufferSource buffers) {
        PoseStack local = new PoseStack();
        DeskGeometry.toModel(local, state);
        DeskGeometry.toSlot(local, state.getValue(ControlDeskBlock.SHAPE), slot);
        Vector3f above = local.last().pose().transformPosition(new Vector3f(0, 0.5f, 0));
        ms.pushPose();
        ms.translate(above.x(), above.y(), above.z());
        ms.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        ms.scale(LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);
        MutableComponent title = module.label().isEmpty()
                ? Component.translatable(module.function().translationKey())
                : Component.literal(module.label());
        Component detail = !module.active()
                ? Component.translatable("create_nuclearindustry.control_module.inactive_short")
                : module.isController()
                ? Component.translatable("create_nuclearindustry.control_module.controller_state", module.formatSetpoint(),
                module.engaged() && module.demand() >= 0
                        ? Component.literal(Math.round(module.demand() * 100) + "%")
                        : Component.translatable("create_nuclearindustry.control_module.standby"))
                : Component.translatable("create_nuclearindustry.control_module.state", Math.round(module.shown() * 100), module.signal());
        int background = (int) (Minecraft.getInstance().options.getBackgroundOpacity(0.25f) * 255.0f) << 24;
        font.drawInBatch(title, -font.width(title) / 2f, -10, 0xFFFFFFFF, false, ms.last().pose(), buffers,
                Font.DisplayMode.SEE_THROUGH, background, LightTexture.FULL_BRIGHT);
        font.drawInBatch(detail, -font.width(detail) / 2f, 0, module.active() ? 0xFFB8F0C4 : 0xFFFF7A6A, false,
                ms.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH, background, LightTexture.FULL_BRIGHT);
        ms.popPose();
    }
}
