package org.papiricoh.create_nuclearindustry.accelerator.blockentity;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.accelerator.api.AcceleratorAttachment;
import org.papiricoh.create_nuclearindustry.accelerator.api.AcceleratorSupply;
import org.papiricoh.create_nuclearindustry.accelerator.multiblock.AttachmentRole;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;

import java.util.List;
import java.util.Optional;

/**
 * A proxy onto the controller's helium loop, like the tokamak's cryogenic port but both ways at
 * once: filling it puts liquid helium into the controller's liquid tank, draining it takes warm
 * helium out of the controller's return tank. It knows its controller from
 * {@link AcceleratorAttachment#bind}, and checks that controller still counts it as a cryogenic
 * feed before every transfer.
 */
public class AcceleratorCryoFeedBlockEntity extends SmartBlockEntity implements AcceleratorAttachment, IHaveGoggleInformation {
    @Nullable
    private BlockPos controller;
    private final IFluidHandler handler = new Handler();

    public AcceleratorCryoFeedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    public IFluidHandler fluidHandler() {
        return handler;
    }

    @Override
    public void contribute(AcceleratorSupply.Builder supply) {}

    @Override
    public void bind(@Nullable BlockPos controller) {
        BlockPos next = controller == null ? null : controller.immutable();
        if (!java.util.Objects.equals(next, this.controller)) {
            this.controller = next;
            setChanged();
            sendData();
        }
    }

    /** The controller whose loop this feed reaches, while that controller's structure still lists it. */
    public Optional<AcceleratorControllerBlockEntity> linkedController() {
        if (level == null || controller == null || !level.isLoaded(controller)) {
            return Optional.empty();
        }
        if (!(level.getBlockEntity(controller) instanceof AcceleratorControllerBlockEntity c)) {
            return Optional.empty();
        }
        if (level.isClientSide) {
            return Optional.of(c);
        }
        return c.getStructure().filter(s -> s.attachments(AttachmentRole.CRYO).contains(worldPosition)).map(s -> c);
    }

    private Optional<FluidTank> liquid() {
        return linkedController().map(AcceleratorControllerBlockEntity::liquidHeliumTank);
    }

    private Optional<FluidTank> warm() {
        return linkedController().map(AcceleratorControllerBlockEntity::warmHeliumTank);
    }

    // ------------------------------------------------------------------ goggles

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        String k = "create_nuclearindustry.accelerator.cryo_feed.goggles.";
        tooltip.add(Component.literal("    ").append(getBlockState().getBlock().getName()).withStyle(ChatFormatting.GOLD));
        Optional<AcceleratorControllerBlockEntity> linked = linkedController();
        if (linked.isEmpty()) {
            tooltip.add(Component.literal("     ").append(Component.translatable(k + "not_linked")).withStyle(ChatFormatting.RED));
            return true;
        }
        AcceleratorControllerBlockEntity c = linked.get();
        tooltip.add(Component.literal("     ").append(Component.translatable(k + "linked")).withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("     ").append(Component.translatable(k + "helium", c.readings().liquidHelium(),
                AcceleratorControllerBlockEntity.HELIUM_TANK_CAPACITY)).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("     ").append(Component.translatable(k + "hint")).withStyle(ChatFormatting.DARK_GRAY));
        return true;
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (controller != null) {
            tag.putLong("Controller", controller.asLong());
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        controller = tag.contains("Controller") ? BlockPos.of(tag.getLong("Controller")) : null;
    }

    /** Tank 0 takes liquid helium in, tank 1 gives warm helium back. */
    private final class Handler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return (tank == 0 ? liquid() : warm()).map(t -> t.getFluid().copy()).orElse(FluidStack.EMPTY);
        }

        @Override
        public int getTankCapacity(int tank) {
            return (tank == 0 ? liquid() : warm()).map(FluidTank::getCapacity).orElse(0);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && NuclearFluidHelper.isLiquidHelium(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!NuclearFluidHelper.isLiquidHelium(resource)) {
                return 0;
            }
            return liquid().map(t -> t.fill(resource, action)).orElse(0);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !NuclearFluidHelper.isHelium(resource)) {
                return FluidStack.EMPTY;
            }
            FluidStack stored = getFluidInTank(1);
            if (stored.isEmpty() || !FluidStack.isSameFluidSameComponents(stored, resource)) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return warm().map(t -> t.drain(maxDrain, action)).orElse(FluidStack.EMPTY);
        }
    }
}
