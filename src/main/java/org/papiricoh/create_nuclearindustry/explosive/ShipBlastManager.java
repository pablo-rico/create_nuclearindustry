package org.papiricoh.create_nuclearindustry.explosive;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.PerformanceConfig;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseConfig;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry;
import org.papiricoh.create_nuclearindustry.civildefense.blast.BlastWaveModel;
import org.papiricoh.create_nuclearindustry.explosive.blast.BlastHash;
import org.papiricoh.create_nuclearindustry.explosive.blast.DetonationProfile;
import org.papiricoh.create_nuclearindustry.explosive.blast.ThermalPulse;
import org.papiricoh.create_nuclearindustry.integration.sable.AeronauticsConfig;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * What a nuclear detonation does to the blocks of the ships around it, the way {@link NuclearBlastManager}
 * does it to the terrain: blown away in the severe zone, wrecked by chance in the moderate one, burnt where
 * they face the fireball in the thermal one. The zones are fixed by where each ship was at the moment of
 * the detonation; its blocks are then worked through nearest first, a share of the crater's budget a tick.
 * A blast wave also shatters the fragile blocks of the ships it strikes. Jobs live in memory only.
 */
public final class ShipBlastManager {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final List<Job> JOBS = new ArrayList<>();

    private ShipBlastManager() {}

    /**
     * Queues what a detonation at the world point {@code centre} does to the ships in its reach;
     * {@code source} is the ship the warhead was aboard, if any, which takes the full force.
     */
    public static void start(ServerLevel level, Vec3 centre, DetonationProfile profile, @Nullable ShipSpace.Ship source) {
        if (!ShipSpace.enabled() || !AeronauticsConfig.blastDamageOn()) {
            return;
        }
        double reach = Math.min(ExplosiveConfig.craterMaxReach,
                Math.max(Math.max(profile.moderateRadius(), profile.fireballRadius()), profile.thermalRadius()));
        for (ShipSpace.Ship ship : ShipSpace.shipsIn(level, new AABB(centre, centre).inflate(reach))) {
            Vec3 local = ship.toLocal(centre);
            Job job = Job.crater(level, ship, local, profile, ship.equals(source), reach);
            if (job != null) {
                JOBS.add(job);
            }
        }
    }

    /** Queues the shattering of a ship's fragile blocks by a blast wave of {@code yieldKg} from {@code centre}. */
    public static void shatter(ServerLevel level, ShipSpace.Ship ship, Vec3 centre, double yieldKg) {
        if (!AeronauticsConfig.blastDamageOn() || !CivilDefenseConfig.blastWaveBreakFragileBlocks) {
            return;
        }
        Job job = Job.fragile(level, ship, centre, yieldKg);
        if (job != null) {
            JOBS.add(job);
        }
    }

    /** Ship jobs still to finish, in every level. */
    public static int pending() {
        return JOBS.size();
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || JOBS.isEmpty()) {
            return;
        }
        int budget = Math.max(1, PerformanceConfig.blastBlocksPerTick / 2);
        int fires = ExplosiveConfig.thermalFiresPerTick;
        Iterator<Job> jobs = JOBS.iterator();
        while (jobs.hasNext() && budget > 0) {
            Job job = jobs.next();
            if (job.level != level) {
                continue;
            }
            int[] used = job.work(budget, fires);
            budget -= used[0];
            fires -= used[1];
            if (job.done()) {
                jobs.remove();
            }
        }
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        JOBS.clear();
    }

    private static final class Job {
        private final ServerLevel level;
        private final UUID ship;
        /** Plot blocks, nearest the blast first. */
        private final long[] blocks;
        private final double[] distances;
        private final Vec3 localCentre;
        private final Vec3 worldCentre;
        private final DetonationProfile profile;
        private final double yieldKg;
        private final boolean fragileOnly;
        private final Explosion explosion;
        private int next;

        private Job(ServerLevel level, ShipSpace.Ship ship, long[] blocks, double[] distances, Vec3 localCentre, Vec3 worldCentre,
                    @Nullable DetonationProfile profile, double yieldKg, boolean fragileOnly) {
            this.level = level;
            this.ship = ship.id();
            this.blocks = blocks;
            this.distances = distances;
            this.localCentre = localCentre;
            this.worldCentre = worldCentre;
            this.profile = profile;
            this.yieldKg = yieldKg;
            this.fragileOnly = fragileOnly;
            this.explosion = new Explosion(level, null, localCentre.x, localCentre.y, localCentre.z,
                    (float) Math.max(1, profile == null ? 1 : profile.fireballRadius()), false, Explosion.BlockInteraction.DESTROY);
        }

        @Nullable
        static Job crater(ServerLevel level, ShipSpace.Ship ship, Vec3 local, DetonationProfile profile, boolean own, double reach) {
            List<Hit> found = new ArrayList<>();
            forEachBlock(level, ship, pos -> {
                double d = Vec3.atCenterOf(pos).distanceTo(local);
                if (own) {
                    // The warhead's own ship takes the blast at point-blank range.
                    d = Math.min(d, profile.severeRadius());
                }
                if (d <= reach) {
                    found.add(new Hit(pos.asLong(), d));
                }
            });
            return found.isEmpty() ? null : sorted(level, ship, found, local, ship.toWorld(local), profile, 0, false);
        }

        @Nullable
        static Job fragile(ServerLevel level, ShipSpace.Ship ship, Vec3 centre, double yieldKg) {
            Vec3 local = ship.toLocal(centre);
            List<Hit> found = new ArrayList<>();
            forEachBlock(level, ship, pos -> {
                if (level.getBlockState(pos).is(CivilDefenseRegistry.BLAST_FRAGILE)) {
                    found.add(new Hit(pos.asLong(), Vec3.atCenterOf(pos).distanceTo(local)));
                }
            });
            return found.isEmpty() ? null : sorted(level, ship, found, local, centre, null, yieldKg, true);
        }

        private record Hit(long pos, double distance) {}

        private static Job sorted(ServerLevel level, ShipSpace.Ship ship, List<Hit> found, Vec3 local, Vec3 world,
                                  @Nullable DetonationProfile profile, double yieldKg, boolean fragileOnly) {
            found.sort((a, b) -> Double.compare(a.distance(), b.distance()));
            long[] blocks = new long[found.size()];
            double[] distances = new double[found.size()];
            for (int i = 0; i < blocks.length; i++) {
                blocks[i] = found.get(i).pos();
                distances[i] = found.get(i).distance();
            }
            return new Job(level, ship, blocks, distances, local, world, profile, yieldKg, fragileOnly);
        }

        private static void forEachBlock(ServerLevel level, ShipSpace.Ship ship, java.util.function.Consumer<BlockPos> action) {
            BoundingBox box = ship.localBounds();
            for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                if (!level.getBlockState(pos).isAir()) {
                    action.accept(pos.immutable());
                }
            }
        }

        boolean done() {
            return next >= blocks.length;
        }

        /** Works through up to {@code budget} blocks; the blocks it looked at and the fires it lit. */
        int[] work(int budget, int fires) {
            int looked = 0;
            int lit = 0;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            while (next < blocks.length && looked < budget) {
                pos.set(blocks[next]);
                double d = distances[next];
                next++;
                looked++;
                // A block that broke off with another ship, or is gone, is left to that ship.
                ShipSpace.Ship holder = ShipSpace.shipAt(level, pos);
                if (holder == null || !holder.id().equals(ship)) {
                    continue;
                }
                BlockState state = level.getBlockState(pos);
                if (state.isAir()) {
                    continue;
                }
                if (fragileOnly) {
                    double kPa = BlastWaveModel.overpressure(holder.toWorld(Vec3.atCenterOf(pos)).distanceTo(worldCentre), yieldKg);
                    if (kPa >= CivilDefenseConfig.blastWaveFragileOverpressure) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET);
                    }
                    continue;
                }
                lit += hit(pos.immutable(), state, d, fires - lit);
            }
            return new int[]{looked, lit};
        }

        /** The blast on one block {@code d} blocks from it; the fires it lit. */
        private int hit(BlockPos pos, BlockState state, double d, int fires) {
            double severe = Math.max(profile.severeRadius(), profile.fireballRadius());
            if (d <= severe) {
                remove(pos, state);
                return 0;
            }
            if (d <= profile.moderateRadius()) {
                double span = Math.max(1, profile.moderateRadius() - severe);
                double chance = 0.2 + 0.6 * (1 - (d - severe) / span);
                if (state.is(BlockTags.LEAVES) || BlastHash.unit(pos.getX(), pos.getY(), pos.getZ(), 41) < chance) {
                    remove(pos, state);
                    return 0;
                }
            }
            return d <= profile.thermalRadius() ? burn(pos, state, ThermalPulse.strength(d, profile.thermalRadius()), fires) : 0;
        }

        /** The flash on a block that sees the fireball: the face turned to it is open. */
        private int burn(BlockPos pos, BlockState state, double strength, int fires) {
            if (strength < 1) {
                return 0;
            }
            Vec3 toward = localCentre.subtract(Vec3.atCenterOf(pos));
            Direction face = Direction.getNearest(toward.x, toward.y, toward.z);
            BlockPos front = pos.relative(face);
            if (!level.getBlockState(front).isAir()) {
                return 0;
            }
            if (state.is(BlockTags.LEAVES) && ThermalPulse.burnsLeaves(strength)) {
                remove(pos, state);
                return 0;
            }
            if (state.is(BlockTags.LOGS) && ThermalPulse.chars(strength) && !state.is(ExplosiveRegistry.CHARRED_LOG.get())) {
                BlockState charred = ExplosiveRegistry.CHARRED_LOG.get().defaultBlockState();
                if (state.hasProperty(RotatedPillarBlock.AXIS)) {
                    charred = charred.setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS));
                }
                level.setBlock(pos, charred, QUIET);
                return 0;
            }
            if (ExplosiveConfig.thermalFires && fires > 0 && state.isFlammable(level, pos, face)
                    && BlastHash.unit(pos.getX(), pos.getY(), pos.getZ(), 53) < ThermalPulse.fireChance(strength)) {
                level.setBlock(front, BaseFireBlock.getState(level, front), QUIET);
                return 1;
            }
            return 0;
        }

        private void remove(BlockPos pos, BlockState state) {
            if (state.is(Blocks.BEDROCK) || state.getExplosionResistance(level, pos, explosion) > ExplosiveConfig.blastProofResistance) {
                return;
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET);
        }
    }

    /** For tests: forgets every queued job. */
    public static void clear() {
        JOBS.clear();
    }
}
