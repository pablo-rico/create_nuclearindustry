package org.papiricoh.create_nuclearindustry.controlroom;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * One block of a control room screen. Each monitor works out the rectangle of same-facing monitors
 * it belongs to; the bottom-left one (as seen from the front) is the origin, which holds the link,
 * samples the target once a second, keeps two minutes of history and draws the whole screen.
 */
public class ControlMonitorBlockEntity extends SmartBlockEntity implements ControlRoomDevice {
    public static final int MAX_SIZE = 8;
    public static final int HISTORY = 120;
    private static final int SAMPLE_INTERVAL = 20;
    private static final int CLUSTER_CHECK_INTERVAL = 40;

    @Nullable
    private GlobalPos target;
    private int page;
    @Nullable
    private BlockPos origin;
    private int width = 1;
    private int height = 1;
    private boolean clusterDirty = true;
    private int clusterCheckCounter;
    private int sampleCounter = SAMPLE_INTERVAL;
    private MonitorReading reading = MonitorReading.NONE;
    private final float[] historyA = new float[HISTORY];
    private final float[] historyB = new float[HISTORY];
    private int historySize;

    public ControlMonitorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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
        if (clusterDirty || ++clusterCheckCounter >= CLUSTER_CHECK_INTERVAL) {
            clusterCheckCounter = 0;
            updateCluster();
        }
        if (isOrigin() && ++sampleCounter >= SAMPLE_INTERVAL) {
            sampleCounter = 0;
            sample();
        }
    }

    public void requestClusterUpdate() {
        clusterDirty = true;
    }

    // ------------------------------------------------------------------ cluster

    public Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
    }

    /** The viewer's right when looking at the screen. */
    public Direction right() {
        return facing().getCounterClockWise();
    }

    public boolean isOrigin() {
        return origin == null || origin.equals(getBlockPos());
    }

    public int screenWidth() {
        return width;
    }

    public int screenHeight() {
        return height;
    }

    /** The monitor that holds the link and draws this screen. */
    public Optional<ControlMonitorBlockEntity> origin() {
        if (isOrigin()) {
            return Optional.of(this);
        }
        return level != null && level.getBlockEntity(origin) instanceof ControlMonitorBlockEntity monitor
                ? Optional.of(monitor)
                : Optional.empty();
    }

    private void updateCluster() {
        clusterDirty = false;
        Direction facing = facing();
        Direction right = facing.getCounterClockWise();
        Set<BlockPos> members = connected(facing, right);
        int minR = Integer.MAX_VALUE, maxR = Integer.MIN_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (BlockPos member : members) {
            int r = along(member, right);
            minR = Math.min(minR, r);
            maxR = Math.max(maxR, r);
            minY = Math.min(minY, member.getY());
            maxY = Math.max(maxY, member.getY());
        }
        int newWidth = maxR - minR + 1;
        int newHeight = maxY - minY + 1;
        BlockPos newOrigin = getBlockPos();
        if (newWidth <= MAX_SIZE && newHeight <= MAX_SIZE && members.size() == newWidth * newHeight) {
            for (BlockPos member : members) {
                if (along(member, right) == minR && member.getY() == minY) {
                    newOrigin = member;
                }
            }
        } else {
            newWidth = 1;
            newHeight = 1;
        }
        boolean changed = width != newWidth || height != newHeight || !newOrigin.equals(origin == null ? getBlockPos() : origin);
        width = newWidth;
        height = newHeight;
        origin = newOrigin.equals(getBlockPos()) ? null : newOrigin;
        if (isOrigin() && target == null) {
            adoptTargetFrom(members);
        }
        if (changed) {
            sampleCounter = SAMPLE_INTERVAL;
            notifyUpdate();
        }
    }

    /** A screen grown from linked monitors keeps the link of one of them. */
    private void adoptTargetFrom(Set<BlockPos> members) {
        for (BlockPos member : members) {
            if (!member.equals(getBlockPos()) && level.getBlockEntity(member) instanceof ControlMonitorBlockEntity monitor
                    && monitor.target != null) {
                target = monitor.target;
                monitor.target = null;
                monitor.setChanged();
                return;
            }
        }
    }

    /** Same-facing monitors reachable sideways or up and down in the screen's plane. */
    private Set<BlockPos> connected(Direction facing, Direction right) {
        Set<BlockPos> found = new HashSet<>();
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        found.add(getBlockPos());
        open.add(getBlockPos());
        int limit = MAX_SIZE * MAX_SIZE + 1;
        while (!open.isEmpty() && found.size() <= limit) {
            BlockPos current = open.poll();
            for (BlockPos next : new BlockPos[]{current.relative(right), current.relative(right.getOpposite()), current.above(), current.below()}) {
                if (found.contains(next) || !level.isLoaded(next)) {
                    continue;
                }
                BlockState state = level.getBlockState(next);
                if (state.getBlock() == getBlockState().getBlock() && state.getValue(BlockStateProperties.HORIZONTAL_FACING) == facing) {
                    found.add(next);
                    open.add(next);
                }
            }
        }
        return found;
    }

    private static int along(BlockPos pos, Direction axis) {
        return pos.getX() * axis.getStepX() + pos.getZ() * axis.getStepZ();
    }

    // ------------------------------------------------------------------ link and sampling

    @Override
    @Nullable
    public GlobalPos getTarget() {
        return origin().map(monitor -> monitor.target).orElse(null);
    }

    @Override
    public void setTarget(@Nullable GlobalPos newTarget) {
        Optional<ControlMonitorBlockEntity> screen = origin();
        if (screen.isPresent() && screen.get() != this) {
            screen.get().setTarget(newTarget);
            return;
        }
        target = newTarget;
        historySize = 0;
        page = 0;
        sampleCounter = SAMPLE_INTERVAL;
        if (level != null && !level.isClientSide) {
            sample();
        }
        notifyUpdate();
    }

    private void sample() {
        MonitorReading next = MonitorReading.sample(LinkedTarget.resolve(level, getBlockPos(), target));
        if (next.kind() != reading.kind()) {
            historySize = 0;
        }
        int previousSignal = reading.comparatorSignal();
        reading = next;
        if (next.kind() != TargetKind.NONE && next.kind() != TargetKind.NO_SIGNAL) {
            record(next.seriesA(), next.seriesB());
        }
        if (previousSignal != next.comparatorSignal()) {
            for (BlockPos member : members()) {
                level.updateNeighbourForOutputSignal(member, getBlockState().getBlock());
            }
        }
        notifyUpdate();
    }

    private void record(float a, float b) {
        if (historySize == HISTORY) {
            System.arraycopy(historyA, 1, historyA, 0, HISTORY - 1);
            System.arraycopy(historyB, 1, historyB, 0, HISTORY - 1);
            historySize--;
        }
        historyA[historySize] = a;
        historyB[historySize] = b;
        historySize++;
    }

    /** Every block of this screen, from the origin. */
    public List<BlockPos> members() {
        List<BlockPos> members = new java.util.ArrayList<>(width * height);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                members.add(getBlockPos().relative(right(), i).above(j));
            }
        }
        return members;
    }

    public MonitorReading getReading() {
        return reading;
    }

    public int historySize() {
        return historySize;
    }

    public float historyA(int index) {
        return historyA[index];
    }

    public float historyB(int index) {
        return historyB[index];
    }

    public int getPage() {
        return page;
    }

    public void nextPage() {
        page = (page + 1) % pageCount();
        notifyUpdate();
    }

    /**
     * Big screens switch between readings and the graph; a single block steps through its most
     * important readings, the first rows the renderer lists for the kind of target.
     */
    public int pageCount() {
        if (width > 1 || height > 1) {
            return 2;
        }
        return switch (reading.kind()) {
            case REACTOR -> reading.flag(MonitorReading.R_PWR) ? 10 : 6;
            case STEAM_GENERATOR, CONTAINMENT -> 4;
            case TOKAMAK -> 4;
            case TURBINE, COOLING_TOWER -> 3;
            case AREA -> 2;
            default -> 1;
        };
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        ControlRoomDevice.writeTarget(tag, target);
        tag.putInt("Page", page);
        if (origin != null) {
            tag.putLong("Origin", origin.asLong());
        }
        tag.putInt("Width", width);
        tag.putInt("Height", height);
        tag.put("Reading", reading.write());
        tag.putIntArray("HistoryA", bits(historyA, historySize));
        tag.putIntArray("HistoryB", bits(historyB, historySize));
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        target = ControlRoomDevice.readTarget(tag);
        page = Math.max(0, tag.getInt("Page"));
        origin = tag.contains("Origin") ? BlockPos.of(tag.getLong("Origin")) : null;
        width = Math.max(1, Math.min(MAX_SIZE, tag.contains("Width") ? tag.getInt("Width") : 1));
        height = Math.max(1, Math.min(MAX_SIZE, tag.contains("Height") ? tag.getInt("Height") : 1));
        reading = tag.contains("Reading") ? MonitorReading.read(tag.getCompound("Reading")) : MonitorReading.NONE;
        int[] a = tag.getIntArray("HistoryA");
        int[] b = tag.getIntArray("HistoryB");
        historySize = Math.min(HISTORY, Math.min(a.length, b.length));
        for (int i = 0; i < historySize; i++) {
            historyA[i] = Float.intBitsToFloat(a[i]);
            historyB[i] = Float.intBitsToFloat(b[i]);
        }
        clusterDirty = true;
    }

    private static int[] bits(float[] values, int size) {
        int[] bits = new int[size];
        for (int i = 0; i < size; i++) {
            bits[i] = Float.floatToIntBits(values[i]);
        }
        return bits;
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        clusterDirty = true;
    }
}
