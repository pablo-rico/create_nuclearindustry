package org.papiricoh.create_nuclearindustry.antimatter.conduit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.accelerator.physics.ConduitTransfer;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterConfig;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterHolder;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterHolders;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The conduits joined to each other and the antimatter holders at their ends. Built by walking the
 * conduits from one of them and shared by all its members; any change beside a member makes it
 * {@link #invalidate invalid}, and the next member to tick builds a new one.
 *
 * <p>Once a tick (whichever member ticks first runs it): the network takes
 * {@code antimatter.conduit.fePerBlock} per segment from its members' buffers together, wherever
 * the cable comes in. Fully fed, it moves up to {@code antimatter.conduit.ratePerTick} by
 * {@link ConduitTransfer}: first from holders that only give (target stations) into the others, then
 * from traps into holders that only take, then between traps towards an equal fill. Short of energy
 * it moves nothing; it holds nothing, so it never explodes.
 */
public final class ConduitNetwork {
    private final List<AntimatterConduitBlockEntity> members;
    private final List<BlockPos> holderPositions;
    private final boolean tooLong;
    private boolean valid = true;
    private long lastTick = Long.MIN_VALUE;
    private boolean powered;
    private double movedLastTick;

    private ConduitNetwork(List<AntimatterConduitBlockEntity> members, List<BlockPos> holderPositions, boolean tooLong) {
        this.members = members;
        this.holderPositions = holderPositions;
        this.tooLong = tooLong;
    }

    /** Walks the conduits joined to the one at {@code start} and hands them the new network. */
    public static ConduitNetwork build(Level level, BlockPos start) {
        int max = Math.max(1, AntimatterConfig.conduit().maxSegments());
        List<AntimatterConduitBlockEntity> members = new ArrayList<>();
        Set<BlockPos> holders = new LinkedHashSet<>();
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        boolean tooLong = false;
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (!(level.getBlockEntity(pos) instanceof AntimatterConduitBlockEntity conduit)) {
                continue;
            }
            if (members.size() >= max) {
                tooLong = true;
                break;
            }
            members.add(conduit);
            for (Direction side : Direction.values()) {
                BlockPos next = pos.relative(side);
                if (!level.isLoaded(next) || !seen.add(next)) {
                    continue;
                }
                if (level.getBlockState(next).getBlock() instanceof AntimatterConduitBlock) {
                    queue.add(next);
                } else if (AntimatterHolders.isHolder(level, next)) {
                    holders.add(AntimatterHolders.master(level, next).immutable());
                }
            }
        }
        ConduitNetwork network = new ConduitNetwork(List.copyOf(members), List.copyOf(holders), tooLong);
        members.forEach(member -> member.joinNetwork(network));
        return network;
    }

    /** Makes the networks of the conduits at and around {@code pos} look again. */
    public static void invalidateAround(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof AntimatterConduitBlockEntity conduit) {
            conduit.invalidateNetwork();
        }
        for (Direction side : Direction.values()) {
            if (level.getBlockEntity(pos.relative(side)) instanceof AntimatterConduitBlockEntity conduit) {
                conduit.invalidateNetwork();
            }
        }
    }

    public void invalidate() {
        valid = false;
    }

    public boolean valid() {
        return valid;
    }

    public int size() {
        return members.size();
    }

    public List<BlockPos> holderPositions() {
        return holderPositions;
    }

    /** Every segment got its energy on the last tick. */
    public boolean powered() {
        return powered;
    }

    /** Antimatter moved on the last tick, ng. */
    public double movedLastTick() {
        return movedLastTick;
    }

    /** Runs the network once per game tick; the members call it as they tick. */
    void tick(Level level) {
        long now = level.getGameTime();
        if (lastTick == now) {
            return;
        }
        lastTick = now;
        ConduitTransfer.Params p = AntimatterConfig.conduit();
        powered = !tooLong && drawEnergy(ConduitTransfer.demand(p, members.size()));
        movedLastTick = ConduitTransfer.flows(p, members.size(), powered) ? move(level, p.ratePerTick()) : 0;
        boolean flowing = movedLastTick > 0;
        for (AntimatterConduitBlockEntity member : members) {
            member.setStatus(powered, flowing);
        }
    }

    /** Takes the network's demand from its members' buffers together; nothing unless all of it is there. */
    private boolean drawEnergy(long demand) {
        if (demand <= 0) {
            return true;
        }
        long available = 0;
        for (AntimatterConduitBlockEntity member : members) {
            available += member.buffer().getEnergyStored();
        }
        if (available < demand) {
            return false;
        }
        long left = demand;
        for (AntimatterConduitBlockEntity member : members) {
            if (left <= 0) {
                break;
            }
            left -= member.buffer().take((int) Math.min(Integer.MAX_VALUE, left));
        }
        return left <= 0;
    }

    private double move(Level level, double rate) {
        List<AntimatterHolder> sources = new ArrayList<>();
        List<AntimatterHolder> sinks = new ArrayList<>();
        List<AntimatterHolder> stores = new ArrayList<>();
        Set<AntimatterHolder> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (BlockPos pos : holderPositions) {
            AntimatterHolder holder = level.isLoaded(pos) ? AntimatterHolders.at(level, pos) : null;
            if (holder == null || !seen.add(holder)) {
                continue;
            }
            boolean in = holder.acceptsAntimatter();
            boolean out = holder.givesAntimatter();
            if (in && out) {
                stores.add(holder);
            } else if (out) {
                sources.add(holder);
            } else if (in) {
                sinks.add(holder);
            }
        }
        double moved = 0;
        List<AntimatterHolder> takers = new ArrayList<>(sinks);
        takers.addAll(stores);
        moved += carry(rate - moved, sources, takers);
        moved += carry(rate - moved, stores, sinks);
        moved += balance(rate - moved, stores);
        return moved;
    }

    /** From {@code givers} to {@code takers}, in proportion to what each has and has room for. */
    private static double carry(double rate, List<AntimatterHolder> givers, List<AntimatterHolder> takers) {
        if (rate <= ConduitTransfer.MIN_TRANSFER || givers.isEmpty() || takers.isEmpty()) {
            return 0;
        }
        double[] available = new double[givers.size()];
        for (int i = 0; i < givers.size(); i++) {
            available[i] = givers.get(i).extract(rate, true);
        }
        double[] space = new double[takers.size()];
        for (int i = 0; i < takers.size(); i++) {
            space[i] = takers.get(i).insert(rate, true);
        }
        ConduitTransfer.Plan plan = ConduitTransfer.plan(rate, available, space);
        if (plan.total() <= 0) {
            return 0;
        }
        double given = 0;
        for (int i = 0; i < givers.size(); i++) {
            given += givers.get(i).extract(plan.extract()[i], false);
        }
        double scale = plan.total() > 0 ? given / plan.total() : 0;
        for (int i = 0; i < takers.size(); i++) {
            takers.get(i).insert(plan.insert()[i] * scale, false);
        }
        return given;
    }

    /** Between traps, towards the same fill. */
    private static double balance(double rate, List<AntimatterHolder> stores) {
        if (rate <= ConduitTransfer.MIN_TRANSFER || stores.size() < 2) {
            return 0;
        }
        double[] stored = new double[stores.size()];
        double[] capacity = new double[stores.size()];
        for (int i = 0; i < stores.size(); i++) {
            stored[i] = stores.get(i).stored();
            capacity[i] = stores.get(i).capacity();
        }
        double[] delta = ConduitTransfer.balance(rate, stored, capacity);
        double given = 0;
        for (int i = 0; i < stores.size(); i++) {
            if (delta[i] < 0) {
                given += stores.get(i).extract(-delta[i], false);
            }
        }
        double wanted = 0;
        for (double d : delta) {
            wanted += Math.max(0, d);
        }
        double scale = wanted > 0 ? given / wanted : 0;
        for (int i = 0; i < stores.size(); i++) {
            if (delta[i] > 0) {
                stores.get(i).insert(delta[i] * scale, false);
            }
        }
        return given;
    }
}
