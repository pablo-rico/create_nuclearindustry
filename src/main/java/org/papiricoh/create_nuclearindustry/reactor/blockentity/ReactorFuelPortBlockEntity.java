package org.papiricoh.create_nuclearindustry.reactor.blockentity;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFuelPortBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;
import org.papiricoh.create_nuclearindustry.multiblock.ControllerLink;

import java.util.List;
import java.util.Optional;

public class ReactorFuelPortBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    private final ControllerLink<ReactorBlockEntity> link = new ControllerLink<>(this, ReactorBlockEntity.class,
            (reactor, pos) -> reactor.getStructure().map(structure -> structure.containsFuelPort(pos)).orElse(false));

    public ReactorFuelPortBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.REACTOR_FUEL_PORT.get(), pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }
        link.tick();
    }

    @Nullable
    public IItemHandler getItemHandler(Direction direction) {
        if (level == null || direction == null || direction != getBlockState().getValue(ReactorFuelPortBlock.FACING)) {
            return null;
        }
        ReactorBlockEntity reactor = getLinkedReactor();
        return reactor == null ? null : reactor.getFuelItemHandler(getBlockState().getValue(ReactorFuelPortBlock.MODE));
    }

    public void notifyLinkedReactorChanged(Player player) {
        Optional<BlockPos> previousLink = link.linkedPos();
        ReactorBlockEntity reactor = link.refresh().orElse(null);
        if (reactor != null) {
            reactor.requestStructureRevalidation(player);
        } else if (previousLink.isPresent()
                && level != null
                && level.getBlockEntity(previousLink.get()) instanceof ReactorBlockEntity previousReactor) {
            previousReactor.requestStructureRevalidation(player);
        } else if (player != null) {
            player.displayClientMessage(Component.literal("§cFuel port is not attached to a formed reactor shell"), false);
        }
    }

    public void setLinkedController(BlockPos controllerPos) {
        if (link.link(controllerPos)) {
            setChanged();
        }
    }

    public void clearLinkedController(BlockPos controllerPos) {
        if (link.unlink(controllerPos)) {
            setChanged();
        }
    }

    @Nullable
    private ReactorBlockEntity getLinkedReactor() {
        return link.find().orElse(null);
    }

    private ReactorFluidPortMode getMode() {
        return getBlockState().getValue(ReactorFuelPortBlock.MODE);
    }

    private String linkStatus() {
        ReactorBlockEntity reactor = getLinkedReactor();
        if (reactor != null) {
            return "Linked";
        }
        if (link.linkedPos().isPresent()) {
            return "Structure incomplete";
        }
        return "No reactor";
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ReactorFluidPortMode mode = getMode();
        ReactorBlockEntity reactor = getLinkedReactor();
        tooltip.add(Component.literal("Reactor Fuel Port").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  Mode: " + mode.displayName())
                .withStyle(mode == ReactorFluidPortMode.INPUT ? ChatFormatting.AQUA : ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  Connect inventory on: " + getBlockState().getValue(ReactorFuelPortBlock.FACING).getName().toUpperCase())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Status: " + linkStatus())
                .withStyle(reactor != null ? ChatFormatting.GREEN : ChatFormatting.RED));

        if (reactor != null) {
            tooltip.add(Component.literal(String.format("  Fuel: %.1f / %.1f units", reactor.getFuelRemaining(), reactor.getFuelCapacity()))
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("  Accepts fuel assemblies at " + ReactorBlockEntity.MIN_FUEL_ENRICHMENT + "% or more")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("  Fuel input: " + reactor.getInputFuelCount() + " assemblies")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("  Depleted fuel: " + reactor.getOutputFuelCount() + " assemblies")
                    .withStyle(ChatFormatting.GRAY));
            if (reactor.getPendingDepletedFuel() > 0) {
                tooltip.add(Component.literal("  Depleted output blocked: " + reactor.getPendingDepletedFuel())
                        .withStyle(ChatFormatting.RED));
            }
        }
        return true;
    }
}
