package org.papiricoh.create_nuclearindustry.controlroom.link;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.controlroom.ConfigurableDevice;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoomPanels;
import org.papiricoh.create_nuclearindustry.panel.PanelCard;
import org.papiricoh.create_nuclearindustry.panel.PanelSource;

import java.util.List;

/**
 * The code, mode and signals of an {@link AdvancedRedstoneLinkBlock}. It is on the network of its
 * code while loaded with a complete code; a receiver picks the network's signal up on its next tick
 * and passes it on to its neighbours, as Create's link does. The code comes from its screen through
 * {@link ConfigurableDevice}, sanitised.
 */
public class AdvancedRedstoneLinkBlockEntity extends SmartBlockEntity implements CodeLinkable, ConfigurableDevice, PanelSource {
    private String code = "";
    private int transmittedSignal;
    private int receivedSignal;
    private boolean receivedChanged;
    private boolean registered;

    public AdvancedRedstoneLinkBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void initialize() {
        super.initialize();
        join();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        if (!registered) {
            join();
        }
        if (receivedChanged) {
            receivedChanged = false;
            BlockState state = getBlockState();
            boolean powered = receivedSignal > 0;
            if (state.getValue(AdvancedRedstoneLinkBlock.POWERED) != powered) {
                level.setBlock(worldPosition, state.setValue(AdvancedRedstoneLinkBlock.POWERED, powered), 2);
            }
            updateNeighbours();
            notifyUpdate();
        }
    }

    private void updateNeighbours() {
        BlockState state = getBlockState();
        level.updateNeighborsAt(worldPosition, state.getBlock());
        level.updateNeighborsAt(worldPosition.relative(state.getValue(AdvancedRedstoneLinkBlock.FACING).getOpposite()), state.getBlock());
    }

    private void join() {
        if (level != null && !level.isClientSide) {
            AdvancedLinkNetwork.add(level, this);
            registered = true;
        }
    }

    private void leave() {
        if (level != null && !level.isClientSide && registered) {
            AdvancedLinkNetwork.remove(level, this);
            registered = false;
        }
    }

    // ------------------------------------------------------------------ setup

    public String getCode() {
        return code;
    }

    /** Moves the link to another code's network. */
    public void setCode(String newCode) {
        String sanitized = LinkCode.sanitize(newCode);
        if (sanitized.equals(code)) {
            return;
        }
        leave();
        code = sanitized;
        join();
        notifyUpdate();
    }

    @Override
    public boolean configure(int part, CompoundTag config) {
        if (!config.contains("Code")) {
            return false;
        }
        setCode(config.getString("Code"));
        return true;
    }

    /** Switches between transmitting and receiving, as Create's link does on a sneak-click. */
    public void toggleMode() {
        if (level == null) {
            return;
        }
        leave();
        BlockState state = getBlockState();
        boolean receiver = !state.getValue(AdvancedRedstoneLinkBlock.RECEIVER);
        transmittedSignal = 0;
        receivedSignal = 0;
        level.setBlock(worldPosition, state.setValue(AdvancedRedstoneLinkBlock.RECEIVER, receiver)
                .setValue(AdvancedRedstoneLinkBlock.POWERED, false), 3);
        join();
        if (!receiver) {
            transmit(AdvancedRedstoneLinkBlock.powerAround(level, worldPosition));
        }
        updateNeighbours();
        notifyUpdate();
    }

    public boolean isReceiver() {
        return getBlockState().getValue(AdvancedRedstoneLinkBlock.RECEIVER);
    }

    // ------------------------------------------------------------------ signals

    /** A transmitter's input changed: sends it on its code. */
    public void transmit(int strength) {
        if (level == null || isReceiver()) {
            return;
        }
        boolean powered = strength > 0;
        if (getBlockState().getValue(AdvancedRedstoneLinkBlock.POWERED) != powered) {
            level.setBlock(worldPosition, getBlockState().setValue(AdvancedRedstoneLinkBlock.POWERED, powered), 2);
        }
        if (strength != transmittedSignal) {
            transmittedSignal = strength;
            if (LinkCode.complete(code)) {
                AdvancedLinkNetwork.update(level, code);
            }
            notifyUpdate();
        }
    }

    public int receivedSignal() {
        return isReceiver() ? receivedSignal : 0;
    }

    public int signal() {
        return isReceiver() ? receivedSignal : transmittedSignal;
    }

    @Override
    public int transmittedStrength() {
        return isReceiver() ? 0 : transmittedSignal;
    }

    @Override
    public void setReceivedStrength(int strength) {
        if (strength != receivedSignal) {
            receivedSignal = strength;
            receivedChanged = true;
        }
    }

    @Override
    public boolean isListening() {
        return isReceiver();
    }

    /** Only its own flag: the network may ask while chunks unload, when looking the block up would wait for them. */
    @Override
    public boolean isAlive() {
        return !isRemoved();
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public BlockPos location() {
        return worldPosition;
    }

    @Override
    public void invalidate() {
        leave();
        super.invalidate();
    }

    @Override
    public PanelCard panelCard(net.minecraft.world.phys.Vec3 hit, boolean detailed) {
        return ControlRoomPanels.link(this);
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putString("Code", code);
        tag.putInt("Transmit", transmittedSignal);
        tag.putInt("Receive", receivedSignal);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        String read = LinkCode.sanitize(tag.getString("Code"));
        if (!read.equals(code) && registered) {
            leave();
            code = read;
            join();
        } else {
            code = read;
        }
        transmittedSignal = Math.clamp(tag.getInt("Transmit"), 0, 15);
        receivedSignal = Math.clamp(tag.getInt("Receive"), 0, 15);
    }
}
