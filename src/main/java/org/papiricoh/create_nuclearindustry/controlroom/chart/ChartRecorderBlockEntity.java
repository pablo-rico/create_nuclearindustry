package org.papiricoh.create_nuclearindustry.controlroom.chart;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.PerformanceConfig;
import org.papiricoh.create_nuclearindustry.controlroom.ConfigurableDevice;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Config;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoomDevice;
import org.papiricoh.create_nuclearindustry.controlroom.LinkedTarget;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.SharedReadings;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.controlroom.desk.GaugeReading;
import org.papiricoh.create_nuclearindustry.multiblock.SyncThrottle;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;

import java.util.List;

/**
 * A paper chart recorder: up to three pens draw readings of the linked machine on a moving strip of
 * paper. Each sample uses up a little paper; with none left the chart stops. The trace keeps the
 * last {@link ChartBuffer#CAPACITY} samples and can be torn off as a Chart Roll.
 *
 * <p>Clients get the visible end of the trace with the chunk; after that an update carries only the
 * samples drawn since the last one, at most one update every {@link PerformanceConfig#screenSyncTicks}
 * ticks, and the client appends them to its own copy.
 */
public class ChartRecorderBlockEntity extends SmartBlockEntity implements ControlRoomDevice, ConfigurableDevice {
    public static final int MAX_PAPER = 64;
    /** Paper speeds: ticks per sample. */
    public static final int[] SPEEDS = {1, 5, 20};
    /** Samples synced to clients: what the paper window shows. */
    public static final int VISIBLE = 60;
    /** Ink of each pen: red, blue and green, as on real strip chart recorders. */
    public static final int[] PEN_COLORS = {0xFFD23C32, 0xFF2F5FD0, 0xFF2E9E46};

    @Nullable
    private GlobalPos target;
    private TargetKind kind = TargetKind.NONE;
    private final GaugeReading[] pens = new GaugeReading[ChartBuffer.MAX_PENS];
    private int speed = 1;
    private int paper;
    /** Samples drawn on the current sheet. */
    private int sheetUsed;
    private int counter;
    /** Samples drawn since the chart was placed, so the paper's grid scrolls with the trace. */
    private long drawn;
    private final ChartBuffer chart = new ChartBuffer();
    private final PaperHandler paperHandler = new PaperHandler();
    /** Samples drawn that no update has carried yet. */
    private boolean syncDue;
    /** The next update carries the whole visible trace: the chart was cleared or replaced. */
    private boolean fullSyncPending = true;
    private long lastSyncTime = Long.MIN_VALUE / 2;
    /** {@link #drawn} when the last update was broadcast ({@link #sendData}). */
    private long broadcastDrawn;
    /**
     * {@link #drawn} at the broadcast before that: every client watching has at least these samples,
     * so update packets carry the samples after it. Only {@link #sendData} moves it: a packet NeoForge
     * makes for a single player must not make the other clients skip samples.
     */
    private long deltaFromDrawn;
    /** The packets of the current broadcast carry the whole visible trace. */
    private boolean broadcastFull = true;
    /** Game time of the last broadcast. */
    private long broadcastTime = Long.MIN_VALUE;
    /** An update packet was made since the last broadcast: the broadcast's packet may have gone out. */
    private boolean packetSinceBroadcast;
    /** Set while an update packet is written, so {@link #write} sends only the new samples. */
    private boolean writingDelta;
    private int clientSyncs;

    public ChartRecorderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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
        if (++counter >= ticksPerSample()) {
            counter = 0;
            sample();
        }
        if (syncDue && level.getGameTime() - lastSyncTime >= Math.max(1, PerformanceConfig.screenSyncTicks)) {
            notifyUpdate();
        }
    }

    /** Draws one sample, if there is paper. Returns whether it drew. */
    public boolean sample() {
        if (level == null || level.isClientSide || paper <= 0) {
            return false;
        }
        MonitorReading reading = SharedReadings.sample(level, getBlockPos(), target);
        float[] values = new float[ChartBuffer.MAX_PENS];
        for (int pen = 0; pen < values.length; pen++) {
            values[pen] = pens[pen] == null ? 0 : pens[pen].fraction(reading);
        }
        chart.push(values);
        drawn++;
        if (++sheetUsed >= Math.max(1, ControlRoom2Config.chartSamplesPerPaper)) {
            sheetUsed = 0;
            setPaper(paper - 1);
        }
        // Sent with the next update, together with the samples drawn until then.
        syncDue = true;
        setChanged();
        return true;
    }

    // ------------------------------------------------------------------ paper

    public int paper() {
        return paper;
    }

    public boolean running() {
        return paper > 0;
    }

    /** Takes paper from {@code stack} up to the limit; returns how many sheets went in. */
    public int insertPaper(ItemStack stack, boolean simulate) {
        if (!stack.is(Items.PAPER)) {
            return 0;
        }
        int taken = Math.min(stack.getCount(), MAX_PAPER - paper);
        if (taken > 0 && !simulate) {
            setPaper(paper + taken);
        }
        return Math.max(0, taken);
    }

    private void setPaper(int value) {
        int before = paper;
        paper = Mth.clamp(value, 0, MAX_PAPER);
        if (before != paper) {
            setChanged();
            notifyUpdate();
            if (level != null) {
                level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
            }
        }
    }

    /** 0-15, how full the paper tray is. */
    public int comparatorSignal() {
        return paper == 0 ? 0 : Math.max(1, Math.round(paper * 15.0f / MAX_PAPER));
    }

    /** Hoppers and funnels may put paper in; nothing comes out. */
    public IItemHandler paperHandler(@Nullable Direction side) {
        return paperHandler;
    }

    /** Paper left in the tray, as items, for when the recorder is broken. */
    public ItemStack paperDrop() {
        return paper > 0 ? new ItemStack(Items.PAPER, paper) : ItemStack.EMPTY;
    }

    /** Tears the chart off as a Chart Roll and starts a fresh one; empty when nothing was drawn yet. */
    public ItemStack tearOff() {
        if (chart.isEmpty() || level == null) {
            return ItemStack.EMPTY;
        }
        ItemStack roll = new ItemStack(ControlRoom2Registry.CHART_ROLL.get());
        roll.set(ControlRoom2Registry.CHART_DATA.get(), ChartData.of(kind, pens, chart, ticksPerSample(), level.getGameTime()));
        chart.clear();
        fullSyncPending = true;
        notifyUpdate();
        return roll;
    }

    // ------------------------------------------------------------------ setup

    public ChartBuffer chart() {
        return chart;
    }

    public long drawn() {
        return drawn;
    }

    @Nullable
    public GaugeReading pen(int index) {
        return index >= 0 && index < pens.length ? pens[index] : null;
    }

    public TargetKind kind() {
        return kind;
    }

    public int speed() {
        return speed;
    }

    public int ticksPerSample() {
        return SPEEDS[Mth.clamp(speed, 0, SPEEDS.length - 1)];
    }

    public void setPen(int index, @Nullable GaugeReading reading) {
        if (index >= 0 && index < pens.length && pens[index] != reading) {
            pens[index] = reading;
            notifyUpdate();
        }
    }

    public void setSpeed(int value) {
        int clamped = Mth.clamp(value, 0, SPEEDS.length - 1);
        if (clamped != speed) {
            speed = clamped;
            counter = 0;
            notifyUpdate();
        }
    }

    /** Pens: "Pen0".."Pen2" hold a {@link GaugeReading} name or "" for off; "Speed" is 0-2. */
    @Override
    public boolean configure(int part, CompoundTag config) {
        for (int pen = 0; pen < pens.length; pen++) {
            String key = "Pen" + pen;
            if (config.contains(key, Tag.TAG_STRING)) {
                setPen(pen, GaugeReading.parse(config.getString(key)));
            }
        }
        if (config.contains("Speed", Tag.TAG_ANY_NUMERIC)) {
            setSpeed(config.getInt("Speed"));
        }
        return true;
    }

    @Override
    @Nullable
    public GlobalPos getTarget() {
        return target;
    }

    /**
     * A new kind of target gets the {@link GaugeReading#defaultPens} that suit it; the same kind keeps
     * the pens, unless they are still the defaults of the other sort of reactor (one boiling directly,
     * one with a pressurized loop), which are swapped for the right ones.
     */
    @Override
    public void setTarget(@Nullable GlobalPos newTarget) {
        target = newTarget;
        boolean reachable = newTarget != null && level != null && level.dimension().equals(newTarget.dimension())
                && level.isLoaded(newTarget.pos());
        TargetKind newKind = reachable ? LinkedTarget.kindAt(level, newTarget.pos()) : TargetKind.NONE;
        if (newKind != TargetKind.NONE && (newKind != kind || showsDefaults(kind))) {
            boolean pressurized = level.getBlockEntity(newTarget.pos()) instanceof ReactorBlockEntity reactor && reactor.hasPressurizer();
            List<GaugeReading> defaults = GaugeReading.defaultPens(newKind, pressurized);
            for (int pen = 0; pen < pens.length; pen++) {
                pens[pen] = pen < defaults.size() ? defaults.get(pen) : null;
            }
            if (newKind != kind) {
                chart.clear();
                fullSyncPending = true;
            }
        }
        kind = newKind;
        notifyUpdate();
    }

    /** Whether the pens are one of the default sets for {@code kind}, as the recorder chose them. */
    private boolean showsDefaults(TargetKind kind) {
        return sameAs(GaugeReading.defaultPens(kind, false)) || sameAs(GaugeReading.defaultPens(kind, true));
    }

    private boolean sameAs(List<GaugeReading> set) {
        for (int pen = 0; pen < pens.length; pen++) {
            if (pens[pen] != (pen < set.size() ? set.get(pen) : null)) {
                return false;
            }
        }
        return true;
    }

    public Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        ControlRoomDevice.writeTarget(tag, target);
        tag.putString("Kind", kind.name());
        ListTag penList = new ListTag();
        for (GaugeReading pen : pens) {
            penList.add(StringTag.valueOf(pen == null ? "" : pen.name()));
        }
        tag.put("Pens", penList);
        tag.putInt("Speed", speed);
        tag.putInt("Paper", paper);
        tag.putInt("SheetUsed", sheetUsed);
        tag.putLong("Drawn", drawn);
        if (writingDelta) {
            // Only the samples drawn since the last packet; "Drawn" tells the client which it already has.
            int fresh = (int) Math.min(VISIBLE, Math.max(0, drawn - deltaFromDrawn));
            for (int pen = 0; pen < ChartBuffer.MAX_PENS; pen++) {
                tag.putByteArray("Append" + pen, chart.newest(pen, fresh));
            }
            return;
        }
        // Clients only draw the visible end of the trace.
        int keep = clientPacket ? Math.min(VISIBLE, chart.size()) : chart.size();
        for (int pen = 0; pen < ChartBuffer.MAX_PENS; pen++) {
            tag.putByteArray("Trace" + pen, chart.newest(pen, keep));
        }
    }

    /**
     * Every update after the chunk's: the samples drawn since the broadcast before the last one, unless
     * the chart was cleared since. Making the packet changes nothing, so a packet made for a single
     * player repeats the same samples; a client drops those it has by {@code Drawn}.
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
     * broadcast reaches every client. Broadcasts that go out in one packet count as one
     * ({@link SyncThrottle#startsNewBroadcast}).
     */
    @Override
    public void sendData() {
        if (level != null && !level.isClientSide) {
            clientSyncs++;
            syncDue = false;
            long now = level.getGameTime();
            lastSyncTime = now;
            if (SyncThrottle.startsNewBroadcast(packetSinceBroadcast, broadcastTime, now)) {
                deltaFromDrawn = broadcastDrawn;
                broadcastFull = false;
            }
            broadcastFull |= fullSyncPending;
            fullSyncPending = false;
            broadcastDrawn = drawn;
            broadcastTime = now;
            packetSinceBroadcast = false;
        }
        super.sendData();
    }

    /** Client updates this recorder asked for, for tests. */
    public int clientSyncCount() {
        return clientSyncs;
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        target = ControlRoomDevice.readTarget(tag);
        kind = TargetKind.byName(tag.getString("Kind"));
        ListTag penList = tag.getList("Pens", Tag.TAG_STRING);
        for (int pen = 0; pen < pens.length; pen++) {
            pens[pen] = pen < penList.size() ? GaugeReading.parse(penList.getString(pen)) : null;
        }
        speed = Mth.clamp(tag.getInt("Speed"), 0, SPEEDS.length - 1);
        paper = Mth.clamp(tag.getInt("Paper"), 0, MAX_PAPER);
        sheetUsed = Math.max(0, tag.getInt("SheetUsed"));
        long before = drawn;
        drawn = Math.max(0, tag.getLong("Drawn"));
        if (clientPacket && tag.contains("Append0")) {
            appendNew(tag, before);
            return;
        }
        chart.load(tag.getByteArray("Trace0"), tag.getByteArray("Trace1"), tag.getByteArray("Trace2"));
    }

    /** Appends the samples of an update this copy does not have yet: those drawn after {@code had}. */
    private void appendNew(CompoundTag tag, long had) {
        byte[][] series = new byte[ChartBuffer.MAX_PENS][];
        int sent = Integer.MAX_VALUE;
        for (int pen = 0; pen < ChartBuffer.MAX_PENS; pen++) {
            series[pen] = tag.getByteArray("Append" + pen);
            sent = Math.min(sent, series[pen].length);
        }
        int fresh = (int) Math.max(0, Math.min(sent, drawn - had));
        for (int pen = 0; pen < ChartBuffer.MAX_PENS; pen++) {
            series[pen] = java.util.Arrays.copyOfRange(series[pen], series[pen].length - fresh, series[pen].length);
        }
        chart.append(series);
    }

    /** One slot that only takes paper, up to the tray's size. */
    private final class PaperHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return paper > 0 ? new ItemStack(Items.PAPER, paper) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            int taken = insertPaper(stack, simulate);
            return taken == 0 ? stack : stack.copyWithCount(stack.getCount() - taken);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return MAX_PAPER;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(Items.PAPER);
        }
    }
}
