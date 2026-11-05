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
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * What a detonation does to the blocks of the ships around it, the way {@link NuclearBlastManager}
 * does it to the terrain: blown away in the severe zone, wrecked by chance in the moderate one, burnt where
 * they face the fireball in the thermal one. The zones are fixed by where each ship was at the moment of
 * the detonation. Each job first looks through its ship's blocks, {@code explosive.ship.scanBlocksPerTick}
 * a tick across all jobs, then works through the ones in reach nearest first, a share of the crater's
 * budget a tick. A blast wave also shatters the fragile blocks of the ships it strikes. Jobs are saved with
 * the level ({@link ShipBlastJobs}): one under way when the world closes looks its ship over again when it
 * opens, and finishes.
 */
public final class ShipBlastManager {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

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
            ShipBlastJobs.get(level).add(new Job(ship.id(), ship.toLocal(centre), centre, profile, 0, false, ship.equals(source), reach));
        }
    }

    /** Queues the shattering of a ship's fragile blocks by a blast wave of {@code yieldKg} from {@code centre}. */
    public static void shatter(ServerLevel level, ShipSpace.Ship ship, Vec3 centre, double yieldKg) {
        if (!AeronauticsConfig.blastDamageOn() || !CivilDefenseConfig.blastWaveBreakFragileBlocks) {
            return;
        }
        ShipBlastJobs.get(level).add(new Job(ship.id(), ship.toLocal(centre), centre, null, yieldKg, true, false, Double.MAX_VALUE));
    }

    /** Ship jobs still to finish in {@code level}. */
    public static int pending(ServerLevel level) {
        ShipBlastJobs jobs = ShipBlastJobs.getIfPresent(level);
        return jobs == null ? 0 : jobs.jobs().size();
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ShipBlastJobs data = ShipBlastJobs.getIfPresent(level);
        if (data == null || data.jobs().isEmpty()) {
            return;
        }
        int scan = Math.max(1, ExplosiveConfig.shipScanBlocksPerTick);
        int budget = Math.max(1, PerformanceConfig.blastBlocksPerTick / 2);
        int fires = ExplosiveConfig.thermalFiresPerTick;
        Iterator<Job> jobs = data.jobs().iterator();
        while (jobs.hasNext() && (budget > 0 || scan > 0)) {
            Job job = jobs.next();
            ShipSpace.Ship ship = ShipSpace.byId(level, job.ship);
            if (ship == null) {
                jobs.remove();
                continue;
            }
            if (!job.scanned()) {
                scan -= job.scan(level, ship, scan);
                continue;
            }
            int[] used = job.work(level, budget, fires);
            budget -= used[0];
            fires -= used[1];
            if (job.done()) {
                jobs.remove();
            }
        }
        data.setDirty();
    }

    /** One detonation's work on one ship: looking its blocks over, then breaking the ones in reach, nearest first. */
    public static final class Job {
        private final UUID ship;
        private final Vec3 localCentre;
        private final Vec3 worldCentre;
        @Nullable
        private final DetonationProfile profile;
        private final double yieldKg;
        private final boolean fragileOnly;
        private final boolean own;
        private final double reach;
        /** How far the look over the ship's box has got; not saved, so a loaded job looks again. */
        private long scanIndex;
        private LongArrayList foundBlocks = new LongArrayList();
        private DoubleArrayList foundDistances = new DoubleArrayList();
        /** Plot blocks, nearest the blast first, once the look is done. */
        private long[] blocks;
        private double[] distances;
        private int next;
        private ServerLevel level;
        private Explosion explosion;

        Job(UUID ship, Vec3 localCentre, Vec3 worldCentre, @Nullable DetonationProfile profile, double yieldKg, boolean fragileOnly,
            boolean own, double reach) {
            this.ship = ship;
            this.localCentre = localCentre;
            this.worldCentre = worldCentre;
            this.profile = profile;
            this.yieldKg = yieldKg;
            this.fragileOnly = fragileOnly;
            this.own = own;
            this.reach = reach;
        }

        boolean scanned() {
            return blocks != null;
        }

        /** Looks over up to {@code budget} positions of the ship's box; how many it looked at. */
        int scan(ServerLevel level, ShipSpace.Ship holder, int budget) {
            BoundingBox box = holder.localBounds();
            long xs = box.getXSpan();
            long ys = box.getYSpan();
            long zs = box.getZSpan();
            long volume = xs * ys * zs;
            int looked = 0;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            while (scanIndex < volume && looked < budget) {
                long i = scanIndex++;
                looked++;
                pos.set(box.minX() + (int) (i % xs), box.minY() + (int) ((i / xs) % ys), box.minZ() + (int) (i / (xs * ys)));
                BlockState state = level.getBlockState(pos);
                if (state.isAir() || fragileOnly && !state.is(CivilDefenseRegistry.BLAST_FRAGILE)) {
                    continue;
                }
                double d = Vec3.atCenterOf(pos).distanceTo(localCentre);
                if (own && profile != null) {
                    // The warhead's own ship takes the blast at point-blank range.
                    d = Math.min(d, profile.severeRadius());
                }
                if (d <= reach) {
                    foundBlocks.add(pos.asLong());
                    foundDistances.add(d);
                }
            }
            if (scanIndex >= volume) {
                sort();
            }
            return looked;
        }

        private void sort() {
            int n = foundBlocks.size();
            Integer[] order = new Integer[n];
            for (int i = 0; i < n; i++) {
                order[i] = i;
            }
            java.util.Arrays.sort(order, (a, b) -> Double.compare(foundDistances.getDouble(a), foundDistances.getDouble(b)));
            blocks = new long[n];
            distances = new double[n];
            for (int i = 0; i < n; i++) {
                blocks[i] = foundBlocks.getLong(order[i]);
                distances[i] = foundDistances.getDouble(order[i]);
            }
            foundBlocks = null;
            foundDistances = null;
        }

        boolean done() {
            return scanned() && next >= blocks.length;
        }

        /** Works through up to {@code budget} blocks; the blocks it looked at and the fires it lit. */
        int[] work(ServerLevel level, int budget, int fires) {
            if (this.level != level) {
                this.level = level;
                this.explosion = new Explosion(level, null, localCentre.x, localCentre.y, localCentre.z,
                        (float) Math.max(1, profile == null ? 1 : profile.fireballRadius()), false, Explosion.BlockInteraction.DESTROY);
            }
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

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Ship", ship);
            putVec(tag, "Local", localCentre);
            putVec(tag, "World", worldCentre);
            if (profile != null) {
                ListTag numbers = new ListTag();
                for (double value : profile.encode()) {
                    numbers.add(DoubleTag.valueOf(value));
                }
                tag.put("Profile", numbers);
            }
            tag.putDouble("Yield", yieldKg);
            tag.putBoolean("FragileOnly", fragileOnly);
            tag.putBoolean("Own", own);
            tag.putDouble("Reach", reach);
            return tag;
        }

        @Nullable
        static Job load(CompoundTag tag) {
            if (!tag.hasUUID("Ship")) {
                return null;
            }
            DetonationProfile profile = null;
            if (tag.contains("Profile")) {
                ListTag numbers = tag.getList("Profile", Tag.TAG_DOUBLE);
                double[] values = new double[numbers.size()];
                for (int i = 0; i < values.length; i++) {
                    values[i] = numbers.getDouble(i);
                }
                profile = DetonationProfile.decode(values);
            }
            boolean fragileOnly = tag.getBoolean("FragileOnly");
            if (profile == null && !fragileOnly) {
                return null;
            }
            return new Job(tag.getUUID("Ship"), getVec(tag, "Local"), getVec(tag, "World"), profile, tag.getDouble("Yield"), fragileOnly,
                    tag.getBoolean("Own"), tag.getDouble("Reach"));
        }

        private static void putVec(CompoundTag tag, String key, Vec3 v) {
            tag.putDouble(key + "X", v.x);
            tag.putDouble(key + "Y", v.y);
            tag.putDouble(key + "Z", v.z);
        }

        private static Vec3 getVec(CompoundTag tag, String key) {
            return new Vec3(tag.getDouble(key + "X"), tag.getDouble(key + "Y"), tag.getDouble(key + "Z"));
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
            if (state.is(Blocks.BEDROCK) || state.getExplosionResistance(level, pos, explosion) > profile.blastProof(ExplosiveConfig.blastProofResistance)) {
                return;
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET);
        }
    }
}
