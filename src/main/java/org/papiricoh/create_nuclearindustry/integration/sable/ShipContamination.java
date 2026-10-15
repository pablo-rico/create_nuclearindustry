package org.papiricoh.create_nuclearindustry.integration.sable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.radiation.contamination.ChunkContamination;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;
import org.papiricoh.create_nuclearindustry.radiation.contamination.ContaminationData;

/**
 * A ship's own contamination, one value for the whole ship in the ground's units. It is kept with the
 * ship, so it travels and is saved with it, and decays lazily like the ground's. A ship takes it up
 * flying through contaminated air (every {@value #CHECK_INTERVAL} ticks), from fallout that comes
 * down on it, and from accidents aboard; those aboard take a dose from it; a scrubber aboard cleans it.
 */
public final class ShipContamination {
    public static final String KEY = "contamination";
    static final int CHECK_INTERVAL = 100;

    private ShipContamination() {}

    /** The ship's contamination now. */
    public static float get(ShipSpace.Ship ship) {
        CompoundTag tag = ship.data(KEY);
        if (tag.isEmpty()) {
            return 0;
        }
        float now = ChunkContamination.decay(tag.getFloat("Level"), ship.level().getGameTime() - tag.getLong("Updated"),
                Config.contaminationHalfLifeTicks);
        return now < Contamination.MIN_LEVEL ? 0 : now;
    }

    public static void set(ShipSpace.Ship ship, float value) {
        value = Mth.clamp(value, 0, (float) Config.contaminationMax);
        CompoundTag tag = new CompoundTag();
        if (value >= Contamination.MIN_LEVEL) {
            tag.putFloat("Level", value);
            tag.putLong("Updated", ship.level().getGameTime());
        }
        ship.setData(KEY, tag);
        showSeverity(ship, value);
    }

    /** Players near the ship are told how contaminated it is, for the dust over its deck. */
    private static void showSeverity(ShipSpace.Ship ship, float value) {
        if (ship.level() instanceof ServerLevel level) {
            ShipSeverityPayload.send(level, ship, ChunkContamination.severity(value));
        }
    }

    /** Adds (or with a negative amount removes) contamination; returns the new value. */
    public static float add(ShipSpace.Ship ship, float amount) {
        float value = Mth.clamp(get(ship) + amount, 0, (float) Config.contaminationMax);
        set(ship, value);
        return value < Contamination.MIN_LEVEL ? 0 : value;
    }

    /** The ship whose plot holds this chunk, if any: plot chunks belong to one ship each. */
    @Nullable
    public static ShipSpace.Ship shipOf(Level level, ChunkPos chunk) {
        return ShipSpace.enabled() ? ShipSpace.shipAt(level, new Vec3(chunk.getMiddleBlockX() + 0.5, 0, chunk.getMiddleBlockZ() + 0.5)) : null;
    }

    /** Fallout coming down: the ships within its reach take what falls where they are. */
    public static void fallout(ServerLevel level, ContaminationData.FalloutEvent event) {
        if (!AeronauticsConfig.contaminationOn() || !ShipSpace.enabled()) {
            return;
        }
        double reach = event.reach();
        AABB around = new AABB(event.x() - reach, level.getMinBuildHeight(), event.z() - reach,
                event.x() + reach, level.getMaxBuildHeight(), event.z() + reach);
        for (ShipSpace.Ship ship : ShipSpace.shipsIn(level, around)) {
            Vec3 centre = ship.worldBounds().getCenter();
            float amount = event.at(centre.x, centre.z);
            if (amount > 0) {
                add(ship, amount);
            }
        }
    }

    /** Every few seconds each ship takes up some of the contamination it flies through. */
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % CHECK_INTERVAL != 0
                || !AeronauticsConfig.contaminationOn() || !Config.contaminationEnabled) {
            return;
        }
        for (ShipSpace.Ship ship : ShipSpace.all(level)) {
            uptake(level, ship);
            // Decay is worked out when read: keep what clients are shown in step with it.
            showSeverity(ship, get(ship));
        }
    }

    /** One check of a ship among the contamination around it; public so tests can drive it. */
    public static void uptake(ServerLevel level, ShipSpace.Ship ship) {
        AABB bounds = ship.worldBounds();
        Vec3 centre = bounds.getCenter();
        BlockPos under = BlockPos.containing(centre);
        if (!level.isLoaded(under)) {
            return;
        }
        float ground = Contamination.get(level, new ChunkPos(under));
        if (ground <= 0) {
            return;
        }
        double height = bounds.minY - level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, under.getX(), under.getZ());
        double ambient = ground * ShipContaminationModel.heightFactor(height);
        float current = get(ship);
        float next = ShipContaminationModel.uptake(current, ambient, AeronauticsConfig.contaminationUptake);
        if (next > current) {
            set(ship, next);
        }
    }
}
