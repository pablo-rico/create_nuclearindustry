package org.papiricoh.create_nuclearindustry.fusion.heating;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakAuxiliary;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakReadings;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakSupply;

/**
 * Lights its viewport from what the controller says the plasma is doing. It offers the plasma
 * nothing. If the controller stops talking (broken, unloaded), the window goes dark on its own.
 */
public class FusionViewportBlockEntity extends BlockEntity implements TokamakAuxiliary {
    /** Ticks without news from a controller before the window goes dark. */
    static final int OBSERVE_TIMEOUT = 60;

    private long lastObserved = Long.MIN_VALUE;

    public FusionViewportBlockEntity(BlockPos pos, BlockState state) {
        super(PlasmaHeatingRegistry.VIEWPORT_ENTITY.get(), pos, state);
    }

    @Override
    public void contribute(TokamakSupply.Builder supply) {}

    @Override
    public void observe(TokamakReadings readings) {
        if (level == null || level.isClientSide) {
            return;
        }
        lastObserved = readings.formed() ? level.getGameTime() : Long.MIN_VALUE;
        setGlow(PlasmaHeatingConfig.viewportLight ? ViewportGlow.level(readings) : 0);
    }

    void serverTick() {
        if (level != null && level.getGameTime() % 20 == 0 && getBlockState().getValue(FusionViewportBlock.GLOW) > 0
                && level.getGameTime() - lastObserved > OBSERVE_TIMEOUT) {
            setGlow(0);
        }
    }

    /** Current light level of the window. */
    public int glow() {
        return getBlockState().getValue(FusionViewportBlock.GLOW);
    }

    private void setGlow(int glow) {
        BlockState state = getBlockState();
        if (level != null && state.hasProperty(FusionViewportBlock.GLOW) && state.getValue(FusionViewportBlock.GLOW) != glow) {
            level.setBlock(getBlockPos(), state.setValue(FusionViewportBlock.GLOW, glow), Block.UPDATE_CLIENTS);
        }
    }
}
