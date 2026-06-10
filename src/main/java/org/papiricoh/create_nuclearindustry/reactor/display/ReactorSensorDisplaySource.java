package org.papiricoh.create_nuclearindustry.reactor.display;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorTemperatureSensorBlockEntity;

import java.util.List;
import java.util.Optional;

public class ReactorSensorDisplaySource extends DisplaySource {
    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        if (!(context.getSourceBlockEntity() instanceof ReactorTemperatureSensorBlockEntity sensor)) {
            return List.of(Component.literal("No sensor"));
        }

        Optional<ReactorBlockEntity> reactor = sensor.getLinkedReactor();
        if (reactor.isEmpty()) {
            return List.of(Component.literal("No reactor"));
        }

        ReactorBlockEntity linked = reactor.get();
        ReactorBlockEntity.ReactorDisplaySnapshot snapshot = linked.getDisplaySnapshot();
        if (!snapshot.formed()) {
            return List.of(
                    Component.literal("State: " + snapshot.statusDisplayName()),
                    Component.literal("Missing: " + snapshot.validationMessage())
            );
        }
        return List.of(
                Component.literal(String.format("Temp: %.0f C", snapshot.coreTemperature())),
                Component.literal("Coolant: " + snapshot.coolantAmount() + "/" + snapshot.tankCapacity() + " mB"),
                Component.literal("Steam: " + snapshot.steamAmount() + "/" + snapshot.tankCapacity() + " mB"),
                Component.literal("State: " + snapshot.state().getDisplayName()),
                Component.literal(String.format("Stress: %.0f%%", snapshot.thermalStress())),
                Component.literal("Rods: " + snapshot.insertedControlRodSegments() + "/" + snapshot.expectedControlRodSegments())
        );
    }

    @Override
    public int getPassiveRefreshTicks() {
        return 20;
    }
}
