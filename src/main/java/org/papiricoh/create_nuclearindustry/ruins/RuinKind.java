package org.papiricoh.create_nuclearindustry.ruins;

import com.mojang.serialization.Codec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** The three kinds of abandoned nuclear site, each with its own {@code ruins.*} switch and chance. */
public enum RuinKind implements StringRepresentable {
    POWER_PLANT,
    CIVIL_SHELTER,
    MILITARY_BUNKER;

    public static final Codec<RuinKind> CODEC = StringRepresentable.fromEnum(RuinKind::values);

    public boolean enabled() {
        return switch (this) {
            case POWER_PLANT -> RuinsConfig.powerPlants;
            case CIVIL_SHELTER -> RuinsConfig.civilShelters;
            case MILITARY_BUNKER -> RuinsConfig.militaryBunkers;
        };
    }

    public double chance() {
        return switch (this) {
            case POWER_PLANT -> RuinsConfig.powerPlantChance;
            case CIVIL_SHELTER -> RuinsConfig.civilShelterChance;
            case MILITARY_BUNKER -> RuinsConfig.militaryBunkerChance;
        };
    }

    /**
     * The slope limit a structure of this kind checks: the configured one for power plants, whose
     * structure asks for a check at all with a {@code max_slope} of 0 or more; -1 for no check.
     */
    public int maxSlope(int structureMaxSlope) {
        if (structureMaxSlope < 0) {
            return -1;
        }
        return this == POWER_PLANT ? RuinsConfig.powerPlantMaxSlope : structureMaxSlope;
    }

    /** Whether a place the structure set picked gets a ruin: the switch, then the chance. */
    public boolean allowed(RandomSource random) {
        if (!enabled()) {
            return false;
        }
        double chance = chance();
        return chance >= 1.0 || chance > 0.0 && random.nextDouble() < chance;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
