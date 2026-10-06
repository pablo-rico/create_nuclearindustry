package org.papiricoh.create_nuclearindustry.coolingtower.blockentity;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.coolingtower.block.CoolingTowerFluidPortBlock;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
import org.papiricoh.create_nuclearindustry.multiblock.ControllerLink;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;

import java.util.List;
import java.util.Optional;

/**
 * A fluid port in a cooling tower. INPUT takes steam into the tower, OUTPUT gives water back.
 * The handler is on every face and the port also trades with adjacent tanks and pipes.
 */
public class CoolingTowerPortBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    public static final int TRANSFER_PER_TICK = 500;

    private final ControllerLink<CoolingTowerControllerBlockEntity> link = new ControllerLink<>(this,
            CoolingTowerControllerBlockEntity.class,
            (controller, pos) -> controller.getStructure().map(s -> s.ownsPort(pos)).orElse(false));
    private final IFluidHandler input = new PortHandler(true);
    private final IFluidHandler output = new PortHandler(false);
    private boolean clientLinked;

    public CoolingTowerPortBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.COOLING_TOWER_FLUID_PORT.get(), pos, state);
    }

    private ReactorFluidPortMode mode() {
        return getBlockState().getValue(CoolingTowerFluidPortBlock.MODE);
    }

    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }
        // Only a real change of controller may invalidate the capability: Create's pipes reset
        // their fluid network on every invalidation, so doing it on each rescan stops the flow.
        var before = link.linkedPos();
        if (link.tick() && !link.linkedPos().equals(before)) {
            sync();
        }
        if (link.linkedPos().isEmpty()) {
            return;
        }
        boolean in = mode() == ReactorFluidPortMode.INPUT;
        for (Direction side : Direction.values()) {
            BlockPos neighbourPos = getBlockPos().relative(side);
            if (level.getBlockEntity(neighbourPos) instanceof CoolingTowerPortBlockEntity) {
                continue;
            }
            IFluidHandler neighbour = level.getCapability(Capabilities.FluidHandler.BLOCK, neighbourPos, side.getOpposite());
            if (neighbour == null) {
                continue;
            }
            if (in) {
                FluidUtil.tryFluidTransfer(input, neighbour, TRANSFER_PER_TICK, true);
            } else {
                FluidUtil.tryFluidTransfer(neighbour, output, TRANSFER_PER_TICK, true);
            }
        }
    }

    public IFluidHandler getFluidHandler(Direction direction) {
        return mode() == ReactorFluidPortMode.INPUT ? input : output;
    }

    /** Called after the port block changed (wrench): the mode change must reach the controller. */
    public void refreshLink() {
        link.linkedPos().ifPresent(pos -> {
            if (level.getBlockEntity(pos) instanceof CoolingTowerControllerBlockEntity controller) {
                controller.requestStructureRevalidation(null);
            }
        });
        if (level != null) {
            for (CoolingTowerControllerBlockEntity controller : MultiblockRegistry.affecting(level, getBlockPos(),
                    CoolingTowerControllerBlockEntity.class)) {
                controller.requestStructureRevalidation(null);
            }
        }
        sync();
    }

    void link(BlockPos controllerPos) {
        if (link.link(controllerPos)) {
            sync();
        }
    }

    void unlink(BlockPos controllerPos) {
        if (link.unlink(controllerPos)) {
            sync();
        }
    }

    private Optional<CoolingTowerControllerBlockEntity> controller() {
        return link.find();
    }

    public boolean isLinked() {
        return level != null && level.isClientSide ? clientLinked : link.linkedPos().isPresent();
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.invalidateCapabilities(getBlockPos());
            BlockState state = getBlockState();
            level.sendBlockUpdated(getBlockPos(), state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean in = mode() == ReactorFluidPortMode.INPUT;
        tooltip.add(Component.literal("Cooling Tower Port").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(in ? "  Steam input" : "  Water output").withStyle(in ? ChatFormatting.AQUA : ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  Pipes or tanks on any side; wrench toggles the mode").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Status: " + (isLinked() ? "Linked" : "No tower"))
                .withStyle(isLinked() ? ChatFormatting.GREEN : ChatFormatting.RED));
        return true;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("Linked", link.linkedPos().isPresent());
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        clientLinked = tag.getBoolean("Linked");
    }

    private final class PortHandler implements IFluidHandler {
        private final boolean input;

        private PortHandler(boolean input) {
            this.input = input;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return controller().map(c -> (input ? c.getSteamTank() : c.getWaterTank()).getFluid().copy()).orElse(FluidStack.EMPTY);
        }

        @Override
        public int getTankCapacity(int tank) {
            return controller().map(c -> (input ? c.getSteamTank() : c.getWaterTank()).getCapacity()).orElse(0);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return input && NuclearFluidHelper.isTurbineSteam(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return input ? controller().map(c -> c.fillSteam(resource, action)).orElse(0) : 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (input) {
                return FluidStack.EMPTY;
            }
            FluidStack stored = getFluidInTank(0);
            return FluidStack.isSameFluidSameComponents(stored, resource) ? drain(resource.getAmount(), action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return input ? FluidStack.EMPTY : controller().map(c -> c.drainWater(maxDrain, action)).orElse(FluidStack.EMPTY);
        }
    }
}
