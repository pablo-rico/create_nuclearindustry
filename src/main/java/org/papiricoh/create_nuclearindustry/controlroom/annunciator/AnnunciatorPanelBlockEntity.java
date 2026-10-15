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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearSounds;
import org.papiricoh.create_nuclearindustry.controlroom.AlarmMetric;
import org.papiricoh.create_nuclearindustry.controlroom.ConfigurableDevice;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Config;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoomDevice;
import org.papiricoh.create_nuclearindustry.controlroom.LinkedTarget;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorTheme;
import org.papiricoh.create_nuclearindustry.controlroom.PanelCluster;
import org.papiricoh.create_nuclearindustry.controlroom.SharedReadings;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorBoardLayout.Button;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlModule;
import org.papiricoh.create_nuclearindustry.reactor.control.AutoScram;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * An annunciator panel: six alarm windows, each watching its own machine for one {@link AlarmMetric}
 * past a threshold, run through the {@link AnnunciatorLogic} sequence, with a group nameplate.
 * <p>
 * Panels hung together on one wall in a rectangle make a board (found like a screen's monitors, with
 * {@link PanelCluster}); a lone panel is a board of one. Each panel keeps its windows and checks them
 * itself. The board's bottom-left panel, its origin, holds what the board shares: its style, its
 * first-out window, the horn, SILENCE and the lamp test. ACK, RESET, SILENCE and TEST, from a button
 * or from redstone at any panel, work the whole board. While an alarm waits for an acknowledgement
 * every panel powers the wall it hangs on.
 */
public class AnnunciatorPanelBlockEntity extends SmartBlockEntity implements ControlRoomDevice, ConfigurableDevice {
    public static final int WINDOWS = 6;
    /** Panels a board may span. */
    public static final int MAX_WIDTH = 16;
    public static final int MAX_HEIGHT = 8;
    /** Ticks between horn blasts for an alarm; a warning sounds every other one. */
    private static final int HORN_INTERVAL = 20;
    /** How long a press of TEST lights the board. */
    public static final int LAMP_TEST_TICKS = 60;
    /** The origin looks at its board's shape again this often, in case a change went unnoticed. */
    private static final int CLUSTER_FALLBACK_INTERVAL = 100;

    /** One window's setup. */
    public static final class Window {
        @Nullable
        private GlobalPos target;
        private TargetKind kind = TargetKind.NONE;
        private AlarmMetric metric = AlarmMetric.TEMPERATURE;
        /** 35 of 100 is 1400 °C for the default temperature metric, the automatic SCRAM's default (40 °C a step). */
        private int setting = AutoScram.DEFAULT_THRESHOLD / 40;
        private String label = "";
        private AnnunciatorSeverity severity = AlarmMetric.TEMPERATURE.defaultSeverity();

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

        public AnnunciatorSeverity severity() {
            return severity;
        }

        CompoundTag write() {
            CompoundTag tag = new CompoundTag();
            ControlRoomDevice.writeTarget(tag, target);
            tag.putString("Kind", kind.name());
            tag.putString("Metric", metric.name());
            tag.putInt("Setting", setting);
            tag.putString("Label", label);
            tag.putString("Severity", severity.name());
            return tag;
        }

        void read(CompoundTag tag) {
            target = ControlRoomDevice.readTarget(tag);
            kind = TargetKind.byName(tag.getString("Kind"));
            metric = metricByName(tag.getString("Metric"), AlarmMetric.TEMPERATURE);
            setting = Mth.clamp(tag.getInt("Setting"), 0, 100);
            label = ControlModule.sanitizeLabel(tag.getString("Label"));
            // Windows from before severities take their metric's.
            severity = AnnunciatorSeverity.byName(tag.getString("Severity"), metric.defaultSeverity());
        }
    }

    private final Window[] windows = new Window[WINDOWS];
    private final AnnunciatorLogic logic = new AnnunciatorLogic(WINDOWS);
    private String nameplate = "";
    private MonitorTheme theme = MonitorTheme.STANDARD;
    /** What powers this panel's sides: ACK, RESET, SILENCE, TEST. */
    private final boolean[] inputs = new boolean[4];
    private boolean outputOn;
    private int comparator;

    // The board: every panel knows its origin and the board's size.
    @Nullable
    private BlockPos origin;
    private int width = 1;
    private int height = 1;
    private boolean clusterDirty = true;
    private int clusterCheckCounter;

    // Held by the origin for the whole board.
    @Nullable
    private BlockPos firstOutPanel;
    private int firstOutWindow = -1;
    private boolean silenced;
    private int lampTest;
    private int hornCounter;
    /** The board's inputs as last seen, for rising edges. */
    private final boolean[] boardInputs = new boolean[4];

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
        if (clusterDirty || isOrigin() && ++clusterCheckCounter >= CLUSTER_FALLBACK_INTERVAL) {
            updateCluster();
        }
        // On the same ticks as the other devices on its (first) machine, so they share its sample.
        if (SharedReadings.due(level, getBlockPos(), firstTarget(), ControlRoom2Config.annunciatorInterval)) {
            check(true);
        }
        if (isOrigin()) {
            boardTick();
        }
    }

    /** The origin sounds the horn while the board waits for an acknowledgement, and runs the lamp test. */
    private void boardTick() {
        if (lampTest > 0 && --lampTest == 0) {
            sync();
        }
        if (++hornCounter >= HORN_INTERVAL) {
            hornCounter = 0;
            AnnunciatorSeverity waiting = unacknowledgedSeverity();
            if (waiting == null) {
                silenced = false;
            } else if (!silenced && (waiting == AnnunciatorSeverity.ALARM || level.getGameTime() / HORN_INTERVAL % 2 == 0)) {
                horn(waiting);
            }
        }
    }

    /** The machine the first linked window watches, which sets the ticks this panel checks on. */
    @Nullable
    private GlobalPos firstTarget() {
        for (Window window : windows) {
            if (window != null && window.target != null) {
                return window.target;
            }
        }
        return null;
    }

    /** Samples every window's target (each machine once) and steps the alarm sequence. */
    public void check() {
        check(false);
    }

    /**
     * Steps the alarm sequence. The periodic check takes the samples other devices took of the same
     * machines this tick ({@code shareSamples}); a check after the operator changed something takes its own.
     */
    public void check(boolean shareSamples) {
        if (level == null || level.isClientSide) {
            return;
        }
        Map<GlobalPos, MonitorReading> samples = new HashMap<>();
        boolean[] tripped = new boolean[WINDOWS];
        boolean[] latching = new boolean[WINDOWS];
        for (int i = 0; i < WINDOWS; i++) {
            Window window = windows[i];
            latching[i] = window.severity.latching();
            if (window.target == null) {
                continue;
            }
            MonitorReading sample = samples.computeIfAbsent(window.target,
                    target -> shareSamples ? SharedReadings.sample(level, getBlockPos(), target)
                            : MonitorReading.sample(LinkedTarget.resolve(level, getBlockPos(), target)));
            tripped[i] = window.metric.tripped(sample, window.setting);
        }
        int flags = logic.update(tripped, latching);
        if ((flags & AnnunciatorLogic.NEW_ALERT) != 0) {
            int first = logic.lastNewAlert();
            AnnunciatorSeverity worst = null;
            for (int i = 0; i < WINDOWS; i++) {
                if (logic.state(i) == AnnunciatorLogic.State.ALERT && tripped[i]) {
                    worst = AnnunciatorSeverity.worst(worst, windows[i].severity);
                }
            }
            AnnunciatorSeverity sound = worst;
            board().ifPresent(board -> board.boardAlert(getBlockPos(), first, sound));
        }
        if ((flags & AnnunciatorLogic.RINGBACK) != 0) {
            ringback();
        }
        if ((flags & AnnunciatorLogic.CHANGED) != 0) {
            changed();
        }
    }

    /** A panel of this board raised a new alert: the origin marks the first-out, lifts SILENCE and sounds at once. */
    private void boardAlert(BlockPos panel, int window, @Nullable AnnunciatorSeverity severity) {
        if (firstOutPanel == null && window >= 0) {
            firstOutPanel = panel.immutable();
            firstOutWindow = window;
        }
        silenced = false;
        hornCounter = 0;
        horn(severity == null ? AnnunciatorSeverity.WARNING : severity);
        sync();
    }

    // ------------------------------------------------------------------ the operator

    /** A button pressed at any panel of the board. Returns whether it did anything. */
    public boolean press(Button button) {
        AnnunciatorPanelBlockEntity board = board().orElse(this);
        return switch (button) {
            case ACK -> board.acknowledgeBoard();
            case RESET -> board.resetBoard();
            case SILENCE -> board.silenceBoard();
            case TEST -> board.lampTestBoard();
        };
    }

    /** The operator acknowledges the flashing windows of the whole board. */
    public boolean acknowledge() {
        return press(Button.ACK);
    }

    /** The operator resets the windows in ringback on the whole board. */
    public boolean reset() {
        return press(Button.RESET);
    }

    private boolean acknowledgeBoard() {
        boolean[] acted = {false, false};
        forEachPanel(panel -> {
            if (panel.logic.acknowledge()) {
                acted[0] = true;
                for (int i = 0; i < WINDOWS; i++) {
                    // Windows that cleared before the acknowledgement go straight to ringback.
                    if (panel.logic.state(i) == AnnunciatorLogic.State.RINGBACK) {
                        acted[1] = true;
                    }
                }
                panel.changed();
            }
        });
        if (acted[1]) {
            ringback();
        }
        if (acted[0]) {
            silenced = false;
            sync();
        }
        return acted[0];
    }

    private boolean resetBoard() {
        boolean[] acted = {false};
        forEachPanel(panel -> {
            if (panel.logic.reset()) {
                acted[0] = true;
                panel.changed();
            }
        });
        if (acted[0] && firstOutPanel != null && !(level.getBlockEntity(firstOutPanel) instanceof AnnunciatorPanelBlockEntity panel
                && panel.logic.state(Mth.clamp(firstOutWindow, 0, WINDOWS - 1)) != AnnunciatorLogic.State.NORMAL)) {
            clearFirstOut();
        }
        return acted[0];
    }

    private boolean silenceBoard() {
        if (silenced || unacknowledgedSeverity() == null) {
            return false;
        }
        silenced = true;
        sync();
        return true;
    }

    private boolean lampTestBoard() {
        lampTest = LAMP_TEST_TICKS;
        sync();
        return true;
    }

    private void clearFirstOut() {
        firstOutPanel = null;
        firstOutWindow = -1;
        sync();
    }

    /** The most urgent severity among windows of the board waiting for an acknowledgement, or null. */
    @Nullable
    private AnnunciatorSeverity unacknowledgedSeverity() {
        AnnunciatorSeverity[] worst = {null};
        forEachPanel(panel -> {
            for (int i = 0; i < WINDOWS; i++) {
                if (panel.logic.state(i) == AnnunciatorLogic.State.ALERT) {
                    worst[0] = AnnunciatorSeverity.worst(worst[0], panel.windows[i].severity);
                }
            }
        });
        return worst[0];
    }

    private void horn(AnnunciatorSeverity severity) {
        SoundEvent sound = severity == AnnunciatorSeverity.ALARM ? AllNuclearSounds.ANNUNCIATOR_ALARM.get()
                : AllNuclearSounds.ANNUNCIATOR_WARNING.get();
        level.playSound(null, getBlockPos(), sound, SoundSource.BLOCKS, 1.2f, 1.0f);
    }

    private void ringback() {
        if (level != null) {
            level.playSound(null, getBlockPos(), AllNuclearSounds.ANNUNCIATOR_RINGBACK.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        }
    }

    /** This panel's windows changed: sync it and refresh what the board puts out. */
    private void changed() {
        setChanged();
        notifyUpdate();
        AnnunciatorPanelBlockEntity board = board().orElse(this);
        board.refreshOutputs();
    }

    /** The origin works out the board's common alarm and comparator reading and hands them to every panel. */
    private void refreshOutputs() {
        int[] active = {0};
        boolean[] waiting = {false};
        forEachPanel(panel -> {
            active[0] += panel.logic.active();
            waiting[0] |= panel.logic.unacknowledged();
        });
        int signal = Math.min(15, active[0] * 2 + (waiting[0] ? 1 : 0));
        forEachPanel(panel -> panel.setOutputs(waiting[0], signal));
    }

    private void setOutputs(boolean on, int signal) {
        if (level == null) {
            return;
        }
        if (on != outputOn) {
            outputOn = on;
            setChanged();
            level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
            level.updateNeighborsAt(getBlockPos().relative(facing().getOpposite()), getBlockState().getBlock());
        }
        if (signal != comparator) {
            comparator = signal;
            setChanged();
            level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
        }
    }

    // ------------------------------------------------------------------ redstone

    /** Full power into the wall while any window of the board waits for an acknowledgement. */
    public boolean outputOn() {
        return outputOn;
    }

    /** 0-15: two per lit window of the board, one more while one waits for an acknowledgement. */
    public int comparatorSignal() {
        return comparator;
    }

    /**
     * Reads this panel's sides (the viewer's left acknowledges, the right resets, above silences,
     * below holds the lamp test) and lets the origin act on the board's rising edges.
     */
    public void redstoneChanged() {
        if (level == null || level.isClientSide) {
            return;
        }
        Direction left = facing().getClockWise();
        Direction right = facing().getCounterClockWise();
        inputs[Button.ACK.ordinal()] = powered(left);
        inputs[Button.RESET.ordinal()] = powered(right);
        inputs[Button.SILENCE.ordinal()] = powered(Direction.UP);
        inputs[Button.TEST.ordinal()] = powered(Direction.DOWN);
        board().orElse(this).boardInputsChanged();
    }

    /** Power from {@code side}, not counting a panel of the board there. */
    private boolean powered(Direction side) {
        BlockPos from = getBlockPos().relative(side);
        return !(level.getBlockState(from).getBlock() instanceof AnnunciatorPanelBlock) && level.getSignal(from, side) > 0;
    }

    private void boardInputsChanged() {
        boolean[] now = new boolean[4];
        forEachPanel(panel -> {
            for (int i = 0; i < now.length; i++) {
                now[i] |= panel.inputs[i];
            }
        });
        for (Button button : Button.values()) {
            int i = button.ordinal();
            boolean rising = now[i] && !boardInputs[i];
            boardInputs[i] = now[i];
            if (button == Button.TEST) {
                if (now[i] != (lampTest > 0) || rising) {
                    // Held from below: lit while the power stays, then the usual few seconds.
                    lampTest = now[i] ? Integer.MAX_VALUE : Math.min(lampTest, LAMP_TEST_TICKS);
                    sync();
                }
            } else if (rising) {
                press(button);
            }
        }
    }

    // ------------------------------------------------------------------ the board

    public boolean isOrigin() {
        return origin == null || origin.equals(getBlockPos());
    }

    /** The board's origin, or empty while it is not loaded. */
    public java.util.Optional<AnnunciatorPanelBlockEntity> board() {
        if (isOrigin()) {
            return java.util.Optional.of(this);
        }
        return level != null && level.getBlockEntity(origin) instanceof AnnunciatorPanelBlockEntity panel
                ? java.util.Optional.of(panel) : java.util.Optional.empty();
    }

    public int boardWidth() {
        return width;
    }

    public int boardHeight() {
        return height;
    }

    /** To the viewer's right along the board. */
    public Direction right() {
        return facing().getCounterClockWise();
    }

    public void requestClusterUpdate() {
        clusterDirty = true;
    }

    /** Every panel of this board, from the origin: left to right, then bottom to top. */
    public List<BlockPos> boardPanels() {
        BlockPos start = origin == null ? getBlockPos() : origin;
        List<BlockPos> panels = new ArrayList<>(width * height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                panels.add(start.relative(right(), x).above(y));
            }
        }
        return panels;
    }

    /** Runs {@code action} on every loaded panel of the board, this one's origin included. */
    private void forEachPanel(Consumer<AnnunciatorPanelBlockEntity> action) {
        if (level == null) {
            action.accept(this);
            return;
        }
        for (BlockPos pos : boardPanels()) {
            if (pos.equals(getBlockPos())) {
                action.accept(this);
            } else if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof AnnunciatorPanelBlockEntity panel) {
                action.accept(panel);
            }
        }
    }

    /** Every loaded panel of this panel's board, read from its origin: left to right, then bottom to top. */
    public List<AnnunciatorPanelBlockEntity> boardMembers() {
        List<AnnunciatorPanelBlockEntity> members = new ArrayList<>();
        board().orElse(this).forEachPanel(members::add);
        return members;
    }

    /** The panel of the board at {@code blockX} from the left and {@code blockY} from the top, or null. */
    @Nullable
    public AnnunciatorPanelBlockEntity panelAt(int blockX, int blockY) {
        if (level == null || blockX < 0 || blockY < 0 || blockX >= width || blockY >= height) {
            return null;
        }
        BlockPos start = origin == null ? getBlockPos() : origin;
        BlockPos pos = start.relative(right(), blockX).above(height - 1 - blockY);
        return pos.equals(getBlockPos()) ? this : level.getBlockEntity(pos) instanceof AnnunciatorPanelBlockEntity panel ? panel : null;
    }

    /** Where this panel sits on its board: from the left, and from the top. */
    public int[] boardPosition() {
        BlockPos start = origin == null ? getBlockPos() : origin;
        int x = PanelCluster.along(getBlockPos(), right()) - PanelCluster.along(start, right());
        int y = height - 1 - (getBlockPos().getY() - start.getY());
        return new int[]{x, y};
    }

    /** What a click at {@code pixel} on this panel's face (from {@link AnnunciatorPanelBlock#pixel}) lands on. */
    public AnnunciatorBoardLayout.Hit hitAt(@Nullable float[] pixel) {
        if (pixel == null) {
            return AnnunciatorBoardLayout.Hit.NONE;
        }
        int[] at = boardPosition();
        return new AnnunciatorBoardLayout(width, height)
                .hit(at[0] * AnnunciatorBoardLayout.BLOCK + pixel[0], at[1] * AnnunciatorBoardLayout.BLOCK + 16 - pixel[1]);
    }

    /** Works out the board this panel is part of and hands the answer to every panel of it. */
    private void updateCluster() {
        PanelCluster cluster = PanelCluster.find(level, getBlockPos(), facing(), state -> state.getBlock() == getBlockState().getBlock(),
                MAX_WIDTH, MAX_HEIGHT);
        AnnunciatorPanelBlockEntity newOrigin = null;
        for (BlockPos member : cluster.members()) {
            AnnunciatorPanelBlockEntity panel = member.equals(getBlockPos()) ? this
                    : level.getBlockEntity(member) instanceof AnnunciatorPanelBlockEntity other ? other : null;
            if (panel == null) {
                continue;
            }
            if (cluster.rectangle()) {
                panel.applyCluster(cluster.origin(), cluster.width(), cluster.height());
                if (member.equals(cluster.origin())) {
                    newOrigin = panel;
                }
            } else {
                // Not a rectangle: every panel is a board of its own.
                panel.applyCluster(member, 1, 1);
                panel.adoptBoard();
            }
        }
        if (newOrigin != null) {
            newOrigin.adoptBoard();
        }
    }

    private void applyCluster(BlockPos newOrigin, int newWidth, int newHeight) {
        clusterDirty = false;
        clusterCheckCounter = 0;
        boolean changed = width != newWidth || height != newHeight || !newOrigin.equals(origin == null ? getBlockPos() : origin);
        width = newWidth;
        height = newHeight;
        origin = newOrigin.equals(getBlockPos()) ? null : newOrigin.immutable();
        if (changed) {
            setChanged();
            notifyUpdate();
        }
    }

    /**
     * A new or changed board, on its origin: it keeps a style one of its panels was set to, forgets a
     * first-out on a panel no longer on it, and works out its outputs again.
     */
    private void adoptBoard() {
        if (theme == MonitorTheme.STANDARD) {
            forEachPanel(panel -> {
                if (theme == MonitorTheme.STANDARD && panel.theme != MonitorTheme.STANDARD) {
                    theme = panel.theme;
                }
            });
            if (theme != MonitorTheme.STANDARD) {
                MonitorTheme chosen = theme;
                forEachPanel(panel -> panel.setTheme(chosen));
            }
        }
        if (firstOutPanel != null && !boardPanels().contains(firstOutPanel)) {
            clearFirstOut();
        }
        refreshOutputs();
    }

    // ------------------------------------------------------------------ what the renderer and screen read

    public AnnunciatorLogic logic() {
        return logic;
    }

    public Window window(int index) {
        return windows[Mth.clamp(index, 0, WINDOWS - 1)];
    }

    public String nameplate() {
        return nameplate;
    }

    /** The board's style, from the origin. */
    public MonitorTheme theme() {
        return board().map(board -> board.theme).orElse(theme);
    }

    /** Whether window {@code index} of this panel is the board's first-out. */
    public boolean isFirstOut(int index) {
        AnnunciatorPanelBlockEntity board = board().orElse(null);
        return board != null && getBlockPos().equals(board.firstOutPanel) && board.firstOutWindow == index
                && logic.state(index) != AnnunciatorLogic.State.NORMAL;
    }

    /** Whether the board's lamps are all lit for the test. */
    public boolean lampTest() {
        return board().map(board -> board.lampTest > 0).orElse(false);
    }

    /** Whether the board's horn has been silenced while alarms still wait. */
    public boolean silenced() {
        return board().map(board -> board.silenced).orElse(false);
    }

    public Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
    }

    private void setTheme(MonitorTheme next) {
        if (theme != next) {
            theme = next;
            setChanged();
            notifyUpdate();
        }
    }

    /** Syncs this panel (the origin's board state goes with it). */
    private void sync() {
        setChanged();
        notifyUpdate();
    }

    // ------------------------------------------------------------------ setup

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
                    window.severity = window.metric.defaultSeverity();
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

    /**
     * "Metric", "Setting", "Label", "Severity" and "Unlink" for window {@code part}, "Nameplate" for this
     * panel and "Theme" for the whole board; values are sanitised. A "Preset" sets up every window
     * instead, from {@link AnnunciatorPreset} with the thresholds of the plant config; its "Labels" list
     * holds the client's translations of the window labels.
     */
    @Override
    public boolean configure(int part, CompoundTag config) {
        if (part < 0 || part >= WINDOWS) {
            return false;
        }
        if (config.contains("Preset", Tag.TAG_STRING)) {
            AnnunciatorPreset preset = AnnunciatorPreset.byName(config.getString("Preset"));
            if (preset == null) {
                return false;
            }
            applyPreset(preset, config.getList("Labels", Tag.TAG_STRING));
            return true;
        }
        if (config.contains("Theme", Tag.TAG_STRING)) {
            MonitorTheme chosen = MonitorTheme.byName(config.getString("Theme"));
            AnnunciatorPanelBlockEntity board = board().orElse(this);
            board.theme = chosen;
            board.forEachPanel(panel -> panel.setTheme(chosen));
        }
        if (config.contains("Nameplate", Tag.TAG_STRING)) {
            nameplate = ControlModule.sanitizeLabel(config.getString("Nameplate"));
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
        if (config.contains("Severity", Tag.TAG_STRING)) {
            window.severity = AnnunciatorSeverity.byName(config.getString("Severity"), window.severity);
        }
        if (config.getBoolean("Unlink")) {
            window.target = null;
            window.kind = TargetKind.NONE;
        }
        check();
        sync();
        return true;
    }

    /**
     * Sets every window's metric, threshold, severity and label from {@code preset}; links stay as they
     * are. {@code labels} may hold up to one translated label per window: blank or missing ones take the
     * preset's own.
     */
    public void applyPreset(AnnunciatorPreset preset, ListTag labels) {
        List<AnnunciatorPreset.Window> setup = preset.windows(plantConfig());
        for (int i = 0; i < WINDOWS && i < setup.size(); i++) {
            AnnunciatorPreset.Window entry = setup.get(i);
            Window window = windows[i];
            window.metric = metricByName(entry.metric(), window.metric);
            window.setting = Mth.clamp(entry.setting(), 0, 100);
            window.severity = entry.severity();
            String label = i < labels.size() ? ControlModule.sanitizeLabel(labels.getString(i)) : "";
            window.label = label.isEmpty() ? ControlModule.sanitizeLabel(entry.label()) : label;
        }
        check();
        sync();
    }

    /** The plant settings preset thresholds follow, from the config in force. */
    public static AnnunciatorPreset.Plant plantConfig() {
        return new AnnunciatorPreset.Plant(PwrConfig.pressureSetpoint, PwrConfig.reliefOpenPressure, PwrConfig.naturalCirculation,
                PwrConfig.coreTemperatureRise);
    }

    private static AlarmMetric metricByName(String name, AlarmMetric fallback) {
        for (AlarmMetric metric : AlarmMetric.values()) {
            if (metric.name().equals(name)) {
                return metric;
            }
        }
        return fallback;
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

    /** A click on a window links (or unlinks) that window; a click anywhere else on the panel, all six. */
    @Override
    public void setTarget(@Nullable GlobalPos target, Vec3 hit) {
        AnnunciatorBoardLayout.Hit at = hitAt(AnnunciatorPanelBlock.pixel(getBlockState(), getBlockPos(), hit));
        linkWindow(at.kind() == AnnunciatorBoardLayout.HitKind.WINDOW ? at.index() : -1, target);
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
        tag.putString("Nameplate", nameplate);
        tag.putString("Theme", theme.name());
        byte[] powered = new byte[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            powered[i] = (byte) ((inputs[i] ? 1 : 0) | (boardInputs[i] ? 2 : 0));
        }
        tag.putByteArray("Inputs", powered);
        tag.putBoolean("Output", outputOn);
        tag.putInt("Comparator", comparator);
        if (origin != null) {
            tag.putLong("Origin", origin.asLong());
        }
        tag.putInt("Width", width);
        tag.putInt("Height", height);
        if (firstOutPanel != null) {
            tag.putLong("BoardFirstOut", firstOutPanel.asLong());
            tag.putInt("BoardFirstOutWindow", firstOutWindow);
        }
        tag.putBoolean("Silenced", silenced);
        tag.putInt("LampTest", lampTest);
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
        nameplate = ControlModule.sanitizeLabel(tag.getString("Nameplate"));
        theme = MonitorTheme.byName(tag.getString("Theme"));
        byte[] powered = tag.getByteArray("Inputs");
        for (int i = 0; i < inputs.length; i++) {
            byte value = i < powered.length ? powered[i] : 0;
            inputs[i] = (value & 1) != 0;
            boardInputs[i] = (value & 2) != 0;
        }
        if (!tag.contains("Inputs")) {
            // Panels from before boards kept only ACK and RESET.
            inputs[Button.ACK.ordinal()] = boardInputs[Button.ACK.ordinal()] = tag.getBoolean("AckPowered");
            inputs[Button.RESET.ordinal()] = boardInputs[Button.RESET.ordinal()] = tag.getBoolean("ResetPowered");
        }
        outputOn = tag.getBoolean("Output");
        comparator = tag.contains("Comparator") ? Mth.clamp(tag.getInt("Comparator"), 0, 15)
                : Math.min(15, logic.active() * 2 + (logic.unacknowledged() ? 1 : 0));
        origin = tag.contains("Origin") ? BlockPos.of(tag.getLong("Origin")) : null;
        width = Mth.clamp(tag.contains("Width") ? tag.getInt("Width") : 1, 1, MAX_WIDTH);
        height = Mth.clamp(tag.contains("Height") ? tag.getInt("Height") : 1, 1, MAX_HEIGHT);
        if (tag.contains("BoardFirstOut")) {
            firstOutPanel = BlockPos.of(tag.getLong("BoardFirstOut"));
            firstOutWindow = Mth.clamp(tag.getInt("BoardFirstOutWindow"), 0, WINDOWS - 1);
        } else if (tag.contains("FirstOut") && logic.firstOut() >= 0 && !clientPacket) {
            // A panel from before boards: its own first-out is its board's.
            firstOutPanel = getBlockPos();
            firstOutWindow = logic.firstOut();
        } else {
            firstOutPanel = null;
            firstOutWindow = -1;
        }
        silenced = tag.getBoolean("Silenced");
        lampTest = Math.max(0, tag.getInt("LampTest"));
        if (!clientPacket) {
            clusterDirty = true;
        }
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        clusterDirty = true;
    }
}
