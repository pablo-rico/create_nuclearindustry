package org.papiricoh.create_nuclearindustry.missile.flight;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.civildefense.ThreatRegistry;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.MissileRegistry;
import org.papiricoh.create_nuclearindustry.missile.entity.MissileEntity;
import org.papiricoh.create_nuclearindustry.missile.physics.BallisticModel;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileFlight;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Everything flying in a level, saved with it. Each flight is worked out here every tick from its
 * {@link MissileFlight}, with no entity and no chunks loaded, so a missile crosses the world without
 * dragging its chunks along. Near players it shows as a puppet {@link MissileEntity}; where its chunk
 * is loaded it can hit the ground on the way; in its last moments it forces the chunk it comes down in,
 * so the impact always lands. Sirens are warned by every flight while it flies.
 */
public final class FlightTracker extends SavedData {
    private static final String NAME = "create_nuclearindustry_flights";
    /** Ticks before impact the impact's chunk is forced, so the warhead goes off in a loaded world. */
    private static final int FORCE_IMPACT_CHUNK_TICKS = 40;

    private final Map<UUID, Flight> flights = new LinkedHashMap<>();

    public static FlightTracker get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(FlightTracker::new, FlightTracker::load, null), NAME);
    }

    /** The level's tracker if it ever had one, else null; never creates it. */
    @Nullable
    public static FlightTracker getIfPresent(ServerLevel level) {
        return level.getDataStorage().get(new Factory<>(FlightTracker::new, FlightTracker::load, null), NAME);
    }

    public Collection<Flight> flights() {
        return Collections.unmodifiableCollection(flights.values());
    }

    @Nullable
    public Flight get(UUID id) {
        return flights.get(id);
    }

    /**
     * Sends {@code design} from {@code start} towards {@code aim}, the point it is to come down on (above
     * {@code target} for an airburst, or off it by the guidance's miss).
     */
    public Flight launch(ServerLevel level, MissileDesign design, Vec3 start, Vec3 velocity, Vec3 aim, BlockPos target,
                         Warhead warhead, @Nullable UUID owner) {
        MissileFlight flight = new MissileFlight(design, Flight.v3(start), Flight.v3(aim), Flight.v3(velocity));
        Flight launched = new Flight(UUID.randomUUID(), Flight.Kind.MISSILE, flight, warhead, target, owner, level.getGameTime());
        flights.put(launched.id(), launched);
        setDirty();
        report(level, launched, MissileConfig.flightParams());
        return launched;
    }

    /** Takes a flight out without it landing: its threat, puppet and forced chunk go with it. */
    public void remove(ServerLevel level, Flight flight) {
        if (flights.remove(flight.id()) != null) {
            release(level, flight);
            setDirty();
        }
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            FlightTracker tracker = getIfPresent(level);
            if (tracker != null && !tracker.flights.isEmpty()) {
                tracker.tick(level);
            }
        }
    }

    /** Flies everything one tick on. Public for tests and benchmarks; the level ticks it itself. */
    public void tick(ServerLevel level) {
        BallisticModel.Params params = MissileConfig.flightParams();
        for (Flight flight : List.copyOf(flights.values())) {
            Vec3 from = flight.position();
            MissileFlight.Step step = flight.flight().step(params);
            Vec3 impact = step.impact() ? flight.position() : null;
            if (impact == null && !flight.flight().rising(params)) {
                impact = groundHit(level, from, flight.position());
            }
            if (impact != null) {
                flights.remove(flight.id());
                release(level, flight);
                flight.warhead().detonate(level, impact);
                continue;
            }
            report(level, flight, params);
            forceImpactChunk(level, flight, params);
            showPuppet(level, flight);
        }
        setDirty();
    }

    private static void report(ServerLevel level, Flight flight, BallisticModel.Params params) {
        int ticks = Mth.ceil(flight.flight().ticksToImpact(params));
        ThreatRegistry.report(level, flight.id(), BlockPos.containing(flight.aim()), flight.warhead().threatRadius(), ticks);
    }

    private static void forceImpactChunk(ServerLevel level, Flight flight, BallisticModel.Params params) {
        if (flight.forcedChunk != Flight.NO_CHUNK || !flight.flight().burnedOut()
                || flight.flight().ticksToImpact(params) > FORCE_IMPACT_CHUNK_TICKS) {
            return;
        }
        ChunkPos chunk = new ChunkPos(BlockPos.containing(flight.aim()));
        if (!level.getForcedChunks().contains(chunk.toLong())) {
            level.setChunkForced(chunk.x, chunk.z, true);
            flight.forcedChunk = chunk.toLong();
        }
    }

    /**
     * Where the flight met the ground between {@code from} and {@code to}, or null. Only looked for where
     * every chunk on the way is loaded: a flight never loads one to find out.
     */
    @Nullable
    private static Vec3 groundHit(ServerLevel level, Vec3 from, Vec3 to) {
        if (to.y < level.getMinBuildHeight()) {
            return to;
        }
        if (Math.min(from.y, to.y) >= level.getMaxBuildHeight()) {
            return null;
        }
        int samples = Math.max(1, Mth.ceil(from.distanceTo(to) / 8.0));
        for (int i = 0; i <= samples; i++) {
            Vec3 at = from.lerp(to, i / (double) samples);
            if (!level.hasChunk(Mth.floor(at.x) >> 4, Mth.floor(at.z) >> 4)) {
                return null;
            }
        }
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : null;
    }

    /** Shows the flight as a puppet entity while a player is near and its chunk has its entities loaded. */
    private static void showPuppet(ServerLevel level, Flight flight) {
        Vec3 pos = flight.position();
        Entity puppet = flight.puppet == null ? null : level.getEntity(flight.puppet);
        BlockPos block = BlockPos.containing(pos);
        boolean loaded = pos.y >= level.getMinBuildHeight() && level.areEntitiesLoaded(ChunkPos.asLong(block));
        if (!loaded || !watched(level, pos)) {
            if (puppet != null) {
                puppet.discard();
            }
            flight.puppet = null;
            return;
        }
        if (puppet instanceof MissileEntity missile) {
            missile.follow(flight);
            return;
        }
        MissileEntity missile = new MissileEntity(MissileRegistry.MISSILE.get(), level);
        missile.follow(flight);
        if (level.addFreshEntity(missile)) {
            flight.puppet = missile.getUUID();
        }
    }

    private static boolean watched(ServerLevel level, Vec3 pos) {
        double range = MissileConfig.showDistance;
        for (ServerPlayer player : level.players()) {
            double dx = player.getX() - pos.x;
            double dz = player.getZ() - pos.z;
            if (dx * dx + dz * dz <= range * range) {
                return true;
            }
        }
        return false;
    }

    private static void release(ServerLevel level, Flight flight) {
        ThreatRegistry.withdraw(level, flight.id());
        if (flight.puppet != null) {
            Entity puppet = level.getEntity(flight.puppet);
            if (puppet != null) {
                puppet.discard();
            }
            flight.puppet = null;
        }
        if (flight.forcedChunk != Flight.NO_CHUNK) {
            ChunkPos chunk = new ChunkPos(flight.forcedChunk);
            level.setChunkForced(chunk.x, chunk.z, false);
            flight.forcedChunk = Flight.NO_CHUNK;
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        flights.values().forEach(flight -> list.add(flight.save()));
        tag.put("Flights", list);
        return tag;
    }

    /** Public for tests: a tracker as it would load from {@code tag}. */
    public static FlightTracker load(CompoundTag tag, HolderLookup.Provider registries) {
        FlightTracker tracker = new FlightTracker();
        for (Tag t : tag.getList("Flights", Tag.TAG_COMPOUND)) {
            Flight flight = Flight.load((CompoundTag) t);
            tracker.flights.put(flight.id(), flight);
        }
        return tracker;
    }
}
