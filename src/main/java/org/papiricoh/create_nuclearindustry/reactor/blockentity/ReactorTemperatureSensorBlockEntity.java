package org.papiricoh.create_nuclearindustry.reactor.blockentity;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorTemperatureSensorBlock;
import org.papiricoh.create_nuclearindustry.reactor.event.ReactorManager;
import org.papiricoh.create_nuclearindustry.reactor.multiblock.ReactorStructureValidator;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ReactorTemperatureSensorBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    private static final int LINK_SCAN_INTERVAL = 20;
    private static final double MIN_SIGNAL_TEMPERATURE = 20.0;
    private static final double MAX_SIGNAL_TEMPERATURE = 3500.0;

    private Optional<BlockPos> linkedController = Optional.empty();
    private int linkScanCounter;

    public ReactorTemperatureSensorBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.REACTOR_TEMPERATURE_SENSOR.get(), pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (++linkScanCounter >= LINK_SCAN_INTERVAL) {
            linkScanCounter = 0;
            linkedController = findLinkedReactor().map(ReactorBlockEntity::getBlockPos);
            updatePower();
        }
    }

    public void notifyLinkedReactorChanged(Player player) {
        Optional<ReactorBlockEntity> reactor = findLinkedReactor();
        linkedController = reactor.map(ReactorBlockEntity::getBlockPos);
        reactor.ifPresent(linkedReactor -> linkedReactor.requestStructureRevalidation(player));
        updatePower();
        setChanged();
    }

    public void setLinkedController(BlockPos controllerPos) {
        linkedController = Optional.of(controllerPos);
        updatePower();
        setChanged();
    }

    public void clearLinkedController(BlockPos controllerPos) {
        if (linkedController.map(controllerPos::equals).orElse(false)) {
            linkedController = Optional.empty();
            updatePower();
            setChanged();
        }
    }

    public Optional<ReactorBlockEntity> getLinkedReactor() {
        return findLinkedReactor();
    }

    private Optional<ReactorBlockEntity> findLinkedReactor() {
        if (level == null) {
            return Optional.empty();
        }
        Optional<ReactorBlockEntity> cached = getCachedLinkedReactor();
        if (cached.isPresent()) {
            return cached;
        }
        return ReactorManager.getReactorsInRange(level, getBlockPos(), ReactorStructureValidator.MAX_WIDTH + ReactorStructureValidator.MAX_HEIGHT)
                .stream()
                .filter(reactor -> reactor.getStructure().map(structure -> structure.containsTemperatureSensor(getBlockPos())).orElse(false))
                .sorted(Comparator.comparingLong(reactor -> reactor.getBlockPos().asLong()))
                .findFirst();
    }

    private Optional<ReactorBlockEntity> getCachedLinkedReactor() {
        if (level == null || linkedController.isEmpty()) {
            return Optional.empty();
        }
        if (level.getBlockEntity(linkedController.get()) instanceof ReactorBlockEntity reactor
                && reactor.getStructure().map(structure -> structure.containsTemperatureSensor(getBlockPos())).orElse(false)) {
            return Optional.of(reactor);
        }
        linkedController = Optional.empty();
        return Optional.empty();
    }

    private void updatePower() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.hasProperty(ReactorTemperatureSensorBlock.POWER)) {
            return;
        }
        int power = findLinkedReactor()
                .map(reactor -> powerFromTemperature(reactor.getCoreTemperature()))
                .orElse(0);
        if (state.getValue(ReactorTemperatureSensorBlock.POWER) == power) {
            return;
        }
        BlockState updated = state.setValue(ReactorTemperatureSensorBlock.POWER, power);
        level.setBlock(getBlockPos(), updated, Block.UPDATE_CLIENTS);
        level.updateNeighborsAt(getBlockPos(), updated.getBlock());
        level.updateNeighborsAt(getBlockPos().relative(updated.getValue(ReactorTemperatureSensorBlock.FACING)), updated.getBlock());
        setChanged();
    }

    private static int powerFromTemperature(double temperature) {
        double normalized = (temperature - MIN_SIGNAL_TEMPERATURE) / (MAX_SIGNAL_TEMPERATURE - MIN_SIGNAL_TEMPERATURE);
        return Math.max(0, Math.min(15, (int) Math.round(normalized * 15.0)));
    }

    private String linkStatus() {
        Optional<ReactorBlockEntity> reactor = findLinkedReactor();
        if (reactor.isPresent()) {
            return "Linked";
        }
        if (linkedController.isPresent()) {
            return "Structure incomplete";
        }
        return "No reactor";
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("Temperature Sensor").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  Output side: " + getBlockState().getValue(ReactorTemperatureSensorBlock.FACING).getName().toUpperCase())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Redstone: " + getBlockState().getValue(ReactorTemperatureSensorBlock.POWER))
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  Status: " + linkStatus())
                .withStyle(findLinkedReactor().isPresent() ? ChatFormatting.GREEN : ChatFormatting.RED));
        findLinkedReactor().ifPresent(reactor -> {
            tooltip.add(Component.literal(String.format("  Temperature: %.0f C", reactor.getCoreTemperature())).withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.literal("  Coolant: " + reactor.getCoolantAmount() + "/" + reactor.getTankCapacity() + " mB").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("  Steam: " + reactor.getSteamAmount() + "/" + reactor.getTankCapacity() + " mB").withStyle(ChatFormatting.GRAY));
        });
        return true;
    }
}
