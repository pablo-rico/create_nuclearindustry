package org.papiricoh.create_nuclearindustry.missile.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.civildefense.ThreatRegistry;
import org.papiricoh.create_nuclearindustry.explosive.DirtyWarheadStats;
import org.papiricoh.create_nuclearindustry.explosive.NuclearDetonation;

/**
 * A missile in flight: it flies from its pad to its target along a ballistic arc (climb, cruise,
 * descent) and detonates there through {@link NuclearDetonation}. The server moves it; position and
 * rotation reach clients through the entity tracker, and clients only draw its trail.
 *
 * <p>The missile forces the chunk it is in, so it keeps flying far from players and resumes its
 * flight after a restart. That chunk is saved with the missile: one loaded again (after a restart
 * or once its chunk loads) still knows which chunk is its own and lets it go when it moves on or
 * detonates. Missiles saved before it was saved forced the chunk their flight had reached; one
 * adopts that chunk only when its save proves it is that one ({@link #adoptLegacyForcedChunk}),
 * so an admin's {@code /forceload} elsewhere is never released by a missile.
 */
public class MissileEntity extends Entity {
    private double startX, startY, startZ;
    private double targetX, targetY, targetZ;
    private float power;
    private int hRadius;
    private float intensity;
    /** Radioactivity of non-fissile material in the warhead, in rad/s. */
    private float salting;
    /** No fissile core: the warhead only scatters {@link #salting}. */
    private boolean dirty;
    private int flightTicks = 100;
    private int age;
    private double arcHeight = 60.0;
    // The forced chunk that follows the missile, so it keeps ticking far from every player.
    private int forcedChunkX = Integer.MIN_VALUE;
    private int forcedChunkZ = Integer.MIN_VALUE;
    /** Saved by a version that did not save its forced chunk; see {@link #adoptLegacyForcedChunk}. */
    private boolean legacyForcedChunk;

    public MissileEntity(EntityType<? extends MissileEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    /** Sets the flight and the warhead when a pad launches it (server side). */
    public void configure(Vec3 start, Vec3 target, float power, int hRadius, float intensity) {
        this.startX = start.x;
        this.startY = start.y;
        this.startZ = start.z;
        this.targetX = target.x;
        this.targetY = target.y;
        this.targetZ = target.z;
        this.power = power;
        this.hRadius = hRadius;
        this.intensity = intensity;
        this.age = 0;

        double horizontalDistance = Math.hypot(target.x - start.x, target.z - start.z);
        // Flight time and arc height grow with the distance, with minimums so the arc still shows.
        this.flightTicks = flightTicks(start, target);
        this.arcHeight = Mth.clamp(40 + horizontalDistance * 0.5, 50, 180);
        setPos(start.x, start.y, start.z);
    }

    /** Ticks a missile takes from {@code start} to {@code target}; launch pads use it to warn ahead of the launch. */
    public static int flightTicks(Vec3 start, Vec3 target) {
        double horizontalDistance = Math.hypot(target.x - start.x, target.z - start.z);
        return (int) Mth.clamp(40 + horizontalDistance * 0.8, 60, 600);
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            spawnTrail();
            return;
        }

        if (legacyForcedChunk && level() instanceof ServerLevel serverLevel) {
            adoptLegacyForcedChunk(serverLevel);
        }
        age++;
        double t = flightTicks <= 0 ? 1.0 : (double) age / flightTicks;
        if (t >= 1.0) {
            detonate();
            return;
        }

        reportThreat();
        Vec3 previous = position();
        Vec3 next = trajectory(t);
        setPos(next.x, next.y, next.z);
        updateRotation(next.subtract(previous));
        followChunk(next);
    }

    /** Tells sirens near the target how long they have; reported every tick, so it survives a reload. */
    private void reportThreat() {
        int radius = dirty ? DirtyWarheadStats.falloutRadius(salting) : NuclearDetonation.falloutRadius(hRadius);
        ThreatRegistry.report(level(), getUUID(), BlockPos.containing(targetX, targetY, targetZ), radius, flightTicks - age);
    }

    /** Keeps the missile's chunk forced (letting the last one go) so it never stops ticking. */
    private void followChunk(Vec3 pos) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int cx = Mth.floor(pos.x) >> 4;
        int cz = Mth.floor(pos.z) >> 4;
        if (cx == forcedChunkX && cz == forcedChunkZ) {
            return;
        }
        releaseForcedChunk(serverLevel);
        serverLevel.setChunkForced(cx, cz, true);
        forcedChunkX = cx;
        forcedChunkZ = cz;
    }

    private void releaseForcedChunk(ServerLevel serverLevel) {
        if (forcedChunkX != Integer.MIN_VALUE) {
            serverLevel.setChunkForced(forcedChunkX, forcedChunkZ, false);
            forcedChunkX = Integer.MIN_VALUE;
            forcedChunkZ = Integer.MIN_VALUE;
        }
    }

    /**
     * Takes the chunk a missile saved by an older version forced as its own, so it is released like
     * any other. The older version forced, after each tick of flight, the chunk of
     * {@link #trajectoryAt(int)} for the missile's age; the chunk is adopted only when the save shows
     * exactly that: the missile has flown at least one tick, stands where its own flight put it (not
     * moved by a command since), and that chunk is still forced. Anything else (a missile that never
     * ticked, one displaced, a chunk forced by someone else) is left alone: a stray forced chunk can
     * be freed with {@code /forceload remove}, an admin's released one could not be told apart.
     * Returns whether a chunk was adopted. Public for tests.
     */
    public boolean adoptLegacyForcedChunk(ServerLevel serverLevel) {
        legacyForcedChunk = false;
        if (forcedChunkX != Integer.MIN_VALUE || age < 1 || flightTicks <= 0 || age >= flightTicks) {
            return false;
        }
        Vec3 flown = trajectoryAt(age);
        if (flown.distanceToSqr(position()) > 1.0e-6) {
            return false;
        }
        int cx = Mth.floor(flown.x) >> 4;
        int cz = Mth.floor(flown.z) >> 4;
        if (!serverLevel.getForcedChunks().contains(ChunkPos.asLong(cx, cz))) {
            return false;
        }
        forcedChunkX = cx;
        forcedChunkZ = cz;
        return true;
    }

    /** Where the flight puts the missile after {@code ticks} ticks. */
    public Vec3 trajectoryAt(int ticks) {
        return trajectory(flightTicks <= 0 ? 1.0 : (double) ticks / flightTicks);
    }

    /** The chunk the missile keeps forced, or null; for tests. */
    @Nullable
    public ChunkPos getForcedChunk() {
        return forcedChunkX == Integer.MIN_VALUE ? null : new ChunkPos(forcedChunkX, forcedChunkZ);
    }

    private Vec3 trajectory(double t) {
        double x = Mth.lerp(t, startX, targetX);
        double z = Mth.lerp(t, startZ, targetZ);
        double base = Mth.lerp(t, startY, targetY);
        double y = base + 4.0 * arcHeight * t * (1.0 - t);
        return new Vec3(x, y, z);
    }

    private void updateRotation(Vec3 velocity) {
        double horizontal = Math.hypot(velocity.x, velocity.z);
        if (velocity.lengthSqr() < 1.0e-6) {
            return;
        }
        setYRot((float) (Mth.atan2(-velocity.x, velocity.z) * (180.0 / Math.PI)));
        setXRot((float) (-Mth.atan2(velocity.y, horizontal) * (180.0 / Math.PI)));
    }

    private void spawnTrail() {
        Vec3 pos = position();
        for (int i = 0; i < 2; i++) {
            double ox = (random.nextDouble() - 0.5) * 0.3;
            double oz = (random.nextDouble() - 0.5) * 0.3;
            level().addParticle(ParticleTypes.SMOKE, pos.x + ox, pos.y, pos.z + oz, 0.0, -0.02, 0.0);
        }
        level().addParticle(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 0.0, -0.05, 0.0);
    }

    /** A dirty warhead: no fissile core, only {@code load} rad/s of radioactive material. */
    public void configureDirty(Vec3 start, Vec3 target, float load) {
        configure(start, target, 0, 0, 0);
        this.dirty = true;
        this.salting = load;
    }

    public void setSalting(float load) {
        this.salting = Math.max(0, load);
    }

    public boolean isDirty() {
        return dirty;
    }

    public float getSalting() {
        return salting;
    }

    private void detonate() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            discard();
            return;
        }
        releaseForcedChunk(serverLevel);
        ThreatRegistry.withdraw(serverLevel, getUUID());
        Vec3 impact = new Vec3(targetX, targetY, targetZ);
        if (dirty) {
            NuclearDetonation.detonateDirty(serverLevel, impact, salting);
        } else {
            NuclearDetonation.detonate(serverLevel, impact, power, hRadius, intensity, salting);
        }
        discard();
    }

    /**
     * Leaving the level for good (detonated, killed, discarded, sent to another dimension) lets the
     * forced chunk go. Unloading with its chunk keeps it: the missile is saved with it and resumes.
     */
    @Override
    public void onRemovedFromLevel() {
        RemovalReason reason = getRemovalReason();
        if (level() instanceof ServerLevel serverLevel && reason != null
                && (reason.shouldDestroy() || reason == RemovalReason.CHANGED_DIMENSION)) {
            releaseForcedChunk(serverLevel);
            ThreatRegistry.withdraw(serverLevel, getUUID());
        }
        super.onRemovedFromLevel();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // No synced data: the entity tracker carries position and rotation.
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        startX = tag.getDouble("StartX");
        startY = tag.getDouble("StartY");
        startZ = tag.getDouble("StartZ");
        targetX = tag.getDouble("TargetX");
        targetY = tag.getDouble("TargetY");
        targetZ = tag.getDouble("TargetZ");
        power = tag.getFloat("Power");
        hRadius = tag.getInt("HRadius");
        intensity = tag.getFloat("Intensity");
        salting = tag.getFloat("Salting");
        dirty = tag.getBoolean("Dirty");
        flightTicks = tag.getInt("FlightTicks");
        age = tag.getInt("Age");
        arcHeight = tag.getDouble("ArcHeight");
        if (tag.contains("ForcedChunk")) {
            ChunkPos forced = new ChunkPos(tag.getLong("ForcedChunk"));
            forcedChunkX = forced.x;
            forcedChunkZ = forced.z;
            legacyForcedChunk = false;
        } else {
            forcedChunkX = Integer.MIN_VALUE;
            forcedChunkZ = Integer.MIN_VALUE;
            // "NoForcedChunk" marks a missile saved by this version that had none; older ones did not say.
            legacyForcedChunk = !tag.getBoolean("NoForcedChunk");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("StartX", startX);
        tag.putDouble("StartY", startY);
        tag.putDouble("StartZ", startZ);
        tag.putDouble("TargetX", targetX);
        tag.putDouble("TargetY", targetY);
        tag.putDouble("TargetZ", targetZ);
        tag.putFloat("Power", power);
        tag.putInt("HRadius", hRadius);
        tag.putFloat("Intensity", intensity);
        tag.putFloat("Salting", salting);
        tag.putBoolean("Dirty", dirty);
        tag.putInt("FlightTicks", flightTicks);
        tag.putInt("Age", age);
        tag.putDouble("ArcHeight", arcHeight);
        if (forcedChunkX != Integer.MIN_VALUE) {
            tag.putLong("ForcedChunk", ChunkPos.asLong(forcedChunkX, forcedChunkZ));
        } else {
            tag.putBoolean("NoForcedChunk", true);
        }
    }
}
