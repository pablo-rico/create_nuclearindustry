package org.papiricoh.create_nuclearindustry.isotope;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A machine whose hidden state a Radiography Camera can show: what wears, cracks or clogs inside
 * it without showing on the outside. Machines put their most telling line first: a weak source
 * shows only the first few.
 */
public interface RadiographyTarget {
    /** Lines the radiograph shows, most important first. */
    List<Component> radiograph();
}
