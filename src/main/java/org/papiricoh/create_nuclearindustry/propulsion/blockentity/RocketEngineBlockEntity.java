package org.papiricoh.create_nuclearindustry.propulsion.blockentity;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements;
import org.papiricoh.create_nuclearindustry.advancement.NuclearEvent;
import org.papiricoh.create_nuclearindustry.controlroom.PlantReadable;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace;
import org.papiricoh.create_nuclearindustry.maintenance.incident.IncidentLog;
import org.papiricoh.create_nuclearindustry.maintenance.incident.IncidentType;
import org.papiricoh.create_nuclearindustry.propulsion.EngineStructure;
import org.papiricoh.create_nuclearindustry.propulsion.PropulsionConfig;
import org.papiricoh.create_nuclearindustry.propulsion.RocketEngines;
import org.papiricoh.create_nuclearindustry.propulsion.block.RocketNozzleBlock;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;

import java.util.List;

/**
 * What both nuclear engines share: the line of segments and the nozzle they are built from, the
 * throttle from the strongest redstone signal around the controller, and the thrust the ship's
 * physics reads every step. Aboard a Create Aeronautics ship the thrust pushes the ship along the
 * engine's axis, away from where the exhaust leaves; on the ground the engine is a test stand that
 * fires without moving anything. The exhaust is measured where the engine really is.
 */
public abstract class RocketEngineBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, PlantReadable {
    private static final int SCAN_INTERVAL = 20;
    private static final int SYNC_INTERVAL = 10;
    /** How far the jet reaches, blocks, for where its activity lands. */
    protected static final double JET_LENGTH = 16.0;

    @Nullable
    private EngineStructure structure;
    private boolean scanRequested = true;
    private int scanTimer;
    private int syncTimer;
    protected double throttle;
    private double thrust;
    /** Activity of the exhaust waiting to be laid where the jet lands. */
    private double exhaustActivity;
    private boolean failed;
    // Client copies
    private boolean formed;
    private int segments;

    protected RocketEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    /** The segment block this engine is built of. */
    protected abstract Block segmentBlock();

    protected abstract Block nozzleBlock();

    protected abstract int maxSegments();

    /** One tick of the engine with the structure built; returns its thrust. */
    protected abstract double run(EngineStructure structure);

    /** One tick with no engine built behind the controller. */
    protected abstract void idle();

    /** What those aboard earn when it first pushes their ship. */
    protected abstract NuclearEvent flightEvent();

    @Override
    public void initialize() {
        super.initialize();
        if (level != null && !level.isClientSide) {
            RocketEngines.add(this);
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        RocketEngines.remove(this);
    }

    public void requestScan() {
        scanRequested = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            if (thrust > 0 && isFormed()) {
                plume();
            }
            return;
        }
        if (scanRequested || ++scanTimer >= SCAN_INTERVAL) {
            scanRequested = false;
            scanTimer = 0;
            scan();
        }
        throttle = level.getBestNeighborSignal(worldPosition) / 15.0;
        double next = 0;
        if (structure != null && !failed) {
            next = run(structure) * PropulsionConfig.thrustScale;
        } else {
            idle();
        }
        boolean wasFiring = thrust > 0;
        thrust = next;
        if (wasFiring != thrust > 0) {
            light(thrust > 0);
            sendData();
            if (thrust > 0 && ShipSpace.onShip(level, worldPosition)) {
                NuclearAdvancements.awardNearby(level, worldPosition, NuclearAdvancements.WITNESS_RADIUS, flightEvent());
            }
        }
        if (exhaustActivity > 0 && level.getGameTime() % 20 == 0) {
            layExhaust();
        }
        if (++syncTimer >= SYNC_INTERVAL) {
            syncTimer = 0;
            sendData();
        }
    }

    private void scan() {
        EngineStructure found = EngineStructure.scan(level, worldPosition, exhaust(), segmentBlock(), nozzleBlock(), maxSegments());
        if (!java.util.Objects.equals(found, structure)) {
            if (structure != null && found == null) {
                light(false);
            }
            structure = found;
            setChanged();
            sendData();
        }
    }

    /** Lights or puts out the nozzle's jet. */
    private void light(boolean lit) {
        if (structure == null || level == null) {
            return;
        }
        BlockState nozzle = level.getBlockState(structure.nozzle());
        if (nozzle.getBlock() instanceof RocketNozzleBlock && nozzle.getValue(RocketNozzleBlock.LIT) != lit) {
            level.setBlock(structure.nozzle(), nozzle.setValue(RocketNozzleBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    /** Client: the jet out of the nozzle's open face, thicker with the throttle. */
    private void plume() {
        Direction out = exhaust();
        BlockPos nozzle = worldPosition.relative(out, segments() + 1);
        Vec3 dir = Vec3.atLowerCornerOf(out.getNormal());
        Vec3 mouth = Vec3.atCenterOf(nozzle).add(dir.scale(0.55));
        // Aboard a ship the jet leaves where the ship is drawn, the way the nozzle points there.
        ShipSpace.Ship ship = ShipSpace.shipAt(level, worldPosition);
        if (ship != null) {
            mouth = ship.toWorld(mouth);
            dir = ship.toWorldDirection(dir).normalize();
        }
        net.minecraft.util.RandomSource random = level.random;
        boolean magnetic = level.getBlockState(nozzle).getBlock() instanceof RocketNozzleBlock block && block.jet() == RocketNozzleBlock.Jet.MAGNETIC;
        int count = 1 + (int) Math.round(throttle * 4);
        for (int i = 0; i < count; i++) {
            double x = mouth.x + (random.nextDouble() - 0.5) * 0.25;
            double y = mouth.y + (random.nextDouble() - 0.5) * 0.25;
            double z = mouth.z + (random.nextDouble() - 0.5) * 0.25;
            if (magnetic) {
                double speed = 0.6 + random.nextDouble() * 0.7;
                level.addParticle(i % 2 == 0 ? net.minecraft.core.particles.ParticleTypes.END_ROD
                        : net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, x, y, z, dir.x * speed, dir.y * speed, dir.z * speed);
            } else {
                boolean flame = i == 0;
                double speed = flame ? 0.45 + random.nextDouble() * 0.3 : 0.2 + random.nextDouble() * 0.2;
                level.addParticle(flame ? net.minecraft.core.particles.ParticleTypes.FLAME : net.minecraft.core.particles.ParticleTypes.CLOUD,
                        x, y, z, dir.x * speed, dir.y * speed, dir.z * speed);
            }
        }
    }

    /** Where the exhaust leaves, in the axes the engine is built in. */
    public Direction exhaust() {
        return getBlockState().getValue(BlockStateProperties.FACING);
    }

    /** Thrust now; the ship is pushed opposite to {@link #exhaust()}. */
    public double thrust() {
        return thrust;
    }

    /** The throttle from redstone, 0-1. */
    public double throttle() {
        return throttle;
    }

    @Nullable
    public EngineStructure structure() {
        return structure;
    }

    public boolean isFormed() {
        return level != null && level.isClientSide ? formed : structure != null;
    }

    public int segments() {
        return level != null && level.isClientSide ? segments : structure == null ? 0 : structure.segments();
    }

    public boolean failed() {
        return failed;
    }

    /** Where the thrust is applied, in the engine's own axes: the nozzle's centre. */
    public Vec3 thrustPoint() {
        return Vec3.atCenterOf(structure == null ? worldPosition : structure.nozzle());
    }

    /** The push on the ship, in the engine's own axes. */
    public Vec3 thrustVector() {
        Vec3 out = Vec3.atLowerCornerOf(exhaust().getNormal());
        return out.scale(-thrust);
    }

    /** The mouth of the nozzle where it really is. */
    protected Vec3 worldMouth() {
        Vec3 local = thrustPoint().add(Vec3.atLowerCornerOf(exhaust().getNormal()).scale(0.6));
        return ShipSpace.toWorld(level, local);
    }

    /** Where the exhaust leaves, in the world. */
    protected Vec3 worldExhaust() {
        Vec3 local = Vec3.atLowerCornerOf(exhaust().getNormal());
        ShipSpace.Ship ship = ShipSpace.shipAt(level, worldPosition);
        return ship == null ? local : ship.toWorldDirection(local).normalize();
    }

    /** Activity the exhaust carries away, laid where the jet lands every second. */
    protected void releaseActivity(double amount) {
        exhaustActivity += Math.max(0, amount);
    }

    /** The jet's activity settles on the ground where it ends: the first solid block, or under its end. */
    private void layExhaust() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        Vec3 from = worldMouth();
        Vec3 direction = worldExhaust();
        Vec3 end = from.add(direction.scale(JET_LENGTH));
        for (double step = 1; step <= JET_LENGTH; step++) {
            Vec3 at = from.add(direction.scale(step));
            BlockPos block = BlockPos.containing(at);
            if (server.isLoaded(block) && !server.getBlockState(block).getCollisionShape(server, block).isEmpty()) {
                end = at;
                break;
            }
        }
        BlockPos landing = BlockPos.containing(end);
        if (server.isLoaded(landing)) {
            Contamination.add(server.getChunkAt(landing), (float) exhaustActivity);
        }
        exhaustActivity = 0;
    }

    /** The engine blew up: it stops for good until its worn part is mended. */
    protected void fail(String detail) {
        failed = true;
        thrust = 0;
        light(false);
        IncidentLog.record(level, worldPosition, IncidentType.ENGINE_FAILURE, 0, detail);
        setChanged();
        sendData();
    }

    /** Lang key of what the engine is doing, for goggles, monitors and Display Links. */
    public abstract String statusKey();

    /** Whether it is cold and still enough to open for repairs. */
    public abstract boolean idleForRepair();

    /** Mends the worn part by {@code amount}; true if anything was mended. */
    public abstract boolean repair(double amount);

    /** A mended engine may fire again. */
    protected void clearFailure() {
        failed = false;
        setChanged();
    }

    public int comparatorSignal() {
        return (int) Math.round(Math.min(1.0, thrust <= 0 ? 0 : 0.0667 + throttle) * 15);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putDouble("Thrust", thrust);
        tag.putDouble("Throttle", throttle);
        tag.putBoolean("Failed", failed);
        tag.putDouble("Exhaust", exhaustActivity);
        if (clientPacket) {
            tag.putBoolean("Formed", structure != null);
            tag.putInt("Segments", structure == null ? 0 : structure.segments());
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        thrust = tag.getDouble("Thrust");
        throttle = tag.getDouble("Throttle");
        failed = tag.getBoolean("Failed");
        exhaustActivity = tag.getDouble("Exhaust");
        if (clientPacket) {
            formed = tag.getBoolean("Formed");
            segments = tag.getInt("Segments");
        }
        scanRequested = true;
    }

    static String fmt(double value, int decimals) {
        return String.format(java.util.Locale.ROOT, "%." + decimals + "f", value);
    }
}
