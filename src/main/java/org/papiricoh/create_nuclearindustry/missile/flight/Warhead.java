package org.papiricoh.create_nuclearindustry.missile.flight;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.explosive.DetonationCause;
import org.papiricoh.create_nuclearindustry.explosive.DirtyWarheadStats;
import org.papiricoh.create_nuclearindustry.explosive.conventional.ConventionalConfig;
import org.papiricoh.create_nuclearindustry.explosive.conventional.ConventionalDetonation;
import org.papiricoh.create_nuclearindustry.explosive.conventional.ConventionalKind;
import org.papiricoh.create_nuclearindustry.explosive.conventional.ConventionalProfile;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveConfig;
import org.papiricoh.create_nuclearindustry.explosive.NuclearDetonation;
import org.papiricoh.create_nuclearindustry.explosive.ThermonuclearLoad;
import org.papiricoh.create_nuclearindustry.explosive.ThermonuclearStats;

import java.util.Locale;

/**
 * What a missile carries down: a fission warhead (maybe salted), a dirty one, a thermonuclear one, a
 * high-explosive one, a cluster of bomblets, a thermobaric charge, or nothing at all (a test shot, which only lands).
 *
 * @param power      fission blast power
 * @param hRadius    fission destruction radius
 * @param intensity  fission fallout intensity
 * @param salting    radioactivity of non-fissile material packed in, rad/s: the whole load of a dirty warhead
 * @param thermo     a thermonuclear warhead's parts
 * @param charge     a non-nuclear warhead's charge, TNT-block equivalents (all its bomblets' for a cluster)
 * @param bomblets   how many bomblets a cluster warhead frees
 */
public record Warhead(Kind kind, float power, int hRadius, float intensity, float salting, ThermonuclearLoad thermo, float charge,
                      int bomblets) {
    public static final Warhead INERT = new Warhead(Kind.INERT, 0, 0, 0, 0, ThermonuclearLoad.EMPTY, 0, 0);

    public enum Kind {
        // Saved by name: append only.
        INERT, FISSION, DIRTY, THERMONUCLEAR, CONVENTIONAL, CLUSTER, THERMOBARIC
    }

    /** Whether a warhead may be set to go off in the air over its target. */
    public enum AirburstMode {
        /** The operator chooses: in the air its wave reaches further, on the ground it digs a crater. */
        CHOICE,
        /** Always in the air: a cluster warhead opening, a thermobaric cloud lighting at its best height. */
        ALWAYS,
        /** Only on the ground: a dirty bomb, or nothing to set off. */
        NEVER
    }

    public static Warhead fission(float power, int hRadius, float intensity, float salting) {
        return new Warhead(Kind.FISSION, power, hRadius, intensity, salting, ThermonuclearLoad.EMPTY, 0, 0);
    }

    public static Warhead dirty(float load) {
        return new Warhead(Kind.DIRTY, 0, 0, 0, load, ThermonuclearLoad.EMPTY, 0, 0);
    }

    public static Warhead thermonuclear(ThermonuclearLoad load) {
        return new Warhead(Kind.THERMONUCLEAR, 0, 0, 0, 0, load, 0, 0);
    }

    /** High explosive of {@code charge} TNT-block equivalents: a fireball, a crater, fragments and a shock wave, no radiation. */
    public static Warhead conventional(float charge) {
        return new Warhead(Kind.CONVENTIONAL, 0, 0, 0, 0, ThermonuclearLoad.EMPTY, Math.max(0, charge), 0);
    }

    /** {@code bomblets} bomblets of {@code charge} TNT-block equivalents in all, freed high over the target. */
    public static Warhead cluster(int bomblets, float charge) {
        return new Warhead(Kind.CLUSTER, 0, 0, 0, 0, ThermonuclearLoad.EMPTY, Math.max(0, charge), Math.max(0, bomblets));
    }

    /** A thermobaric charge of {@code charge} TNT-block equivalents: a cloud of fuel lit in the air over the target. */
    public static Warhead thermobaric(float charge) {
        return new Warhead(Kind.THERMOBARIC, 0, 0, 0, 0, ThermonuclearLoad.EMPTY, Math.max(0, charge), 0);
    }

    /** Whether it may, must or cannot go off in the air. */
    public AirburstMode airburstMode() {
        return airburstMode(kind);
    }

    /** Whether a warhead of {@code kind} may, must or cannot go off in the air. */
    public static AirburstMode airburstMode(Kind kind) {
        return switch (kind) {
            case FISSION, THERMONUCLEAR, CONVENTIONAL -> AirburstMode.CHOICE;
            case CLUSTER, THERMOBARIC -> AirburstMode.ALWAYS;
            case DIRTY, INERT -> AirburstMode.NEVER;
        };
    }

    /** Whether it goes off in the air when the operator asked for an airburst or not. */
    public boolean bursts(boolean airburstAsked) {
        return switch (airburstMode()) {
            case CHOICE -> airburstAsked;
            case ALWAYS -> true;
            case NEVER -> false;
        };
    }

    /** How high over its target it goes off, blocks: 0 on the ground. */
    public double burstHeight(boolean airburstAsked) {
        if (!bursts(airburstAsked)) {
            return 0;
        }
        return switch (kind) {
            case FISSION, THERMONUCLEAR -> org.papiricoh.create_nuclearindustry.missile.item.TargetDesignatorItem.airburstHeight(destructionRadius());
            case CONVENTIONAL -> ConventionalProfile.airburstHeight(ConventionalKind.HIGH_EXPLOSIVE, charge, ConventionalConfig.settings(),
                    ConventionalConfig.airburstHeightFactor);
            case THERMOBARIC -> ConventionalProfile.airburstHeight(ConventionalKind.THERMOBARIC, charge, ConventionalConfig.settings(),
                    ConventionalConfig.thermobaricHeightFactor);
            case CLUSTER -> ConventionalConfig.clusterOpenHeight;
            case DIRTY, INERT -> 0;
        };
    }

    /** How its blast goes off, for a non-nuclear warhead set to {@code airburstAsked}: null for a nuclear or inert one. */
    public @Nullable ConventionalProfile blast(boolean airburstAsked) {
        return switch (kind) {
            case CONVENTIONAL -> ConventionalProfile.of(ConventionalKind.HIGH_EXPLOSIVE, charge, burstHeight(airburstAsked),
                    ConventionalConfig.settings());
            case THERMOBARIC -> ConventionalProfile.of(ConventionalKind.THERMOBARIC, charge, burstHeight(airburstAsked),
                    ConventionalConfig.settings());
            case CLUSTER -> ConventionalProfile.of(ConventionalKind.BOMBLET, bomblets > 0 ? charge / bomblets : 0, 0,
                    ConventionalConfig.settings());
            default -> null;
        };
    }

    /** The destruction radius, blocks, which an airburst is set above. */
    public int destructionRadius() {
        return switch (kind) {
            case FISSION -> hRadius;
            case THERMONUCLEAR -> thermo.stats(ExplosiveConfig.thermonuclearCaps()).radius();
            default -> 0;
        };
    }

    /**
     * How far around the impact sirens should sound: as far as its fallout or its blast wave reaches, whichever is further;
     * for a cluster, the ground its bomblets scatter over and their own waves.
     */
    public int threatRadius() {
        return switch (kind) {
            case INERT -> 0;
            case FISSION -> Math.max(NuclearDetonation.falloutRadius(hRadius), salting > 0 ? DirtyWarheadStats.falloutRadius(salting) : 0);
            case DIRTY -> DirtyWarheadStats.falloutRadius(salting);
            case THERMONUCLEAR -> thermo.stats(ExplosiveConfig.thermonuclearCaps()).falloutRadius();
            case CONVENTIONAL, THERMOBARIC -> (int) Math.ceil(Math.max(blast(false).waveRadius(), blast(true).waveRadius()));
            case CLUSTER -> (int) Math.ceil(ConventionalConfig.clusterFootprintRadius + blast(true).waveRadius());
        };
    }

    /** Whether there is anything to go off: a dirty warhead has no yield, only its load. */
    public boolean nuclear() {
        return kind == Kind.FISSION || kind == Kind.THERMONUCLEAR;
    }

    /** Sets it off at {@code at}, nobody's doing. */
    public void detonate(ServerLevel level, Vec3 at) {
        detonate(level, at, Vec3.ZERO, DetonationCause.NONE);
    }

    /** Sets it off at {@code at}, coming in at {@code velocity} (blocks per tick), its harm {@code cause}'s. */
    public void detonate(ServerLevel level, Vec3 at, Vec3 velocity, DetonationCause cause) {
        switch (kind) {
            case INERT -> {
            }
            case FISSION -> NuclearDetonation.detonate(level, at, power, hRadius, intensity, salting, cause);
            case DIRTY -> NuclearDetonation.detonateDirty(level, at, salting, cause);
            case THERMONUCLEAR -> {
                ThermonuclearStats.Result stats = thermo.stats(ExplosiveConfig.thermonuclearCaps());
                NuclearDetonation.detonateThermonuclear(level, at, stats, cause);
            }
            case CONVENTIONAL -> ConventionalDetonation.detonate(level, at, ConventionalKind.HIGH_EXPLOSIVE, charge, cause);
            case THERMOBARIC -> ConventionalDetonation.detonate(level, at, ConventionalKind.THERMOBARIC, charge, cause);
            case CLUSTER -> org.papiricoh.create_nuclearindustry.missile.cluster.ClusterDispenser.open(level, at, velocity, bomblets,
                    bomblets > 0 ? charge / bomblets : 0, cause);
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Kind", kind.name().toLowerCase(Locale.ROOT));
        tag.putFloat("Power", power);
        tag.putInt("HRadius", hRadius);
        tag.putFloat("Intensity", intensity);
        tag.putFloat("Salting", salting);
        tag.putFloat("Charge", charge);
        tag.putInt("Bomblets", bomblets);
        if (kind == Kind.THERMONUCLEAR) {
            CompoundTag thermoTag = new CompoundTag();
            thermo.write(thermoTag);
            tag.put("Thermonuclear", thermoTag);
        }
        return tag;
    }

    public static Warhead load(CompoundTag tag) {
        Kind kind;
        try {
            kind = Kind.valueOf(tag.getString("Kind").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            kind = Kind.INERT;
        }
        ThermonuclearLoad thermo = kind == Kind.THERMONUCLEAR ? ThermonuclearLoad.read(tag.getCompound("Thermonuclear")) : ThermonuclearLoad.EMPTY;
        float charge = tag.getFloat("Charge");
        if (kind == Kind.CONVENTIONAL && !tag.contains("Charge")) {
            // Saved when its power was 4 times the cube root of the TNT loaded.
            double tnt = tag.getFloat("Power") / 4.0;
            charge = (float) (tnt * tnt * tnt);
        }
        float power = kind == Kind.CONVENTIONAL ? 0 : tag.getFloat("Power");
        return new Warhead(kind, power, tag.getInt("HRadius"), tag.getFloat("Intensity"), tag.getFloat("Salting"), thermo, charge,
                tag.getInt("Bomblets"));
    }
}
