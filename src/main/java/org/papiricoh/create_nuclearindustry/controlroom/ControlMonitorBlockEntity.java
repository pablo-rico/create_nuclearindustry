package org.papiricoh.create_nuclearindustry.controlroom;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.PerformanceConfig;
import org.papiricoh.create_nuclearindustry.controlroom.desk.GaugeReading;
import org.papiricoh.create_nuclearindustry.multiblock.SyncThrottle;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * One block of a control room screen. Each monitor belongs to a rectangle of same-facing monitors;
 * the bottom-left one (as seen from the front) is the origin, which holds the link, samples the
 * target once a second, keeps two minutes of history and draws the whole screen.
 *
 * <p>The screen's shape is worked out by one monitor for all of them when a block next to one of
 * them changes (and by the origin, as a safety net, every {@link #CLUSTER_FALLBACK_INTERVAL} ticks),
 * not by every monitor every two seconds. Clients get the history with the chunk; later updates carry
 * only the samples recorded since the last one, and go out when the reading changed by a visible
 * step, at most every {@link PerformanceConfig#screenSyncTicks} ticks, or every
 * {@link PerformanceConfig#forcedSyncTicks} ticks while the graph has new samples.
 *
 * <p>The origin also holds the screen's {@link MonitorConfig}, set up with a wrench from any of its
 * monitors: the setup goes to the origin as the link does, and to the new origin when screens join.
 */
public class ControlMonitorBlockEntity extends SmartBlockEntity implements ControlRoomDevice, ConfigurableDevice {
    public static final int MAX_SIZE = 8;
    public static final int HISTORY = 120;
    private static final int SAMPLE_INTERVAL = 20;
    /** The origin looks at its screen's shape again this often, in case a change went unnoticed. */
    public static final int CLUSTER_FALLBACK_INTERVAL = 600;
    /** A reading that moved by this share of its size (and at least the absolute step) is worth an update. */
    private static final double VISIBLE_RELATIVE = 0.005;
    private static final double VISIBLE_ABSOLUTE = 0.001;
    /** One pixel row of the graph. */
    private static final double VISIBLE_SERIES = 1.0 / 120.0;

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
    private MonitorConfig config = MonitorConfig.DEFAULT;
    private MonitorReading reading = MonitorReading.NONE;
    private final float[] historyA = new float[HISTORY];
    private final float[] historyB = new float[HISTORY];
    /** The optional third line; NaN where it plotted nothing (none chosen, or chosen after the sample). */
    private final float[] historyC = new float[HISTORY];
    private int historySize;
    /** Samples recorded since the screen was placed or loaded: a client tells by it which samples it has. */
    private long historySeq;
    /** {@link #historySeq} when the last update was broadcast ({@link #sendData}). */
    private long broadcastSeq;
    /**
     * {@link #historySeq} at the broadcast before that: every client watching has at least these
     * samples, so update packets carry the samples after it. Only {@link #sendData} moves it, never
     * the packet itself: NeoForge also makes update packets for a single player (a block break a
     * claim mod cancelled, say), and such a packet must not make the others skip samples.
     */
    private long deltaFromSeq;
    /** The packets of the current broadcast carry the whole history. */
    private boolean broadcastFull = true;
    /** Game time of the last broadcast. */
    private long broadcastTime = Long.MIN_VALUE;
    /** An update packet was made since the last broadcast: the broadcast's packet may have gone out. */
    private boolean packetSinceBroadcast;
    /** The reading the clients were last sent. */
    private MonitorReading sentReading = MonitorReading.NONE;
    private long lastSyncTime = Long.MIN_VALUE / 2;
    /** The reading changed visibly since the last update. */
    private boolean syncDue;
    /** The next update carries the whole history: it was cleared or the screen changed. */
    private boolean fullSyncPending = true;
    /** Set while an update packet is written, so {@link #write} sends only the new samples. */
    private boolean writingDelta;
    private int clientSyncs;
    private int clusterScans;

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
        if (clusterDirty || isOrigin() && ++clusterCheckCounter >= CLUSTER_FALLBACK_INTERVAL) {
            updateCluster();
        }
        if (!isOrigin()) {
            return;
        }
        if (++sampleCounter >= SAMPLE_INTERVAL) {
            sampleCounter = 0;
            sample();
        }
        long sinceSync = level.getGameTime() - lastSyncTime;
        if (syncDue && sinceSync >= Math.max(1, PerformanceConfig.screenSyncTicks)
                || historySeq != broadcastSeq && sinceSync >= PerformanceConfig.forcedSyncTicks) {
            notifyUpdate();
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

    /**
     * Works out the screen this monitor is part of and hands the answer to every monitor of it, so
     * one search serves the whole screen.
     */
    private void updateCluster() {
        clusterScans++;
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
        boolean rectangle = newWidth <= MAX_SIZE && newHeight <= MAX_SIZE && members.size() == newWidth * newHeight;
        BlockPos newOrigin = getBlockPos();
        for (BlockPos member : members) {
            if (along(member, right) == minR && member.getY() == minY) {
                newOrigin = member;
            }
        }
        ControlMonitorBlockEntity screen = null;
        for (BlockPos member : members) {
            ControlMonitorBlockEntity monitor = member.equals(getBlockPos()) ? this
                    : level.getBlockEntity(member) instanceof ControlMonitorBlockEntity other ? other : null;
            if (monitor == null) {
                continue;
            }
            if (rectangle) {
                monitor.applyCluster(newOrigin, newWidth, newHeight);
                if (member.equals(newOrigin)) {
                    screen = monitor;
                }
            } else {
                // Not a rectangle: every monitor is a screen of its own.
                monitor.applyCluster(member, 1, 1);
            }
        }
        if (screen != null) {
            ControlMonitorBlockEntity donor = screen.target == null ? screen.adoptTargetFrom(members) : null;
            if (screen.config.isDefault()) {
                screen.adoptConfigFrom(members, donor);
            }
            // Only the origin keeps a setup: a member's leftover one must not come back as the screen's
            // when the screen is later reset to its defaults.
            for (BlockPos member : members) {
                if (!member.equals(screen.getBlockPos()) && level.getBlockEntity(member) instanceof ControlMonitorBlockEntity monitor
                        && !monitor.config.isDefault()) {
                    monitor.config = MonitorConfig.DEFAULT;
                    monitor.setChanged();
                }
            }
        }
    }

    /** Takes the screen worked out by a monitor of it. */
    private void applyCluster(BlockPos newOrigin, int newWidth, int newHeight) {
        clusterDirty = false;
        clusterCheckCounter = 0;
        boolean changed = width != newWidth || height != newHeight || !newOrigin.equals(origin == null ? getBlockPos() : origin);
        width = newWidth;
        height = newHeight;
        origin = newOrigin.equals(getBlockPos()) ? null : newOrigin.immutable();
        if (changed) {
            sampleCounter = SAMPLE_INTERVAL;
            fullSyncPending = true;
            notifyUpdate();
        }
    }

    /** A screen grown from linked monitors keeps the link of one of them, which it returns. */
    @Nullable
    private ControlMonitorBlockEntity adoptTargetFrom(Set<BlockPos> members) {
        for (BlockPos member : members) {
            if (!member.equals(getBlockPos()) && level.getBlockEntity(member) instanceof ControlMonitorBlockEntity monitor
                    && monitor.target != null) {
                target = monitor.target;
                monitor.target = null;
                monitor.setChanged();
                return monitor;
            }
        }
        return null;
    }

    /**
     * A screen grown from set-up monitors keeps the setup of one of them: the one whose link it took
     * when that one was set up, so a screen that grows keeps both its link and its setup.
     */
    private void adoptConfigFrom(Set<BlockPos> members, @Nullable ControlMonitorBlockEntity preferred) {
        ControlMonitorBlockEntity donor = preferred != null && !preferred.config.isDefault() ? preferred : null;
        if (donor == null) {
            for (BlockPos member : members) {
                if (!member.equals(getBlockPos()) && level.getBlockEntity(member) instanceof ControlMonitorBlockEntity monitor
                        && !monitor.config.isDefault()) {
                    donor = monitor;
                    break;
                }
            }
        }
        if (donor == null || donor == this) {
            return;
        }
        config = donor.config;
        donor.config = MonitorConfig.DEFAULT;
        donor.setChanged();
        historySize = 0;
        page = 0;
        fullSyncPending = true;
        setChanged();
        notifyUpdate();
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
        fullSyncPending = true;
        page = 0;
        sampleCounter = SAMPLE_INTERVAL;
        if (level != null && !level.isClientSide) {
            sample();
        }
        notifyUpdate();
    }

    // ------------------------------------------------------------------ setup

    /** This screen's setup, from the origin. */
    public MonitorConfig getConfig() {
        return origin().map(monitor -> monitor.config).orElse(MonitorConfig.DEFAULT);
    }

    /**
     * Sets the screen up (on the origin, whichever monitor it is called on). A new first or second
     * series starts the graph again, a new third one only starts its own line again, and a new layout
     * goes back to the first page; the next update carries the whole state.
     */
    public void setConfig(MonitorConfig newConfig) {
        Optional<ControlMonitorBlockEntity> screen = origin();
        if (screen.isPresent() && screen.get() != this) {
            screen.get().setConfig(newConfig);
            return;
        }
        MonitorConfig old = config;
        config = newConfig == null ? MonitorConfig.DEFAULT : newConfig;
        if (old.seriesA() != config.seriesA() || old.seriesB() != config.seriesB()) {
            historySize = 0;
        } else if (old.seriesC() != config.seriesC()) {
            Arrays.fill(historyC, 0, historySize, Float.NaN);
        }
        if (old.layout() != config.layout()) {
            page = 0;
        }
        page = Math.floorMod(page, pageCount());
        fullSyncPending = true;
        setChanged();
        notifyUpdate();
    }

    /**
     * A setup from the wrench screen ({@link ConfigureDevicePayload}), sanitised for what the screen
     * watches and applied on the origin. The screen opens on the clicked monitor, which may be far
     * from the origin of a big screen, so any monitor of it takes the setup.
     */
    @Override
    public boolean configure(int part, CompoundTag tag) {
        ControlMonitorBlockEntity screen = origin().orElse(null);
        if (screen == null) {
            return false;
        }
        screen.setConfig(MonitorConfig.read(tag).sanitize(screen.targetKind()));
        return true;
    }

    /**
     * What this screen watches: the kind of the last reading or, while it has no signal, the kind of
     * the linked block if it is loaded (on the server). NONE when unlinked or unknown.
     */
    public TargetKind targetKind() {
        TargetKind kind = reading.kind();
        if (kind != TargetKind.NONE && kind != TargetKind.NO_SIGNAL) {
            return kind;
        }
        if (target != null && level != null && !level.isClientSide && level.dimension().equals(target.dimension())
                && level.isLoaded(target.pos())) {
            return LinkedTarget.kindAt(level, target.pos());
        }
        return TargetKind.NONE;
    }

    /** What the graph's first line plots for {@code next}: the series the screen was set up with, or the usual one. */
    private float seriesA(MonitorReading next) {
        GaugeReading chosen = config.seriesA();
        return chosen != null && chosen.kind() == next.kind() ? chosen.fraction(next) : next.seriesA();
    }

    private float seriesB(MonitorReading next) {
        GaugeReading chosen = config.seriesB();
        return chosen != null && chosen.kind() == next.kind() ? chosen.fraction(next) : next.seriesB();
    }

    /** What the graph's third line plots for {@code next}: NaN, drawn as nothing, unless one was chosen. */
    private float seriesC(MonitorReading next) {
        GaugeReading chosen = config.seriesC();
        return chosen != null && chosen.kind() == next.kind() ? chosen.fraction(next) : Float.NaN;
    }

    private void sample() {
        MonitorReading next = SharedReadings.sample(level, getBlockPos(), target);
        if (next.kind() != reading.kind()) {
            // The graph starts again: every client needs the whole (empty) history.
            historySize = 0;
            fullSyncPending = true;
            syncDue = true;
        }
        int previousSignal = reading.comparatorSignal();
        reading = next;
        if (next.kind() != TargetKind.NONE && next.kind() != TargetKind.NO_SIGNAL) {
            record(seriesA(next), seriesB(next), seriesC(next));
        }
        if (previousSignal != next.comparatorSignal()) {
            for (BlockPos member : members()) {
                level.updateNeighbourForOutputSignal(member, getBlockState().getBlock());
            }
        }
        if (visiblyChanged(sentReading, next)) {
            syncDue = true;
        }
        setChanged();
    }

    /** Whether a screen showing {@code shown} would look different showing {@code next}. */
    public static boolean visiblyChanged(MonitorReading shown, MonitorReading next) {
        if (shown.kind() != next.kind() || !shown.status().equals(next.status()) || shown.values().length != next.values().length) {
            return true;
        }
        for (int i = 0; i < next.values().length; i++) {
            if (SyncThrottle.visiblyDifferent(shown.values()[i], next.values()[i], VISIBLE_ABSOLUTE, VISIBLE_RELATIVE)) {
                return true;
            }
        }
        return SyncThrottle.visiblyDifferent(shown.seriesA(), next.seriesA(), VISIBLE_SERIES, 0)
                || SyncThrottle.visiblyDifferent(shown.seriesB(), next.seriesB(), VISIBLE_SERIES, 0);
    }

    private void record(float a, float b, float c) {
        if (historySize == HISTORY) {
            System.arraycopy(historyA, 1, historyA, 0, HISTORY - 1);
            System.arraycopy(historyB, 1, historyB, 0, HISTORY - 1);
            System.arraycopy(historyC, 1, historyC, 0, HISTORY - 1);
            historySize--;
        }
        historyA[historySize] = a;
        historyB[historySize] = b;
        historyC[historySize] = c;
        historySize++;
        historySeq++;
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

    /** The third line at {@code index}: NaN where it plots nothing. */
    public float historyC(int index) {
        return historyC[index];
    }

    public int getPage() {
        return page;
    }

    public void nextPage() {
        page = (page + 1) % pageCount();
        notifyUpdate();
    }

    /**
     * Big screens laid out {@link MonitorLayout#AUTO} switch between readings and the graph; the
     * other layouts show one page, unless the style's readings need more ({@link MonitorPaging}): a
     * mimic board's instruments are bigger than a list's rows. A single block steps through its most
     * important readings (the first rows of its kind of target) or through every row it was set up to
     * show, then shows the graph when laid out {@link MonitorLayout#BOTH}; laid out
     * {@link MonitorLayout#CHART} it only plots.
     */
    public int pageCount() {
        if (width > 1 || height > 1) {
            int rows = config.theme() == MonitorTheme.STANDARD ? 0 : MonitorRow.visible(reading, config).size();
            return MonitorPaging.pageCount(config.theme(), config.layout(), width, height, rows);
        }
        if (config.layout() == MonitorLayout.CHART) {
            return 1;
        }
        int readings = config.customRows() ? Math.max(1, MonitorRow.visible(reading, config).size()) : readingPages();
        return readings + (config.layout() == MonitorLayout.BOTH ? 1 : 0);
    }

    /** Pages of a single block whose rows nobody arranged: the first rows of its kind of target. */
    private int readingPages() {
        return switch (reading.kind()) {
            case REACTOR -> reading.flag(MonitorReading.R_PWR) ? 10 : 6;
            case STEAM_GENERATOR, CONTAINMENT -> 4;
            case TOKAMAK -> 4;
            case TURBINE, COOLING_TOWER -> 3;
            case AREA -> 2;
            default -> 1;
        };
    }

    // ------------------------------------------------------------------ client updates

    /**
     * Every update after the chunk's: the samples recorded since the broadcast before the last one,
     * unless the history was cleared since. Making the packet changes nothing, so a packet made for a
     * single player repeats the same samples; a client drops those it has by their sequence number.
     */
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this, (blockEntity, registries) -> packetTag(registries));
    }

    private CompoundTag packetTag(HolderLookup.Provider registries) {
        packetSinceBroadcast = true;
        CompoundTag tag = new CompoundTag();
        writingDelta = !broadcastFull && !fullSyncPending;
        try {
            write(tag, registries, true);
        } finally {
            writingDelta = false;
        }
        return tag;
    }

    /**
     * Broadcasts an update to every player watching; the level makes its one packet when it next sends
     * the chunk's changes. The delta bookkeeping moves here and not in {@link #getUpdatePacket}: only a
     * broadcast reaches every client.
     */
    @Override
    public void sendData() {
        if (level != null && !level.isClientSide) {
            clientSyncs++;
            syncDue = false;
            sentReading = reading;
            long now = level.getGameTime();
            lastSyncTime = now;
            if (SyncThrottle.startsNewBroadcast(packetSinceBroadcast, broadcastTime, now)) {
                deltaFromSeq = broadcastSeq;
                broadcastFull = false;
            }
            broadcastFull |= fullSyncPending;
            fullSyncPending = false;
            broadcastSeq = historySeq;
            broadcastTime = now;
            packetSinceBroadcast = false;
        }
        super.sendData();
    }


    /** Client updates this monitor asked for, for tests. */
    public int clientSyncCount() {
        return clientSyncs;
    }

    /** Searches for the shape of its screen this monitor made, for tests. */
    public int clusterScanCount() {
        return clusterScans;
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
        if (clientPacket) {
            tag.putLong("HistorySeq", historySeq);
        }
        if (writingDelta) {
            int fresh = (int) Math.min(historySize, Math.max(0, historySeq - deltaFromSeq));
            tag.putIntArray("AppendA", bits(historyA, historySize - fresh, historySize));
            tag.putIntArray("AppendB", bits(historyB, historySize - fresh, historySize));
            if (config.seriesC() != null) {
                tag.putIntArray("AppendC", bits(historyC, historySize - fresh, historySize));
            }
            return;
        }
        tag.putIntArray("HistoryA", bits(historyA, 0, historySize));
        tag.putIntArray("HistoryB", bits(historyB, 0, historySize));
        // Most screens have no third line: they send and save nothing for it.
        if (config.seriesC() != null) {
            tag.putIntArray("HistoryC", bits(historyC, 0, historySize));
        }
        // The setup seldom changes: it goes with the whole state, never in a delta.
        if (!config.isDefault()) {
            tag.put("Config", config.write());
        }
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
        clusterDirty = true;
        long had = historySeq;
        long seq = tag.getLong("HistorySeq");
        if (clientPacket && tag.contains("AppendA")) {
            // An update: only the samples recorded after those this copy has.
            int[] a = tag.getIntArray("AppendA");
            int[] b = tag.getIntArray("AppendB");
            int[] c = tag.getIntArray("AppendC");
            int sent = Math.min(a.length, b.length);
            int fresh = (int) Math.max(0, Math.min(sent, seq - had));
            for (int i = sent - fresh; i < sent; i++) {
                record(Float.intBitsToFloat(a[i]), Float.intBitsToFloat(b[i]), at(c, i));
            }
            historySeq = seq;
            return;
        }
        historySeq = seq;
        config = tag.contains("Config", Tag.TAG_COMPOUND) ? MonitorConfig.read(tag.getCompound("Config")) : MonitorConfig.DEFAULT;
        int[] a = tag.getIntArray("HistoryA");
        int[] b = tag.getIntArray("HistoryB");
        int[] c = tag.getIntArray("HistoryC");
        historySize = Math.min(HISTORY, Math.min(a.length, b.length));
        for (int i = 0; i < historySize; i++) {
            historyA[i] = Float.intBitsToFloat(a[i]);
            historyB[i] = Float.intBitsToFloat(b[i]);
            historyC[i] = at(c, i);
        }
    }

    /** The sample at {@code index} of a third line sent as bits, or NaN where none was sent. */
    private static float at(int[] bits, int index) {
        return index < bits.length ? Float.intBitsToFloat(bits[index]) : Float.NaN;
    }

    private static int[] bits(float[] values, int from, int to) {
        int[] bits = new int[Math.max(0, to - from)];
        for (int i = from; i < to; i++) {
            bits[i - from] = Float.floatToIntBits(values[i]);
        }
        return bits;
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        clusterDirty = true;
    }

    /** Turned with a wrench: the neighbours hear of it, and this monitor looks at its screen again itself. */
    @Override
    @SuppressWarnings("deprecation")
    public void setBlockState(BlockState state) {
        if (state != getBlockState()) {
            clusterDirty = true;
        }
        super.setBlockState(state);
    }
}
