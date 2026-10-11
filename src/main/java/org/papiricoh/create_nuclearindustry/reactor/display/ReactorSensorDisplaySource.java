package org.papiricoh.create_nuclearindustry.reactor.display;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.MutableComponent;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockText;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorTemperatureSensorBlockEntity;

import java.util.List;
import java.util.Optional;

public class ReactorSensorDisplaySource extends DisplaySource {
    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        if (!(context.getSourceBlockEntity() instanceof ReactorTemperatureSensorBlockEntity sensor)) {
            return List.of(ReactorText.display("no_sensor"));
        }

        Optional<ReactorBlockEntity> reactor = sensor.getLinkedReactor();
        if (reactor.isEmpty()) {
            return List.of(ReactorText.display("no_reactor"));
        }

        ReactorBlockEntity linked = reactor.get();
        ReactorBlockEntity.ReactorDisplaySnapshot snapshot = linked.getDisplaySnapshot();
        if (!snapshot.formed()) {
            return List.of(
                    ReactorText.display("sensor.state", MultiblockText.text(snapshot.statusDisplayName())),
                    ReactorText.display("sensor.missing", MultiblockText.text(snapshot.validationMessage()))
            );
        }
        return List.of(
                ReactorText.display("sensor.temperature", Math.round(snapshot.coreTemperature())),
                ReactorText.display("sensor.coolant", snapshot.coolantAmount(), snapshot.tankCapacity()),
                ReactorText.display("sensor.steam", snapshot.steamAmount(), snapshot.tankCapacity()),
                ReactorText.display("sensor.state", ReactorText.state(snapshot.state())),
                ReactorText.display("sensor.stress", Math.round(snapshot.thermalStress())),
                ReactorText.display("sensor.rods", snapshot.insertedControlRodSegments(), snapshot.expectedControlRodSegments())
        );
    }

    @Override
    public int getPassiveRefreshTicks() {
        return 20;
    }
}
