package org.papiricoh.create_nuclearindustry.controlroom;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;

import java.util.List;
import java.util.Optional;

/**
 * Keeps the link to a reactor and a copy of what the panel's face shows, refreshed twice a second.
 * Rods still only move physically: the rod buttons set a target that Redstone Ports in Insert rods
 * and Withdraw rods mode turn into signals for the pistons.
 */
public class ReactorControlPanelBlockEntity extends SmartBlockEntity implements ControlRoomDevice {
    private static final int REFRESH_INTERVAL = 10;

    @Nullable
    private GlobalPos target;
    private int refreshCounter;
    private PanelState shown = PanelState.NONE;

    public ReactorControlPanelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level != null && !level.isClientSide && ++refreshCounter >= REFRESH_INTERVAL) {
            refreshCounter = 0;
            refresh();
        }
    }

    public Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
    }

    @Override
    @Nullable
    public GlobalPos getTarget() {
        return target;
    }

    @Override
    public void setTarget(@Nullable GlobalPos newTarget) {
        target = newTarget;
        refresh();
        notifyUpdate();
    }

    public Optional<ReactorBlockEntity> reactor() {
        return LinkedTarget.reactorOf(this, target);
    }

    public PanelState getShown() {
        return shown;
    }

    /** Acts on the linked reactor. Tells the player when there is none to act on. */
    public void press(PanelButton button, @Nullable Player player) {
        Optional<ReactorBlockEntity> linked = reactor();
        if (linked.isEmpty() || !linked.get().isFormed()) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("create_nuclearindustry.control_panel.no_reactor")
                        .withStyle(ChatFormatting.RED), true);
            }
            return;
        }
        ReactorBlockEntity reactor = linked.get();
        switch (button) {
            case SCRAM -> {
                if (reactor.isScramRequested()) {
                    reactor.resetScram();
                } else {
                    reactor.setManualScram(true);
                }
            }
            case AUTO_SCRAM -> reactor.toggleAutoScram();
            case RODS_IN -> reactor.adjustRodTarget(PanelButton.ROD_STEP);
            case RODS_OUT -> reactor.adjustRodTarget(-PanelButton.ROD_STEP);
            case LIMIT_DOWN -> reactor.adjustAutoScramThreshold(-PanelButton.LIMIT_STEP);
            case LIMIT_UP -> reactor.adjustAutoScramThreshold(PanelButton.LIMIT_STEP);
        }
        refresh();
    }

    private void refresh() {
        if (level == null || level.isClientSide) {
            return;
        }
        LinkedTarget.Resolved resolved = LinkedTarget.resolve(level, getBlockPos(), target);
        PanelState next = resolved instanceof LinkedTarget.Reactor linked
                ? PanelState.of(linked.reactor())
                : new PanelState(resolved.kind(), false, false, false, 0, false, -1, 0, "");
        if (!next.equals(shown)) {
            shown = next;
            notifyUpdate();
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        ControlRoomDevice.writeTarget(tag, target);
        tag.put("Shown", shown.write());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        target = ControlRoomDevice.readTarget(tag);
        shown = PanelState.read(tag.getCompound("Shown"));
    }

    /** What the panel face shows about its reactor. */
    public record PanelState(TargetKind kind, boolean formed, boolean scramRequested, boolean autoEnabled, int threshold,
                             boolean tripped, float rodTarget, float rods, String status) {
        public static final PanelState NONE = new PanelState(TargetKind.NONE, false, false, false, 0, false, -1, 0, "");

        static PanelState of(ReactorBlockEntity reactor) {
            return new PanelState(TargetKind.REACTOR, reactor.isFormed(), reactor.isScramRequested(),
                    reactor.getAutoScram().isEnabled(), reactor.getAutoScram().getThreshold(), reactor.getAutoScram().isTripped(),
                    reactor.getEffectiveRodTarget(), reactor.getControlRodInsertionRatio(),
                    org.papiricoh.create_nuclearindustry.reactor.display.ReactorReadout.STATUS.read(reactor).getString());
        }

        CompoundTag write() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Kind", kind.name());
            tag.putBoolean("Formed", formed);
            tag.putBoolean("Scram", scramRequested);
            tag.putBoolean("Auto", autoEnabled);
            tag.putInt("Threshold", threshold);
            tag.putBoolean("Tripped", tripped);
            tag.putFloat("RodTarget", rodTarget);
            tag.putFloat("Rods", rods);
            tag.putString("Status", status);
            return tag;
        }

        static PanelState read(CompoundTag tag) {
            if (!tag.contains("Kind")) {
                return NONE;
            }
            return new PanelState(TargetKind.byName(tag.getString("Kind")), tag.getBoolean("Formed"), tag.getBoolean("Scram"),
                    tag.getBoolean("Auto"), tag.getInt("Threshold"), tag.getBoolean("Tripped"), tag.getFloat("RodTarget"),
                    tag.getFloat("Rods"), tag.getString("Status"));
        }
    }
}
