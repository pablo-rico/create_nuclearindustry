package org.papiricoh.create_nuclearindustry.fusion.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
import org.papiricoh.create_nuclearindustry.fusion.block.FusionFluidPortBlock;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakStructure;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;

/** Coolant in (INPUT) or plasma steam out (OUTPUT). */
public class FusionFluidPortBlockEntity extends TokamakPortBlockEntity {

    private final IFluidHandler coolantIn = new ControllerFluidHandler() {
        @Override
        public FluidStack getFluidInTank(int tank) {
            return controller().map(c -> c.getCoolantTank().getFluid().copy()).orElse(FluidStack.EMPTY);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return NuclearFluidHelper.isCoolant(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return NuclearFluidHelper.isCoolant(resource)
                    ? controller().map(c -> c.getCoolantTank().fill(resource, action)).orElse(0) : 0;
        }
    };

    private final IFluidHandler steamOut = new ControllerFluidHandler() {
        @Override
        public FluidStack getFluidInTank(int tank) {
            return controller().map(c -> c.getPlasmaSteamTank().getFluid().copy()).orElse(FluidStack.EMPTY);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return controller().map(c -> c.getPlasmaSteamTank().drain(maxDrain, action)).orElse(FluidStack.EMPTY);
        }
    };

    public FusionFluidPortBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.FUSION_FLUID_PORT.get(), pos, state, TokamakStructure::containsFluidPort);
    }

    @Override
    protected ReactorFluidPortMode getMode() {
        return getBlockState().getValue(FusionFluidPortBlock.MODE);
    }

    @Override
    protected IFluidHandler inputHandler() {
        return coolantIn;
    }

    @Override
    protected IFluidHandler outputHandler() {
        return steamOut;
    }

    @Override
    protected Component portName() {
        return Component.translatable("block.create_nuclearindustry.fusion_fluid_port");
    }

    @Override
    protected Component modeDescription(ReactorFluidPortMode mode) {
        return Component.literal(mode == ReactorFluidPortMode.INPUT ? "Coolant input (water or heavy water)" : "Plasma steam output");
    }

    /** Right-click with a bucket: fills coolant (INPUT) or takes plasma steam (OUTPUT). */
    public boolean tryBucketInteraction(Player player, InteractionHand hand) {
        return FluidUtil.interactWithFluidHandler(player, hand, getFluidHandler(null));
    }
}
