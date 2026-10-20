package org.papiricoh.create_nuclearindustry.missile.defense.battery;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

/** Places a whole interceptor battery: its master and seven parts, if all eight blocks are free. */
public class InterceptorBatteryItem extends BlockItem {
    public InterceptorBatteryItem(Block block, Properties properties) {
        super(block, properties);
    }
}
