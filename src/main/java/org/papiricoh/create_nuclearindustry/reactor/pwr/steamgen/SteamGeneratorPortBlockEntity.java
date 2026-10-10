package org.papiricoh.create_nuclearindustry.reactor.pwr.steamgen;

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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.papiricoh.create_nuclearindustry.multiblock.ControllerLink;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;

import java.util.List;
import java.util.Optional;

/**
 * A steam generator port. It holds no fluid itself: its handler, on every face, reaches the
 * controller's tank for its {@link SteamGeneratorPortRole}. Pumps and pipes do the moving.
 */
public class SteamGeneratorPortBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    private final ControllerLink<SteamGeneratorControllerBlockEntity> link = new ControllerLink<>(this,
            SteamGeneratorControllerBlockEntity.class,
            (controller, pos) -> controller.getStructure().map(s -> s.ownsPort(pos)).orElse(false));
    private final IFluidHandler handler = new PortHandler();
    private boolean clientLinked;

    public SteamGeneratorPortBlockEntity(BlockPos pos, BlockState state) {
        super(PwrRegistry.STEAM_GENERATOR_PORT_ENTITY.get(), pos, state);
    }

    public SteamGeneratorPortRole role() {
        return getBlockState().getValue(SteamGeneratorPortBlock.ROLE);
    }

    public void tick() {
        // Only a real change of controller may invalidate the capability: Create's pipes reset
        // their fluid network on every invalidation, so doing it on each rescan stops the flow.
        var before = link.linkedPos();
        if (link.tick() && !link.linkedPos().equals(before)) {
            sync();
        }
    }

    public IFluidHandler getFluidHandler(Direction direction) {
        return handler;
    }

    /** Called after the role changed (wrench): the controller recounts its ports. */
    public void refreshLink() {
        if (level == null) {
            return;
        }
        link.linkedPos().ifPresent(pos -> {
            if (level.getBlockEntity(pos) instanceof SteamGeneratorControllerBlockEntity controller) {
                controller.requestStructureRevalidation(null);
            }
        });
        for (SteamGeneratorControllerBlockEntity controller : MultiblockRegistry.affecting(level, getBlockPos(),
                SteamGeneratorControllerBlockEntity.class)) {
            controller.requestStructureRevalidation(null);
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

    private Optional<SteamGeneratorControllerBlockEntity> controller() {
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
        tooltip.add(Component.translatable("block.create_nuclearindustry.steam_generator_port").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  ").append(Component.translatable(role().translationKey())).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("  ").append(Component.translatable("create_nuclearindustry.steam_generator.port_hint"))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  ").append(Component.translatable(isLinked()
                        ? "create_nuclearindustry.steam_generator.linked" : "create_nuclearindustry.steam_generator.unlinked"))
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
            return controller().map(c -> c.tankFor(role()).getFluid().copy()).orElse(FluidStack.EMPTY);
        }

        @Override
        public int getTankCapacity(int tank) {
            return controller().map(c -> c.tankFor(role()).getCapacity()).orElse(0);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return controller().map(c -> c.accepts(role(), stack)).orElse(false);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return controller().map(c -> c.fill(role(), resource, action)).orElse(0);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack stored = getFluidInTank(0);
            return FluidStack.isSameFluidSameComponents(stored, resource) ? drain(resource.getAmount(), action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return controller().map(c -> c.drain(role(), maxDrain, action)).orElse(FluidStack.EMPTY);
        }
    }
}
