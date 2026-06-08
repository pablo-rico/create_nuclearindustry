package org.papiricoh.create_nuclearindustry.missile.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.explosive.NuclearBlastManager;

/**
 * Misil ICBM: vuela del punto de lanzamiento al objetivo describiendo un arco balistico
 * (ascenso -> crucero -> descenso) y al impactar detona reutilizando {@link NuclearBlastManager}.
 * El movimiento es autoritario en servidor; la posicion/rotacion se sincronizan por el tracker de
 * entidades y el cliente solo dibuja la estela de particulas.
 */
public class MissileEntity extends Entity {
    private double startX, startY, startZ;
    private double targetX, targetY, targetZ;
    private float power;
    private int hRadius;
    private int vRadius;
    private float intensity;
    private int flightTicks = 100;
    private int age;
    private double arcHeight = 60.0;
    // Chunk forzado a cargar que sigue al misil, para que siga tickeando aunque vuele lejos del jugador.
    private int forcedChunkX = Integer.MIN_VALUE;
    private int forcedChunkZ = Integer.MIN_VALUE;

    public MissileEntity(EntityType<? extends MissileEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    /** Configura la trayectoria y la carga al ser lanzado desde la plataforma (lado servidor). */
    public void configure(Vec3 start, Vec3 target, float power, int hRadius, int vRadius, float intensity) {
        this.startX = start.x;
        this.startY = start.y;
        this.startZ = start.z;
        this.targetX = target.x;
        this.targetY = target.y;
        this.targetZ = target.z;
        this.power = power;
        this.hRadius = hRadius;
        this.vRadius = vRadius;
        this.intensity = intensity;
        this.age = 0;

        double horizontalDistance = Math.hypot(target.x - start.x, target.z - start.z);
        // Duracion y altura de arco escaladas con la distancia (con minimos para que se vea el arco).
        this.flightTicks = (int) Mth.clamp(40 + horizontalDistance * 0.8, 60, 600);
        this.arcHeight = Mth.clamp(40 + horizontalDistance * 0.5, 50, 180);
        setPos(start.x, start.y, start.z);
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            spawnTrail();
            return;
        }

        age++;
        double t = flightTicks <= 0 ? 1.0 : (double) age / flightTicks;
        if (t >= 1.0) {
            detonate();
            return;
        }

        Vec3 previous = position();
        Vec3 next = trajectory(t);
        setPos(next.x, next.y, next.z);
        updateRotation(next.subtract(previous));
        followChunk(next);
    }

    /** Mantiene cargado el chunk actual del misil (liberando el anterior) para que no deje de tickear. */
    private void followChunk(Vec3 pos) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int cx = Mth.floor(pos.x) >> 4;
        int cz = Mth.floor(pos.z) >> 4;
        if (cx == forcedChunkX && cz == forcedChunkZ) {
            return;
        }
        releaseForcedChunk(serverLevel);
        serverLevel.setChunkForced(cx, cz, true);
        forcedChunkX = cx;
        forcedChunkZ = cz;
    }

    private void releaseForcedChunk(ServerLevel serverLevel) {
        if (forcedChunkX != Integer.MIN_VALUE) {
            serverLevel.setChunkForced(forcedChunkX, forcedChunkZ, false);
            forcedChunkX = Integer.MIN_VALUE;
            forcedChunkZ = Integer.MIN_VALUE;
        }
    }

    private Vec3 trajectory(double t) {
        double x = Mth.lerp(t, startX, targetX);
        double z = Mth.lerp(t, startZ, targetZ);
        double base = Mth.lerp(t, startY, targetY);
        double y = base + 4.0 * arcHeight * t * (1.0 - t);
        return new Vec3(x, y, z);
    }

    private void updateRotation(Vec3 velocity) {
        double horizontal = Math.hypot(velocity.x, velocity.z);
        if (velocity.lengthSqr() < 1.0e-6) {
            return;
        }
        setYRot((float) (Mth.atan2(-velocity.x, velocity.z) * (180.0 / Math.PI)));
        setXRot((float) (-Mth.atan2(velocity.y, horizontal) * (180.0 / Math.PI)));
    }

    private void spawnTrail() {
        Vec3 pos = position();
        for (int i = 0; i < 2; i++) {
            double ox = (random.nextDouble() - 0.5) * 0.3;
            double oz = (random.nextDouble() - 0.5) * 0.3;
            level().addParticle(ParticleTypes.SMOKE, pos.x + ox, pos.y, pos.z + oz, 0.0, -0.02, 0.0);
        }
        level().addParticle(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 0.0, -0.05, 0.0);
    }

    private void detonate() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            discard();
            return;
        }
        releaseForcedChunk(serverLevel);
        serverLevel.explode(null, targetX, targetY, targetZ,
                Math.min(64.0f, power * 0.35f), Level.ExplosionInteraction.BLOCK);
        NuclearBlastManager.start(serverLevel, BlockPos.containing(targetX, targetY, targetZ), hRadius, vRadius, intensity);
        discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel serverLevel) {
            releaseForcedChunk(serverLevel);
        }
        super.remove(reason);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // El misil no usa datos sincronizados: la posicion/rotacion las propaga el tracker de entidades.
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        startX = tag.getDouble("StartX");
        startY = tag.getDouble("StartY");
        startZ = tag.getDouble("StartZ");
        targetX = tag.getDouble("TargetX");
        targetY = tag.getDouble("TargetY");
        targetZ = tag.getDouble("TargetZ");
        power = tag.getFloat("Power");
        hRadius = tag.getInt("HRadius");
        vRadius = tag.getInt("VRadius");
        intensity = tag.getFloat("Intensity");
        flightTicks = tag.getInt("FlightTicks");
        age = tag.getInt("Age");
        arcHeight = tag.getDouble("ArcHeight");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("StartX", startX);
        tag.putDouble("StartY", startY);
        tag.putDouble("StartZ", startZ);
        tag.putDouble("TargetX", targetX);
        tag.putDouble("TargetY", targetY);
        tag.putDouble("TargetZ", targetZ);
        tag.putFloat("Power", power);
        tag.putInt("HRadius", hRadius);
        tag.putInt("VRadius", vRadius);
        tag.putFloat("Intensity", intensity);
        tag.putInt("FlightTicks", flightTicks);
        tag.putInt("Age", age);
        tag.putDouble("ArcHeight", arcHeight);
    }
}
