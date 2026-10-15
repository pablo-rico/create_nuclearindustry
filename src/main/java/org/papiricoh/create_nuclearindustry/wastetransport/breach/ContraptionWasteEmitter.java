package org.papiricoh.create_nuclearindustry.wastetransport.breach;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitter;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitters;
import org.papiricoh.create_nuclearindustry.wastetransport.WasteTransportConfig;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The radiation of every waste container riding one contraption (a train carriage, a piston's
 * load): mounted blocks have no block entity to emit, so one emitter per contraption sums their
 * cargo, each through its own shielding or breach, from the middle of the contraption. Its waste
 * containers' movement behaviours refresh it once a second; when they stop (the contraption was
 * put down, or its chunk unloaded) it goes quiet and drops out of the index.
 */
public final class ContraptionWasteEmitter implements RadiationEmitter {
    /** Ticks without a refresh after which the emitter counts as gone. */
    static final int STALE_TICKS = 60;

    private static final Map<AbstractContraptionEntity, ContraptionWasteEmitter> EMITTERS = new WeakHashMap<>();

    private final WeakReference<AbstractContraptionEntity> entity;
    private final Level level;
    private Vec3 origin;
    private double strength;
    private long lastUpdate = Long.MIN_VALUE / 2;
    private boolean forgotten;

    private ContraptionWasteEmitter(AbstractContraptionEntity entity) {
        this.entity = new WeakReference<>(entity);
        this.level = entity.level();
        this.origin = entity.getBoundingBox().getCenter();
    }

    /** Refreshes (at most once a tick) the emitter of {@code entity}, creating it if needed. Server side. */
    @Nullable
    public static ContraptionWasteEmitter update(AbstractContraptionEntity entity) {
        if (entity.level().isClientSide) {
            return null;
        }
        ContraptionWasteEmitter emitter = EMITTERS.computeIfAbsent(entity, ContraptionWasteEmitter::new);
        long now = entity.level().getGameTime();
        if (emitter.lastUpdate != now) {
            emitter.lastUpdate = now;
            emitter.forgotten = false;
            emitter.recompute(entity);
            RadiationEmitters.register(emitter);
        }
        return emitter;
    }

    /** The emitter of {@code entity}, if one is running. */
    @Nullable
    public static ContraptionWasteEmitter of(AbstractContraptionEntity entity) {
        return EMITTERS.get(entity);
    }

    /** Stops the emitter of {@code entity}: its containers were put down. */
    public static void forget(AbstractContraptionEntity entity) {
        ContraptionWasteEmitter emitter = EMITTERS.remove(entity);
        if (emitter != null) {
            emitter.forgotten = true;
            RadiationEmitters.unregister(emitter);
        }
    }

    private void recompute(AbstractContraptionEntity entity) {
        origin = ShipSpace.toWorld(level, entity.getBoundingBox().getCenter());
        Contraption contraption = entity.getContraption();
        double total = 0;
        if (contraption != null) {
            for (MountedWasteStorage storage : MountedWasteStorage.of(contraption.getStorage())) {
                total += BreachModel.emission(storage.activity(), storage.isBreached(), WasteTransportConfig.shieldedTransmission,
                        WasteTransportConfig.breachedTransmission, Double.MAX_VALUE);
            }
        }
        strength = Math.min(WasteTransportConfig.maxEmission, total);
    }

    @Override
    public Level getLevel() {
        return level;
    }

    @Override
    public boolean isRemoved() {
        AbstractContraptionEntity carrier = entity.get();
        return forgotten || carrier == null || carrier.isRemoved() || level.getGameTime() - lastUpdate > STALE_TICKS;
    }

    @Override
    public Vec3 radiationOrigin() {
        return origin;
    }

    @Override
    public double radiationStrength() {
        return isRemoved() ? 0 : strength;
    }
}
