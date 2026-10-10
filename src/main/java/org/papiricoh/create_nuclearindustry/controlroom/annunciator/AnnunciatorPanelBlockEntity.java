package org.papiricoh.create_nuclearindustry.controlroom.annunciator;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.controlroom.AlarmMetric;
import org.papiricoh.create_nuclearindustry.controlroom.ConfigurableDevice;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Config;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoomBlocks;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoomDevice;
import org.papiricoh.create_nuclearindustry.controlroom.LinkedTarget;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlModule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An annunciator panel: six alarm windows, each watching its own machine for one {@link AlarmMetric}
 * past a threshold, run through the {@link AnnunciatorLogic} sequence. While any window waits for an
 * acknowledgement the horn sounds and the panel powers its back and bottom.
 */
public class AnnunciatorPanelBlockEntity extends SmartBlockEntity implements ControlRoomDevice, ConfigurableDevice {
    public static final int WINDOWS = 6;
    private static final int HORN_INTERVAL = 20;

    /** One window's setup. */
    public static final class Window {
        @Nullable
        private GlobalPos target;
        private TargetKind kind = TargetKind.NONE;
        private AlarmMetric metric = AlarmMetric.TEMPERATURE;
        /** 70 of 100 is 2800 °C for the default temperature metric, the automatic SCRAM's default. */
        private int setting = 70;
        private String label = "";

        @Nullable
        public GlobalPos target() {
            return target;
        }

        /** What the window was linked to, judged when it was linked. */
        public TargetKind kind() {
            return kind;
        }

        public AlarmMetric metric() {
            return metric;
        }

        public int setting() {
            return setting;
        }

        public String label() {
            return label;
        }

        CompoundTag write() {
            CompoundTag tag = new CompoundTag();
            ControlRoomDevice.writeTarget(tag, target);
            tag.putString("Kind", kind.name());
            tag.putString("Metric", metric.name());
            tag.putInt("Setting", setting);
            tag.putString("Label", label);
            return tag;
        }

        void read(CompoundTag tag) {
            target = ControlRoomDevice.readTarget(tag);
            kind = TargetKind.byName(tag.getString("Kind"));
            metric = metricByName(tag.getString("Metric"), AlarmMetric.TEMPERATURE);
            setting = Mth.clamp(tag.getInt("Setting"), 0, 100);
            label = ControlModule.sanitizeLabel(tag.getString("Label"));
        }
    }

    private final Window[] windows = new Window[WINDOWS];
    private final AnnunciatorLogic logic = new AnnunciatorLogic(WINDOWS);
    private int checkCounter;
    private int hornCounter;
    private boolean ackPowered;
    private boolean resetPowered;
    private boolean outputOn;

    public AnnunciatorPanelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        for (int i = 0; i < WINDOWS; i++) {
            windows[i] = new Window();
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        if (++checkCounter >= Math.max(1, ControlRoom2Config.annunciatorInterval)) {
            checkCounter = 0;
            check();
        }
        if (logic.unacknowledged() && ++hornCounter >= HORN_INTERVAL) {
            hornCounter = 0;
            horn();
        }
    }

    /** Samples every window's target (each machine once) and steps the alarm sequence. */
    public void check() {
        if (level == null || level.isClientSide) {
            return;
        }
        Map<GlobalPos, MonitorReading> samples = new HashMap<>();
        boolean[] tripped = new boolean[WINDOWS];
        for (int i = 0; i < WINDOWS; i++) {
            Window window = windows[i];
            if (window.target == null) {
                continue;
            }
            MonitorReading sample = samples.computeIfAbsent(window.target,
                    target -> MonitorReading.sample(LinkedTarget.resolve(level, getBlockPos(), target)));
            tripped[i] = window.metric.tripped(sample, window.setting);
        }
        int flags = logic.update(tripped);
        if ((flags & AnnunciatorLogic.NEW_ALERT) != 0) {
            hornCounter = 0;
            horn();
        }
        if ((flags & AnnunciatorLogic.RINGBACK) != 0) {
            ringback();
        }
        if ((flags & AnnunciatorLogic.CHANGED) != 0) {
            changed();
        }
    }

    /** The operator acknowledges the flashing windows. */
    public boolean acknowledge() {
        if (!logic.acknowledge()) {
            return false;
        }
        if (logic.active() > 0 && level != null) {
            // Windows that cleared before the acknowledgement go straight to ringback.
            for (int i = 0; i < WINDOWS; i++) {
                if (logic.state(i) == AnnunciatorLogic.State.RINGBACK) {
                    ringback();
                    break;
                }
            }
        }
        changed();
        return true;
    }

    /** The operator resets the windows in ringback. */
    public boolean reset() {
        if (!logic.reset()) {
            return false;
        }
        changed();
        return true;
    }

    private void horn() {
        level.playSound(null, getBlockPos(), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 1.2f, 1.6f);
    }

    private void ringback() {
        if (level != null) {
            level.playSound(null, getBlockPos(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.8f, 0.8f);
        }
    }

    private void changed() {
        setChanged();
        notifyUpdate();
        boolean on = logic.unacknowledged();
        if (on != outputOn && level != null) {
            outputOn = on;
            level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
            level.updateNeighborsAt(getBlockPos().below(), getBlockState().getBlock());
            level.updateNeighborsAt(getBlockPos().relative(facing().getOpposite()), getBlockState().getBlock());
        }
        if (level != null) {
            level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
        }
    }

    // ------------------------------------------------------------------ redstone

    /** Full power out of the back and bottom while any window waits for an acknowledgement. */
    public boolean outputOn() {
        return outputOn;
    }

    /** 0-15: two per window that is not dark. */
    public int comparatorSignal() {
        return Math.min(15, logic.active() * 2 + (logic.unacknowledged() ? 1 : 0));
    }

    /** The viewer's left side acknowledges and the right side resets, on a rising redstone edge. */
    public void redstoneChanged() {
        if (level == null || level.isClientSide) {
            return;
        }
        Direction left = facing().getClockWise();
        Direction right = facing().getCounterClockWise();
        boolean ack = level.getSignal(getBlockPos().relative(left), left) > 0;
        boolean reset = level.getSignal(getBlockPos().relative(right), right) > 0;
        if (ack && !ackPowered) {
            acknowledge();
        }
        if (reset && !resetPowered) {
            reset();
        }
        ackPowered = ack;
        resetPowered = reset;
    }

    // ------------------------------------------------------------------ windows

    public AnnunciatorLogic logic() {
        return logic;
    }

    public Window window(int index) {
        return windows[Mth.clamp(index, 0, WINDOWS - 1)];
    }

    public Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
    }

    /** Links one window, or every window when {@code index} is negative. */
    public void linkWindow(int index, @Nullable GlobalPos target) {
        TargetKind kind = target != null && level != null && level.dimension().equals(target.dimension())
                && level.isLoaded(target.pos()) ? LinkedTarget.kindAt(level, target.pos()) : TargetKind.NONE;
        for (int i = 0; i < WINDOWS; i++) {
            if (index < 0 || index == i) {
                Window window = windows[i];
                window.target = target;
                window.kind = kind;
                if (target != null && !window.metric.watches(kind)) {
                    window.metric = firstMetricFor(kind, window.metric);
                }
            }
        }
        check();
        notifyUpdate();
    }

    /** The first metric that suits {@code kind}, or {@code fallback} when none does. */
    private static AlarmMetric firstMetricFor(TargetKind kind, AlarmMetric fallback) {
        for (AlarmMetric metric : AlarmMetric.values()) {
            if (metric.watches(kind)) {
                return metric;
            }
        }
        return fallback;
    }

    /** "Metric", "Setting", "Label" and "Unlink" for window {@code part}; values are sanitised. */
    @Override
    public boolean configure(int part, CompoundTag config) {
        if (part < 0 || part >= WINDOWS) {
            return false;
        }
        Window window = windows[part];
        if (config.contains("Metric", Tag.TAG_STRING)) {
            window.metric = metricByName(config.getString("Metric"), window.metric);
        }
        if (config.contains("Setting", Tag.TAG_ANY_NUMERIC)) {
            window.setting = Mth.clamp(config.getInt("Setting"), 0, 100);
        }
        if (config.contains("Label", Tag.TAG_STRING)) {
            window.label = ControlModule.sanitizeLabel(config.getString("Label"));
        }
        if (config.getBoolean("Unlink")) {
            window.target = null;
            window.kind = TargetKind.NONE;
        }
        check();
        notifyUpdate();
        return true;
    }

    private static AlarmMetric metricByName(String name, AlarmMetric fallback) {
        for (AlarmMetric metric : AlarmMetric.values()) {
            if (metric.name().equals(name)) {
                return metric;
            }
        }
        return fallback;
    }

    /** The window a front-face click landed on, -1 elsewhere. */
    public static int windowAt(float[] pixel) {
        if (pixel == null) {
            return -1;
        }
        int column = pixel[0] >= 1 && pixel[0] < 7 ? 0 : pixel[0] >= 9 && pixel[0] < 15 ? 1 : -1;
        int row = pixel[1] >= 12 && pixel[1] < 15 ? 0 : pixel[1] >= 8 && pixel[1] < 11 ? 1 : pixel[1] >= 4 && pixel[1] < 7 ? 2 : -1;
        return column < 0 || row < 0 ? -1 : row * 2 + column;
    }

    /** 1 for the acknowledge button, 2 for reset, 0 elsewhere. */
    public static int buttonAt(float[] pixel) {
        if (pixel == null || pixel[1] < 1 || pixel[1] >= 3) {
            return 0;
        }
        return pixel[0] >= 2 && pixel[0] < 7 ? 1 : pixel[0] >= 9 && pixel[0] < 14 ? 2 : 0;
    }

    // ------------------------------------------------------------------ links

    /** The first linked window's target. */
    @Override
    @Nullable
    public GlobalPos getTarget() {
        for (Window window : windows) {
            if (window.target != null) {
                return window.target;
            }
        }
        return null;
    }

    @Override
    public void setTarget(@Nullable GlobalPos target) {
        linkWindow(-1, target);
    }

    /** A click on a window links (or unlinks) that window; a click anywhere else, all of them. */
    @Override
    public void setTarget(@Nullable GlobalPos target, Vec3 hit) {
        linkWindow(windowAt(ControlRoomBlocks.frontFacePixel(facing(), getBlockPos(), hit)), target);
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        ListTag list = new ListTag();
        for (Window window : windows) {
            list.add(window.write());
        }
        tag.put("Windows", list);
        tag.putIntArray("States", logic.saveStates());
        tag.putInt("FirstOut", logic.firstOut());
        tag.putBoolean("AckPowered", ackPowered);
        tag.putBoolean("ResetPowered", resetPowered);
        tag.putBoolean("Output", outputOn);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        ListTag list = tag.getList("Windows", Tag.TAG_COMPOUND);
        for (int i = 0; i < WINDOWS; i++) {
            windows[i] = new Window();
            if (i < list.size()) {
                windows[i].read(list.getCompound(i));
            }
        }
        logic.load(tag.getIntArray("States"), tag.contains("FirstOut") ? tag.getInt("FirstOut") : -1);
        ackPowered = tag.getBoolean("AckPowered");
        resetPowered = tag.getBoolean("ResetPowered");
        outputOn = tag.getBoolean("Output");
    }
}
