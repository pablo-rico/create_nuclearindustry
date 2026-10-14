package org.papiricoh.create_nuclearindustry.controlroom.servo;

import com.simibubi.create.content.contraptions.piston.LinearActuatorBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlockEntity;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencerInstructions;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoomPanels;
import org.papiricoh.create_nuclearindustry.controlroom.KeyedScrollOptionBehaviour;
import org.papiricoh.create_nuclearindustry.panel.PanelCard;
import org.papiricoh.create_nuclearindustry.panel.PanelSource;

import java.util.List;

/**
 * A Servo Gearbox: passes the rotation from one end of its shaft to the other, and turns the far end
 * only as much as it takes to put the Mechanical Pistons or Rope Pulleys it drives where the
 * redstone signal says: {@code signal} fifteenths of the stroke set on its side (cut to how far the
 * actuator reaches), outwards or inwards with the signal as set on its other side.
 *
 * <p>It reads where the nearest actuator stands and moves it whole blocks at a time. Each move tells
 * the actuator its length as Create's Sequenced Gearshift does, so the actuator stops on the block
 * by itself; then the servo lets its output go still and the actuator sets its blocks down. A
 * comparator reads where the actuator stands, on the signal's scale.
 */
public class ServoGearboxBlockEntity extends SplitShaftBlockEntity implements PanelSource {
    /** The longest stroke that can be set, in blocks. */
    public static final int MAX_STROKE = 64;
    public static final int DEFAULT_STROKE = 8;
    /** Ticks between two changes of the output, so a jittery signal never makes the shafts flicker. */
    static final int SWITCH_COOLDOWN = 8;
    /** Ticks a move may make no headway before the servo gives up on it, and then waits to try again. */
    static final int STALL_TICKS = 40;
    static final int STALL_COOLDOWN = 100;
    /** Ticks between two searches for the actuators while the output is still. */
    static final int SCAN_INTERVAL = 20;
    /** How far a move may drift the wrong way before the servo takes it that it turned the actuator backwards. */
    static final float WRONG_WAY_SLACK = 0.3f;

    /** What the servo is doing, for its card. Saved by name. */
    public enum Status {
        /** Nothing turns its input. */
        NO_ROTATION,
        /** Its output turns no piston or pulley. */
        NO_ACTUATOR,
        /** The actuator stands where the signal asks. */
        HOLDING,
        EXTENDING,
        RETRACTING,
        /** A move made no headway (something in the way); the servo tries again shortly. */
        STALLED
    }

    /** The mode's own behaviour type: it shares the block entity with the stroke's scroll value. */
    public static final BehaviourType<KeyedScrollOptionBehaviour<ServoMode>> MODE = new BehaviourType<>("servo_mode");

    private KeyedScrollOptionBehaviour<ServoMode> mode;
    private ScrollValueBehaviour stroke;

    /** -1, 0 or 1: how the output turns against the input. 0 holds the actuator still. */
    private int output;
    /** The block the current move goes to, or -1 between moves. */
    private int movingTo = -1;
    private float moveStart;
    private float lastOffset;
    private int stallTicks;
    private int cooldown;
    /** Flips when a move turned out to run the wrong way: the gearing reversed what the search saw. */
    private int directionCorrection = 1;
    private boolean inputChanged;
    private boolean probing;

    private ServoActuators.Drive drive = ServoActuators.Drive.NONE;
    private int scanCountdown;
    private int comparator;
    private int syncCountdown;

    // What the card shows, kept on both sides.
    private Status status = Status.NO_ROTATION;
    private int signal;
    private int target;
    private float position;
    private int range;
    private int actuators;

    public ServoGearboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        mode = new KeyedScrollOptionBehaviour<>(MODE, "ServoMode", ServoMode.class, Component.translatable("create_nuclearindustry.servo.mode"), this,
                new CenteredSideValueBoxTransform((state, direction) -> modeFace(state, direction)));
        stroke = new ScrollValueBehaviour(Component.translatable("create_nuclearindustry.servo.stroke"), this,
                new CenteredSideValueBoxTransform((state, direction) -> strokeFace(state, direction)))
                .between(1, MAX_STROKE);
        stroke.value = DEFAULT_STROKE;
        behaviours.add(mode);
        behaviours.add(stroke);
    }

    /** The mode is set on the two faces across the first axis square to the shaft, the stroke on the other two. */
    private static boolean modeFace(BlockState state, Direction direction) {
        Direction.Axis shaft = state.hasProperty(ServoGearboxBlock.AXIS) ? state.getValue(ServoGearboxBlock.AXIS) : Direction.Axis.Y;
        return direction.getAxis() == (shaft == Direction.Axis.Z ? Direction.Axis.X : Direction.Axis.Z);
    }

    private static boolean strokeFace(BlockState state, Direction direction) {
        Direction.Axis shaft = state.hasProperty(ServoGearboxBlock.AXIS) ? state.getValue(ServoGearboxBlock.AXIS) : Direction.Axis.Y;
        return direction.getAxis() != shaft && !modeFace(state, direction);
    }

    // ------------------------------------------------------------------ kinetics

    /** The input end passes on as it is; the output end turns as the servo says. */
    @Override
    public float getRotationSpeedModifier(Direction face) {
        if (isVirtual() || probing) {
            return 1;
        }
        return hasSource() && face != getSourceFacing() ? output : 1;
    }

    /** The end of the shaft away from the input, or null with nothing turning the input. */
    @Nullable
    public Direction outputFace() {
        if (!hasSource()) {
            return null;
        }
        Direction source = getSourceFacing();
        return source.getAxis() == getBlockState().getValue(ServoGearboxBlock.AXIS) ? source.getOpposite() : null;
    }

    /** How fast {@code neighbour}, against the output, turns for each RPM the output would put out. */
    float ratioThroughOutput(KineticBlockEntity neighbour) {
        probing = true;
        try {
            return ServoActuators.modifier(this, neighbour);
        } finally {
            probing = false;
        }
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        if (output != 0 && previousSpeed != 0) {
            // The actuator would take the new speed with the old length: stop and plan again.
            inputChanged = true;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        if (cooldown > 0) {
            cooldown--;
        }
        if (needsSpeedUpdate()) {
            // The output was just changed: the shafts beyond it join up again this tick.
            return;
        }
        signal = level.getBestNeighborSignal(worldPosition);
        Direction outputFace = outputFace();
        if (getSpeed() == 0 || outputFace == null) {
            halt(false);
            show(Status.NO_ROTATION, null);
            return;
        }
        LinearActuatorBlockEntity actuator = actuator(outputFace);
        if (actuator == null) {
            halt(false);
            show(Status.NO_ACTUATOR, null);
            return;
        }
        range = ServoActuators.range(actuator);
        target = ServoPlan.target(signal, stroke.getValue(), range, mode.get().extendsWithSignal());
        position = actuator.offset;
        if (output != 0) {
            follow(actuator);
        } else {
            plan(actuator);
        }
        show(status, actuator);
    }

    /** The nearest actuator, searched for again now and then while the output is still. */
    @Nullable
    private LinearActuatorBlockEntity actuator(Direction outputFace) {
        LinearActuatorBlockEntity primary = drive.primary();
        boolean lost = primary == null || primary.isRemoved() || primary.getLevel() != level;
        if (output == 0 && (lost || --scanCountdown <= 0) || output != 0 && lost) {
            drive = ServoActuators.find(this, outputFace);
            scanCountdown = SCAN_INTERVAL;
            actuators = drive.actuators().size();
        }
        return drive.primary();
    }

    /** While moving: stop on arrival, when the target changes, when it runs the wrong way, or when it is stuck. */
    private void follow(LinearActuatorBlockEntity actuator) {
        if (inputChanged || ServoPlan.arrived(position, movingTo) || target != movingTo) {
            inputChanged = false;
            halt(false);
            status = ServoPlan.arrived(position, target) ? Status.HOLDING : status;
            return;
        }
        // The gearing beyond the output can only be read for certain once it turns: a gearshift with
        // nothing turning it says nothing of which way it will. Check the actuator's real speed.
        int wanted = Integer.signum(movingTo - Math.round(moveStart));
        int turning = (int) Math.signum(actuator.getSpeed()) * ServoActuators.extensionSign(actuator);
        if (turning != 0 && turning != wanted || ServoPlan.movingAway(moveStart, position, movingTo, WRONG_WAY_SLACK)) {
            directionCorrection = -directionCorrection;
            halt(false);
            return;
        }
        if (Math.abs(position - lastOffset) > 1.0e-4f) {
            lastOffset = position;
            stallTicks = 0;
        } else if (++stallTicks > STALL_TICKS) {
            halt(true);
            status = Status.STALLED;
        }
    }

    /** While still: set off for the target when the actuator is not on it. */
    private void plan(LinearActuatorBlockEntity actuator) {
        int steps = ServoPlan.stepsTo(position, target);
        if (steps == 0) {
            status = Status.HOLDING;
            return;
        }
        if (status != Status.STALLED) {
            status = steps > 0 ? Status.EXTENDING : Status.RETRACTING;
        }
        if (cooldown > 0 || actuator.running && actuator.getSpeed() != 0) {
            return;
        }
        float ratio = drive.ratio();
        float speed = getSpeed();
        if (ratio == 0 || speed == 0) {
            return;
        }
        // The actuator's offset grows with its own speed (turned over by some pistons); pick the
        // output that turns it the right way.
        int towards = Integer.signum(steps) * ServoActuators.extensionSign(actuator) * directionCorrection;
        int newOutput = towards * (int) Math.signum(speed) * (int) Math.signum(ratio);
        float actuatorSpeed = Math.abs(speed * ratio);
        // Create's length of a move: the actuator stops after this many blocks at the speed it gets.
        sequenceContext = new SequencedGearshiftBlockEntity.SequenceContext(SequencerInstructions.TURN_DISTANCE,
                Math.abs(steps) / actuatorSpeed);
        movingTo = target;
        moveStart = position;
        lastOffset = position;
        stallTicks = 0;
        status = steps > 0 ? Status.EXTENDING : Status.RETRACTING;
        setOutput(newOutput);
    }

    /** Stops the output, if it turns; {@code stalled} waits longer before the next try. */
    private void halt(boolean stalled) {
        movingTo = -1;
        stallTicks = 0;
        if (output != 0) {
            sequenceContext = null;
            setOutput(0);
        }
        if (stalled) {
            cooldown = STALL_COOLDOWN;
        }
    }

    /**
     * Changes how the output turns: lets the shafts beyond it go, then joins them up again next
     * tick with the new speed (and the length of the move), as Create's gearshifts do.
     */
    private void setOutput(int newOutput) {
        if (newOutput == output || level == null) {
            return;
        }
        output = newOutput;
        detachKinetics();
        updateSpeed = true;
        cooldown = Math.max(cooldown, SWITCH_COOLDOWN);
        ServoGearboxBlock.Motion motion = output == 0 ? ServoGearboxBlock.Motion.IDLE
                : movingTo > Math.round(moveStart) ? ServoGearboxBlock.Motion.EXTENDING : ServoGearboxBlock.Motion.RETRACTING;
        BlockState state = getBlockState();
        if (state.hasProperty(ServoGearboxBlock.MOTION) && state.getValue(ServoGearboxBlock.MOTION) != motion) {
            level.setBlock(worldPosition, state.setValue(ServoGearboxBlock.MOTION, motion), 2);
        }
        sendData();
    }

    /** Keeps the card's numbers and the comparator up to date, sending them to the clients when they change. */
    private void show(Status newStatus, @Nullable LinearActuatorBlockEntity actuator) {
        boolean changed = newStatus != status;
        status = newStatus;
        if (actuator == null) {
            changed |= range != 0 || actuators != 0;
            range = 0;
            target = 0;
            position = 0;
            actuators = 0;
            drive = ServoActuators.Drive.NONE;
        }
        int newComparator = actuator == null ? 0
                : ServoPlan.comparator(position, stroke.getValue(), range, mode.get().extendsWithSignal());
        if (newComparator != comparator) {
            comparator = newComparator;
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            changed = true;
        }
        if (--syncCountdown <= 0 && (changed || output != 0 || shownChanged())) {
            syncCountdown = output != 0 ? 4 : 1;
            rememberShown();
            sendData();
        }
    }

    private Status sentStatus;
    private int sentSignal = -1;
    private int sentTarget = -1;
    private int sentPosition = -1;

    private boolean shownChanged() {
        return sentStatus != status || sentSignal != signal || sentTarget != target || sentPosition != Math.round(position * 16);
    }

    private void rememberShown() {
        sentStatus = status;
        sentSignal = signal;
        sentTarget = target;
        sentPosition = Math.round(position * 16);
    }

    // ------------------------------------------------------------------ reading

    /** 0-15, where the actuator stands along the stroke, on the signal's scale. */
    public int comparatorSignal() {
        return comparator;
    }

    public int output() {
        return output;
    }

    public Status status() {
        return status;
    }

    public int signal() {
        return signal;
    }

    /** The block the signal puts the actuator at. */
    public int target() {
        return target;
    }

    /** Where the nearest actuator stands, in blocks out. */
    public float position() {
        return position;
    }

    /** How far the nearest actuator reaches. */
    public int range() {
        return range;
    }

    /** How many actuators the output turns. */
    public int actuators() {
        return actuators;
    }

    public int stroke() {
        return stroke.getValue();
    }

    public ServoMode mode() {
        return mode.get();
    }

    /** Sets the stroke and mode, as scrolling the sides would; for tests and builders. */
    public void configure(int newStroke, ServoMode newMode) {
        stroke.setValue(Mth.clamp(newStroke, 1, MAX_STROKE));
        mode.setValue(newMode.ordinal());
    }

    @Override
    public PanelCard panelCard(Vec3 hit, boolean detailed) {
        return ControlRoomPanels.servo(this);
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Output", output);
        tag.putInt("MovingTo", movingTo);
        tag.putFloat("MoveStart", moveStart);
        tag.putInt("Correction", directionCorrection);
        tag.putInt("Cooldown", cooldown);
        tag.putString("Status", status.name());
        tag.putInt("Signal", signal);
        tag.putInt("Target", target);
        tag.putFloat("Position", position);
        tag.putInt("Range", range);
        tag.putInt("Actuators", actuators);
        tag.putInt("Comparator", comparator);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        output = Mth.clamp(tag.getInt("Output"), -1, 1);
        movingTo = tag.contains("MovingTo") ? tag.getInt("MovingTo") : -1;
        moveStart = tag.getFloat("MoveStart");
        lastOffset = moveStart;
        directionCorrection = tag.getInt("Correction") < 0 ? -1 : 1;
        cooldown = Math.max(0, tag.getInt("Cooldown"));
        try {
            status = Status.valueOf(tag.getString("Status"));
        } catch (IllegalArgumentException e) {
            status = Status.NO_ROTATION;
        }
        signal = tag.getInt("Signal");
        target = tag.getInt("Target");
        position = tag.getFloat("Position");
        range = tag.getInt("Range");
        actuators = tag.getInt("Actuators");
        comparator = Mth.clamp(tag.getInt("Comparator"), 0, 15);
    }
}
