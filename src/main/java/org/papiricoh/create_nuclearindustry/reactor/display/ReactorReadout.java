package org.papiricoh.create_nuclearindustry.reactor.display;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorRedstonePortBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorTemperatureSensorBlockEntity;

import java.util.Optional;
import java.util.function.Function;

/** One value of a fission reactor, as a single Display Link line. */
public enum ReactorReadout {
    TEMPERATURE("reactor_temperature", true, reactor -> Component.literal(Math.round(reactor.getCoreTemperature()) + "°C")),
    POWER("reactor_power", true, reactor -> Component.literal(Math.round(reactor.getPowerOutput()) + " MW")),
    FUEL("reactor_fuel", true, reactor -> Component.literal(percent(reactor.getFuelRemaining(), reactor.getFuelCapacity()) + "%")),
    CONTROL_RODS("reactor_control_rods", true,
            reactor -> Component.literal(Math.round(reactor.getControlRodInsertionRatio() * 100) + "%")),
    COOLANT("reactor_coolant", true, reactor -> Component.literal(reactor.getCoolantAmount() + " mB")),
    STEAM("reactor_steam", true, reactor -> Component.literal(reactor.getSteamAmount() + " mB")),
    STATUS("reactor_status", false, ReactorReadout::status);

    private final String id;
    private final boolean numeric;
    private final Function<ReactorBlockEntity, MutableComponent> reading;

    ReactorReadout(String id, boolean numeric, Function<ReactorBlockEntity, MutableComponent> reading) {
        this.id = id;
        this.numeric = numeric;
        this.reading = reading;
    }

    public String id() {
        return id;
    }

    public boolean numeric() {
        return numeric;
    }

    /** The reading, or a dash while there is no formed reactor. */
    public MutableComponent read(ReactorBlockEntity reactor) {
        if (this != STATUS && !reactor.isFormed()) {
            return Component.literal("-");
        }
        return reading.apply(reactor);
    }

    /** The reactor behind a Display Link source: the controller itself, or a linked sensor or redstone port. */
    public static Optional<ReactorBlockEntity> reactorOf(BlockEntity source) {
        if (source instanceof ReactorBlockEntity reactor) {
            return Optional.of(reactor);
        }
        if (source instanceof ReactorTemperatureSensorBlockEntity sensor) {
            return sensor.getLinkedReactor();
        }
        if (source instanceof ReactorRedstonePortBlockEntity port) {
            return port.getLinkedReactor();
        }
        return Optional.empty();
    }

    private static long percent(double amount, double capacity) {
        return capacity <= 0 ? 0 : Math.round(amount / capacity * 100);
    }

    private static MutableComponent status(ReactorBlockEntity reactor) {
        if (!reactor.isFormed()) {
            return Component.literal("Unformed");
        }
        if (reactor.getAutoScram().isTripped()) {
            return Component.literal("AUTO SCRAM");
        }
        if (reactor.isScramRequested()) {
            return Component.literal("SCRAM");
        }
        return Component.literal(reactor.getReactorState().getDisplayName());
    }
}
