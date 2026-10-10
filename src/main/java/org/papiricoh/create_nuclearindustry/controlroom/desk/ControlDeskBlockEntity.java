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
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoomDevice;
import org.papiricoh.create_nuclearindustry.controlroom.LinkedTarget;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * One block of a control console. Desks touching side by side or round a corner join into a
 * console; its first block (lowest x, then z) holds the console's link and samples the target,
 * once per tick at most, for every module on every desk. Each desk keeps up to four modules,
 * works out what they show twice a second and puts their redstone out below or on a link frequency.
 */
public class ControlDeskBlockEntity extends SmartBlockEntity implements ControlRoomDevice {
    public static final int SLOTS = 4;
    public static final int MAX_CONSOLE = 64;
    private static final int EVALUATE_INTERVAL = 10;
    private static final int CLUSTER_CHECK_INTERVAL = 40;

    private final ControlModule[] modules = new ControlModule[SLOTS];
    private final ModuleLink[] links = new ModuleLink[SLOTS];
    /** This desk's copy of the console link; the origin's copy is the one in force. */
    @Nullable
    private GlobalPos target;
    @Nullable
    private BlockPos origin;
    private int consoleSize = 1;
    private boolean clusterDirty = true;
    private int clusterCheckCounter;
    private int evaluateCounter;
    private int belowSignal;

    // Origin only: the sample shared by the console this tick.
    private MonitorReading sample = MonitorReading.NONE;
    private long sampledAt = Long.MIN_VALUE;

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
        if (clusterDirty || ++clusterCheckCounter >= CLUSTER_CHECK_INTERVAL) {
            clusterCheckCounter = 0;
            updateCluster();
        }
        boolean pulseEnded = false;
        for (ControlModule module : modules) {
            if (module != null && module.tickPulse()) {
                pulseEnded = true;
            }
        }
        if (pulseEnded || ++evaluateCounter >= EVALUATE_INTERVAL) {
            evaluateCounter = 0;
            evaluate();
        }
    }

    public Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
    }

    public boolean hasSlots() {
        BlockState state = getBlockState();
        return !state.hasProperty(ControlDeskBlock.SHAPE) || state.getValue(ControlDeskBlock.SHAPE).hasSlots();
    }

    // ------------------------------------------------------------------ modules

    @Nullable
    public ControlModule module(int slot) {
        return slot >= 0 && slot < SLOTS ? modules[slot] : null;
    }

    /** Fits a new module into an empty slot; false when it is taken or the desk has no slots. */
    public boolean insert(int slot, ModuleType type) {
        if (slot < 0 || slot >= SLOTS || modules[slot] != null || !hasSlots()) {
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

    private void updateCluster() {
        clusterDirty = false;
        Set<BlockPos> members = connected();
        BlockPos first = members.stream()
                .min(Comparator.<BlockPos>comparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ))
                .orElse(getBlockPos());
        boolean changed = consoleSize != members.size() || !first.equals(origin == null ? getBlockPos() : origin);
        consoleSize = members.size();
        origin = first.equals(getBlockPos()) ? null : first;
        if (isOrigin() && target == null) {
            // A console grown from linked desks keeps one of their links.
            for (BlockPos member : members) {
                if (level.getBlockEntity(member) instanceof ControlDeskBlockEntity desk && desk.target != null) {
                    target = desk.target;
                    break;
                }
            }
        }
        if (!isOrigin()) {
            origin().ifPresent(console -> target = console.target);
        }
        if (changed) {
            notifyUpdate();
        }
    }

    /** Desks reachable side by side on this level, up to {@link #MAX_CONSOLE}. */
    private Set<BlockPos> connected() {
        Set<BlockPos> found = new HashSet<>();
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        found.add(getBlockPos());
        open.add(getBlockPos());
        while (!open.isEmpty() && found.size() < MAX_CONSOLE) {
            BlockPos current = open.poll();
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos next = current.relative(direction);
                if (!found.contains(next) && level.isLoaded(next)
                        && level.getBlockState(next).getBlock() == getBlockState().getBlock()) {
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
            console.sample = MonitorReading.sample(console.consoleTarget());
            console.sampledAt = now;
        }
        return console.sample;
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
        for (int slot = 0; slot < SLOTS; slot++) {
            ControlModule module = modules[slot];
            if (module == null) {
                continue;
            }
            changed |= module.evaluate(resolved, reading);
            if (module.output() == OutputMode.BELOW) {
                below = Math.max(below, module.signal());
            }
            ModuleLink link = links[slot];
            if (link != null && link.setStrength(module.signal())) {
                Create.REDSTONE_LINK_NETWORK_HANDLER.updateNetworkOf(level, link);
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

    /** Keeps one network entry per module set to LINK with a frequency, matching its setup. */
    private void refreshLinks() {
        if (level == null || level.isClientSide) {
            return;
        }
        for (int slot = 0; slot < SLOTS; slot++) {
            ControlModule module = modules[slot];
            boolean wanted = module != null && module.output() == OutputMode.LINK && module.hasFrequency();
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
        }
    }

    @Nullable
    public ModuleLink link(int slot) {
        return slot >= 0 && slot < SLOTS ? links[slot] : null;
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
