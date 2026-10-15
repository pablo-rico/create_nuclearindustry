package org.papiricoh.create_nuclearindustry.wastetransport.breach;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.papiricoh.create_nuclearindustry.maintenance.MaintenanceKitItem;

/**
 * A placed waste container (cistern or cask). Sealed it lets almost nothing out; breached it
 * irradiates through the hole and leaks into the ground until a Maintenance Kit patches it. The
 * breach belongs to the whole multiblock, kept by its controller.
 */
public interface WasteContainer {

    /** Bare dose rate of everything the whole container holds, in rad/s. */
    double bareActivity();

    /** How badly the container is breached: 0 sealed, 1 freshly holed. */
    float breachDamage();

    default boolean isBreached() {
        return breachDamage() > 0;
    }

    /** Marks the whole container freshly breached; true if it was sealed until now. Server side. */
    boolean markBreached();

    /** Throws out the share of the contents a breach spills (a cistern's fluid); called once per breach. */
    default void loseSpilledContents() {}

    /**
     * Breaches the container at {@code where}. Only the change from sealed to breached spills:
     * breaching it again does nothing. Returns whether it spilled.
     */
    default boolean breach(ServerLevel level, BlockPos where) {
        double activity = bareActivity();
        if (!markBreached()) {
            return false;
        }
        loseSpilledContents();
        BreachEffects.spill(level, where, activity);
        return true;
    }

    /**
     * Patches the breach by {@code amount} (a Maintenance Kit gives {@code maintenance.kitRepair});
     * {@link MaintenanceKitItem.Outcome#NOTHING_TO_DO} when it is sealed.
     */
    MaintenanceKitItem.Outcome repair(double amount);
}
