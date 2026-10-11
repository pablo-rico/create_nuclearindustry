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
            player.displayClientMessage(Component.translatable(ReactorBlockEntity.PORT + "fuel_not_attached").withStyle(ChatFormatting.RED), false);
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

    private Component linkStatus() {
        return ReactorBlockEntity.portLinkStatus(getLinkedReactor() != null, link.linkedPos().isPresent());
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ReactorFluidPortMode mode = getMode();
        ReactorBlockEntity reactor = getLinkedReactor();
        tooltip.add(Component.translatable("block.create_nuclearindustry.reactor_fuel_port").withStyle(ChatFormatting.GOLD));
        tooltip.add(ReactorBlockEntity.portLine("mode", mode == ReactorFluidPortMode.INPUT ? ChatFormatting.AQUA : ChatFormatting.YELLOW,
                ReactorBlockEntity.portMode(mode)));
        tooltip.add(ReactorBlockEntity.portLine("connect_inventory", ChatFormatting.GRAY,
                ReactorBlockEntity.portSide(getBlockState().getValue(ReactorFuelPortBlock.FACING))));
        tooltip.add(ReactorBlockEntity.portLine("status", reactor != null ? ChatFormatting.GREEN : ChatFormatting.RED, linkStatus()));

        if (reactor != null) {
            tooltip.add(ReactorBlockEntity.goggleLine("fuel", ChatFormatting.GRAY,
                    String.format(java.util.Locale.ROOT, "%.1f", reactor.getFuelRemaining()),
                    String.format(java.util.Locale.ROOT, "%.1f", reactor.getFuelCapacity())));
            tooltip.add(ReactorBlockEntity.portLine("accepts", ChatFormatting.GRAY,
                    String.format(java.util.Locale.ROOT, "%.1f", ReactorBlockEntity.MIN_FUEL_ENRICHMENT)));
            tooltip.add(ReactorBlockEntity.goggleLine("fuel_input", ChatFormatting.GRAY, reactor.getInputFuelCount()));
            tooltip.add(ReactorBlockEntity.goggleLine("depleted", ChatFormatting.GRAY, reactor.getOutputFuelCount()));
            if (reactor.getPendingDepletedFuel() > 0) {
                tooltip.add(ReactorBlockEntity.goggleLine("output_blocked", ChatFormatting.RED, reactor.getPendingDepletedFuel()));
            }
        }
        return true;
    }
}
