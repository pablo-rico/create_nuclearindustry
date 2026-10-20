package org.papiricoh.create_nuclearindustry.missile.silo;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.Locale;

/** The launch controller's live state, timing and target for a Display Link. */
public class LaunchControllerDisplaySource extends DisplaySource {
    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        if (!(context.getSourceBlockEntity() instanceof SiloControllerEntity controller))
            return List.of(Component.translatable("silo.create_nuclearindustry.display.offline"));
        return read(controller);
    }

    /** Builds the same readout for a controller, also usable by the controller's GameTest. */
    public List<MutableComponent> read(SiloControllerEntity controller) {
        List<MutableComponent> lines = new java.util.ArrayList<>();
        lines.add(Component.translatable("silo.create_nuclearindustry.display.state",
                Component.translatable("silo.create_nuclearindustry.state." + controller.state().name().toLowerCase(Locale.ROOT))));
        if (controller.state() == SiloControllerEntity.State.COUNTDOWN) {
            lines.add(Component.translatable("silo.create_nuclearindustry.display.countdown", (controller.countdown() + 19) / 20,
                    (controller.launchDelay() + 19) / 20));
        } else {
            lines.add(Component.translatable("silo.create_nuclearindustry.display.idle"));
        }
        lines.add(Component.translatable("silo.create_nuclearindustry.display.reach", Math.round(controller.reach())));
        BlockPos target = controller.target();
        lines.add(target == null
                ? Component.translatable("silo.create_nuclearindustry.display.no_target")
                : Component.translatable("silo.create_nuclearindustry.display.target", target.getX(), target.getY(), target.getZ()));
        return lines;
    }

    @Override
    public int getPassiveRefreshTicks() {
        return 20;
    }
}
