package org.papiricoh.create_nuclearindustry.explosive;

import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.PerformanceConfig;
import org.papiricoh.create_nuclearindustry.explosive.blast.BlastHash;
import org.papiricoh.create_nuclearindustry.explosive.blast.CraterShape;
import org.papiricoh.create_nuclearindustry.explosive.blast.DetonationProfile;
import org.papiricoh.create_nuclearindustry.explosive.blast.ThermalPulse;
import org.papiricoh.create_nuclearindustry.integration.claims.ClaimGuards;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;
import org.papiricoh.create_nuclearindustry.radiation.cache.RadiationCaches;

import java.util.Comparator;
import java.util.List;

/**
 * Works a nuclear detonation's damage to the ground through, column by column from ground zero
 * outwards: the crater bowl with its raised rim, glassy floor and ejecta; the severe zone where whatever
 * stands above the ground is blown away; the moderate zone where part of it is wrecked and what is left
 * hanging falls; and the flash that scorches the ground, burns leaves, chars trees and starts fires (see
 * {@link DetonationProfile} for the zones).
 * <p>
 * The jobs are saved with their level ({@link BlastJobs}) and carry on after a restart. All of a level's
 * jobs share one budget of columns and blocks per tick ({@code explosive.blastColumnsPerTick} and
 * {@code explosive.blastBlocksPerTick} in {@link PerformanceConfig}), taken in turns. Each column starts at
 * the top of the world there and only goes down as far as it has to. A column in a chunk that is not
 * loaded is skipped and its chunk noted; the job then loads those chunks itself, generating any that never
 * were, a few at a time ({@code explosive.crater.chunksAtOnce}) and nearest first, works them through as
 * they come in and lets each go once its sweep is past it. With {@code explosive.crater.loadChunks} off they
 * wait for something else to load them. Distant Horizons picks the crater up in its distant terrain when
 * those chunks are saved, at no cost here.
 * <p>
 * Blocks change without neighbour updates, so after each tick's work the job tells what it changed
 * itself: the radiation caches of every chunk it touched and the multiblock controllers its box reaches.
 * <p>
 * Chunks a land claim keeps explosions out of ({@link ClaimGuards}, with {@code explosive.respectClaims})
 * are left as they are: their columns are skipped without loading them, and nothing the blast throws
 * from next door (rim, ejecta) lands in them.
 */
public final class NuclearBlastManager {
    /**
     * How the blast changes blocks: clients are told, but neighbours are neither updated nor reshaped, so
     * the water round a crater stays still and no change sets off a cascade of others.
     */
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    /** A column's ground is looked for this far below its top: below it, the top is taken as the ground. */
    private static final int GROUND_SEARCH = 64;
    /** A charred or burning tree is followed this far down from the top. */
    private static final int CANOPY_DEPTH = 32;
    /** Holds a chunk the crater is loading at full status, without ticking it, until the crater is done with it. */
    private static final TicketType<ChunkPos> TICKET = TicketType.create("create_nuclearindustry:nuclear_blast",
            Comparator.comparingLong(ChunkPos::toLong));

    private NuclearBlastManager() {}

    /** Starts working a detonation's damage at {@code centre} through, saved with the level. */
    public static BlastJob start(ServerLevel level, BlockPos centre, DetonationProfile profile) {
        BlastJobs jobs = BlastJobs.get(level);
        BlastJob job = new BlastJob(jobs.nextId(), centre, profile, groundZero(level, centre, profile), false);
        job.attach(level);
        jobs.add(job);
        return job;
    }

    /**
     * A plain hole that is not worked through on its own: {@link BlastJob#step()} digs one tick's worth.
     * A bowl {@code radius} wide, {@code depth} deep below {@code centre} at its middle. For tests.
     */
    @Nullable
    public static BlastJob create(ServerLevel level, BlockPos centre, int radius, int depth, float intensity) {
        if (radius <= 0 || depth <= 0) {
            return null;
        }
        BlastJob job = new BlastJob(0, centre, DetonationProfile.craterOnly(radius, depth, intensity), centre.getY(), true);
        job.attach(level);
        return job;
    }

    /** Jobs still being worked through in {@code level}, saved with it. */
    public static int pending(ServerLevel level) {
        return BlastJobs.get(level).jobs().size();
    }

    /** Drops every job of {@code level}, as if they had finished, and the chunks they held; for tests and commands. */
    public static void cancelAll(ServerLevel level) {
        BlastJobs.get(level).clear();
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlastJobs data = BlastJobs.get(level);
        List<BlastJob> jobs = data.jobs();
        if (jobs.isEmpty()) {
            return;
        }
        int columns = PerformanceConfig.blastColumnsPerTick;
        int blocks = PerformanceConfig.blastBlocksPerTick;
        // Each job in turn from where last tick stopped, all sharing the one budget.
        int first = data.turn(jobs.size());
        for (int i = 0; i < jobs.size() && columns > 0 && blocks > 0; i++) {
            BlastJob job = jobs.get((first + i) % jobs.size());
            job.attach(level);
            job.work(columns, blocks);
            columns -= job.lastColumns;
            blocks -= job.lastBlocks;
        }
        for (BlastJob job : jobs) {
            job.attach(level);
            job.fetch();
        }
        data.removeFinished();
        data.setDirty();
    }

    /** A chunk a job skipped has loaded: queue it to be worked through on the next ticks. */
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        BlastJobs data = BlastJobs.getIfPresent(level);
        if (data == null || data.jobs().isEmpty()) {
            return;
        }
        long key = chunk.getPos().toLong();
        for (BlastJob job : data.jobs()) {
            if (job.deferred.contains(key)) {
                job.loaded.add(key);
            }
        }
    }

    /**
     * Where the bowl is measured from: the ground under a burst near the ground, the burst itself for one
     * underground. A plain hole is measured from its centre; for a detonation, ground zero's chunk is loaded
     * by {@link NuclearDetonation#burstHeight} already.
     */
    private static int groundZero(ServerLevel level, BlockPos centre, DetonationProfile profile) {
        LevelChunk chunk = profile.craterRadius() > 0 && profile.power() > 0
                ? level.getChunkSource().getChunkNow(centre.getX() >> 4, centre.getZ() >> 4) : null;
        if (chunk == null) {
            return centre.getY();
        }
        int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, centre.getX(), centre.getZ());
        int ground = findGround(chunk, centre.getX(), centre.getZ(), Math.min(top, centre.getY()));
        return Math.min(centre.getY(), ground);
    }

    /** The highest natural ground block at or below {@code from} in a column, or {@code from} when none is near. */
    static int findGround(LevelChunk chunk, int x, int z, int from) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int bottom = Math.max(chunk.getMinBuildHeight(), from - GROUND_SEARCH);
        for (int y = from; y >= bottom; y--) {
            BlockState state = chunk.getBlockState(pos.set(x, y, z));
            if (isGround(state)) {
                return y;
            }
        }
        return from;
    }

    /** Natural ground: what the blast leaves standing while it sweeps away what was built or grew on it. */
    public static boolean isGround(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER) || state.is(BlockTags.TERRACOTTA) || state.is(Tags.Blocks.GRAVELS)
                || state.is(Tags.Blocks.ORES) || state.is(Tags.Blocks.SANDSTONE_BLOCKS) || state.is(Tags.Blocks.COBBLESTONES)
                || state.is(Tags.Blocks.OBSIDIANS) || state.is(Blocks.CLAY) || state.is(Blocks.BEDROCK) || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE) || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.BASALT)
                || state.is(BlockTags.NYLIUM) || state.is(ExplosiveRegistry.TRINITITE.get()) || state.is(Blocks.MUD)
                || state.is(Blocks.END_STONE);
    }

    /** One detonation's damage being worked through. */
    public static final class BlastJob {
        final int id;
        final BlockPos centre;
        final DetonationProfile profile;
        /** The height the bowl is measured from. */
        final int groundZero;
        /** A plain hole for tests: no ground search, measured from the centre. */
        final boolean plainHole;
        private final CraterShape shape;
        private final int reach;
        // Square ring cursor (Chebyshev distance) sweeping from the centre outwards.
        int ring;
        int idx;
        /** Chunks skipped because they were not loaded, worked through when they load; nearest first. */
        final LongLinkedOpenHashSet deferred = new LongLinkedOpenHashSet();
        /** Blocks this job has made fall so far, and fires it has started. */
        int fallen;
        int fires;

        // Not saved.
        @Nullable
        private ServerLevel level;
        /** The chunk of the column being worked: its blocks are read straight from it. */
        @Nullable
        private LevelChunk current;
        @Nullable
        private Explosion explosion;
        /** Deferred chunks that have loaded, waiting for their turn; each once. */
        final LongLinkedOpenHashSet loaded = new LongLinkedOpenHashSet();
        /** Chunks this job holds loaded with its {@link #TICKET}. */
        private final LongOpenHashSet held = new LongOpenHashSet();
        /** Never loads chunks itself, whatever the config says; for tests. */
        private boolean waitsForLoads;
        private int lastColumns;
        private int lastBlocks;
        private int fallenThisTick;
        private int firesThisTick;
        private final LongOpenHashSet dugChunks = new LongOpenHashSet();
        /** Whether a claim protects each chunk looked at so far; asked once per chunk per job. */
        private final Long2BooleanOpenHashMap claimed = new Long2BooleanOpenHashMap();
        private int dugMinX, dugMinY, dugMinZ, dugMaxX, dugMaxY, dugMaxZ;
        @Nullable
        private BoundingBox lastDug;

        BlastJob(int id, BlockPos centre, DetonationProfile profile, int groundZero, boolean plainHole) {
            this.id = id;
            this.centre = centre.immutable();
            this.profile = profile;
            this.groundZero = groundZero;
            this.plainHole = plainHole;
            this.shape = new CraterShape(profile);
            this.reach = (int) Math.ceil(profile.blockReach());
        }

        void attach(ServerLevel level) {
            if (this.level != level) {
                this.level = level;
                this.explosion = new Explosion(level, null, centre.getX() + 0.5, centre.getY() + 0.5, centre.getZ() + 0.5,
                        (float) Math.max(1, profile.fireballRadius()), false, Explosion.BlockInteraction.DESTROY);
            }
        }

        public DetonationProfile profile() {
            return profile;
        }

        /** Whether every column has been worked through, the deferred chunks too. */
        public boolean finished() {
            return ring > reach && deferred.isEmpty();
        }

        /** Works one tick's worth through on the whole budget of the performance config; true once finished. */
        public boolean step() {
            work(PerformanceConfig.blastColumnsPerTick, PerformanceConfig.blastBlocksPerTick);
            return finished();
        }

        /**
         * Works columns through until {@code columnBudget} columns or {@code blockBudget} changed blocks:
         * first the deferred chunks that have loaded, then the sweep outwards. A column is always finished,
         * so a step goes at most one column over the block budget.
         */
        void work(int columnBudget, int blockBudget) {
            int columns = 0;
            int changed = 0;
            fallenThisTick = 0;
            firesThisTick = 0;
            dugMinX = dugMinY = dugMinZ = Integer.MAX_VALUE;
            dugMaxX = dugMaxY = dugMaxZ = Integer.MIN_VALUE;
            while (!loaded.isEmpty() && columns < columnBudget && changed < blockBudget) {
                long key = loaded.removeFirstLong();
                if (!deferred.contains(key)) {
                    continue;
                }
                ChunkPos chunk = new ChunkPos(key);
                if (level.getChunkSource().getChunkNow(chunk.x, chunk.z) == null) {
                    continue;
                }
                deferred.remove(key);
                for (int i = 0; i < 256; i++) {
                    int dx = chunk.getMinBlockX() + (i & 15) - centre.getX();
                    int dz = chunk.getMinBlockZ() + (i >> 4) - centre.getZ();
                    if (passed(dx, dz) && inReach(dx, dz)) {
                        changed += column(dx, dz);
                        columns++;
                    }
                }
            }
            while (ring <= reach && columns < columnBudget && changed < blockBudget) {
                int[] cell = perimeterCell(ring, idx);
                int dx = cell[0];
                int dz = cell[1];
                advance();
                if (!inReach(dx, dz)) {
                    // A corner of the square ring outside the disc: skipped without touching the world.
                    continue;
                }
                changed += column(dx, dz);
                columns++;
            }
            lastColumns = columns;
            lastBlocks = changed;
            announce();
        }

        /**
         * Loads the chunks the sweep had to skip, generating those that never were: nearest first, at most
         * {@code explosive.crater.chunksAtOnce} on their way at once. Each is held with a ticket until it has
         * been worked through and the sweep has gone right past it, then let go to unload (and be saved) as
         * usual. Chunks already loaded by then only need their turn.
         */
        void fetch() {
            LongIterator done = held.iterator();
            while (done.hasNext()) {
                long key = done.nextLong();
                if (!deferred.contains(key) && swept(key)) {
                    unhold(key);
                    done.remove();
                }
            }
            if (waitsForLoads || !ExplosiveConfig.craterLoadsChunks || deferred.isEmpty()) {
                return;
            }
            int coming = 0;
            LongIterator next = deferred.iterator();
            while (next.hasNext()) {
                long key = next.nextLong();
                boolean ready = level.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key)) != null;
                if (held.contains(key)) {
                    // One that has come in waits for its turn, even if it was not reachable yet when it loaded.
                    if (ready) {
                        loaded.add(key);
                    } else {
                        coming++;
                    }
                    continue;
                }
                if (coming >= ExplosiveConfig.chunksAtOnce) {
                    break;
                }
                ChunkPos chunk = new ChunkPos(key);
                level.getChunkSource().addRegionTicket(TICKET, chunk, 0, chunk);
                held.add(key);
                if (ready) {
                    loaded.add(key);
                } else {
                    coming++;
                }
            }
        }

        /** Lets go of every chunk this job holds: it has finished, or been dropped. */
        void release() {
            if (level != null) {
                LongIterator iterator = held.iterator();
                while (iterator.hasNext()) {
                    unhold(iterator.nextLong());
                }
            }
            held.clear();
        }

        private void unhold(long key) {
            ChunkPos chunk = new ChunkPos(key);
            level.getChunkSource().removeRegionTicket(TICKET, chunk, 0, chunk);
        }

        /** Whether the sweep has gone past every column of a chunk. */
        private boolean swept(long key) {
            int x0 = (ChunkPos.getX(key) << 4) - centre.getX();
            int z0 = (ChunkPos.getZ(key) << 4) - centre.getZ();
            int far = Math.max(Math.max(Math.abs(x0), Math.abs(x0 + 15)), Math.max(Math.abs(z0), Math.abs(z0 + 15)));
            return ring > Math.min(far, reach);
        }

        /** Makes this job leave the chunks it skips for something else to load, as with the config off; for tests. */
        public void waitForLoads() {
            waitsForLoads = true;
        }

        /** Chunks this job is holding loaded. */
        public int heldChunks() {
            return held.size();
        }

        private void advance() {
            int count = ring == 0 ? 1 : 8 * ring;
            if (++idx >= count) {
                idx = 0;
                ring++;
            }
        }

        /** Whether the sweep has gone past a column already. */
        private boolean passed(int dx, int dz) {
            int r = Math.max(Math.abs(dx), Math.abs(dz));
            return r < ring || r == ring && perimeterIndex(r, dx, dz) < idx;
        }

        private boolean inReach(int dx, int dz) {
            return (double) dx * dx + (double) dz * dz <= (double) reach * reach;
        }

        /** Where a column of ring {@code r} comes in its sweep: the inverse of {@link #perimeterCell}. */
        static int perimeterIndex(int r, int dx, int dz) {
            if (r == 0) {
                return 0;
            }
            int side = 2 * r;
            if (dz == -r && dx < r) {
                return dx + r;
            }
            if (dx == r && dz < r) {
                return side + dz + r;
            }
            if (dz == r && dx > -r) {
                return 2 * side + r - dx;
            }
            return 3 * side + r - dz;
        }

        static int[] perimeterCell(int r, int i) {
            if (r == 0) {
                return new int[]{0, 0};
            }
            int side = 2 * r;
            if (i < side) {
                return new int[]{-r + i, -r};
            }
            i -= side;
            if (i < side) {
                return new int[]{r, -r + i};
            }
            i -= side;
            if (i < side) {
                return new int[]{r - i, r};
            }
            i -= side;
            return new int[]{-r, r - i};
        }

        // ------------------------------------------------------------------ one column

        /** Works one column through; returns the blocks it changed. */
        private int column(int dx, int dz) {
            int x = centre.getX() + dx;
            int z = centre.getZ() + dz;
            long chunkKey = ChunkPos.asLong(x >> 4, z >> 4);
            if (claimed(chunkKey)) {
                return 0;
            }
            LevelChunk chunk = deferred.contains(chunkKey) ? null : level.getChunkSource().getChunkNow(x >> 4, z >> 4);
            if (chunk == null) {
                deferred.add(chunkKey);
                return 0;
            }
            current = chunk;
            int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            int changed = 0;
            double r = CraterShape.distance(dx, dz);
            if (shape.inBowl(dx, dz)) {
                changed += bowl(chunk, x, z, dx, dz, top);
            } else {
                int ground = plainHole ? top : findGround(chunk, x, z, top);
                if (r <= profile.severeRadius()) {
                    changed += clearAbove(x, z, top, ground);
                } else if (r <= profile.moderateRadius()) {
                    changed += wreck(x, z, top, ground, r);
                }
                int pile = shape.rimPile(dx, dz);
                if (pile > 0) {
                    changed += pile(x, z, ground, pile);
                } else if (shape.ejecta(x, z, dx, dz)) {
                    changed += pile(x, z, ground, 1);
                }
            }
            if (r <= profile.thermalRadius() && !plainHole) {
                changed += scorch(chunk, x, z, ThermalPulse.strength(r, profile.thermalRadius()));
            }
            if (changed > 0) {
                dugChunks.add(chunkKey);
            }
            return changed;
        }

        /** Digs the bowl down to its surface, raises its lip above the ground near the edge and glazes the floor. */
        private int bowl(LevelChunk chunk, int x, int z, int dx, int dz, int top) {
            int floor = groundZero + (int) Math.round(shape.bowlSurface(dx, dz));
            int changed = 0;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            // A plain hole is cleared down to its floor; a crater keeps it to glaze.
            for (int y = top; y > floor || plainHole && y == floor; y--) {
                if (remove(pos.set(x, y, z))) {
                    changed++;
                }
            }
            if (!plainHole) {
                int ground = findGround(chunk, x, z, Math.min(top, floor));
                for (int y = ground + 1; y <= floor; y++) {
                    changed += place(pos.set(x, y, z), debris(x, y, z)) ? 1 : 0;
                }
                BlockState floorState = floorState(shape.floor(x, z, dx, dz));
                if (floorState != null && place(pos.set(x, floor, z), floorState)) {
                    changed++;
                }
            }
            return changed;
        }

        /** Blows away everything above the ground in a column that is not blast-proof. */
        private int clearAbove(int x, int z, int top, int ground) {
            int changed = 0;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int y = top; y > ground; y--) {
                if (remove(pos.set(x, y, z))) {
                    changed++;
                }
            }
            return changed;
        }

        /**
         * Wrecks part of what stands above the ground, more the closer to the severe zone: leaves are
         * stripped, trees are felled by chance (a whole trunk at once), anything else goes block by block by
         * chance. What is left hanging over a gap falls.
         */
        private int wreck(int x, int z, int top, int ground, double r) {
            double span = Math.max(1, profile.moderateRadius() - profile.severeRadius());
            double closeness = 1 - (r - profile.severeRadius()) / span;
            double chance = 0.2 + 0.6 * closeness;
            // A tree stands or falls whole, not log by log.
            boolean felled = BlastHash.unit(x, z, 47) < 0.25 + 0.7 * closeness;
            int changed = 0;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int y = top; y > ground; y--) {
                pos.set(x, y, z);
                BlockState state = state(pos);
                if (state.isAir()) {
                    continue;
                }
                if (state.is(BlockTags.LOGS)) {
                    if (felled && remove(pos)) {
                        changed++;
                    }
                    continue;
                }
                boolean leaves = state.is(BlockTags.LEAVES);
                if ((leaves || BlastHash.unit(x, y, z, 41) < chance) && remove(pos)) {
                    changed++;
                }
            }
            // Bottom up: whatever now hangs over air comes down.
            for (int y = ground + 2; y <= top; y++) {
                pos.set(x, y, z);
                BlockState state = state(pos);
                if (state.isAir() || state.is(BlockTags.LOGS) || !state(pos.below()).isAir()) {
                    continue;
                }
                if (fallenThisTick >= ExplosiveConfig.fallingDebrisPerTick || fallen >= ExplosiveConfig.maxFallingDebris) {
                    break;
                }
                if (!blastProof(pos, state) && state.getFluidState().isEmpty() && !nearFluid(pos)) {
                    FallingBlockEntity.fall(level, pos.immutable(), state);
                    fallen++;
                    fallenThisTick++;
                    changed++;
                }
            }
            return changed;
        }

        /** Raises the rim or drops ejecta: {@code height} blocks of debris on the ground. */
        private int pile(int x, int z, int ground, int height) {
            int changed = 0;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int y = ground + 1; y <= ground + height; y++) {
                if (place(pos.set(x, y, z), debris(x, y, z))) {
                    changed++;
                }
            }
            return changed;
        }

        /**
         * The flash on a column: grass and plants burn off, leaves burn away and exposed logs char, and
         * fires start on what burns. Only what the fireball can see: the top of the column.
         */
        private int scorch(LevelChunk chunk, int x, int z, double strength) {
            if (strength < 1) {
                return 0;
            }
            int changed = 0;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            int top = exposedTop(chunk, x, z);
            boolean canopy = ThermalPulse.burnsLeaves(strength);
            boolean charring = ThermalPulse.chars(strength);
            if (canopy || charring) {
                for (int y = top; y > top - CANOPY_DEPTH; y--) {
                    pos.set(x, y, z);
                    BlockState state = state(pos);
                    if (state.is(BlockTags.LEAVES) && canopy) {
                        changed += remove(pos) ? 1 : 0;
                    } else if (state.is(BlockTags.LOGS) && charring && !state.is(ExplosiveRegistry.CHARRED_LOG.get())) {
                        BlockState charred = ExplosiveRegistry.CHARRED_LOG.get().defaultBlockState();
                        if (state.hasProperty(RotatedPillarBlock.AXIS)) {
                            charred = charred.setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS));
                        }
                        changed += place(pos, charred) ? 1 : 0;
                    } else if (!state.isAir()) {
                        break;
                    }
                }
                top = exposedTop(chunk, x, z);
            }
            if (ThermalPulse.scorches(strength)) {
                pos.set(x, top, z);
                BlockState state = state(pos);
                if (state.canBeReplaced() && state.getFluidState().isEmpty() && !state.isAir()) {
                    changed += remove(pos) ? 1 : 0;
                    top--;
                    pos.set(x, top, z);
                    state = state(pos);
                }
                if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM) || state.is(Blocks.DIRT_PATH)) {
                    changed += place(pos, Blocks.COARSE_DIRT.defaultBlockState()) ? 1 : 0;
                }
            }
            if (ExplosiveConfig.thermalFires && firesThisTick < ExplosiveConfig.thermalFiresPerTick) {
                pos.set(x, top, z);
                BlockState state = state(pos);
                BlockPos above = pos.above();
                if (state.isFlammable(level, pos, Direction.UP) && state(above).isAir()
                        && BlastHash.unit(x, z, 53) < ThermalPulse.fireChance(strength)) {
                    level.setBlock(above, BaseFireBlock.getState(level, above), QUIET);
                    fires++;
                    firesThisTick++;
                    changed++;
                }
            }
            return changed;
        }

        /** The highest block of a column the flash can light: invisible blocks (barriers, light) let it through. */
        private int exposedTop(LevelChunk chunk, int x, int z) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            int bottom = chunk.getMinBuildHeight();
            int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            while (y > bottom && state(pos.set(x, y, z)).getRenderShape() == RenderShape.INVISIBLE) {
                y--;
            }
            return y;
        }

        // ------------------------------------------------------------------ block changes

        /**
         * Whether water or lava touches a block. A falling block updates its neighbours, which would set
         * the water round a crater flowing into it: that is left still, as the crater leaves it.
         */
        /**
         * The loaded chunk a block is in, or null: the column's own chunk first, else a lookup that never
         * loads. Reading through the level would load (even generate) a neighbour at the edge of what is
         * loaded, on the server thread.
         */
        @Nullable
        private LevelChunk chunkAt(BlockPos pos) {
            LevelChunk chunk = current;
            if (chunk != null && chunk.getPos().x == pos.getX() >> 4 && chunk.getPos().z == pos.getZ() >> 4) {
                return chunk;
            }
            return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        }

        /** A block, without loading anything: air in a chunk that is not loaded. */
        private BlockState state(BlockPos pos) {
            LevelChunk chunk = chunkAt(pos);
            return chunk == null ? Blocks.AIR.defaultBlockState() : chunk.getBlockState(pos);
        }

        /** A neighbour in a chunk that is not loaded counts as fluid: nothing falls there. */
        private boolean nearFluid(BlockPos pos) {
            BlockPos.MutableBlockPos next = new BlockPos.MutableBlockPos();
            for (Direction direction : Direction.values()) {
                next.setWithOffset(pos, direction);
                LevelChunk chunk = chunkAt(next);
                if (chunk == null || !chunk.getFluidState(next).isEmpty()) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Changes a block in a loaded chunk; false where the chunk is not loaded or a claim protects it. A
         * block entity still waiting in its chunk's data is taken out first, or the chunk would try to save it
         * for the new block.
         */
        private boolean set(BlockPos pos, BlockState from, BlockState to) {
            LevelChunk chunk = chunkAt(pos);
            if (chunk == null || claimed(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4))) {
                return false;
            }
            if (from.hasBlockEntity()) {
                chunk.getBlockEntity(pos);
            }
            level.setBlock(pos, to, QUIET);
            return true;
        }

        /** Whether a land claim keeps this job out of chunk {@code key}. */
        private boolean claimed(long key) {
            if (claimed.containsKey(key)) {
                return claimed.get(key);
            }
            boolean protectedChunk = ClaimGuards.protectsFromExplosions(level, ChunkPos.getX(key), ChunkPos.getZ(key));
            claimed.put(key, protectedChunk);
            return protectedChunk;
        }

        private boolean blastProof(BlockPos pos, BlockState state) {
            return state.is(Blocks.BEDROCK) || state.is(Blocks.END_PORTAL_FRAME)
                    || state.getExplosionResistance(level, pos, explosion) > ExplosiveConfig.blastProofResistance;
        }

        /** Turns a block to air unless it is air already or blast-proof; true if it changed. */
        private boolean remove(BlockPos pos) {
            BlockState state = state(pos);
            if (state.isAir() || blastProof(pos, state) || !set(pos, state, Blocks.AIR.defaultBlockState())) {
                return false;
            }
            dug(pos);
            return true;
        }

        /** Puts a block where there is none or the blast may replace one; true if it changed. */
        private boolean place(BlockPos pos, BlockState state) {
            BlockState there = state(pos);
            if (there == state || !there.isAir() && blastProof(pos, there) || !set(pos, there, state)) {
                return false;
            }
            dug(pos);
            return true;
        }

        private static BlockState debris(int x, int y, int z) {
            return switch (CraterShape.debris(x, y, z)) {
                case COARSE_DIRT -> Blocks.COARSE_DIRT.defaultBlockState();
                case GRAVEL -> Blocks.GRAVEL.defaultBlockState();
                case COBBLESTONE -> Blocks.COBBLESTONE.defaultBlockState();
                case TUFF -> Blocks.TUFF.defaultBlockState();
            };
        }

        @Nullable
        private static BlockState floorState(CraterShape.Floor floor) {
            return switch (floor) {
                case NONE -> null;
                case TRINITITE -> ExplosiveRegistry.TRINITITE.get().defaultBlockState();
                case OBSIDIAN -> Blocks.OBSIDIAN.defaultBlockState();
                case BASALT -> Blocks.BASALT.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y);
                case MAGMA -> Blocks.MAGMA_BLOCK.defaultBlockState();
            };
        }

        private void dug(BlockPos pos) {
            dugMinX = Math.min(dugMinX, pos.getX());
            dugMinY = Math.min(dugMinY, pos.getY());
            dugMinZ = Math.min(dugMinZ, pos.getZ());
            dugMaxX = Math.max(dugMaxX, pos.getX());
            dugMaxY = Math.max(dugMaxY, pos.getY());
            dugMaxZ = Math.max(dugMaxZ, pos.getZ());
        }

        /**
         * Tells the radiation caches and the multiblock controllers about this step's changes: raw state
         * changes fire no block event, so without this cached rays would still see vanished walls and a
         * sealed building would keep thinking itself sealed. One change per chunk is enough (the radiation
         * change log is per chunk), and one box for the controllers.
         */
        private void announce() {
            if (dugMinX > dugMaxX) {
                lastDug = null;
                dugChunks.clear();
                return;
            }
            BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
            var iterator = dugChunks.iterator();
            while (iterator.hasNext()) {
                long chunk = iterator.nextLong();
                probe.set(ChunkPos.getX(chunk) << 4, centre.getY(), ChunkPos.getZ(chunk) << 4);
                RadiationCaches.blockChanged(level, probe);
            }
            dugChunks.clear();
            lastDug = new BoundingBox(dugMinX, dugMinY, dugMinZ, dugMaxX, dugMaxY, dugMaxZ);
            MultiblockRegistry.areaChanged(level, lastDug);
        }

        /** The box of the blocks the last step changed, or null if it changed none. */
        @Nullable
        public BoundingBox lastDug() {
            return lastDug;
        }

        /** Columns worked through by the last step. */
        public int lastColumns() {
            return lastColumns;
        }

        /** Blocks changed by the last step. */
        public int lastBlocks() {
            return lastBlocks;
        }

        /** Chunks skipped because they were not loaded, still to be worked through. */
        public int deferredChunks() {
            return deferred.size();
        }

        /** Blocks this job has made fall. */
        public int fallen() {
            return fallen;
        }

        /** Where the sweep has got to: its ring and the column within it. */
        public int ring() {
            return ring;
        }

        public int index() {
            return idx;
        }

        public BlockPos centre() {
            return centre;
        }
    }
}
