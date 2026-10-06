package org.papiricoh.create_nuclearindustry.fusion.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
import org.papiricoh.create_nuclearindustry.fusion.block.FusionFuelInjectorBlock;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakStructure;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;

/**
 * INPUT: takes deuterium and tritium into the controller's fuel tanks.
 * OUTPUT: hands out tritium bred beyond the operating reserve.
 */
public class FusionFuelInjectorBlockEntity extends TokamakPortBlockEntity {
    public static final int CELL_AMOUNT = 250;

    private final IFluidHandler fuelIn = new ControllerFluidHandler() {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return controller().map(c -> (tank == 0 ? c.getDeuteriumTank() : c.getTritiumTank()).getFluid().copy())
                    .orElse(FluidStack.EMPTY);
        }

        @Override
        public int getTankCapacity(int tank) {
            return controller().map(c -> FusionReactorBlockEntity.FUEL_TANK_CAPACITY).orElse(0);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 ? NuclearFluidHelper.isDeuterium(stack) : NuclearFluidHelper.isTritium(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return controller().map(c -> c.fillFuel(resource, action)).orElse(0);
        }
    };

    private final IFluidHandler tritiumOut = new ControllerFluidHandler() {
        @Override
        public FluidStack getFluidInTank(int tank) {
            return controller().map(c -> c.drainSurplusTritium(Integer.MAX_VALUE, FluidAction.SIMULATE)).orElse(FluidStack.EMPTY);
        }

        @Override
        public int getTankCapacity(int tank) {
            return controller().map(c -> FusionReactorBlockEntity.FUEL_TANK_CAPACITY).orElse(0);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return controller().map(c -> c.drainSurplusTritium(maxDrain, action)).orElse(FluidStack.EMPTY);
        }
    };

    public FusionFuelInjectorBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.FUSION_FUEL_INJECTOR.get(), pos, state, TokamakStructure::containsInjector);
    }

    @Override
    protected ReactorFluidPortMode getMode() {
        return getBlockState().getValue(FusionFuelInjectorBlock.MODE);
    }

    @Override
    protected IFluidHandler inputHandler() {
        return fuelIn;
    }

    @Override
    protected IFluidHandler outputHandler() {
        return tritiumOut;
    }

    @Override
    protected Component portName() {
        return Component.translatable("block.create_nuclearindustry.fusion_fuel_injector");
    }

    @Override
    protected Component modeDescription(ReactorFluidPortMode mode) {
        return Component.literal(mode == ReactorFluidPortMode.INPUT
                ? "Fuel input (deuterium, tritium)"
                : "Bred tritium output (keeps " + FusionReactorBlockEntity.TRITIUM_RESERVE + " mB)");
    }

    /**
     * Hand interaction with gas cells. INPUT: a full cell is emptied into the tokamak and the
     * empty cell returned. OUTPUT: an empty cell is filled from surplus tritium.
     *
     * @return the stack to give back, or {@code null} if nothing happened
     */
    public ItemStack useCell(ItemStack held) {
        if (controller().isEmpty()) {
            return null;
        }
        if (getMode() == ReactorFluidPortMode.INPUT) {
            FluidStack contents = held.is(AllNuclearItems.DEUTERIUM_CELL.get())
                    ? new FluidStack(AllNuclearFluids.DEUTERIUM.get(), CELL_AMOUNT)
                    : held.is(AllNuclearItems.TRITIUM_CELL.get())
                    ? new FluidStack(AllNuclearFluids.TRITIUM.get(), CELL_AMOUNT)
                    : FluidStack.EMPTY;
            if (contents.isEmpty() || fuelIn.fill(contents, IFluidHandler.FluidAction.SIMULATE) < CELL_AMOUNT) {
                return null;
            }
            fuelIn.fill(contents, IFluidHandler.FluidAction.EXECUTE);
            return new ItemStack(AllNuclearItems.EMPTY_GAS_CELL.get());
        }
        if (!held.is(AllNuclearItems.EMPTY_GAS_CELL.get())
                || tritiumOut.drain(CELL_AMOUNT, IFluidHandler.FluidAction.SIMULATE).getAmount() < CELL_AMOUNT) {
            return null;
        }
        tritiumOut.drain(CELL_AMOUNT, IFluidHandler.FluidAction.EXECUTE);
        return new ItemStack(AllNuclearItems.TRITIUM_CELL.get());
    }
}
