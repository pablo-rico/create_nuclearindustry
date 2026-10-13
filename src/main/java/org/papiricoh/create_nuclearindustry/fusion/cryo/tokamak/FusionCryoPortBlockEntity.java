package org.papiricoh.create_nuclearindustry.fusion.cryo.tokamak;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.TokamakPortBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.cryo.CryoRegistry;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakStructure;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * The tokamak's cryogenic port: a proxy onto the controller's helium loop
 * ({@link FusionReactorBlockEntity#cryoTanks()}). INPUT fills the liquid helium tank, OUTPUT drains
 * the warm helium tank; both trade with tanks right next to them, so a cryogenic plant can stand
 * against the shell.
 */
public class FusionCryoPortBlockEntity extends TokamakPortBlockEntity {

    private final IFluidHandler liquidHeliumIn = new CryoHandler(FusionReactorBlockEntity.CryoTanks::liquidHelium, true);
    private final IFluidHandler warmHeliumOut = new CryoHandler(FusionReactorBlockEntity.CryoTanks::warmHelium, false);

    public FusionCryoPortBlockEntity(BlockPos pos, BlockState state) {
        super(CryoRegistry.FUSION_CRYO_PORT_ENTITY.get(), pos, state, TokamakStructure::containsPart);
    }

    static String modeKey(ReactorFluidPortMode mode) {
        return mode == ReactorFluidPortMode.INPUT ? "create_nuclearindustry.cryo_port.input" : "create_nuclearindustry.cryo_port.output";
    }

    @Override
    protected ReactorFluidPortMode getMode() {
        return getBlockState().getValue(FusionCryoPortBlock.MODE);
    }

    @Override
    protected IFluidHandler inputHandler() {
        return liquidHeliumIn;
    }

    @Override
    protected IFluidHandler outputHandler() {
        return warmHeliumOut;
    }

    @Override
    protected Component portName() {
        return Component.translatable("block.create_nuclearindustry.fusion_cryo_port");
    }

    @Override
    protected Component modeDescription(ReactorFluidPortMode mode) {
        return Component.translatable(modeKey(mode));
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ReactorFluidPortMode mode = getMode();
        tooltip.add(Component.literal("    ").append(portName()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("     ").append(modeDescription(mode))
                .withStyle(mode == ReactorFluidPortMode.INPUT ? ChatFormatting.AQUA : ChatFormatting.GOLD));
        tooltip.add(Component.literal("     ").append(Component.translatable("create_nuclearindustry.cryo_port.hint"))
                .withStyle(ChatFormatting.GRAY));
        boolean linked = hasLinkedReactor();
        tooltip.add(Component.literal("     ").append(Component.translatable(linked
                        ? "create_nuclearindustry.cryo_port.linked" : "create_nuclearindustry.cryo_port.unlinked"))
                .withStyle(linked ? ChatFormatting.GREEN : ChatFormatting.RED));
        return true;
    }

    /** One of the controller's two helium tanks, seen from outside: fill-only or drain-only. */
    private final class CryoHandler implements IFluidHandler {
        private final Function<FusionReactorBlockEntity.CryoTanks, FluidTank> tank;
        private final boolean input;

        private CryoHandler(Function<FusionReactorBlockEntity.CryoTanks, FluidTank> tank, boolean input) {
            this.tank = tank;
            this.input = input;
        }

        private Optional<FluidTank> target() {
            return controller().map(c -> tank.apply(c.cryoTanks()));
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int index) {
            return target().map(t -> t.getFluid().copy()).orElse(FluidStack.EMPTY);
        }

        @Override
        public int getTankCapacity(int index) {
            return target().map(FluidTank::getCapacity).orElse(0);
        }

        @Override
        public boolean isFluidValid(int index, FluidStack stack) {
            return input && NuclearFluidHelper.isLiquidHelium(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!input || !NuclearFluidHelper.isLiquidHelium(resource)) {
                return 0;
            }
            return target().map(t -> t.fill(resource, action)).orElse(0);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack stored = getFluidInTank(0);
            if (stored.isEmpty() || !FluidStack.isSameFluidSameComponents(stored, resource)) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (input) {
                return FluidStack.EMPTY;
            }
            return target().map(t -> t.drain(maxDrain, action)).orElse(FluidStack.EMPTY);
        }
    }
}
