package org.papiricoh.create_nuclearindustry.reactor.pwr.accident;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseConfig;
import org.papiricoh.create_nuclearindustry.civildefense.ThreatRegistry;

/**
 * A plant accident sounds the air raid sirens around it: a meltdown, a breached vessel, a steam
 * explosion, a burst containment or a hydrogen burn raises a hazard on the {@link ThreatRegistry}
 * for {@link CivilDefenseConfig#plantAlarmSeconds}, so every automatic siren whose reach
 * ({@link CivilDefenseConfig#sirenThreatRadius}) covers the spot sounds the attack warning. Each new
 * accident starts its own alarm, so a chain of them keeps the sirens going.
 */
public final class PlantAlarm {

    private PlantAlarm() {}

    /** Sounds the sirens in reach of {@code at}; nothing on clients or with the alarm turned off. */
    public static void raise(Level level, BlockPos at) {
        if (level.isClientSide || CivilDefenseConfig.plantAlarmSeconds <= 0) {
            return;
        }
        ThreatRegistry.raise(level, at, 0, CivilDefenseConfig.plantAlarmSeconds * 20);
    }
}
