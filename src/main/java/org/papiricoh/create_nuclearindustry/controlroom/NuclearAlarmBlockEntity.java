package org.papiricoh.create_nuclearindustry.controlroom;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Samples its target twice a second and compares the chosen reading with the threshold. Alarms on one
 * machine check on the same ticks and share its sample ({@link SharedReadings}); alarms on different
 * machines check on different ticks, so a server full of alarms does not do all of it in one tick.
 */
public class NuclearAlarmBlockEntity extends SmartBlockEntity implements ControlRoomDevice {
    private static final int CHECK_INTERVAL = 10;
    private static final int RING_INTERVAL = 40;
    /** 70 of 100 is 2800 °C for the default temperature alarm, the automatic SCRAM's default. */
    private static final int DEFAULT_SETTING = 70;

    @Nullable
    private GlobalPos target;
    private ScrollOptionBehaviour<AlarmMetric> metric;
    private ScrollValueBehaviour threshold;
    private int ringCounter;

    public NuclearAlarmBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        metric = new ScrollOptionBehaviour<>(AlarmMetric.class, Component.translatable("create_nuclearindustry.alarm.metric"),
                this, new CenteredSideValueBoxTransform((state, direction) -> direction == Direction.UP));
        metric.withCallback(value -> check());
        threshold = new ScrollValueBehaviour(Component.translatable("create_nuclearindustry.alarm.threshold"), this,
                new CenteredSideValueBoxTransform((state, direction) -> direction.getAxis().isHorizontal()))
                .between(0, 100)
                .withFormatter(setting -> metric.get().format(setting))
                .withCallback(value -> check());
        threshold.value = DEFAULT_SETTING;
        behaviours.add(metric);
        behaviours.add(threshold);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        if (SharedReadings.due(level, getBlockPos(), target, CHECK_INTERVAL)) {
            check(true);
        }
        if (isTripped() && ++ringCounter >= RING_INTERVAL) {
            ringCounter = 0;
            level.playSound(null, getBlockPos(), SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.0f, 1.4f);
        }
    }

    public boolean isTripped() {
        return getBlockState().getValue(NuclearAlarmBlock.LIT);
    }

    public AlarmMetric getMetric() {
        return metric.get();
    }

    public void setMetric(AlarmMetric value) {
        metric.setValue(value.ordinal());
    }

    public int getSetting() {
        return threshold.getValue();
    }

    public void setSetting(int value) {
        threshold.setValue(value);
    }

    /** Samples the target now and lights or clears the alarm. */
    public void check() {
        check(false);
    }

    /**
     * Lights or clears the alarm. The twice-a-second check takes the sample other devices took of the
     * same machine this tick ({@code shareSample}); a check after the operator changed something takes
     * its own.
     */
    public void check(boolean shareSample) {
        if (level == null || level.isClientSide) {
            return;
        }
        MonitorReading sample = shareSample ? SharedReadings.sample(level, getBlockPos(), target)
                : MonitorReading.sample(LinkedTarget.resolve(level, getBlockPos(), target));
        boolean tripped = metric.get().tripped(sample, threshold.getValue());
        BlockState state = getBlockState();
        if (state.getValue(NuclearAlarmBlock.LIT) != tripped) {
            level.setBlock(getBlockPos(), state.setValue(NuclearAlarmBlock.LIT, tripped), Block.UPDATE_ALL);
            ringCounter = RING_INTERVAL;
        }
    }

    @Override
    @Nullable
    public GlobalPos getTarget() {
        return target;
    }

    @Override
    public void setTarget(@Nullable GlobalPos newTarget) {
        target = newTarget;
        check();
        notifyUpdate();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        ControlRoomDevice.writeTarget(tag, target);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        target = ControlRoomDevice.readTarget(tag);
    }
}
