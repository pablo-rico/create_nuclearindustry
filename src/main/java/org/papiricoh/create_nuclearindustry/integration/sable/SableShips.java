package org.papiricoh.create_nuclearindustry.integration.sable;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import org.joml.Vector3d;
import org.papiricoh.create_nuclearindustry.propulsion.RocketEngines;
import org.papiricoh.create_nuclearindustry.propulsion.blockentity.RocketEngineBlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.neoforge.event.ForgeSableSubLevelContainerReadyEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** {@link ShipSpace} over Sable's sub-levels. The only class besides the ship content that imports Sable. */
final class SableShips implements ShipSpace.Backend {
    private SableShips() {}

    static void install(IEventBus modEventBus) {
        ShipSpace.install(new SableShips());
        NeoForge.EVENT_BUS.addListener(ShipContamination::onLevelTick);
        NeoForge.EVENT_BUS.addListener(ForgeSableSubLevelContainerReadyEvent.class,
                event -> event.getContainer().addObserver(new SplitObserver()));
        NeoForge.EVENT_BUS.addListener(ForgeSablePrePhysicsTickEvent.class, SableShips::pushShips);
    }

    /** Every physics step, each nuclear engine aboard a ship pushes it at its nozzle. */
    private static void pushShips(ForgeSablePrePhysicsTickEvent event) {
        SubLevelPhysicsSystem system = event.getPhysicsSystem();
        double step = event.getTimeStep();
        for (RocketEngineBlockEntity engine : RocketEngines.firing()) {
            Level level = engine.getLevel();
            if (level == null || SubLevelPhysicsSystem.get(level) != system
                    || !(Sable.HELPER.getContaining(level, engine.getBlockPos()) instanceof ServerSubLevel ship)) {
                continue;
            }
            Vec3 at = engine.thrustPoint();
            Vec3 push = engine.thrustVector().scale(step);
            RigidBodyHandle handle = system.getPhysicsHandle(ship);
            if (handle == null || !handle.isValid() || ship.getMassTracker() == null || ship.getMassTracker().isInvalid()) {
                continue;
            }
            // About the centre of mass, waking a ship at rest: a steady thrust would not wake one on its own.
            var centre = ship.getMassTracker().getCenterOfMass();
            Vector3d impulse = new Vector3d(push.x, push.y, push.z);
            Vector3d arm = new Vector3d(at.x - centre.x(), at.y - centre.y(), at.z - centre.z());
            handle.applyLinearAndAngularImpulse(impulse, arm.cross(impulse, new Vector3d()), true);
            // Shown with the ship's other forces in Simulated's diagrams.
            ship.getOrCreateQueuedForceGroup(ForceGroups.PROPULSION.get()).recordPointForce(new Vector3d(at.x, at.y, at.z), impulse);
        }
    }

    /** A ship broken in two: each part keeps the contamination of the ship it came from. */
    private static final class SplitObserver implements SubLevelObserver {
        @Override
        public void onSubLevelAdded(SubLevel subLevel) {
            if (!(subLevel instanceof ServerSubLevel server) || server.getSplitFromSubLevel() == null) {
                return;
            }
            SubLevelContainer container = SubLevelContainer.getContainer(server.getLevel());
            SubLevel parent = container == null ? null : container.getSubLevel(server.getSplitFromSubLevel());
            ShipSpace.Ship from = wrap(parent);
            ShipSpace.Ship to = wrap(server);
            if (from != null && to != null && ShipContamination.get(to) <= 0) {
                ShipContamination.set(to, ShipContamination.get(from));
            }
        }
    }

    @Override
    public @Nullable ShipSpace.Ship shipAt(Level level, Vec3 position) {
        return wrap(Sable.HELPER.getContaining(level, position));
    }

    @Override
    public List<ShipSpace.Ship> shipsIn(Level level, AABB worldBox) {
        // Each ship's own box, rather than the physics broad phase, which lags a new ship by a tick.
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return List.of();
        }
        List<ShipSpace.Ship> ships = new ArrayList<>();
        for (SubLevel subLevel : container.getAllSubLevels()) {
            if (!subLevel.isRemoved()) {
                SableShip ship = new SableShip(subLevel);
                if (ship.worldBounds().intersects(worldBox)) {
                    ships.add(ship);
                }
            }
        }
        return ships;
    }

    @Override
    public @Nullable ShipSpace.Ship riding(Entity entity) {
        return wrap(Sable.HELPER.getTrackingOrVehicleSubLevel(entity));
    }

    @Override
    public @Nullable ShipSpace.Ship byId(Level level, UUID id) {
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        return container == null ? null : wrap(container.getSubLevel(id));
    }

    @Override
    public List<ShipSpace.Ship> all(Level level) {
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return List.of();
        }
        List<ShipSpace.Ship> ships = new ArrayList<>();
        for (SubLevel subLevel : container.getAllSubLevels()) {
            ShipSpace.Ship ship = wrap(subLevel);
            if (ship != null) {
                ships.add(ship);
            }
        }
        return ships;
    }

    @Override
    public boolean inPlotGrid(Level level, Vec3 position) {
        return Sable.HELPER.isInPlotGrid(level, position);
    }

    private static @Nullable ShipSpace.Ship wrap(@Nullable SubLevelAccess access) {
        return access instanceof SubLevel subLevel && !subLevel.isRemoved() ? new SableShip(subLevel) : null;
    }

    /** The Sable sub-level behind a ship, for the ship content that talks to Sable directly. */
    static @Nullable SubLevel subLevel(ShipSpace.Ship ship) {
        return ship instanceof SableShip sable ? sable.subLevel : null;
    }

    private record SableShip(SubLevel subLevel) implements ShipSpace.Ship {
        @Override
        public UUID id() {
            return subLevel.getUniqueId();
        }

        @Override
        public Level level() {
            return subLevel.getLevel();
        }

        @Override
        public AABB worldBounds() {
            // From the plot's blocks and the current pose: Sable's own box lags a new ship by a few ticks.
            BoundingBox box = localBounds();
            double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
            for (int corner = 0; corner < 8; corner++) {
                Vec3 world = toWorld(new Vec3((corner & 1) == 0 ? box.minX() : box.maxX() + 1,
                        (corner & 2) == 0 ? box.minY() : box.maxY() + 1, (corner & 4) == 0 ? box.minZ() : box.maxZ() + 1));
                minX = Math.min(minX, world.x);
                minY = Math.min(minY, world.y);
                minZ = Math.min(minZ, world.z);
                maxX = Math.max(maxX, world.x);
                maxY = Math.max(maxY, world.y);
                maxZ = Math.max(maxZ, world.z);
            }
            return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }

        @Override
        public BoundingBox localBounds() {
            return subLevel.getPlot().getBoundingBox().toMojang();
        }

        private Pose3dc pose() {
            return subLevel.logicalPose();
        }

        @Override
        public Vec3 toWorld(Vec3 local) {
            return pose().transformPosition(local);
        }

        @Override
        public Vec3 toLocal(Vec3 world) {
            return pose().transformPositionInverse(world);
        }

        @Override
        public Vec3 toWorldDirection(Vec3 local) {
            return pose().transformNormal(local);
        }

        @Override
        public Vec3 toLocalDirection(Vec3 world) {
            return pose().transformNormalInverse(world);
        }

        @Override
        public Vec3 velocity(Vec3 worldPoint) {
            return Sable.HELPER.getVelocity(subLevel.getLevel(), subLevel, toLocal(worldPoint));
        }

        @Override
        public double mass() {
            if (subLevel instanceof ServerSubLevel server && server.getMassTracker() != null && !server.getMassTracker().isInvalid()) {
                return server.getMassTracker().getMass();
            }
            BoundingBox box = localBounds();
            return Math.max(1, box.getXSpan() * box.getYSpan() * box.getZSpan());
        }

        @Override
        public void impulse(Vec3 worldPoint, Vec3 worldImpulse) {
            if (!(subLevel instanceof ServerSubLevel server)) {
                return;
            }
            RigidBodyHandle handle = RigidBodyHandle.of(server);
            if (handle == null || !handle.isValid()) {
                return;
            }
            // At a plot point, about the ship's centre of mass; Sable wakes a ship at rest.
            handle.applyImpulseAtPoint(toLocal(worldPoint), toLocalDirection(worldImpulse));
        }

        @Override
        public CompoundTag data(String key) {
            if (subLevel instanceof ServerSubLevel server && server.getUserDataTag() != null) {
                return server.getUserDataTag().getCompound(key);
            }
            return new CompoundTag();
        }

        @Override
        public void setData(String key, CompoundTag tag) {
            if (!(subLevel instanceof ServerSubLevel server)) {
                return;
            }
            CompoundTag all = server.getUserDataTag() == null ? new CompoundTag() : server.getUserDataTag().copy();
            if (tag.isEmpty()) {
                all.remove(key);
            } else {
                all.put(key, tag);
            }
            server.setUserDataTag(all);
        }

        @Override
        public boolean isRemoved() {
            return subLevel.isRemoved();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof SableShip ship && ship.subLevel == subLevel;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(subLevel);
        }
    }
}
