package org.papiricoh.create_nuclearindustry.explosive;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import org.papiricoh.create_nuclearindustry.explosive.blast.DetonationStyle;

/**
 * Posted on {@link net.neoforged.neoforge.common.NeoForge#EVENT_BUS} when a nuclear or dirty
 * device goes off, before its blast, so seismic detectors, sirens and data packs can react.
 *
 * @param power  blast power (the nuclear yield scale of {@link NuclearDetonation}, or the dirty bomb's blast)
 * @param radius destruction radius of a nuclear device, fallout radius of a dirty one, in blocks
 */
public class NuclearDetonationEvent extends Event {
    private final ServerLevel level;
    private final Vec3 center;
    private final float power;
    private final int radius;
    private final DetonationStyle style;

    public NuclearDetonationEvent(ServerLevel level, Vec3 center, float power, int radius, boolean dirty) {
        this(level, center, power, radius, dirty ? DetonationStyle.DIRTY : DetonationStyle.FISSION);
    }

    public NuclearDetonationEvent(ServerLevel level, Vec3 center, float power, int radius, DetonationStyle style) {
        this.level = level;
        this.center = center;
        this.power = power;
        this.radius = radius;
        this.style = style;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public Vec3 getCenter() {
        return center;
    }

    public float getPower() {
        return power;
    }

    public int getRadius() {
        return radius;
    }

    /** A dirty bomb: a small conventional blast and fallout, no crater. */
    public boolean isDirty() {
        return style == DetonationStyle.DIRTY;
    }

    /** A thermonuclear warhead: a fission primary that lit a fusion secondary. */
    public boolean isThermonuclear() {
        return style == DetonationStyle.THERMONUCLEAR;
    }

    public DetonationStyle getStyle() {
        return style;
    }
}
