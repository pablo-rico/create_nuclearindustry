package org.papiricoh.create_nuclearindustry.missile.cluster;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.explosive.DetonationCause;
import org.papiricoh.create_nuclearindustry.explosive.conventional.ConventionalDetonation;
import org.papiricoh.create_nuclearindustry.explosive.conventional.ConventionalKind;
import org.papiricoh.create_nuclearindustry.explosive.conventional.ConventionalRegistry;

import java.util.UUID;

/**
 * One of a cluster warhead's bomblets, falling from where the warhead opened. It goes off where it hits. A dud lies where it
 * landed and goes off later, at a moment drawn when it opened, or at once if anything hurts it or another blast reaches it.
 */
public class BombletEntity extends ThrowableItemProjectile {
    private float charge;
    private boolean dud;
    private boolean landed;
    /** Ticks a landed dud still lies before it goes off. */
    private int fuse;
    @Nullable
    private UUID cause;

    public BombletEntity(EntityType<? extends BombletEntity> type, Level level) {
        super(type, level);
    }

    /** A bomblet of {@code charge} TNT equivalents at {@code at}, going {@code velocity}; a dud waits {@code dudTicks} once down. */
    public static BombletEntity create(ServerLevel level, Vec3 at, Vec3 velocity, float charge, boolean dud, int dudTicks,
                                       DetonationCause cause) {
        BombletEntity bomblet = new BombletEntity(ConventionalRegistry.BOMBLET.get(), level);
        bomblet.setPos(at);
        bomblet.setDeltaMovement(velocity);
        bomblet.charge = charge;
        bomblet.dud = dud;
        bomblet.fuse = dudTicks;
        bomblet.cause = cause.owner();
        return bomblet;
    }

    public boolean dud() {
        return dud;
    }

    public boolean landed() {
        return landed;
    }

    @Override
    protected Item getDefaultItem() {
        return ConventionalRegistry.CLUSTER_BOMBLET.get();
    }

    @Override
    public void tick() {
        if (!landed) {
            super.tick();
            return;
        }
        baseTick();
        if (!level().isClientSide && --fuse <= 0) {
            explode();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level().isClientSide) {
            return;
        }
        if (!dud) {
            explode();
            return;
        }
        landed = true;
        setDeltaMovement(Vec3.ZERO);
        setNoGravity(true);
        setPos(result.getLocation());
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide && !dud) {
            explode();
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) {
            return false;
        }
        if (!level().isClientSide && isAlive()) {
            explode();
        }
        return true;
    }

    private void explode() {
        if (!(level() instanceof ServerLevel server) || isRemoved()) {
            return;
        }
        discard();
        ConventionalDetonation.detonate(server, position(), ConventionalKind.BOMBLET, charge, DetonationCause.of(cause));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Charge", charge);
        tag.putBoolean("Dud", dud);
        tag.putBoolean("Landed", landed);
        tag.putInt("Fuse", fuse);
        if (cause != null) {
            tag.putUUID("Cause", cause);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        charge = tag.getFloat("Charge");
        dud = tag.getBoolean("Dud");
        landed = tag.getBoolean("Landed");
        fuse = tag.getInt("Fuse");
        cause = tag.hasUUID("Cause") ? tag.getUUID("Cause") : null;
        if (landed) {
            setNoGravity(true);
        }
    }
}
