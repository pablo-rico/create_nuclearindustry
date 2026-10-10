package org.papiricoh.create_nuclearindustry.radiation;

import org.papiricoh.create_nuclearindustry.waste.WastePackageBlock;

/**
 * A drum of untreated waste: the leakiest waste package (form {@code raw} in the
 * {@code waste_package} data map). Outside a good repository it slowly leaks into its chunk's
 * ground; well buried it is inert.
 */
public class RadioactiveWasteBarrelBlock extends WastePackageBlock {
    public RadioactiveWasteBarrelBlock(Properties properties) {
        super(properties);
    }
}
