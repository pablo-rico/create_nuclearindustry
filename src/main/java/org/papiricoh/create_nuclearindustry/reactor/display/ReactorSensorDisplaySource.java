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
        return List.of(
                Component.literal(String.format("Temp: %.0f C", linked.getCoreTemperature())),
                Component.literal("Coolant: " + linked.getCoolantAmount() + "/" + linked.getTankCapacity() + " mB"),
                Component.literal("Steam: " + linked.getSteamAmount() + "/" + linked.getTankCapacity() + " mB"),
                Component.literal("State: " + linked.getReactorState().getDisplayName()),
                Component.literal(String.format("Stress: %.0f%%", linked.getThermalStress())),
                Component.literal("Rods: " + linked.getInsertedControlRodSegments() + "/" + linked.getExpectedControlRodSegments())
        );
    }

    @Override
    public int getPassiveRefreshTicks() {
        return 20;
    }
}
