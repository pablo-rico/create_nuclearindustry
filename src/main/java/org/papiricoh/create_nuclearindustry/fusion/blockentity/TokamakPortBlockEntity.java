package org.papiricoh.create_nuclearindustry.fusion.blockentity;

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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakStructure;
import org.papiricoh.create_nuclearindustry.multiblock.ControllerLink;
import org.papiricoh.create_nuclearindustry.multiblock.NeighbourFluidHandlers;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;

import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * A fluid port in the tokamak shell. The handler is exposed on every face, and the port
 * also trades directly with adjacent tanks so long pipe runs around a big torus are not
 * needed.
 *
 * <p>The capability is one handler for the life of the port, which picks the input or output side
 * on every call, so it is never invalidated: Create's pipe networks keep the handler they drain from
 * and never look it up again, and an invalidation (each time the tokamak re-formed) left them stuck
 * until the world was reloaded.
 */
public abstract class TokamakPortBlockEntity extends BlockEntity implements IHaveGoggleInformation, FusionReactorBlockEntity.TokamakPart {
    /** Matches {@link FusionReactorBlockEntity#PORT_THROUGHPUT}, so a port fed by a tank alongside keeps up. */
    private static final int ADJACENT_TRANSFER_PER_TICK = FusionReactorBlockEntity.PORT_THROUGHPUT;

    private final ControllerLink<FusionReactorBlockEntity> link;
    private boolean clientLinked;
    /** Tanks and pipes around the port; other tokamak ports are left out. */
    private final NeighbourFluidHandlers neighbours = new NeighbourFluidHandlers(this, be -> be instanceof TokamakPortBlockEntity);
    private final IFluidHandler handler = new ModeHandler();

    protected TokamakPortBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                     BiPredicate<TokamakStructure, BlockPos> ownsPort) {
        super(type, pos, state);
        this.link = new ControllerLink<>(this, FusionReactorBlockEntity.class,
                (controller, partPos) -> controller.getStructure().map(s -> ownsPort.test(s, partPos)).orElse(false));
    }

    protected abstract ReactorFluidPortMode getMode();

    protected abstract IFluidHandler inputHandler();

    protected abstract IFluidHandler outputHandler();

    protected abstract Component portName();

    protected abstract Component modeDescription(ReactorFluidPortMode mode);

    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }
        // Clients only hear of real changes of controller, not of every rescan.
        var before = link.linkedPos();
        if (link.tick() && !link.linkedPos().equals(before)) {
            syncLinkState();
        }
        if (link.linkedPos().isPresent()) {
            exchangeWithNeighbours();
        }
    }

    private void exchangeWithNeighbours() {
        boolean input = getMode() == ReactorFluidPortMode.INPUT;
        for (Direction side : Direction.values()) {
            IFluidHandler neighbour = neighbours.get(side);
            if (neighbour == null) {
                continue;
            }
            if (input) {
                FluidUtil.tryFluidTransfer(inputHandler(), neighbour, ADJACENT_TRANSFER_PER_TICK, true);
            } else {
                FluidUtil.tryFluidTransfer(neighbour, outputHandler(), ADJACENT_TRANSFER_PER_TICK, true);
            }
        }
    }

    public IFluidHandler getFluidHandler(Direction direction) {
        return handler;
    }

    private IFluidHandler current() {
        return getMode() == ReactorFluidPortMode.INPUT ? inputHandler() : outputHandler();
    }

    protected Optional<FusionReactorBlockEntity> controller() {
        return link.find();
    }

    public boolean hasLinkedReactor() {
        return level != null && level.isClientSide ? clientLinked : controller().isPresent();
    }

    public void notifyLinkedReactorChanged(Player player) {
        Optional<FusionReactorBlockEntity> previous = link.linkedPos()
                .map(pos -> level.getBlockEntity(pos) instanceof FusionReactorBlockEntity c ? c : null);
        previous.ifPresent(c -> c.requestStructureRevalidation(player));
        link.refresh().ifPresent(c -> c.requestStructureRevalidation(player));
        syncLinkState();
    }

    @Override
    public void link(BlockPos controllerPos) {
        if (link.link(controllerPos)) {
            syncLinkState();
        }
    }

    @Override
    public void unlink(BlockPos controllerPos) {
        if (link.unlink(controllerPos)) {
            syncLinkState();
        }
    }

    /** Tells clients the link changed. The capability stays as it is (see the class comment). */
    private void syncLinkState() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(getBlockPos(), state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ReactorFluidPortMode mode = getMode();
        tooltip.add(Component.literal("    ").append(portName()).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("  ").append(modeDescription(mode))
                .withStyle(mode == ReactorFluidPortMode.INPUT ? ChatFormatting.AQUA : ChatFormatting.GOLD));
        tooltip.add(Component.literal("  ").append(Component.translatable("create_nuclearindustry.tokamak.port.hint"))
                .withStyle(ChatFormatting.GRAY));
        boolean linked = hasLinkedReactor();
        tooltip.add(Component.literal("  ").append(Component.translatable(linked
                        ? "create_nuclearindustry.tokamak.port.linked" : "create_nuclearindustry.tokamak.port.unlinked"))
                .withStyle(linked ? ChatFormatting.GREEN : ChatFormatting.RED));
        return true;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        tag.putBoolean("linked", link.linkedPos().isPresent());
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider provider) {
        handleUpdateTag(pkt.getTag(), provider);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        clientLinked = tag.getBoolean("linked");
    }

    /** The input or output handler, whichever the port's mode picks now. */
    private final class ModeHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return current().getTanks();
        }

        @Override
        public net.neoforged.neoforge.fluids.FluidStack getFluidInTank(int tank) {
            return current().getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            return current().getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, net.neoforged.neoforge.fluids.FluidStack stack) {
            return current().isFluidValid(tank, stack);
        }

        @Override
        public int fill(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) {
            return current().fill(resource, action);
        }

        @Override
        public net.neoforged.neoforge.fluids.FluidStack drain(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) {
            return current().drain(resource, action);
        }

        @Override
        public net.neoforged.neoforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
            return current().drain(maxDrain, action);
        }
    }

    /** A handler that delegates to the linked controller, or holds nothing while unlinked. */
    protected abstract class ControllerFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public int getTankCapacity(int tank) {
            return controller().map(c -> FusionReactorBlockEntity.TANK_CAPACITY).orElse(0);
        }

        @Override
        public int fill(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public net.neoforged.neoforge.fluids.FluidStack drain(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) {
            net.neoforged.neoforge.fluids.FluidStack stored = getFluidInTank(0);
            if (stored.isEmpty() || !net.neoforged.neoforge.fluids.FluidStack.isSameFluidSameComponents(stored, resource)) {
                return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public net.neoforged.neoforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
            return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
        }
    }
}
