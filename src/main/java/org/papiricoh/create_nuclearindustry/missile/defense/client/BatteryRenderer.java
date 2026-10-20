package org.papiricoh.create_nuclearindustry.missile.defense.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.render.CachedBuffers;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;
import org.papiricoh.create_nuclearindustry.missile.defense.battery.BatteryGeometry;
import org.papiricoh.create_nuclearindustry.missile.defense.battery.InterceptorBatteryEntity;

/**
 * Draws a battery's turret and canister over its base: the turret turned by the battery's yaw, the canister raised by its pitch,
 * a round's nose in each ready tube and a cover over each still loading, and a short kick back after each launch.
 */
public class BatteryRenderer implements BlockEntityRenderer<InterceptorBatteryEntity> {
    /** Ticks a launch's kick lasts, and how far back it throws the canister, blocks. */
    private static final float RECOIL_TICKS = 6, RECOIL = 0.16f;

    public BatteryRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(InterceptorBatteryEntity battery, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (battery.getLevel() == null) return;
        float yaw = battery.yaw(partial), pitch = battery.pitch(partial);
        float since = battery.getLevel().getGameTime() + partial - battery.lastFired();
        float recoil = since >= 0 && since < RECOIL_TICKS ? (1 - since / RECOIL_TICKS) * RECOIL : 0;
        float[] turret = BatteryGeometry.TURRET_PIVOT;
        partial(DefensePartials.TURRET, battery)
                .translate(turret[0], turret[1], turret[2]).rotateYDegrees(-yaw).translate(-turret[0], -turret[1], -turret[2])
                .light(light).renderInto(pose, buffers.getBuffer(RenderType.solid()));
        canister(partial(DefensePartials.CANISTER, battery), yaw, pitch, recoil, 0, 0)
                .light(light).renderInto(pose, buffers.getBuffer(RenderType.solid()));
        for (int i = 0; i < InterceptorBatteryEntity.TUBES; i++) {
            if (battery.tubeInventory().getStackInSlot(i).isEmpty()) continue;
            float[] offset = BatteryGeometry.TUBE_OFFSETS[i];
            PartialModel model = battery.loadingTicks(i) == 0 ? DefensePartials.TUBE_ROUND : DefensePartials.TUBE_COVER;
            canister(partial(model, battery), yaw, pitch, recoil, offset[0], offset[1])
                    .light(light).renderInto(pose, buffers.getBuffer(RenderType.solid()));
        }
    }

    private static SuperByteBuffer partial(PartialModel model, InterceptorBatteryEntity battery) {
        return CachedBuffers.partial(model, battery.getBlockState());
    }

    /** {@code buffer}, modelled on the canister's axis at rest, turned and raised with it and shifted across its face by (x, y). */
    private static SuperByteBuffer canister(SuperByteBuffer buffer, float yaw, float pitch, float recoil, float x, float y) {
        float[] pivot = BatteryGeometry.CANISTER_PIVOT;
        return buffer.translate(pivot[0], pivot[1], pivot[2]).rotateYDegrees(-yaw).rotateXDegrees(pitch).translate(x, y, recoil)
                .translate(-pivot[0], -pivot[1], -pivot[2]);
    }

    /** The canister swings past its base and above it: up to a quarter block beyond the footprint and three blocks high. */
    @Override
    public AABB getRenderBoundingBox(InterceptorBatteryEntity battery) {
        return new AABB(battery.getBlockPos()).expandTowards(1, 2, 1).inflate(0.25, 0, 0.25);
    }
}
