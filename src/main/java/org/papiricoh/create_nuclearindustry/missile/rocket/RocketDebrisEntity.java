package org.papiricoh.create_nuclearindustry.missile.rocket;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.StructureTransform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.flight.Flight;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightEvents;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightFxEvent;
import org.papiricoh.create_nuclearindustry.missile.flight.RocketFxSource;
import org.papiricoh.create_nuclearindustry.missile.flight.RocketShape;
import org.papiricoh.create_nuclearindustry.missile.flight.RocketShapeCodec;
import org.papiricoh.create_nuclearindustry.missile.physics.RocketAttitude;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;

import java.util.List;

/**
 * A spent stage tumbling down after it drops away: its blocks as a contraption, falling under the flight's gravity with a little
 * drag and turning slowly over. Purely a sight. It never places a block nor drops an item: it simply goes when it meets loaded
 * ground (its dust is the rocket effects' {@link FlightFxEvent#DEBRIS_IMPACT}), when it leaves the loaded world, or after 30
 * seconds. The client turns it itself from where it started turning, so only its position travels over the network.
 */
public class RocketDebrisEntity extends AbstractContraptionEntity implements RocketFxSource {
    /** Longest a stage falls before it is simply gone, ticks. */
    public static final int LIFETIME = 600;
    /** Share of its speed kept each tick. */
    static final double DRAG = 0.996;

    /** Where the flight's pivot (the original tail's bottom centre) is, and how it moves. Server side. */
    private V3 pivot = V3.ZERO;
    private V3 velocity = V3.ZERO;
    /** Its turn when it dropped, then how it spins: about {@link #spinAxis}, degrees per tick. */
    private Quaternionf start = new Quaternionf();
    private Vector3f spinAxis = new Vector3f(1, 0, 0);
    private float spinRate;
    private int stage;
    /** The stage it is, rebased onto its own tail. */
    private RocketShape.Stage shape = new RocketShape.Stage(0, 0, List.of(new RocketShape.Nozzle(0, 0, 0)), RocketShape.Propellant.SOLID);
    /** Section index of its tail in the original rocket. */
    private int bottom;
    @Nullable
    private Flight flight;

    public RocketDebrisEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    /** Spawns the stage that just dropped from {@code flight} near players; the flight still shows the rest. */
    static void onFlightEvent(ServerLevel level, Flight flight, FlightFxEvent event, int stage, Vec3 pos, Vec3 vel) {
        if (event != FlightFxEvent.STAGE_SEPARATION && event != FlightFxEvent.PAYLOAD_SEPARATION) return;
        if (flight.cargo() == null || !watched(level, pos)) return;
        if (pos.y < level.getMinBuildHeight() || !level.areEntitiesLoaded(ChunkPos.asLong(BlockPos.containing(pos)))) return;
        RocketDebrisEntity debris = create(level, flight, stage, pos, vel);
        if (debris != null) level.addFreshEntity(debris);
    }

    private static boolean watched(ServerLevel level, Vec3 pos) {
        double range = MissileConfig.showDistance;
        for (ServerPlayer player : level.players()) {
            double dx = player.getX() - pos.x, dz = player.getZ() - pos.z;
            if (dx * dx + dz * dz <= range * range) return true;
        }
        return false;
    }

    /** Stage {@code stage} of {@code flight} falling away from {@code pos}, or null if the flight carries no such blocks. */
    @Nullable
    public static RocketDebrisEntity create(ServerLevel level, Flight flight, int stage, Vec3 pos, Vec3 vel) {
        CompoundTag cargo = flight.cargo();
        RocketShape.Stage original = flight.shape().stage(stage);
        if (cargo == null || original == null) return null;
        Contraption read;
        try {
            read = Contraption.fromNBT(level, cargo, false);
        } catch (RuntimeException e) {
            return null;
        }
        if (!(read instanceof RocketContraption full)) return null;
        RocketContraption part = full.filtered(s -> s == stage, level.registryAccess(), level);
        if (part.blocks().isEmpty()) return null;
        RocketDebrisEntity debris = new RocketDebrisEntity(RocketRegistry.ROCKET_DEBRIS.get(), level);
        debris.flight = flight;
        debris.stage = stage;
        debris.bottom = original.bottom();
        debris.shape = rebase(original);
        debris.pivot = Flight.v3(pos);
        debris.velocity = Flight.v3(vel);
        debris.start = RocketContraptionEntity.toJoml(RocketAttitude.orientation(debris.velocity, flight.heading()));
        // Each stage tumbles its own way, the same way every time.
        long seed = flight.id().getLeastSignificantBits() * 31 + stage;
        java.util.Random random = new java.util.Random(seed);
        debris.spinAxis = new Vector3f((float) random.nextGaussian(), (float) random.nextGaussian() * 0.3f, (float) random.nextGaussian()).normalize();
        debris.spinRate = 1.5f + random.nextFloat() * 2.5f;
        debris.setContraption(part);
        debris.place(0);
        debris.contraptionInitialize();
        return debris;
    }

    /** {@code stage} measured from its own tail, as {@link RocketFxSource#fxStage} wants it. */
    static RocketShape.Stage rebase(RocketShape.Stage stage) {
        List<RocketShape.Nozzle> nozzles = stage.nozzles().stream()
                .map(n -> new RocketShape.Nozzle(n.x(), n.y() - stage.bottom(), n.z())).toList();
        return new RocketShape.Stage(0, stage.top() - stage.bottom(), nozzles, stage.propellant());
    }

    /** Its turn {@code age} ticks after it dropped. */
    Quaternionf attitudeAt(float age) {
        return new Quaternionf().rotateAxis((float) Math.toRadians(spinRate * age), spinAxis).mul(start);
    }

    private void place(float age) {
        V3 anchor = RocketPose.anchor(pivot, RocketContraptionEntity.fromJoml(attitudeAt(age)));
        setPos(anchor.x(), anchor.y(), anchor.z());
    }

    @Override
    protected void tickContraption() {
        if (!(level() instanceof ServerLevel server)) return;
        if (tickCount > LIFETIME) {
            discard();
            return;
        }
        V3 from = pivot;
        velocity = velocity.scale(DRAG).sub(V3.UP.scale(MissileConfig.gravity));
        pivot = pivot.add(velocity);
        Vec3 a = Flight.vec(from), b = Flight.vec(pivot);
        if (b.y < server.getMinBuildHeight() || !server.hasChunkAt(BlockPos.containing(b))) {
            discard();
            return;
        }
        if (server.hasChunkAt(BlockPos.containing(a))) {
            BlockHitResult hit = server.clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, CollisionContext.empty()));
            if (hit.getType() == HitResult.Type.BLOCK) {
                if (flight != null) FlightEvents.fireDebris(server, flight, FlightFxEvent.DEBRIS_IMPACT, stage, hit.getLocation(), Flight.vec(velocity));
                discard();
                return;
            }
        }
        place(tickCount);
    }

    @Override
    public void setPos(double x, double y, double z) {
        super.setPos(x, y, z);
        if (contraption != null && shape != null) {
            Vec3 centre = toGlobalVector(new Vec3(0.5, 1 + bottom + (shape.top() + 1) / 2.0, 0.5), 1);
            setBoundingBox(new AABB(centre, centre).inflate(shape.top() / 2.0 + 2));
        }
    }

    private float age(float partial) {
        return tickCount - 1 + partial;
    }

    @Override
    public Vec3 applyRotation(Vec3 local, float partial) {
        Vector3f v = attitudeAt(age(partial)).transform(new Vector3f((float) local.x, (float) local.y, (float) local.z));
        return new Vec3(v.x, v.y, v.z);
    }

    @Override
    public Vec3 reverseRotation(Vec3 world, float partial) {
        Vector3f v = attitudeAt(age(partial)).conjugate().transform(new Vector3f((float) world.x, (float) world.y, (float) world.z));
        return new Vec3(v.x, v.y, v.z);
    }

    @Override
    public ContraptionRotationState getRotationState() {
        double[] euler = RocketPose.euler(RocketContraptionEntity.fromJoml(attitudeAt(age(1))));
        ContraptionRotationState state = new ContraptionRotationState();
        state.xRotation = (float) euler[0];
        state.yRotation = (float) euler[1];
        state.zRotation = (float) euler[2];
        return state;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void applyLocalTransforms(PoseStack pose, float partial) {
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(attitudeAt(age(partial)));
        pose.translate(-0.5, -0.5, -0.5);
    }

    // ------------------------------------------------------------------ rocket effects

    @Override
    public Vec3 fxPosition(float partial) {
        Vec3 anchor = new Vec3(Mth.lerp(partial, xOld, getX()), Mth.lerp(partial, yOld, getY()), Mth.lerp(partial, zOld, getZ()));
        Vector3f tail = attitudeAt(age(partial)).transform(new Vector3f(0, 0.5f + bottom, 0));
        return anchor.add(0.5, 0.5, 0.5).add(tail.x, tail.y, tail.z);
    }

    @Override
    public Quaternionf fxAttitude(float partial) {
        return attitudeAt(age(partial));
    }

    @Override
    public RocketShape.Stage fxStage() {
        return shape;
    }

    @Override
    public boolean fxBurning() {
        return false;
    }

    // ------------------------------------------------------------------ a sight only

    @Override
    protected StructureTransform makeStructureTransform() {
        return new StructureTransform(blockPosition(), 0, 0, 0);
    }

    @Override
    public void disassemble() {
    }

    @Override
    protected float getStalledAngle() {
        return 0;
    }

    @Override
    protected void handleStallInformation(double x, double y, double z, float angle) {
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    /** Nothing stands on it or is pushed by it: Create's contraption collider would shove players about as it flies. */
    @Override
    public boolean collisionEnabled() {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return false;
    }

    @Override
    public boolean handlePlayerInteraction(Player player, BlockPos localPos, Direction side, InteractionHand hand) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 512 * 512;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox();
    }

    public int stage() {
        return stage;
    }

    @Override
    protected void writeAdditional(CompoundTag tag, HolderLookup.Provider registries, boolean spawnPacket) {
        super.writeAdditional(tag, registries, spawnPacket);
        tag.putFloat("QX", start.x);
        tag.putFloat("QY", start.y);
        tag.putFloat("QZ", start.z);
        tag.putFloat("QW", start.w);
        tag.putFloat("AX", spinAxis.x);
        tag.putFloat("AY", spinAxis.y);
        tag.putFloat("AZ", spinAxis.z);
        tag.putFloat("Spin", spinRate);
        tag.putInt("Stage", stage);
        tag.putInt("Bottom", bottom);
        tag.put("Shape", RocketShapeCodec.save(new RocketShape(shape.top() + 1, 0.5f, List.of(shape), shape.top() + 1)));
    }

    @Override
    protected void readAdditional(CompoundTag tag, boolean spawnData) {
        super.readAdditional(tag, spawnData);
        start = new Quaternionf(tag.getFloat("QX"), tag.getFloat("QY"), tag.getFloat("QZ"), tag.getFloat("QW"));
        spinAxis = new Vector3f(tag.getFloat("AX"), tag.getFloat("AY"), tag.getFloat("AZ"));
        spinRate = tag.getFloat("Spin");
        stage = tag.getInt("Stage");
        bottom = tag.getInt("Bottom");
        RocketShape stored = RocketShapeCodec.load(tag.getCompound("Shape"));
        if (stored.stage(0) != null) shape = stored.stage(0);
    }
}
