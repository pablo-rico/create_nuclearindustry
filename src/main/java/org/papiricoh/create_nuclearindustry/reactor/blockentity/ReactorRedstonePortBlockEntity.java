package org.papiricoh.create_nuclearindustry.reactor.blockentity;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.multiblock.ControllerLink;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorRedstonePortBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.RedstonePortMode;

import java.util.List;
import java.util.Optional;

/** Puts one reading of its reactor out as a 0-15 redstone signal. */
public class ReactorRedstonePortBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    private static final int REFRESH_INTERVAL = 10;
    private static final double MIN_SIGNAL_TEMPERATURE = 20.0;
    private static final double MAX_SIGNAL_TEMPERATURE = 3500.0;
    private static final double MAX_SIGNAL_POWER_MW = 1000.0;

    private final ControllerLink<ReactorBlockEntity> link = new ControllerLink<>(this, ReactorBlockEntity.class,
            (reactor, pos) -> reactor.getStructure().map(structure -> structure.containsRedstonePort(pos)).orElse(false));
    private int refreshCounter;

    public ReactorRedstonePortBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.REACTOR_REDSTONE_PORT.get(), pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }
        boolean relinked = link.tick();
        if (relinked || ++refreshCounter >= REFRESH_INTERVAL) {
            refreshCounter = 0;
            refreshSignal();
        }
    }

    public void setLinkedController(BlockPos controllerPos) {
        link.link(controllerPos);
        refreshSignal();
        setChanged();
    }

    public void clearLinkedController(BlockPos controllerPos) {
        if (link.unlink(controllerPos)) {
            refreshSignal();
            setChanged();
        }
    }

    public Optional<ReactorBlockEntity> getLinkedReactor() {
        return link.find();
    }

    public RedstonePortMode getMode() {
        return getBlockState().getValue(ReactorRedstonePortBlock.MODE);
    }

    public void refreshSignal() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.hasProperty(ReactorRedstonePortBlock.POWER)) {
            return;
        }
        int power = link.find().map(reactor -> signalFor(state.getValue(ReactorRedstonePortBlock.MODE), reactor)).orElse(0);
        if (state.getValue(ReactorRedstonePortBlock.POWER) == power) {
            return;
        }
        BlockState updated = state.setValue(ReactorRedstonePortBlock.POWER, power);
        level.setBlock(getBlockPos(), updated, Block.UPDATE_CLIENTS);
        level.updateNeighborsAt(getBlockPos(), updated.getBlock());
        level.updateNeighborsAt(getBlockPos().relative(updated.getValue(ReactorRedstonePortBlock.FACING)), updated.getBlock());
        setChanged();
    }

    public static int signalFor(RedstonePortMode mode, ReactorBlockEntity reactor) {
        return switch (mode) {
            case TEMPERATURE -> scaled((reactor.getCoreTemperature() - MIN_SIGNAL_TEMPERATURE)
                    / (MAX_SIGNAL_TEMPERATURE - MIN_SIGNAL_TEMPERATURE));
            case POWER -> scaled(reactor.getPowerOutput() / MAX_SIGNAL_POWER_MW);
            case FUEL -> comparator(reactor.getFuelRemaining(), reactor.getFuelCapacity());
            case COOLANT -> comparator(reactor.getCoolantAmount(), reactor.getTankCapacity());
            case STEAM -> comparator(reactor.getSteamAmount(), reactor.getTankCapacity());
            case SCRAM -> reactor.isScramRequested() ? 15 : 0;
            case ROD_INSERT -> reactor.rodDrive() > 0 ? 15 : 0;
            case ROD_WITHDRAW -> reactor.rodDrive() < 0 ? 15 : 0;
        };
    }

    private static int scaled(double fraction) {
        return Math.max(0, Math.min(15, (int) Math.round(fraction * 15.0)));
    }

    /** Like a comparator on a container: any content gives at least 1. */
    private static int comparator(double amount, double capacity) {
        if (amount <= 0 || capacity <= 0) {
            return 0;
        }
        return Math.max(1, Math.min(15, (int) Math.ceil(amount / capacity * 15.0)));
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        BlockState state = getBlockState();
        tooltip.add(Component.translatable("block.create_nuclearindustry.reactor_redstone_port").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  ").append(Component.translatable("create_nuclearindustry.redstone_port.mode",
                Component.translatable(ReactorRedstonePortBlock.modeKey(state.getValue(ReactorRedstonePortBlock.MODE)))))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Output side: " + state.getValue(ReactorRedstonePortBlock.FACING).getName().toUpperCase())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Redstone: " + state.getValue(ReactorRedstonePortBlock.POWER)).withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal(link.find().isPresent() ? "  Status: Linked" : "  Status: No reactor")
                .withStyle(link.find().isPresent() ? ChatFormatting.GREEN : ChatFormatting.RED));
        return true;
    }
}
