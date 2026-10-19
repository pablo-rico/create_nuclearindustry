package org.papiricoh.create_nuclearindustry.missile.flight;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileFlight;
import org.papiricoh.create_nuclearindustry.missile.physics.RocketAttitude;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * One object in flight, kept by the {@link FlightTracker} of its level: its flight, what it carries and
 * the target it was sent to. The entity players see is only a puppet the tracker shows near them.
 */
public final class Flight {

    public enum Kind {
        MISSILE;

        static Kind byName(String name) {
            try {
                return valueOf(name.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return MISSILE;
            }
        }
    }

    public static final long NO_CHUNK = Long.MIN_VALUE;

    private final UUID id;
    private final Kind kind;
    private final MissileFlight flight;
    private final Warhead warhead;
    /** The block it was sent to; the aim point is above it for an airburst and off it by the guidance's miss. */
    private final BlockPos target;
    @Nullable
    private final UUID owner;
    private final long launchedAt;
    @Nullable private final BlockPos origin;
    private final List<String> visualSections;
    private final List<Integer> visualStageStarts;
    /** Its outline for the client effects, in its own frame. */
    private final RocketShape shape;
    /** What a silo assembled it from (a saved Create contraption), to show it as it was built; null for older flights. */
    @Nullable
    private final CompoundTag cargo;
    /** Radians, the way it was launched over the ground (0 is +Z): its heading while it climbs straight up. */
    private final float heading;
    /** The phase it was in last tick, to tell when it changes; not saved. */
    @Nullable
    MissileFlight.Phase lastPhase;
    /** The chunk forced so the impact lands in a loaded world, or {@link #NO_CHUNK}. */
    long forcedChunk = NO_CHUNK;
    /** Its cargo could not be read back into a contraption (a removed block, say): shown as a missile instead; not saved. */
    boolean cargoUnreadable;
    /** The puppet entity showing it, while one is shown; never saved. */
    @Nullable
    UUID puppet;

    Flight(UUID id, Kind kind, MissileFlight flight, Warhead warhead, BlockPos target, @Nullable UUID owner, long launchedAt) {
        this(id, kind, flight, warhead, target, owner, launchedAt, null, List.of(), List.of(), RocketShape.LEGACY, null,
                headingOf(flight));
    }

    Flight(UUID id, Kind kind, MissileFlight flight, Warhead warhead, BlockPos target, @Nullable UUID owner,
           long launchedAt, @Nullable BlockPos origin, List<String> sections, List<Integer> stageStarts,
           RocketShape shape, @Nullable CompoundTag cargo, float heading) {
        this.shape = shape;
        this.cargo = cargo;
        this.heading = heading;
        this.origin = origin == null ? null : origin.immutable();
        this.visualSections = List.copyOf(sections);
        this.visualStageStarts = List.copyOf(stageStarts);
        this.id = id;
        this.kind = kind;
        this.flight = flight;
        this.warhead = warhead;
        this.target = target;
        this.owner = owner;
        this.launchedAt = launchedAt;
    }

    @Nullable public BlockPos origin() { return origin; }
    public RocketShape shape() { return shape; }
    @Nullable public CompoundTag cargo() { return cargo; }
    public float heading() { return heading; }

    /** The heading from where a flight is towards its aim, radians (0 is +Z). */
    static float headingOf(MissileFlight flight) {
        return (float) RocketAttitude.heading(flight.aim().x() - flight.position().x(), flight.aim().z() - flight.position().z());
    }
    public List<String> visualSections() { return visualSections; }
    public List<Integer> visualStageStarts() { return visualStageStarts; }

    /** Offset of the remaining assembly; after the final stage only guidance and warhead remain. */
    public int firstVisualSection() {
        if (visualSections.isEmpty() || visualStageStarts.isEmpty()) return 0;
        int stage = flight.stage();
        return stage >= visualStageStarts.size() ? Math.max(0, visualSections.size() - 2)
                : visualStageStarts.get(Math.max(0, stage));
    }

    public UUID id() {
        return id;
    }

    public Kind kind() {
        return kind;
    }

    public MissileFlight flight() {
        return flight;
    }

    public Warhead warhead() {
        return warhead;
    }

    public BlockPos target() {
        return target;
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    public long launchedAt() {
        return launchedAt;
    }

    public Vec3 position() {
        return vec(flight.position());
    }

    public Vec3 velocity() {
        return vec(flight.velocity());
    }

    public Vec3 aim() {
        return vec(flight.aim());
    }

    public static Vec3 vec(V3 v) {
        return new Vec3(v.x(), v.y(), v.z());
    }

    public static V3 v3(Vec3 v) {
        return new V3(v.x, v.y, v.z);
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("FormatVersion", 2);
        if (origin != null) tag.putLong("Origin", origin.asLong());
        ListTag visuals = new ListTag();
        visualSections.forEach(section -> visuals.add(net.minecraft.nbt.StringTag.valueOf(section)));
        tag.put("VisualSections", visuals);
        tag.putIntArray("VisualStageStarts", visualStageStarts);
        tag.put("Shape", RocketShapeCodec.save(shape));
        if (cargo != null) tag.put("Cargo", cargo);
        tag.putFloat("Heading", heading);
        tag.putUUID("Id", id);
        tag.putString("Kind", kind.name().toLowerCase(Locale.ROOT));
        tag.put("Design", saveDesign(flight.design()));
        putV3(tag, "Aim", flight.aim());
        tag.putDouble("LaunchY", flight.launchY());
        putV3(tag, "Pos", flight.position());
        putV3(tag, "Vel", flight.velocity());
        tag.putInt("Stage", flight.stage());
        tag.putDouble("StagePropellant", flight.stagePropellant());
        tag.putInt("Tick", flight.tick());
        tag.putBoolean("BurnedOut", flight.burnedOut());
        tag.putBoolean("Guided", flight.guidedCutoff());
        tag.putInt("BurnoutTick", flight.burnoutTick());
        putV3(tag, "BurnoutPos", flight.burnoutPosition());
        putV3(tag, "BurnoutVel", flight.burnoutVelocity());
        tag.putDouble("FallTicks", flight.fallTicks());
        tag.putBoolean("Impacted", flight.impacted());
        tag.put("Warhead", warhead.save());
        tag.putLong("Target", target.asLong());
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.putLong("LaunchedAt", launchedAt);
        if (forcedChunk != NO_CHUNK) {
            tag.putLong("ForcedChunk", forcedChunk);
        }
        return tag;
    }

    static Flight load(CompoundTag tag) {
        MissileFlight flight = MissileFlight.restore(loadDesign(tag.getCompound("Design")), getV3(tag, "Aim"), tag.getDouble("LaunchY"), getV3(tag, "Pos"),
                getV3(tag, "Vel"), tag.getInt("Stage"), tag.getDouble("StagePropellant"), tag.getInt("Tick"),
                tag.getBoolean("BurnedOut"), tag.getBoolean("Guided"), tag.getInt("BurnoutTick"), getV3(tag, "BurnoutPos"),
                getV3(tag, "BurnoutVel"), tag.getDouble("FallTicks"), tag.getBoolean("Impacted"));
        List<String> sections = tag.getList("VisualSections", Tag.TAG_STRING).stream().map(Tag::getAsString).limit(17).toList();
        List<Integer> starts = java.util.Arrays.stream(tag.getIntArray("VisualStageStarts")).filter(i -> i >= 0 && i < 17).limit(3).boxed().toList();
        // Flights saved before shapes were: a silo column from its sections, else the old pad missile.
        RocketShape shape = tag.contains("Shape", Tag.TAG_COMPOUND) ? RocketShapeCodec.load(tag.getCompound("Shape"))
                : sections.isEmpty() ? RocketShape.LEGACY : RocketShape.column(sections.size(), starts);
        Flight loaded = new Flight(tag.getUUID("Id"), Kind.byName(tag.getString("Kind")), flight, Warhead.load(tag.getCompound("Warhead")),
                BlockPos.of(tag.getLong("Target")), tag.hasUUID("Owner") ? tag.getUUID("Owner") : null, tag.getLong("LaunchedAt"),
                tag.contains("Origin", Tag.TAG_LONG) ? BlockPos.of(tag.getLong("Origin")) : null, sections, starts, shape,
                tag.contains("Cargo", Tag.TAG_COMPOUND) ? tag.getCompound("Cargo") : null,
                tag.contains("Heading", Tag.TAG_FLOAT) ? tag.getFloat("Heading") : headingOf(flight));
        loaded.forcedChunk = tag.contains("ForcedChunk") ? tag.getLong("ForcedChunk") : NO_CHUNK;
        return loaded;
    }

    public static CompoundTag saveDesign(MissileDesign design) {
        CompoundTag tag = new CompoundTag();
        ListTag stages = new ListTag();
        for (MissileDesign.Stage stage : design.stages()) {
            CompoundTag s = new CompoundTag();
            s.putDouble("Dry", stage.dryMass());
            s.putDouble("Propellant", stage.propellant());
            s.putDouble("Thrust", stage.thrust());
            s.putDouble("Exhaust", stage.exhaustVelocity());
            stages.add(s);
        }
        tag.put("Stages", stages);
        tag.putDouble("Payload", design.payloadMass());
        tag.putDouble("Cep", design.cep());
        return tag;
    }

    public static MissileDesign loadDesign(CompoundTag tag) {
        List<MissileDesign.Stage> stages = new ArrayList<>();
        for (Tag t : tag.getList("Stages", Tag.TAG_COMPOUND)) {
            CompoundTag s = (CompoundTag) t;
            stages.add(new MissileDesign.Stage(s.getDouble("Dry"), s.getDouble("Propellant"), s.getDouble("Thrust"), s.getDouble("Exhaust")));
        }
        return new MissileDesign(stages, tag.getDouble("Payload"), tag.getDouble("Cep"));
    }

    private static void putV3(CompoundTag tag, String key, V3 v) {
        ListTag list = new ListTag();
        list.add(net.minecraft.nbt.DoubleTag.valueOf(v.x()));
        list.add(net.minecraft.nbt.DoubleTag.valueOf(v.y()));
        list.add(net.minecraft.nbt.DoubleTag.valueOf(v.z()));
        tag.put(key, list);
    }

    private static V3 getV3(CompoundTag tag, String key) {
        ListTag list = tag.getList(key, Tag.TAG_DOUBLE);
        if (list.size() < 3) {
            return V3.ZERO;
        }
        return new V3(list.getDouble(0), list.getDouble(1), list.getDouble(2));
    }
}
