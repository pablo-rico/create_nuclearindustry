package org.papiricoh.create_nuclearindustry.fusion.cryo.plant;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.fusion.cryo.CryoConfig;
import org.papiricoh.create_nuclearindustry.fusion.cryo.CryoRegistry;
import org.papiricoh.create_nuclearindustry.multiblock.ControllerLink;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;

import java.util.List;
import java.util.Optional;

/**
 * A cryogenic plant port. It holds no fluid itself: its handler, on every face, reaches the
 * controller's tank for its {@link CryoplantPortMode}. Input ports also pull from tanks right next
 * to them and output ports push into them, so a plant can sit against the tokamak's cryogenic ports.
 */
public class CryoplantPortBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    private final ControllerLink<CryoplantControllerBlockEntity> link = new ControllerLink<>(this,
            CryoplantControllerBlockEntity.class,
            (controller, pos) -> controller.getStructure().map(s -> s.ownsPort(pos)).orElse(false));
    private final IFluidHandler handler = new PortHandler();
    private boolean clientLinked;

    public CryoplantPortBlockEntity(BlockPos pos, BlockState state) {
        super(CryoRegistry.CRYOPLANT_PORT_ENTITY.get(), pos, state);
    }

    public CryoplantPortMode mode() {
        return getBlockState().getValue(CryoplantPortBlock.MODE);
    }

    public void tick() {
        // Only a real change of controller may invalidate the capability: Create's pipes reset
        // their fluid network on every invalidation, so doing it on each rescan stops the flow.
        var before = link.linkedPos();
        if (link.tick() && !link.linkedPos().equals(before)) {
            sync();
        }
        if (link.linkedPos().isPresent()) {
            exchangeWithNeighbours();
        }
    }

    private void exchangeWithNeighbours() {
        if (level == null || CryoConfig.portTransfer <= 0) {
            return;
        }
        boolean input = mode().input();
        for (Direction side : Direction.values()) {
            BlockPos next = getBlockPos().relative(side);
            if (level.getBlockEntity(next) instanceof CryoplantPortBlockEntity) {
                continue;
            }
            IFluidHandler neighbour = level.getCapability(Capabilities.FluidHandler.BLOCK, next, side.getOpposite());
            if (neighbour == null) {
                continue;
            }
            if (input) {
                FluidUtil.tryFluidTransfer(handler, neighbour, CryoConfig.portTransfer, true);
            } else {
                FluidUtil.tryFluidTransfer(neighbour, handler, CryoConfig.portTransfer, true);
            }
        }
    }

    public IFluidHandler getFluidHandler(@Nullable Direction direction) {
        return handler;
    }

    /** Called after the mode changed (wrench): the plant recounts its ports. */
    public void refreshLink(@Nullable Player player) {
        if (level == null) {
            return;
        }
        link.linkedPos().ifPresent(pos -> {
            if (level.getBlockEntity(pos) instanceof CryoplantControllerBlockEntity controller) {
                controller.requestStructureRevalidation(player);
            }
        });
        for (CryoplantControllerBlockEntity controller : MultiblockRegistry.affecting(level, getBlockPos(),
                CryoplantControllerBlockEntity.class)) {
            controller.requestStructureRevalidation(player);
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

    private Optional<CryoplantControllerBlockEntity> controller() {
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
        String k = "create_nuclearindustry.cryoplant.";
        tooltip.add(Component.literal("    ").append(Component.translatable("block.create_nuclearindustry.cryoplant_port"))
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("     ").append(Component.translatable(mode().translationKey())).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("     ").append(Component.translatable(k + "port_hint")).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("     ").append(Component.translatable(isLinked() ? k + "linked" : k + "unlinked"))
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
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return controller().map(c -> c.tankFor(mode()).getFluid().copy()).orElse(FluidStack.EMPTY);
        }

        @Override
        public int getTankCapacity(int tank) {
            return controller().map(c -> c.tankFor(mode()).getCapacity()).orElse(0);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return mode().input() && controller().map(c -> c.tankFor(mode()).isFluidValid(stack)).orElse(false);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!mode().input()) {
                return 0;
            }
            return controller().map(c -> c.tankFor(mode()).fill(resource, action)).orElse(0);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack stored = getFluidInTank(0);
            return FluidStack.isSameFluidSameComponents(stored, resource) ? drain(resource.getAmount(), action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (mode().input()) {
                return FluidStack.EMPTY;
            }
            return controller().map(c -> c.tankFor(mode()).drain(maxDrain, action)).orElse(FluidStack.EMPTY);
        }
    }
}
