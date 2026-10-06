package org.papiricoh.create_nuclearindustry.coolingtower;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.NumericSingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.papiricoh.create_nuclearindustry.coolingtower.blockentity.CoolingTowerControllerBlockEntity;

/** The steam a cooling tower is condensing, in mB/t. */
public class CoolingTowerDisplaySource extends NumericSingleLineDisplaySource {
    @Override
    protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
        if (!(context.getSourceBlockEntity() instanceof CoolingTowerControllerBlockEntity tower)) {
            return Component.literal("-");
        }
        if (tower.getStructure().isEmpty()) {
            return Component.literal("-");
        }
        return Component.literal(Math.round(tower.getRecentUse()) + " mB/t");
    }

    @Override
    protected boolean allowsLabeling(DisplayLinkContext context) {
        return true;
    }

    @Override
    public int getPassiveRefreshTicks() {
        return 20;
    }
}
