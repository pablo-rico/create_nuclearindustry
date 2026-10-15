package org.papiricoh.create_nuclearindustry.reactor.display;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.papiricoh.create_nuclearindustry.reactor.physics.ReactorPhysicsSimulator;
import org.papiricoh.create_nuclearindustry.reactor.physics.kinetics.ShutdownState;

import java.util.Locale;

/** Translated names of the reactor's states, for goggles and Display Links. */
public final class ReactorText {
    /** Lang key prefix of the operating states ({@link ReactorPhysicsSimulator.ReactorState}). */
    public static final String STATE = "create_nuclearindustry.reactor.state.";
    /** Lang key prefix of the Display Link texts. */
    public static final String DISPLAY = "create_nuclearindustry.reactor.display.";

    private ReactorText() {}

    public static MutableComponent state(ReactorPhysicsSimulator.ReactorState state) {
        return Component.translatable(STATE + state.name().toLowerCase(Locale.ROOT));
    }

    /** Lang key prefix of the shutdown states ({@link ShutdownState}). */
    public static final String SHUTDOWN = "create_nuclearindustry.reactor.shutdown.";

    public static MutableComponent shutdown(ShutdownState state) {
        return Component.translatable(SHUTDOWN + state.name().toLowerCase(Locale.ROOT));
    }

    /** A Display Link text, {@code create_nuclearindustry.reactor.display.<id>}. */
    public static MutableComponent display(String id, Object... args) {
        return Component.translatable(DISPLAY + id, args);
    }
}
