package org.papiricoh.create_nuclearindustry.explosive;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Who set a detonation off: the player who launched the missile, armed the bomb or fired the shell, if anyone did. Its damage
 * is theirs, so kills are credited to them and protection mods can tell whose blast it was. Saved with whatever carries it.
 *
 * @param owner the player's id, or null for a blast nobody answers for
 */
public record DetonationCause(@Nullable UUID owner) {
    public static final DetonationCause NONE = new DetonationCause(null);

    public static DetonationCause of(@Nullable Entity entity) {
        return entity == null ? NONE : new DetonationCause(entity.getUUID());
    }

    public static DetonationCause of(@Nullable UUID owner) {
        return owner == null ? NONE : new DetonationCause(owner);
    }

    /** The owner if they are in {@code level} now (a player anywhere on the server counts), else null. */
    public @Nullable Entity entity(ServerLevel level) {
        if (owner == null) {
            return null;
        }
        Entity entity = level.getEntity(owner);
        return entity != null ? entity : level.getServer().getPlayerList().getPlayer(owner);
    }

    /** Damage of {@code type} coming from {@code at}, caused by the owner when there is one. */
    public DamageSource damage(ServerLevel level, ResourceKey<DamageType> type, Vec3 at) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type), null,
                entity(level), at);
    }

    public void save(CompoundTag tag) {
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
    }

    public static DetonationCause load(CompoundTag tag) {
        return tag.hasUUID("Owner") ? new DetonationCause(tag.getUUID("Owner")) : NONE;
    }
}
