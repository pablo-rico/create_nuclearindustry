package org.papiricoh.create_nuclearindustry.missile.flight;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.explosive.DirtyWarheadStats;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveConfig;
import org.papiricoh.create_nuclearindustry.explosive.NuclearDetonation;
import org.papiricoh.create_nuclearindustry.explosive.ThermonuclearLoad;
import org.papiricoh.create_nuclearindustry.explosive.ThermonuclearStats;

import java.util.Locale;

/**
 * What a missile carries down: a fission warhead (maybe salted), a dirty one, a thermonuclear one, or
 * nothing at all (a test shot, which only lands).
 *
 * @param power      fission blast power
 * @param hRadius    fission destruction radius
 * @param intensity  fission fallout intensity
 * @param salting    radioactivity of non-fissile material packed in, rad/s: the whole load of a dirty warhead
 * @param thermo     a thermonuclear warhead's parts
 */
public record Warhead(Kind kind, float power, int hRadius, float intensity, float salting, ThermonuclearLoad thermo) {
    public static final Warhead INERT = new Warhead(Kind.INERT, 0, 0, 0, 0, ThermonuclearLoad.EMPTY);

    public enum Kind {
        INERT, FISSION, DIRTY, THERMONUCLEAR
    }

    public static Warhead fission(float power, int hRadius, float intensity, float salting) {
        return new Warhead(Kind.FISSION, power, hRadius, intensity, salting, ThermonuclearLoad.EMPTY);
    }

    public static Warhead dirty(float load) {
        return new Warhead(Kind.DIRTY, 0, 0, 0, load, ThermonuclearLoad.EMPTY);
    }

    public static Warhead thermonuclear(ThermonuclearLoad load) {
        return new Warhead(Kind.THERMONUCLEAR, 0, 0, 0, 0, load);
    }

    /** The destruction radius, blocks, which an airburst is set above. */
    public int destructionRadius() {
        return switch (kind) {
            case FISSION -> hRadius;
            case THERMONUCLEAR -> thermo.stats(ExplosiveConfig.thermonuclearCaps()).radius();
            default -> 0;
        };
    }

    /** How far around the impact sirens should sound: as far as its fallout reaches. */
    public int threatRadius() {
        return switch (kind) {
            case INERT -> 0;
            case FISSION -> Math.max(NuclearDetonation.falloutRadius(hRadius), salting > 0 ? DirtyWarheadStats.falloutRadius(salting) : 0);
            case DIRTY -> DirtyWarheadStats.falloutRadius(salting);
            case THERMONUCLEAR -> thermo.stats(ExplosiveConfig.thermonuclearCaps()).falloutRadius();
        };
    }

    /** Whether there is anything to go off: a dirty warhead has no yield, only its load. */
    public boolean nuclear() {
        return kind == Kind.FISSION || kind == Kind.THERMONUCLEAR;
    }

    public void detonate(ServerLevel level, Vec3 at) {
        switch (kind) {
            case INERT -> {
            }
            case FISSION -> NuclearDetonation.detonate(level, at, power, hRadius, intensity, salting);
            case DIRTY -> NuclearDetonation.detonateDirty(level, at, salting);
            case THERMONUCLEAR -> {
                ThermonuclearStats.Result stats = thermo.stats(ExplosiveConfig.thermonuclearCaps());
                NuclearDetonation.detonateThermonuclear(level, at, stats);
            }
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Kind", kind.name().toLowerCase(Locale.ROOT));
        tag.putFloat("Power", power);
        tag.putInt("HRadius", hRadius);
        tag.putFloat("Intensity", intensity);
        tag.putFloat("Salting", salting);
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
        return new Warhead(kind, tag.getFloat("Power"), tag.getInt("HRadius"), tag.getFloat("Intensity"), tag.getFloat("Salting"), thermo);
    }
}
