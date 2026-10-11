package org.papiricoh.create_nuclearindustry.reactor.pwr.containment.door;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrConfig;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentModel;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentMonitorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentShape;

import java.util.OptionalDouble;

/**
 * A containment door will not open while the containment it closes is under pressure: opening it
 * would blow the air (and whatever is in it) out through the doorway. The door asks every
 * containment monitor around it whether one of its two blocks is part of that monitor's wall.
 */
public final class ContainmentDoorInterlock {
    private ContainmentDoorInterlock() {}

    /**
     * The pressure, in bar, of a sealed containment whose wall holds the door and that is above the
     * interlock limit; empty when the door may open.
     */
    public static OptionalDouble lockingPressure(Level level, BlockPos lower) {
        if (level.isClientSide || PwrConfig.containmentDoorInterlock <= 0) {
            return OptionalDouble.empty();
        }
        double highest = Double.NEGATIVE_INFINITY;
        for (ContainmentMonitorBlockEntity monitor : MultiblockRegistry.affecting(level, lower, ContainmentMonitorBlockEntity.class)) {
            ContainmentShape shape = monitor.getShape();
            if (!monitor.sealed() || shape == null || !(shape.isWall(lower) || shape.isWall(lower.above()))) {
                continue;
            }
            highest = Math.max(highest, monitor.getPressure());
        }
        return highest - ContainmentModel.ATMOSPHERE > PwrConfig.containmentDoorInterlock
                ? OptionalDouble.of(highest)
                : OptionalDouble.empty();
    }
}
