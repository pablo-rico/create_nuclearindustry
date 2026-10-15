package org.papiricoh.create_nuclearindustry.maintenance;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.maintenance.physics.ComponentHealth;
import org.papiricoh.create_nuclearindustry.maintenance.physics.WearPart;

/**
 * One wearing part of a machine (a steam generator's tubes, a turbine's blades): it wears with the
 * stress its machine reports each tick and, once a second, may fail. The machine decides what a
 * failure does and when it may be repaired. Follows the {@code maintenance.*} switches.
 */
public final class PartWear {
    private static final int CHECK_INTERVAL = 20;

    private final WearPart part;
    private final ComponentHealth health = new ComponentHealth();
    private int checkTimer;

    public PartWear(WearPart part) {
        this.part = part;
    }

    /** One server tick at {@code stress} (1 = rated duty). Returns true on the tick the part fails. */
    public boolean tick(Level level, double stress) {
        if (!MaintenanceConfig.wearOn()) {
            return false;
        }
        ComponentHealth.Params p = MaintenanceConfig.params(part);
        health.wear(p, stress, 1);
        if (++checkTimer < CHECK_INTERVAL) {
            return false;
        }
        checkTimer = 0;
        return MaintenanceConfig.failuresOn() && stress > 0 && health.check(p, level.random.nextDouble());
    }

    /** A transient of {@code severity} (an earthquake). */
    public void shock(double severity) {
        if (MaintenanceConfig.wearOn()) {
            health.shock(MaintenanceConfig.params(part), severity);
        }
    }

    public void fail() {
        health.fail();
    }

    /** Gives back {@code amount} of health and puts a failure right. Returns whether anything needed it. */
    public boolean repair(double amount) {
        boolean needed = health.health() < 1 || health.failed();
        health.repair(amount);
        return needed;
    }

    public double health() {
        return health.health();
    }

    public boolean failed() {
        return health.failed();
    }

    public ComponentHealth.Condition condition() {
        return health.condition();
    }

    /** "Turbine blades: worn (54%)". */
    public Component line() {
        return ReactorMaintenance.part(part.id(), health);
    }

    public void save(CompoundTag tag, String key) {
        tag.putDouble(key, health.health());
        tag.putBoolean(key + "Failed", health.failed());
    }

    public void load(CompoundTag tag, String key) {
        health.set(tag.contains(key) ? tag.getDouble(key) : 1.0, tag.getBoolean(key + "Failed"));
    }
}
