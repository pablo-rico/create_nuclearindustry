package org.papiricoh.create_nuclearindustry.wastetransport.cistern;

import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipAssembly;
import org.papiricoh.create_nuclearindustry.wastetransport.WasteTransportRegistry;
import org.papiricoh.create_nuclearindustry.wastetransport.breach.BreachEffects;

/**
 * A fluid tank for radioactive liquors: connects into multiblocks like Create's tank, but with a
 * shielded shell and no windows to toggle. Broken by hand, the fluid that no longer fits in what
 * is left of it spills into the ground.
 */
public class WasteCisternBlock extends FluidTankBlock {

    public WasteCisternBlock(Properties properties) {
        super(properties, false);
    }

    @Override
    public BlockEntityType<? extends FluidTankBlockEntity> getBlockEntityType() {
        return WasteTransportRegistry.CISTERN_ENTITY.get();
    }

    /** No windows to toggle: the wrench only picks it up (sneaking). */
    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!isMoving && !ShipAssembly.isMoving() && state.getBlock() != newState.getBlock() && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof WasteCisternBlockEntity cistern) {
            spillLostShare(serverLevel, pos, cistern);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /** When a block goes, whatever no longer fits in the blocks left of the multiblock lands on the ground. */
    private static void spillLostShare(ServerLevel level, BlockPos pos, WasteCisternBlockEntity cistern) {
        WasteCisternBlockEntity controller = cistern.controllerCistern();
        if (controller == null) {
            return;
        }
        int amount = controller.getTankInventory().getFluidAmount();
        if (amount <= 0) {
            return;
        }
        int remainingCapacity = (controller.getTotalTankSize() - 1) * FluidTankBlockEntity.getCapacityMultiplier();
        int lost = Math.max(0, amount - remainingCapacity);
        if (lost > 0) {
            BreachEffects.spill(level, pos, controller.bareActivity() * lost / amount);
        }
    }
}
