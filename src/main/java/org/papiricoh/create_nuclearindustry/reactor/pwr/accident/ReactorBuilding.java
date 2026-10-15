package org.papiricoh.create_nuclearindustry.reactor.pwr.accident;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements;
import org.papiricoh.create_nuclearindustry.advancement.NuclearEvent;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace;
import org.papiricoh.create_nuclearindustry.maintenance.incident.IncidentLog;
import org.papiricoh.create_nuclearindustry.maintenance.incident.IncidentType;
import org.papiricoh.create_nuclearindustry.multiblock.SealedVolume;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrConfig;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentModel;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentShape;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentSurvey;

import java.util.List;

/**
 * The ordinary building around a reactor that has no sealed containment: any closed room of
 * normal blocks. Hydrogen from the cladding gathers under its roof instead of escaping, the steam
 * of a leak dilutes it, recombiners inside take it out, and the building, not being airtight, slowly
 * swaps its gas for outside air ({@link ContainmentModel#leak}). Ordinary walls hold
 * {@link org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentRating#DEFAULT_SOLID}
 * bar, so a burn blows the roof off: the Fukushima explosions.
 * <p>
 * The room is measured with the containment's own flood fill, seeded next to the vessel (above
 * its lid first), but only while there is hydrogen to hold and at most every
 * {@link #SURVEY_INTERVAL} ticks, so a reactor in the open costs nothing. The building does not
 * hold fission products back: activity leaves it as it would the open air.
 */
public final class ReactorBuilding {
    /** Ticks between measurements of the building. */
    public static final int SURVEY_INTERVAL = 600;
    /** Ticks between ignition rolls, as in a containment. */
    private static final int BURN_CHECK_INTERVAL = 20;
    /** Seed cells tried around the vessel before giving up: a pocket of air on the lid is no building. */
    private static final int MAX_SEEDS = 4;
    /** Share of the gas's excess temperature lost per tick per m² of wall per m³ of air. */
    private static final double COOLING = 0.008;
    /** Hydrogen fraction below which the building is no longer worth watching. */
    private static final double TRACE = 1.0e-4;
    private static final double MAX_BURN_DAMAGE = 30.0;
    private static final double MIN_POWER = 2.0;

    /** Where activity leaving the building goes: the reactor's open-air release path. */
    @FunctionalInterface
    public interface Release {
        void release(ServerLevel level, BlockPos at, double activity);
    }

    private final ContainmentModel model = ContainmentModel.atmospheric(1, 0);
    @Nullable
    private ContainmentShape shape;
    /** Saved while sealed with hydrogen inside: the first measurement after loading keeps the atmosphere. */
    private boolean restoredSealed;
    @Nullable
    private BoundingBox savedBounds;
    private int surveyTimer;
    private int burnTimer;
    private int explosions;

    // ------------------------------------------------------------------ pure rules

    /** Power of the explosion a burn raising the pressure by {@code rise} bar makes: 2 plus 1.5 per bar, capped. */
    public static double blastPower(double rise, double maxPower) {
        double cap = Math.max(MIN_POWER, maxPower);
        double power = MIN_POWER + 1.5 * (Double.isFinite(rise) ? Math.max(0.0, rise) : 0.0);
        return Math.max(MIN_POWER, Math.min(cap, power));
    }

    /** INES level of a building explosion: 3, or 5 once a tenth of the cladding has failed and the core's activity goes with it. */
    public static int ines(double claddingDamage) {
        return claddingDamage >= 0.1 ? 5 : 3;
    }

    /** Roof blocks the blast tears away around its centre, in blocks: half the power, rounded up. */
    public static int roofRadius(double power) {
        return (int) Math.ceil(power / 2.0);
    }

    // ------------------------------------------------------------------ readings

    /** Whether a closed room around the reactor is holding its gas. */
    public boolean sealed() {
        return shape != null || restoredSealed;
    }

    /** Hydrogen volume fraction in the building, 0-1; 0 without one. */
    public double hydrogenFraction() {
        return sealed() ? model.hydrogenFraction() : 0.0;
    }

    public ContainmentModel model() {
        return model;
    }

    /** The building as last measured, or null; null too right after loading, until it is measured again. */
    @Nullable
    public ContainmentShape shape() {
        return shape;
    }

    /** The air inside, as last measured or saved. */
    @Nullable
    public BoundingBox bounds() {
        return shape != null ? shape.bounds() : savedBounds;
    }

    /** Hydrogen explosions since the reactor was loaded; for tests. */
    public int explosions() {
        return explosions;
    }

    // ------------------------------------------------------------------ what goes in

    /**
     * Hydrogen from the cladding: into the building, measuring it first if it is not known and the
     * last measurement is old enough. Lost to the open air when there is no closed room.
     */
    public void addHydrogen(ServerLevel level, @Nullable BoundingBox reactor, double moles, double temperatureK, Release release) {
        if (!sealed() && surveyTimer <= 0 && reactor != null) {
            resurvey(level, reactor, release);
        }
        if (sealed()) {
            model.addHydrogen(moles, temperatureK);
        }
    }

    /** Steam from a leak: it dilutes the hydrogen in a building already holding some, up to inerting it. */
    public void addSteam(double moles, double temperatureK) {
        if (sealed()) {
            model.addSteam(moles, temperatureK);
        }
    }

    /** Measures the building now, whatever the last measurement: hydrogen is about to come and it cannot wait. */
    public void prepare(ServerLevel level, BoundingBox reactor, Release release) {
        if (shape == null) {
            resurvey(level, reactor, release);
        }
    }

    // ------------------------------------------------------------------ ticking

    /**
     * One tick while the reactor runs: measures the building again now and then, runs its gas
     * (walls, recombiners, leaks), lets it burst if a leak overpressured it, and rolls for ignition
     * every second. Returns true if the hydrogen exploded.
     */
    public boolean tick(ServerLevel level, BoundingBox reactor, double claddingDamage, int rods, Release release) {
        if (surveyTimer > 0) {
            surveyTimer--;
        }
        if (!sealed()) {
            return false;
        }
        if (surveyTimer <= 0) {
            BoundingBox box = bounds();
            if (shape == null && box != null && !level.hasChunksAt(box.minX() - 1, box.minZ() - 1, box.maxX() + 1, box.maxZ() + 1)) {
                // part of the saved building is not loaded yet: measuring now would find it open
                surveyTimer = BURN_CHECK_INTERVAL;
                return false;
            }
            resurvey(level, reactor, release);
        }
        if (shape == null) {
            return false;
        }
        model.tick(shape.recombiners().size(), 0, params());
        double leaked = model.leak(PwrConfig.buildingLeakRate);
        if (leaked > 0) {
            release.release(level, roofCentre(level), leaked);
        }
        if (PwrConfig.containmentRupture && model.pressure() > shape.weakestRating()) {
            burst(level, release);
            return false;
        }
        if (++burnTimer >= BURN_CHECK_INTERVAL) {
            burnTimer = 0;
            if (PwrConfig.hydrogen && model.flammable()
                    && level.random.nextDouble() < ContainmentModel.ignitionChance(model.hydrogenFraction(), PwrConfig.hydrogenIgnitionChance)) {
                return explode(level, claddingDamage, rods, release);
            }
        }
        return false;
    }

    /**
     * A meltdown sets off what the building holds: a room the melt's steam overpressured bursts,
     * otherwise a flammable mixture burns without waiting for the ignition roll. Returns true if
     * the hydrogen exploded.
     */
    public boolean forceIgnition(ServerLevel level, double claddingDamage, int rods, Release release) {
        if (shape == null) {
            return false;
        }
        if (PwrConfig.containmentRupture && model.pressure() > shape.weakestRating()) {
            burst(level, release);
            return false;
        }
        return model.flammable() && explode(level, claddingDamage, rods, release);
    }

    private static ContainmentModel.Params params() {
        return new ContainmentModel.Params(PwrConfig.condensation, COOLING, PwrConfig.recombinerRate, 0.0, 0.0);
    }

    // ------------------------------------------------------------------ measuring

    /**
     * Measures the room around the reactor: a sealed one becomes (or stays) the building, keeping
     * its gas if it already held some; an open one lets the gas out and is forgotten. A building
     * with nothing left in it is forgotten without measuring: there is nothing to hold.
     */
    private void resurvey(ServerLevel level, BoundingBox reactor, Release release) {
        surveyTimer = SURVEY_INTERVAL;
        boolean wasSealed = sealed();
        if (wasSealed && model.hydrogenFraction() < TRACE) {
            forget();
            return;
        }
        ContainmentShape next = measure(level, reactor);
        if (next == null) {
            if (wasSealed) {
                double held = model.depressurize();
                if (held > 0) {
                    release.release(level, centre(bounds()), held);
                }
            }
            forget();
            return;
        }
        if (wasSealed) {
            model.resize(next.volume(), next.wallArea());
        } else {
            model.reset(next.volume(), next.wallArea());
        }
        shape = next;
        savedBounds = next.bounds();
        restoredSealed = false;
    }

    /** The sealed room around the reactor, or null if it stands in the open. */
    @Nullable
    private static ContainmentShape measure(ServerLevel level, BoundingBox reactor) {
        int tried = 0;
        SealedVolume last = null;
        for (BlockPos seed : seeds(reactor)) {
            if (tried >= MAX_SEEDS) {
                break;
            }
            if (level.isOutsideBuildHeight(seed) || !level.isLoaded(seed) || isWall(level, seed)
                    || (last != null && last.contains(seed.getX(), seed.getY(), seed.getZ()))) {
                continue;
            }
            tried++;
            SealedVolume fill = ContainmentSurvey.measure(level, seed, PwrConfig.buildingMaxVolume);
            if (fill.state() != SealedVolume.State.SEALED) {
                // this air reaches the outside: the reactor is in the open
                return null;
            }
            last = fill;
            ContainmentShape shape = ContainmentShape.analyze(level, fill);
            if (shape.encloses(reactor)) {
                return shape;
            }
        }
        return null;
    }

    /** Cells next to the vessel to start the measurement from: above the lid first, centre outwards, then beside it. */
    private static List<BlockPos> seeds(BoundingBox reactor) {
        int cx = (reactor.minX() + reactor.maxX()) / 2, cy = (reactor.minY() + reactor.maxY()) / 2, cz = (reactor.minZ() + reactor.maxZ()) / 2;
        int top = reactor.maxY() + 1;
        java.util.ArrayList<BlockPos> seeds = new java.util.ArrayList<>();
        seeds.add(new BlockPos(cx, top, cz));
        for (int x = reactor.minX(); x <= reactor.maxX(); x++) {
            for (int z = reactor.minZ(); z <= reactor.maxZ(); z++) {
                if (x != cx || z != cz) {
                    seeds.add(new BlockPos(x, top, z));
                }
            }
        }
        seeds.add(new BlockPos(reactor.minX() - 1, cy, cz));
        seeds.add(new BlockPos(reactor.maxX() + 1, cy, cz));
        seeds.add(new BlockPos(cx, cy, reactor.minZ() - 1));
        seeds.add(new BlockPos(cx, cy, reactor.maxZ() + 1));
        return seeds;
    }

    /** Whether gas stops at this block, as the containment survey sees it. */
    private static boolean isWall(Level level, BlockPos pos) {
        return level.getBlockState(pos).isCollisionShapeFullBlock(level, pos);
    }

    private void forget() {
        shape = null;
        savedBounds = null;
        restoredSealed = false;
        burnTimer = 0;
        model.reset(1, 0);
    }

    // ------------------------------------------------------------------ giving way

    /**
     * The hydrogen burns: sirens, an incident, flames and blast damage on whoever is inside, an
     * explosion at the roof that tears it open, and the damaged fuel's activity thrown out. The
     * building is gone after that.
     */
    private boolean explode(ServerLevel level, double claddingDamage, int rods, Release release) {
        double rise = model.burn();
        if (rise <= 0) {
            return false;
        }
        explosions++;
        BlockPos roof = roofCentre(level);
        double power = blastPower(rise, PwrConfig.buildingMaxExplosionPower);
        PlantAlarm.raise(level, roof);
        IncidentLog.record(level, roof, IncidentType.HYDROGEN_EXPLOSION, ines(claddingDamage), "");

        BoundingBox box = shape.bounds();
        float hurt = (float) Math.min(MAX_BURN_DAMAGE, 4.0 + 6.0 * rise);
        AABB area = new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1);
        List<LivingEntity> inside = level.getEntitiesOfClass(LivingEntity.class, area);
        for (LivingEntity entity : inside) {
            // inside a building aboard a ship means inside it in the ship's plot
            if (shape.contains(BlockPos.containing(ShipSpace.localView(level, roof, entity.position())))) {
                entity.hurt(level.damageSources().explosion(null, null), hurt);
                entity.igniteForSeconds(5.0f);
            }
        }
        NuclearAdvancements.awardNearby(level, roof, NuclearAdvancements.WITNESS_RADIUS, NuclearEvent.HYDROGEN_EXPLOSION);

        for (int i = 0; i < 12; i++) {
            double x = box.minX() + level.random.nextDouble() * (box.getXSpan());
            double y = box.minY() + level.random.nextDouble() * (box.getYSpan());
            double z = box.minZ() + level.random.nextDouble() * (box.getZSpan());
            level.sendParticles(ParticleTypes.FLAME, x, y, z, 8, 0.6, 0.6, 0.6, 0.05);
        }
        level.playSound(null, roof, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 6.0f, 0.5f);
        if (PwrConfig.blockDestruction) {
            breakRoof(level, roof, roofRadius(power));
        }
        level.explode(null, roof.getX() + 0.5, roof.getY() + 0.5, roof.getZ() + 0.5, (float) power,
                PwrConfig.blockDestruction ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);

        double activity = model.depressurize()
                + Math.max(0.0, claddingDamage) * Math.max(0, rods) * ReleasePath.ACTIVITY_PER_ROD * PwrConfig.buildingExplosionRelease;
        forget();
        surveyTimer = SURVEY_INTERVAL;
        if (activity > 0) {
            release.release(level, roof, activity);
        }
        return true;
    }

    /** A leak overpressured the room without a burn: its weakest wall gives way and the gas escapes. */
    private void burst(ServerLevel level, Release release) {
        List<BlockPos> wall = shape.weakestWall();
        BlockPos at = wall.isEmpty() ? roofCentre(level) : wall.get(0);
        double held = model.depressurize();
        PlantAlarm.raise(level, at);
        level.sendParticles(ParticleTypes.CLOUD, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 40, 1.5, 1.5, 1.5, 0.2);
        level.playSound(null, at, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0f, 0.6f);
        if (PwrConfig.blockDestruction) {
            for (BlockPos layer : wall) {
                BlockState state = level.getBlockState(layer);
                if (!state.isAir() && state.getDestroySpeed(level, layer) >= 0) {
                    level.destroyBlock(layer, false);
                }
            }
        }
        forget();
        surveyTimer = SURVEY_INTERVAL;
        if (held > 0) {
            release.release(level, at, held);
        }
    }

    /** Breaks the breakable roof blocks over the building's air within {@code radius} columns of {@code centre}. */
    private void breakRoof(ServerLevel level, BlockPos centre, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                BlockPos cell = roofOver(centre.getX() + dx, centre.getZ() + dz);
                if (cell == null) {
                    continue;
                }
                BlockState state = level.getBlockState(cell);
                if (!state.isAir() && state.getDestroySpeed(level, cell) >= 0) {
                    level.destroyBlock(cell, false);
                }
            }
        }
    }

    /** The roof block over column ({@code x}, {@code z}): the highest wall with the building's air right below it. */
    @Nullable
    private BlockPos roofOver(int x, int z) {
        BoundingBox box = shape.bounds();
        for (int y = box.maxY() + 1; y > box.minY(); y--) {
            BlockPos cell = new BlockPos(x, y, z);
            if (shape.isWall(cell) && shape.contains(cell.below())) {
                return cell;
            }
        }
        return null;
    }

    /** The middle of the roof: the roof block over the centre of the air, or just above the air if that column is blocked. */
    private BlockPos roofCentre(ServerLevel level) {
        BoundingBox box = bounds();
        if (box == null) {
            return BlockPos.ZERO;
        }
        int cx = (box.minX() + box.maxX()) / 2, cz = (box.minZ() + box.maxZ()) / 2;
        BlockPos roof = shape == null ? null : roofOver(cx, cz);
        return roof != null ? roof : new BlockPos(cx, box.maxY() + 1, cz);
    }

    private static BlockPos centre(@Nullable BoundingBox box) {
        return box == null ? BlockPos.ZERO : new BlockPos((box.minX() + box.maxX()) / 2, (box.minY() + box.maxY()) / 2,
                (box.minZ() + box.maxZ()) / 2);
    }

    // ------------------------------------------------------------------ persistence

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        boolean sealed = sealed();
        tag.putBoolean("Sealed", sealed);
        if (sealed) {
            tag.putDouble("Volume", model.volume());
            tag.putDouble("Walls", model.wallArea());
            tag.putDouble("Inert", model.inert());
            tag.putDouble("Oxygen", model.oxygen());
            tag.putDouble("Steam", model.steam());
            tag.putDouble("Hydrogen", model.hydrogen());
            tag.putDouble("Temperature", model.temperature());
            tag.putDouble("Activity", model.activity());
            BoundingBox box = bounds();
            if (box != null) {
                tag.putIntArray("Bounds", new int[]{box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()});
            }
        }
        return tag;
    }

    /** Restores a saved building; a sealed one with hydrogen in it is measured again on the next tick. */
    public void load(CompoundTag tag) {
        shape = null;
        burnTimer = 0;
        surveyTimer = 0;
        if (!tag.getBoolean("Sealed") || !(tag.getDouble("Hydrogen") > 0)) {
            forget();
            return;
        }
        model.restore(tag.getDouble("Volume"), tag.getDouble("Walls"), tag.getDouble("Inert"), tag.getDouble("Oxygen"),
                tag.getDouble("Steam"), tag.getDouble("Hydrogen"), tag.getDouble("Temperature"), tag.getDouble("Activity"));
        int[] b = tag.getIntArray("Bounds");
        savedBounds = b.length == 6 ? new BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]) : null;
        restoredSealed = true;
    }
}
