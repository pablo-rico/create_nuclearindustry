package org.papiricoh.create_nuclearindustry.controlroom.desk;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Config;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoomPanels;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoomDevice;
import org.papiricoh.create_nuclearindustry.controlroom.LinkedTarget;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.SharedReadings;
import org.papiricoh.create_nuclearindustry.controlroom.link.AdvancedLinkNetwork;
import org.papiricoh.create_nuclearindustry.controlroom.link.LinkCode;
import org.papiricoh.create_nuclearindustry.panel.PanelCard;
import org.papiricoh.create_nuclearindustry.panel.PanelSource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * One block of a control console: a desk, table or wall panel (see {@link ModuleSurfaceBlock}).
 * Module surfaces touching side by side or round a corner, and wall panels stacked on one wall, join
 * into a console; its first block (lowest x, then z, then y) holds the console's link and samples the target,
 * once per tick at most, for every module on every desk. Each desk keeps up to four modules,
 * works out what they show twice a second and puts their redstone out below or on a link frequency.
 * Controller modules also step their loop every {@link ControlRoom2Config#pidInterval} ticks.
 * Pressurizer levers and the pressure controller repeat their orders every time the modules are
 * worked out, so the pressurizer lets go of them shortly after the desk goes away.
 *
 * <p>Which console a desk belongs to is worked out by one desk for the whole console, when a block
 * next to one of its desks changes (and by the console's first desk, as a safety net, every
 * {@link #CLUSTER_FALLBACK_INTERVAL} ticks), not by every desk every two seconds.
 */
public class ControlDeskBlockEntity extends SmartBlockEntity implements ControlRoomDevice, PanelSource {
    public static final int SLOTS = 4;
    public static final int MAX_CONSOLE = 64;
    private static final int EVALUATE_INTERVAL = 10;
    /** The console's first desk looks at the console again this often, in case a change went unnoticed. */
    public static final int CLUSTER_FALLBACK_INTERVAL = 600;
    /** How often the console's first desk resends its sample while a module setup screen is open. */
    public static final int VIEWER_SYNC_INTERVAL = 20;

    private final ControlModule[] modules = new ControlModule[SLOTS];
    private final ModuleLink[] links = new ModuleLink[SLOTS];
    private final ModuleCodeLink[] codeLinks = new ModuleCodeLink[SLOTS];
    /** This desk's copy of the console link; the origin's copy is the one in force. */
    @Nullable
    private GlobalPos target;
    @Nullable
    private BlockPos origin;
    private int consoleSize = 1;
    private boolean clusterDirty = true;
    private int clusterCheckCounter;
    private int evaluateCounter;
    private int controlCounter;
    private int belowSignal;
    private int clusterScans;

    // Origin only: the sample shared by the console this tick.
    private MonitorReading sample = MonitorReading.NONE;
    private long sampledAt = Long.MIN_VALUE;
    @Nullable
    private DeskShape lastShape;
    /**
     * Origin only, not saved: module setup screens open on this console. While there is one, the desk's
     * sync carries the console's sample every {@link #VIEWER_SYNC_INTERVAL} ticks for its live values.
     */
    private int viewers;
    private int viewerSyncCounter;
    /** Client only: the console summary last synced for an open setup screen, or null. */
    @Nullable
    private ConsoleSummary syncedSummary;
    private long syncedAt = Long.MIN_VALUE;

    public ControlDeskBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void initialize() {
        super.initialize();
        refreshLinks();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        DeskShape shape = getBlockState().hasProperty(ControlDeskBlock.SHAPE) ? getBlockState().getValue(ControlDeskBlock.SHAPE) : null;
        if (shape != lastShape) {
            // Slots opened or closed: links and outputs follow.
            lastShape = shape;
            afterChange();
        }
        if (clusterDirty || isOrigin() && ++clusterCheckCounter >= CLUSTER_FALLBACK_INTERVAL) {
            updateCluster();
        }
        boolean pulseEnded = false;
        for (ControlModule module : modules) {
            if (module != null && module.tickPulse()) {
                pulseEnded = true;
            }
            if (module != null && module.tickCover()) {
                // The cover fell shut: the clients draw it down.
                pulseEnded = true;
            }
        }
        if (++controlCounter >= Math.max(1, ControlRoom2Config.pidInterval)) {
            controlCounter = 0;
            if (control()) {
                pulseEnded = true;
            }
        }
        if (pulseEnded || ++evaluateCounter >= EVALUATE_INTERVAL) {
            evaluateCounter = 0;
            evaluate();
        }
        if (viewers > 0 && isOrigin() && ++viewerSyncCounter >= VIEWER_SYNC_INTERVAL) {
            viewerSyncCounter = 0;
            sendData();
        }
    }

    /** Steps every controller module on this desk; true when one changed its demand. */
    private boolean control() {
        LinkedTarget.Resolved resolved = null;
        MonitorReading reading = null;
        boolean changed = false;
        for (int slot = 0; slot < SLOTS; slot++) {
            ControlModule module = modules[slot];
            if (module == null || !slotOpen(slot) || !module.isController()) {
                continue;
            }
            if (resolved == null) {
                resolved = consoleTarget();
                reading = consoleSample();
            }
            changed |= module.control(resolved, reading, ControlRoom2Config.pidInterval / 20.0, ControlRoom2Config.tuningFor(module.function()));
        }
        if (changed) {
            notifyUpdate();
        }
        return changed;
    }

    public Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
    }

    /**
     * Whether the desk's shape has room for this slot. A desk that turns into a corner keeps the
     * modules of the slots it loses, idle and out of sight, until it is straight again or broken.
     */
    public boolean slotOpen(int slot) {
        BlockState state = getBlockState();
        return !state.hasProperty(ControlDeskBlock.SHAPE) ? slot >= 0 && slot < SLOTS
                : state.getValue(ControlDeskBlock.SHAPE).hasSlot(slot);
    }

    // ------------------------------------------------------------------ modules

    @Nullable
    public ControlModule module(int slot) {
        return slot >= 0 && slot < SLOTS ? modules[slot] : null;
    }

    /** Fits a new module into an empty slot; false when it is taken or the desk's shape has no such slot. */
    public boolean insert(int slot, ModuleType type) {
        if (slot < 0 || slot >= SLOTS || modules[slot] != null || !slotOpen(slot)) {
            return false;
        }
        modules[slot] = new ControlModule(type);
        afterChange();
        return true;
    }

    /** Takes a module out, returning its type, or null when the slot is empty. */
    @Nullable
    public ModuleType remove(int slot) {
        ControlModule module = module(slot);
        if (module == null) {
            return null;
        }
        modules[slot] = null;
        afterChange();
        return module.type();
    }

    /** Applies a setup from the configuration screen. The module keeps its type and live state. */
    public boolean configure(int slot, CompoundTag config) {
        ControlModule module = module(slot);
        if (module == null || level == null) {
            return false;
        }
        module.applyConfig(config, level.registryAccess());
        afterChange();
        return true;
    }

    /** A click on a module with an empty hand. */
    public boolean press(int slot, boolean back) {
        ControlModule module = module(slot);
        if (module == null || level == null) {
            return false;
        }
        boolean acted = module.press(consoleTarget(), back);
        if (acted) {
            evaluate();
        }
        return acted;
    }

    /** What the last click on a module did, for the player who clicked (a refused tokamak start, say), or null. */
    @Nullable
    public net.minecraft.network.chat.Component takeFeedback(int slot) {
        ControlModule module = module(slot);
        return module == null ? null : module.takeFeedback();
    }

    /**
     * Whether this block's four modules make one screen, set up and drawn by slot {@link BlockScreen#HEAD}
     * (see {@link BlockScreen}).
     */
    public boolean mergedScreen() {
        ModuleSurface surface = ModuleSurface.of(getBlockState());
        ModuleType[] types = new ModuleType[SLOTS];
        for (int slot = 0; slot < SLOTS; slot++) {
            ControlModule module = modules[slot];
            types[slot] = module != null && surface != null && surface.hasSlot(getBlockState(), slot) ? module.type() : null;
        }
        return BlockScreen.merged(types);
    }

    private void afterChange() {
        refreshLinks();
        evaluate();
        notifyUpdate();
    }

    // ------------------------------------------------------------------ console

    public boolean isOrigin() {
        return origin == null || origin.equals(getBlockPos());
    }

    public int consoleSize() {
        return consoleSize;
    }

    public Optional<ControlDeskBlockEntity> origin() {
        if (isOrigin()) {
            return Optional.of(this);
        }
        return level != null && level.getBlockEntity(origin) instanceof ControlDeskBlockEntity desk
                ? Optional.of(desk)
                : Optional.empty();
    }

    public void requestClusterUpdate() {
        clusterDirty = true;
    }

    /** Works out the console this desk is part of and hands the answer to every desk of it. */
    private void updateCluster() {
        clusterScans++;
        Set<BlockPos> members = connected();
        BlockPos first = members.stream()
                .min(Comparator.<BlockPos>comparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ)
                        .thenComparingInt(BlockPos::getY))
                .orElse(getBlockPos());
        List<ControlDeskBlockEntity> desks = new ArrayList<>(members.size());
        ControlDeskBlockEntity console = null;
        for (BlockPos member : members) {
            ControlDeskBlockEntity desk = member.equals(getBlockPos()) ? this
                    : level.getBlockEntity(member) instanceof ControlDeskBlockEntity other ? other : null;
            if (desk != null) {
                desks.add(desk);
                if (member.equals(first)) {
                    console = desk;
                }
            }
        }
        if (console != null && console.target == null) {
            // A console grown from linked desks keeps one of their links.
            for (ControlDeskBlockEntity desk : desks) {
                if (desk.target != null) {
                    console.target = desk.target;
                    break;
                }
            }
        }
        for (ControlDeskBlockEntity desk : desks) {
            desk.applyCluster(first, members.size(), console);
        }
    }

    /** Takes the console worked out by a desk of it. */
    private void applyCluster(BlockPos first, int size, @Nullable ControlDeskBlockEntity console) {
        clusterDirty = false;
        clusterCheckCounter = 0;
        boolean changed = consoleSize != size || !first.equals(origin == null ? getBlockPos() : origin);
        consoleSize = size;
        origin = first.equals(getBlockPos()) ? null : first.immutable();
        if (!isOrigin() && console != null) {
            target = console.target;
        }
        if (changed) {
            notifyUpdate();
        }
    }

    /** Searches for its console this desk made, for tests. */
    public int clusterScanCount() {
        return clusterScans;
    }

    /**
     * Module surfaces reachable side by side on this level (and wall panels up and down a wall), up to
     * {@link #MAX_CONSOLE}: see {@link ModuleSurfaceBlock#sameConsole}.
     */
    private Set<BlockPos> connected() {
        Set<BlockPos> found = new HashSet<>();
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        found.add(getBlockPos());
        open.add(getBlockPos());
        while (!open.isEmpty() && found.size() < MAX_CONSOLE) {
            BlockPos current = open.poll();
            BlockState currentState = level.getBlockState(current);
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (!found.contains(next) && level.isLoaded(next)
                        && ModuleSurfaceBlock.sameConsole(currentState, level.getBlockState(next), direction)) {
                    found.add(next);
                    open.add(next);
                }
            }
        }
        return found;
    }

    @Override
    @Nullable
    public GlobalPos getTarget() {
        return origin().map(console -> console.target).orElse(target);
    }

    @Override
    public void setTarget(@Nullable GlobalPos newTarget) {
        Optional<ControlDeskBlockEntity> console = origin();
        if (console.isPresent() && console.get() != this) {
            console.get().setTarget(newTarget);
            return;
        }
        target = newTarget;
        sampledAt = Long.MIN_VALUE;
        if (level != null && !level.isClientSide) {
            for (BlockPos member : connected()) {
                if (level.getBlockEntity(member) instanceof ControlDeskBlockEntity desk) {
                    desk.target = newTarget;
                    desk.evaluate();
                    desk.setChanged();
                }
            }
        }
        notifyUpdate();
    }

    /** What the console is linked to, resolved now. */
    public LinkedTarget.Resolved consoleTarget() {
        ControlDeskBlockEntity console = origin().orElse(this);
        return LinkedTarget.resolve(level, console.getBlockPos(), console.target);
    }

    /** The console's sample of its target, taken at most once per tick. */
    public MonitorReading consoleSample() {
        ControlDeskBlockEntity console = origin().orElse(this);
        long now = level.getGameTime();
        if (console.sampledAt != now) {
            console.sample = SharedReadings.sample(level, console.getBlockPos(), console.target);
            console.sampledAt = now;
        }
        return console.sample;
    }

    /** A module setup screen opened on this console (call on the origin). */
    public void addViewer() {
        viewers++;
        viewerSyncCounter = 0;
    }

    /** A module setup screen on this console closed (call on the desk {@link #addViewer} was called on). */
    public void removeViewer() {
        viewers = Math.max(0, viewers - 1);
    }

    /** Module setup screens open on this console, as counted by its origin. */
    public int viewers() {
        return viewers;
    }

    /**
     * Client side: the console's summary as last synced for an open setup screen, or null when none came
     * at or after game time {@code notBefore} (an older one belongs to a screen opened earlier).
     */
    @Nullable
    public ConsoleSummary syncedSummary(long notBefore) {
        ControlDeskBlockEntity console = origin().orElse(this);
        return console.syncedSummary != null && console.syncedAt >= notBefore ? console.syncedSummary : null;
    }

    // ------------------------------------------------------------------ shown state and redstone

    private void evaluate() {
        if (level == null || level.isClientSide) {
            return;
        }
        LinkedTarget.Resolved resolved = consoleTarget();
        MonitorReading reading = consoleSample();
        boolean changed = false;
        int below = 0;
        boolean merged = mergedScreen();
        long now = level.getGameTime();
        for (int slot = 0; slot < SLOTS; slot++) {
            ControlModule module = modules[slot];
            if (module == null || !slotOpen(slot)) {
                continue;
            }
            module.holdOrders(resolved);
            changed |= module.evaluate(resolved, reading, now, !merged || slot == BlockScreen.HEAD);
            if (module.output() == OutputMode.BELOW) {
                below = Math.max(below, module.signal());
            }
            ModuleLink link = links[slot];
            if (link != null && link.setStrength(module.signal())) {
                Create.REDSTONE_LINK_NETWORK_HANDLER.updateNetworkOf(level, link);
            }
            ModuleCodeLink codeLink = codeLinks[slot];
            if (codeLink != null && codeLink.setStrength(module.signal())) {
                AdvancedLinkNetwork.update(level, codeLink.code());
            }
        }
        if (below != belowSignal) {
            belowSignal = below;
            level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
            level.updateNeighborsAt(getBlockPos().below(), getBlockState().getBlock());
            level.updateNeighborsAt(getBlockPos().relative(facing().getOpposite()), getBlockState().getBlock());
            changed = true;
        }
        if (changed) {
            notifyUpdate();
        }
    }

    /** Redstone put out below and behind the desk: the strongest of its modules set to BELOW. */
    public int belowSignal() {
        return belowSignal;
    }

    /**
     * Keeps one network entry per module set to LINK with a frequency, or to ADVANCED_LINK with a
     * complete code, matching its setup.
     */
    private void refreshLinks() {
        if (level == null || level.isClientSide) {
            return;
        }
        for (int slot = 0; slot < SLOTS; slot++) {
            ControlModule module = modules[slot];
            boolean wanted = module != null && slotOpen(slot) && module.output() == OutputMode.LINK && module.hasFrequency();
            ModuleLink link = links[slot];
            if (link != null && (!wanted || !link.sameKey(module.frequencyFirst(), module.frequencyLast()))) {
                Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, link);
                links[slot] = null;
                link = null;
            }
            if (wanted && link == null) {
                links[slot] = new ModuleLink(this, module.frequencyFirst(), module.frequencyLast(), module.signal());
                Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(level, links[slot]);
            }
            boolean coded = module != null && slotOpen(slot) && module.output() == OutputMode.ADVANCED_LINK
                    && LinkCode.complete(module.linkCode());
            ModuleCodeLink codeLink = codeLinks[slot];
            if (codeLink != null && (!coded || !codeLink.code().equals(module.linkCode()))) {
                AdvancedLinkNetwork.remove(level, codeLink);
                codeLinks[slot] = null;
                codeLink = null;
            }
            if (coded && codeLink == null) {
                codeLinks[slot] = new ModuleCodeLink(this, module.linkCode(), module.signal());
                AdvancedLinkNetwork.add(level, codeLinks[slot]);
            }
        }
    }

    private void removeLinks() {
        if (level == null || level.isClientSide) {
            return;
        }
        for (int slot = 0; slot < SLOTS; slot++) {
            if (links[slot] != null) {
                Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, links[slot]);
                links[slot] = null;
            }
            if (codeLinks[slot] != null) {
                AdvancedLinkNetwork.remove(level, codeLinks[slot]);
                codeLinks[slot] = null;
            }
        }
    }

    @Nullable
    public ModuleLink link(int slot) {
        return slot >= 0 && slot < SLOTS ? links[slot] : null;
    }

    @Override
    public PanelCard panelCard(net.minecraft.world.phys.Vec3 hit, boolean detailed) {
        return ControlRoomPanels.surface(this, hit);
    }

    /** Destroyed or unloaded: leave the Redstone Link network. */
    @Override
    public void invalidate() {
        removeLinks();
        super.invalidate();
    }

    /** Every module, as items, for when the desk is broken. */
    public List<net.minecraft.world.item.ItemStack> drops() {
        List<net.minecraft.world.item.ItemStack> drops = new ArrayList<>();
        for (ControlModule module : modules) {
            if (module != null) {
                drops.add(ControlModuleItem.stackOf(module.type()));
            }
        }
        return drops;
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        ControlRoomDevice.writeTarget(tag, target);
        ListTag list = new ListTag();
        for (int slot = 0; slot < SLOTS; slot++) {
            if (modules[slot] != null) {
                CompoundTag entry = modules[slot].write(registries);
                entry.putInt("Slot", slot);
                list.add(entry);
            }
        }
        tag.put("Modules", list);
        if (origin != null) {
            tag.putLong("Origin", origin.asLong());
        }
        tag.putInt("ConsoleSize", consoleSize);
        tag.putInt("BelowSignal", belowSignal);
        if (clientPacket && viewers > 0 && isOrigin() && level != null && !level.isClientSide) {
            tag.put("ConsoleSample", ConsoleSummary.of(this).write());
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        target = ControlRoomDevice.readTarget(tag);
        java.util.Arrays.fill(modules, null);
        if (tag.contains("Modules", Tag.TAG_LIST)) {
            for (Tag element : tag.getList("Modules", Tag.TAG_COMPOUND)) {
                CompoundTag entry = (CompoundTag) element;
                int slot = entry.getInt("Slot");
                if (slot >= 0 && slot < SLOTS) {
                    modules[slot] = ControlModule.read(entry, registries);
                }
            }
        } else if (tag.contains("Shown")) {
            // A Reactor Control Panel from before desks: its fixed buttons become modules.
            migratePanel();
        }
        origin = tag.contains("Origin") ? BlockPos.of(tag.getLong("Origin")) : null;
        consoleSize = Math.max(1, tag.getInt("ConsoleSize"));
        belowSignal = tag.getInt("BelowSignal");
        if (clientPacket && tag.contains("ConsoleSample", Tag.TAG_COMPOUND)) {
            syncedSummary = ConsoleSummary.read(tag.getCompound("ConsoleSample"));
            syncedAt = level != null ? level.getGameTime() : Long.MIN_VALUE;
        }
        clusterDirty = true;
        if (!clientPacket && level != null && !level.isClientSide) {
            refreshLinks();
        }
    }

    /** The old panel's SCRAM, auto SCRAM, rod and threshold buttons as four modules. */
    private void migratePanel() {
        modules[0] = new ControlModule(ModuleType.PUSH_BUTTON).withFunction(ModuleFunction.SCRAM);
        modules[1] = new ControlModule(ModuleType.LEVER).withFunction(ModuleFunction.AUTO_SCRAM);
        modules[2] = new ControlModule(ModuleType.DIAL).withFunction(ModuleFunction.ROD_TARGET);
        modules[3] = new ControlModule(ModuleType.DIAL).withFunction(ModuleFunction.SCRAM_THRESHOLD);
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        clusterDirty = true;
    }
}
