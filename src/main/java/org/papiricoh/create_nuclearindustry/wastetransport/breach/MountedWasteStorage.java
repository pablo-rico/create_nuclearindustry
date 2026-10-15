package org.papiricoh.create_nuclearindustry.wastetransport.breach;

import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.MountedStorageManager;

import java.util.ArrayList;
import java.util.List;

/** A waste container riding a contraption: its cargo's activity and its breach travel with it. */
public interface MountedWasteStorage {

    /** Bare dose rate of what this storage holds, in rad/s. */
    double activity();

    /** 0 sealed, 1 freshly holed. */
    float breachDamage();

    default boolean isBreached() {
        return breachDamage() > 0;
    }

    /** Marks it freshly breached; true if it was sealed until now. */
    boolean markBreached();

    /** Throws out {@code fraction} of the contents, if a breach loses any (a cistern's fluid). */
    default void loseSpilledContents(double fraction) {}

    /** Loses {@code millibuckets} of fluid to a leak, if it holds fluid. */
    default void leak(int millibuckets) {}

    /** Every waste storage of a contraption's (or a train carriage's) storage manager. */
    static List<MountedWasteStorage> of(MountedStorageManager manager) {
        List<MountedWasteStorage> found = new ArrayList<>();
        try {
            for (MountedItemStorage storage : manager.getAllItemStorages().values()) {
                if (storage instanceof MountedWasteStorage waste) {
                    found.add(waste);
                }
            }
            for (MountedFluidStorage storage : manager.getFluids().storages.values()) {
                if (storage instanceof MountedWasteStorage waste) {
                    found.add(waste);
                }
            }
        } catch (IllegalStateException uninitialised) {
            // A manager still being filled holds nothing to irradiate yet.
        }
        return found;
    }
}
