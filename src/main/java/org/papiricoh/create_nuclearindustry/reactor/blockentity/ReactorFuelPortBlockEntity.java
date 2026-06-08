package org.papiricoh.create_nuclearindustry.reactor.blockentity;

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
import org.papiricoh.create_nuclearindustry.reactor.event.ReactorManager;
import org.papiricoh.create_nuclearindustry.reactor.multiblock.ReactorStructureValidator;

import java.util.Optional;

public class ReactorFuelPortBlockEntity extends BlockEntity {
    private Optional<BlockPos> linkedReactor = Optional.empty();
    private int relinkCooldown;

    public ReactorFuelPortBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.REACTOR_FUEL_PORT.get(), pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (relinkCooldown-- <= 0) {
            relinkCooldown = 20;
            linkedReactor = findLinkedReactor();
        }
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
        linkedReactor = findLinkedReactor();
        ReactorBlockEntity reactor = getLinkedReactor();
        if (reactor != null) {
            reactor.requestStructureRevalidation(player);
        } else if (player != null) {
            player.displayClientMessage(Component.literal("§cFuel port is not attached to a formed reactor shell"), false);
        }
    }

    @Nullable
    private ReactorBlockEntity getLinkedReactor() {
        if (level == null || linkedReactor.isEmpty()) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(linkedReactor.get());
        return blockEntity instanceof ReactorBlockEntity reactor ? reactor : null;
    }

    private Optional<BlockPos> findLinkedReactor() {
        if (level == null) {
            return Optional.empty();
        }
        return ReactorManager.getReactorsInRange(level, getBlockPos(), ReactorStructureValidator.MAX_WIDTH + ReactorStructureValidator.MAX_HEIGHT)
                .stream()
                .filter(reactor -> reactor.getStructure()
                        .map(structure -> structure.fuelPorts.contains(getBlockPos()))
                        .orElse(false))
                .map(ReactorBlockEntity::getBlockPos)
                .findFirst();
    }
}
