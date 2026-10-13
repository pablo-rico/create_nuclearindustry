package org.papiricoh.create_nuclearindustry.reactor.visual;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.reactor.block.UraniumRodBlock;
import org.papiricoh.create_nuclearindustry.reactor.multiblock.ReactorStructureValidator;
import org.papiricoh.create_nuclearindustry.reactor.physics.ReactorPhysicsSimulator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Keeps a formed reactor's fuel channels showing what the controller holds (server side). Every
 * {@link ReactorVisualsConfig#rodUpdateInterval} ticks it works out each channel's looks with
 * {@link RodVisuals}; the channels that changed are then rewritten a few per tick
 * ({@link ReactorVisualsConfig#rodUpdatesPerTick}), with {@link Block#UPDATE_CLIENTS} only: no
 * neighbour updates, so the reactor's structure snapshot never sees a change and nothing revalidates.
 * It remembers which channels it lit, to put them out when the reactor unforms or goes away.
 */
public final class RodVisualSync {
    /** Channels waiting to be rewritten, in fill order, with what they should show. */
    private final Map<BlockPos, RodVisuals.Look> pending = new LinkedHashMap<>();
    /** Channels this reactor set to something other than dark. */
    private final Set<BlockPos> lit = new HashSet<>();
    private ReactorStructureValidator.ReactorStructure orderedFor;
    private List<BlockPos> ordered = List.of();
    private int glow;
    private int timer;
    /** Channel blocks rewritten so far, for tests: a glow step that holds must rewrite none. */
    private int writes;

    /**
     * One tick of a formed reactor: refreshes the targets when due and pushes some of the pending
     * channels. The assemblies are only asked for when the targets are refreshed, so the ticks in
     * between build no list of them.
     */
    public void tick(Level level, ReactorStructureValidator.ReactorStructure structure,
                     Supplier<List<ReactorPhysicsSimulator.AssemblyView>> assemblies, double powerFraction, boolean coolant) {
        if (level == null || level.isClientSide) {
            return;
        }
        if (++timer >= ReactorVisualsConfig.rodUpdateInterval) {
            timer = 0;
            plan(level, structure, assemblies.get(), powerFraction, coolant);
        }
        push(level, ReactorVisualsConfig.rodUpdatesPerTick);
    }

    /** Works the targets out again on the next tick, such as after the structure changed. */
    public void refreshSoon() {
        timer = Integer.MAX_VALUE - 1;
    }

    /** The glow step shown now. */
    public int glow() {
        return glow;
    }

    /** Works out every channel's looks and queues those that differ from what the channel shows. */
    void plan(Level level, ReactorStructureValidator.ReactorStructure structure, List<ReactorPhysicsSimulator.AssemblyView> assemblies,
              double powerFraction, boolean coolant) {
        List<BlockPos> rods = orderedRods(structure);
        glow = RodVisuals.glow(glow, powerFraction);
        List<Double> burns = new ArrayList<>(assemblies.size());
        for (ReactorPhysicsSimulator.AssemblyView assembly : assemblies) {
            burns.add(assembly.burnFraction());
        }
        List<RodVisuals.Look> looks = RodVisuals.looks(rods.size(), burns, glow, coolant);
        pending.clear();
        for (int i = 0; i < rods.size(); i++) {
            BlockPos pos = rods.get(i);
            BlockState state = level.getBlockState(pos);
            if (state.is(AllNuclearBlocks.URANIUM_ROD.get()) && !lookOf(state).equals(looks.get(i))) {
                pending.put(pos, looks.get(i));
            }
        }
    }

    /** Rewrites up to {@code budget} pending channels. */
    void push(Level level, int budget) {
        Iterator<Map.Entry<BlockPos, RodVisuals.Look>> it = pending.entrySet().iterator();
        int done = 0;
        while (it.hasNext() && done < budget) {
            Map.Entry<BlockPos, RodVisuals.Look> next = it.next();
            it.remove();
            if (show(level, next.getKey(), next.getValue())) {
                done++;
            }
        }
    }

    /** Works the looks out and shows them on every channel at once, ignoring the interval and the budget. For tests. */
    public void applyNow(Level level, ReactorStructureValidator.ReactorStructure structure, List<ReactorPhysicsSimulator.AssemblyView> assemblies,
                         double powerFraction, boolean coolant) {
        plan(level, structure, assemblies, powerFraction, coolant);
        push(level, Integer.MAX_VALUE);
    }

    /** Channels still waiting to be rewritten. */
    public int pendingCount() {
        return pending.size();
    }

    /** Channel blocks rewritten since this reactor loaded. For tests. */
    public int writes() {
        return writes;
    }

    /**
     * Puts out every channel this reactor lit and every channel of {@code channels}, at once, and
     * forgets them. For a reactor that unformed or was removed.
     */
    public void reset(Level level, Collection<BlockPos> channels) {
        pending.clear();
        glow = 0;
        orderedFor = null;
        ordered = List.of();
        if (level == null || level.isClientSide) {
            lit.clear();
            return;
        }
        Set<BlockPos> all = new HashSet<>(lit);
        all.addAll(channels);
        lit.clear();
        for (BlockPos pos : all) {
            show(level, pos, RodVisuals.Look.DARK);
        }
    }

    /** Puts out the channels this reactor lit that are not among {@code kept}; for a structure that changed. */
    public void forgetOutside(Level level, Collection<BlockPos> kept) {
        if (level == null || level.isClientSide) {
            return;
        }
        Set<BlockPos> keep = new HashSet<>(kept);
        for (BlockPos pos : List.copyOf(lit)) {
            if (!keep.contains(pos)) {
                show(level, pos, RodVisuals.Look.DARK);
            }
        }
        pending.keySet().removeIf(pos -> !keep.contains(pos));
        refreshSoon();
    }

    /** Every fuel channel of a structure: the fuel columns, from the floor to the lid. */
    public static List<BlockPos> rodsOf(ReactorStructureValidator.ReactorStructure structure) {
        List<BlockPos> rods = new ArrayList<>();
        for (BlockPos column : structure.fuelColumns) {
            for (int y = structure.bottomY + 1; y < structure.topY; y++) {
                rods.add(new BlockPos(column.getX(), y, column.getZ()));
            }
        }
        return rods;
    }

    private List<BlockPos> orderedRods(ReactorStructureValidator.ReactorStructure structure) {
        if (orderedFor != structure) {
            orderedFor = structure;
            ordered = RodVisuals.order(rodsOf(structure), BlockPos::getX, BlockPos::getY, BlockPos::getZ);
        }
        return ordered;
    }

    /** Sets a channel's looks if it is still a fuel channel and shows something else; true if it changed. */
    private boolean show(Level level, BlockPos pos, RodVisuals.Look look) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (!state.is(AllNuclearBlocks.URANIUM_ROD.get())) {
            lit.remove(pos);
            return false;
        }
        if (look.equals(RodVisuals.Look.DARK)) {
            lit.remove(pos);
        } else {
            lit.add(pos.immutable());
        }
        if (lookOf(state).equals(look)) {
            return false;
        }
        level.setBlock(pos, withLook(state, look), Block.UPDATE_CLIENTS);
        writes++;
        return true;
    }

    public static RodVisuals.Look lookOf(BlockState state) {
        return new RodVisuals.Look(state.getValue(UraniumRodBlock.FUEL).ordinal(), state.getValue(UraniumRodBlock.GLOW),
                state.getValue(UraniumRodBlock.COOLANT));
    }

    public static BlockState withLook(BlockState state, RodVisuals.Look look) {
        UraniumRodBlock.Fuel[] fuels = UraniumRodBlock.Fuel.values();
        return state.setValue(UraniumRodBlock.FUEL, fuels[Math.max(0, Math.min(fuels.length - 1, look.fuel()))])
                .setValue(UraniumRodBlock.GLOW, Math.max(0, Math.min(RodVisuals.MAX_GLOW, look.glow())))
                .setValue(UraniumRodBlock.COOLANT, look.coolant());
    }
}
