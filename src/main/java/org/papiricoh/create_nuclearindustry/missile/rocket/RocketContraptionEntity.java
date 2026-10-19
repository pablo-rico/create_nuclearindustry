package org.papiricoh.create_nuclearindustry.missile.rocket;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.StructureTransform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.papiricoh.create_nuclearindustry.missile.flight.Flight;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightTracker;
import org.papiricoh.create_nuclearindustry.missile.physics.RocketAttitude;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * A silo rocket in flight as players see it: the contraption it was built from, posed where its {@link Flight} is and turned the
 * way it flies. A puppet only: the {@link FlightTracker} makes one near players, moves it every tick and takes it away; it is
 * never saved, never collides and cannot be used. On clients it is placed from the flight's own client state
 * ({@code ClientFlights}) each tick, not from the entity tracker, so it cannot lag or fight the rocket effects. When a stage drops
 * away its blocks leave this contraption in place (a falling {@link RocketDebrisEntity} takes them), so nothing is respawned.
 */
public class RocketContraptionEntity extends AbstractContraptionEntity {
    private static final EntityDataAccessor<Quaternionf> ATTITUDE = SynchedEntityData.defineId(RocketContraptionEntity.class, EntityDataSerializers.QUATERNION);
    /** Rockets on this client, positioned each client tick from their flights. */
    private static final Set<RocketContraptionEntity> CLIENT = Collections.newSetFromMap(new WeakHashMap<>());

    @Nullable
    private UUID flightId;
    /** The lowest stage still attached; blocks of lower stages have dropped away. */
    private int shownStage;
    /** How it is turned now (local to world). */
    private Quaternionf attitude = new Quaternionf();
    /** Where the server last put it, for clients that have no news of its flight. */
    @Nullable
    private Vec3 lerpTarget;

    public RocketContraptionEntity(EntityType<?> type, Level level) {
        super(type, level);
        if (level.isClientSide) CLIENT.add(this);
    }

    /** A puppet of {@code flight}, assembled from its saved contraption without the stages already dropped; null if it cannot be. */
    @Nullable
    public static RocketContraptionEntity create(ServerLevel level, Flight flight) {
        CompoundTag cargo = flight.cargo();
        if (cargo == null) return null;
        Contraption read;
        try {
            read = Contraption.fromNBT(level, cargo, false);
        } catch (RuntimeException e) {
            return null;
        }
        if (!(read instanceof RocketContraption full)) return null;
        int attached = attachedStage(flight);
        RocketContraptionEntity entity = new RocketContraptionEntity(RocketRegistry.ROCKET.get(), level);
        entity.flightId = flight.id();
        entity.shownStage = attached;
        entity.setContraption(full.filtered(stage -> stage >= attached, level.registryAccess(), level));
        entity.follow(flight);
        // Ready to draw from its first packet: no frame without a rocket when it replaces another.
        entity.contraptionInitialize();
        return entity;
    }

    /** The lowest stage still attached to {@code flight}: all of them burnt out leaves only the payload. */
    public static int attachedStage(Flight flight) {
        int stages = flight.flight().design().stages().size();
        return flight.flight().burnedOut() ? stages : Math.min(stages, flight.flight().stage());
    }

    @Nullable
    public UUID flightId() {
        return flightId;
    }

    public int shownStage() {
        return shownStage;
    }

    /** Puts it where {@code flight} is now, turned the way it flies, and lets go of the stages that dropped. Server side. */
    public void follow(Flight flight) {
        RocketAttitude.Quat q = RocketAttitude.orientation(Flight.v3(flight.velocity()), flight.heading());
        attitude = toJoml(q);
        entityData.set(ATTITUDE, new Quaternionf(attitude));
        V3 anchor = RocketPose.anchor(Flight.v3(flight.position()), q);
        setPos(anchor.x(), anchor.y(), anchor.z());
        int attached = attachedStage(flight);
        if (attached > shownStage && contraption instanceof RocketContraption rocket) {
            List<BlockPos> dropped = new ArrayList<>();
            for (Map.Entry<BlockPos, Integer> entry : rocket.stageOf().entrySet()) if (entry.getValue() < attached) dropped.add(entry.getKey());
            for (BlockPos local : dropped) {
                StructureBlockInfo info = rocket.blocks().get(local);
                if (info != null && !info.state().isAir()) setBlock(local, new StructureBlockInfo(local, Blocks.AIR.defaultBlockState(), null));
            }
            shownStage = attached;
        }
    }

    static Quaternionf toJoml(RocketAttitude.Quat q) {
        return new Quaternionf((float) q.x(), (float) q.y(), (float) q.z(), (float) q.w());
    }

    static RocketAttitude.Quat fromJoml(Quaternionf q) {
        return new RocketAttitude.Quat(q.x, q.y, q.z, q.w);
    }

    public Quaternionf attitude() {
        return new Quaternionf(attitude);
    }

    /** Rockets on this client; for the client tick that places them. */
    public static List<RocketContraptionEntity> clientRockets() {
        return new ArrayList<>(CLIENT);
    }

    /** Places it from the client's own flight state: at {@code previous} at the start of this tick, {@code now} at its end. */
    public void placeOnClient(V3 previous, V3 now, Quaternionf attitude) {
        this.attitude = attitude;
        setPos(now.x(), now.y(), now.z());
        xo = xOld = previous.x();
        yo = yOld = previous.y();
        zo = zOld = previous.z();
    }

    /** With no news of its flight on this client, falls back on where the server put it. */
    public void placeFromServer() {
        attitude = new Quaternionf(entityData.get(ATTITUDE));
        if (lerpTarget != null) {
            xo = xOld = getX();
            yo = yOld = getY();
            zo = zOld = getZ();
            setPos(lerpTarget.x, lerpTarget.y, lerpTarget.z);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ATTITUDE, new Quaternionf());
    }

    @Override
    protected void tickContraption() {
        if (level().isClientSide) return;
        FlightTracker tracker = level() instanceof ServerLevel server ? FlightTracker.getIfPresent(server) : null;
        if (flightId == null || tracker == null || tracker.get(flightId) == null) discard();
    }

    @Override
    public void setPos(double x, double y, double z) {
        super.setPos(x, y, z);
        if (contraption instanceof RocketContraption rocket) {
            // A cube round the pivot as tall as the rocket, whichever way it leans: culling, light and tracking all see it.
            Vec3 pivot = toGlobalVector(new Vec3(RocketPose.PIVOT.x(), RocketPose.PIVOT.y(), RocketPose.PIVOT.z()), 1);
            double reach = Math.max(2, rocket.height() + 1.5);
            setBoundingBox(new AABB(pivot, pivot).inflate(reach));
        }
    }

    @Override
    public Vec3 applyRotation(Vec3 local, float partialTicks) {
        return toVec3(attitude.transform(new org.joml.Vector3f((float) local.x, (float) local.y, (float) local.z)));
    }

    @Override
    public Vec3 reverseRotation(Vec3 world, float partialTicks) {
        return toVec3(new Quaternionf(attitude).conjugate().transform(new org.joml.Vector3f((float) world.x, (float) world.y, (float) world.z)));
    }

    private static Vec3 toVec3(org.joml.Vector3f v) {
        return new Vec3(v.x, v.y, v.z);
    }

    @Override
    public ContraptionRotationState getRotationState() {
        double[] euler = RocketPose.euler(fromJoml(attitude));
        ContraptionRotationState state = new ContraptionRotationState();
        state.xRotation = (float) euler[0];
        state.yRotation = (float) euler[1];
        state.zRotation = (float) euler[2];
        return state;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void applyLocalTransforms(PoseStack pose, float partialTicks) {
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(new Quaternionf(attitude));
        pose.translate(-0.5, -0.5, -0.5);
    }

    @Override
    protected StructureTransform makeStructureTransform() {
        return new StructureTransform(blockPosition(), 0, 0, 0);
    }

    /** Never put back into the world. */
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

    @OnlyIn(Dist.CLIENT)
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        lerpTarget = new Vec3(x, y, z);
    }

    @Override
    public void teleportTo(double x, double y, double z) {
    }

    @Override
    protected void writeAdditional(CompoundTag tag, HolderLookup.Provider registries, boolean spawnPacket) {
        super.writeAdditional(tag, registries, spawnPacket);
        if (flightId != null) tag.putUUID("Flight", flightId);
        tag.putInt("ShownStage", shownStage);
    }

    @Override
    protected void readAdditional(CompoundTag tag, boolean spawnData) {
        super.readAdditional(tag, spawnData);
        flightId = tag.hasUUID("Flight") ? tag.getUUID("Flight") : null;
        shownStage = tag.getInt("ShownStage");
    }

    @Override
    public void onClientRemoval() {
        super.onClientRemoval();
        CLIENT.remove(this);
    }
}
