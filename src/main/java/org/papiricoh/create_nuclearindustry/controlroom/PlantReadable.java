package org.papiricoh.create_nuclearindustry.controlroom;

/**
 * A plant machine the control room can watch without knowing its class: steam generators,
 * containments and whatever comes next. Implemented by the machine's controller block entity,
 * which is what a Control Room Linker stores.
 */
public interface PlantReadable {

    /** The kind of target this machine is, for monitor layouts and alarm metrics. */
    TargetKind plantKind();

    /** Whether the machine is built and working, so a click on any of its blocks links its controller. */
    boolean plantFormed();

    /** A fresh reading, with values at the indices {@link MonitorReading} defines for {@link #plantKind()}. */
    MonitorReading plantSample();
}
