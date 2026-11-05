package org.papiricoh.create_nuclearindustry.civildefense.blast;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.ChunkPos;
import org.papiricoh.create_nuclearindustry.explosive.DetonationCause;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveConfig;
import org.papiricoh.create_nuclearindustry.explosive.blast.DeafeningModel;
import org.papiricoh.create_nuclearindustry.explosive.fx.ExplosionFxRegistry;
import org.papiricoh.create_nuclearindustry.integration.claims.ClaimGuards;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseConfig;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry;
import org.papiricoh.create_nuclearindustry.civildefense.shelter.ShelterRegistry;
import org.papiricoh.create_nuclearindustry.radiation.RadiationRay;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/**
 * The blast wave of nuclear detonations: a sphere growing from the centre at
 * {@code civilDefense.blastWave.speed} blocks per tick. Each tick it strikes the living entities in
 * the shell it just swept, with the {@link BlastWaveModel} overpressure for their distance times
 * what the blocks on the straight line from the centre let through, so walls and hills shelter
 * people behind them. Optionally it shatters fragile blocks near the surface on its way, never in a
 * protected claim. Waves are saved with their level ({@link BlastWaves}), so one under way when the world
 * closes carries on when it opens again. For fragile blocks each tick walks only the columns of the ring it
 * sweeps ({@link BlastShell#ringColumns}), not the whole square around it. Its damage is whoever set the
 * detonation off's ({@link DetonationCause}); a strong enough wave deafens the players it reaches.
 */
public final class BlastWaveManager {
    /** Columns of the ring looked at per tick for fragile blocks; beyond it, columns are sampled. */
    private static final int FRAGILE_COLUMNS_PER_TICK = 8192;
    /** How far below the top of a column the wave looks for fragile blocks. */
    private static final int FRAGILE_DEPTH = 16;
    /** Shattering effects (particles and sound) per tick; the rest just vanish. */
    private static final int EFFECTS_PER_TICK = 24;
    /**
     * Fragile blocks under cover traced back to the centre per tick: each trace walks every block on the way,
     * so a forest's canopy could ask for thousands a tick. Past it, covered blocks are left whole.
     */
    private static final int COVERED_TRACES_PER_TICK = 256;
    /** Speed below which a push is not worth sending. */
    private static final double MIN_PUSH = 0.01;

    private BlastWaveManager() {}

    /**
     * Starts the wave of a detonation of {@code power} at {@code centre} whose crater is
     * {@code craterRadius} blocks wide; it reaches four crater radii, at most
     * {@code civilDefense.blastWave.maxRadius}.
     */
    public static void start(ServerLevel level, Vec3 centre, float power, int craterRadius) {
        double radius = BlastWaveModel.maxRadius(craterRadius, CivilDefenseConfig.blastWaveMaxRadius);
        start(level, centre, BlastWaveModel.yield(power, CivilDefenseConfig.blastWaveYieldPerPower), radius);
    }

    /** Starts a wave of {@code yieldKg} kg of TNT equivalent that travels {@code maxRadius} blocks. */
    public static void start(ServerLevel level, Vec3 centre, double yieldKg, double maxRadius) {
        start(level, centre, yieldKg, maxRadius, DetonationCause.NONE, 0);
    }

    /**
     * As {@link #start(ServerLevel, Vec3, double, double)}, its harm caused by {@code cause}, getting {@code shelterReach} (0
     * none to 1 all) of a shelter's protection through, as a thermobaric charge's fuel flowing in before it lights does.
     */
    public static void start(ServerLevel level, Vec3 centre, double yieldKg, double maxRadius, DetonationCause cause, double shelterReach) {
        if (yieldKg <= 0 || maxRadius <= 0) {
            return;
        }
        BlastWaves.get(level).add(new Wave(centre, yieldKg, maxRadius, cause, Math.clamp(shelterReach, 0, 1)));
    }

    /** Waves still travelling in {@code level}. */
    public static int active(ServerLevel level) {
        BlastWaves waves = BlastWaves.getIfPresent(level);
        return waves == null ? 0 : waves.waves().size();
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlastWaves waves = BlastWaves.getIfPresent(level);
        if (waves == null || waves.waves().isEmpty()) {
            return;
        }
        Iterator<Wave> iterator = waves.waves().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().tick(level)) {
                iterator.remove();
            }
        }
        // Every tick a wave moves on, so what is saved is always a tick behind at most.
        waves.setDirty();
    }

    /** What one block lets through of the wave, by its explosion resistance; open blast doors are skipped by the walk. */
    public static double blockTransmission(BlockState state) {
        if (state.isAir()) {
            return 1.0;
        }
        return BlastWaveModel.blockTransmission(state.getBlock().getExplosionResistance(), CivilDefenseConfig.blastWaveResistanceScale);
    }

    /**
     * Overpressure, kPa, that reaches {@code body} from a wave of {@code yieldKg} at {@code centre}:
     * the free-air value, times what the blocks in between let through, times the shelter factor.
     */
    public static double overpressureAt(ServerLevel level, Vec3 centre, double yieldKg, Vec3 body) {
        double free = BlastWaveModel.overpressure(body.distanceTo(centre), yieldKg);
        if (free <= 0) {
            return 0;
        }
        ToDoubleFunction<BlockState> perBlock = BlastWaveManager::blockTransmission;
        return free * RadiationRay.walk(level, centre, body, null, perBlock) * ShelterRegistry.blastFactor(level, body);
    }

    /** As {@link #overpressureAt(ServerLevel, Vec3, double, Vec3)}, getting {@code shelterReach} of a shelter's protection through. */
    public static double overpressureAt(ServerLevel level, Vec3 centre, double yieldKg, Vec3 body, double shelterReach) {
        double free = BlastWaveModel.overpressure(body.distanceTo(centre), yieldKg);
        if (free <= 0) {
            return 0;
        }
        double shelter = ShelterRegistry.blastFactor(level, body);
        shelter += (1 - shelter) * shelterReach;
        return free * RadiationRay.walk(level, centre, body, null, BlastWaveManager::blockTransmission) * shelter;
    }

    /** One wave under way: where it started, how strong, how far it has come and what it has already struck. */
    public static final class Wave {
        private final Vec3 centre;
        private final double yieldKg;
        private final double maxRadius;
        private final DetonationCause cause;
        private final double shelterReach;
        private final Set<UUID> struck = new HashSet<>();
        private double radius;
        private ServerLevel level;
        private int tracesLeft;
        private final Long2BooleanOpenHashMap claimed = new Long2BooleanOpenHashMap();

        Wave(Vec3 centre, double yieldKg, double maxRadius, DetonationCause cause, double shelterReach) {
            this.centre = centre;
            this.yieldKg = yieldKg;
            this.maxRadius = maxRadius;
            this.cause = cause;
            this.shelterReach = shelterReach;
        }

        public double radius() {
            return radius;
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putDouble("X", centre.x);
            tag.putDouble("Y", centre.y);
            tag.putDouble("Z", centre.z);
            tag.putDouble("Yield", yieldKg);
            tag.putDouble("MaxRadius", maxRadius);
            tag.putDouble("Radius", radius);
            tag.putDouble("ShelterReach", shelterReach);
            cause.save(tag);
            ListTag ids = new ListTag();
            for (UUID id : struck) {
                ids.add(NbtUtils.createUUID(id));
            }
            tag.put("Struck", ids);
            return tag;
        }

        static Wave load(CompoundTag tag) {
            Wave wave = new Wave(new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z")), tag.getDouble("Yield"),
                    tag.getDouble("MaxRadius"), DetonationCause.load(tag), tag.getDouble("ShelterReach"));
            wave.radius = tag.getDouble("Radius");
            for (Tag id : tag.getList("Struck", Tag.TAG_INT_ARRAY)) {
                wave.struck.add(NbtUtils.loadUUID(id));
            }
            return wave;
        }

        /** Advances one tick in {@code level}; true once the wave has gone its whole way. */
        private boolean tick(ServerLevel level) {
            this.level = level;
            double previous = radius;
            radius = Math.min(maxRadius, radius + CivilDefenseConfig.blastWaveSpeed);
            strike(previous);
            throwShips(previous);
            if (CivilDefenseConfig.blastWaveBreakFragileBlocks && CivilDefenseConfig.blastWaveFragileBlocksPerTick > 0) {
                shatter(previous);
            }
            return radius >= maxRadius;
        }

        /**
         * Hits every living entity whose body lies in the shell from {@code previous} to {@code radius}.
         * One query over the sphere's box with the shell as predicate: the entity lookup only walks the
         * entity sections that hold something, so splitting the shell into many smaller boxes costs
         * more queries without a measured gain.
         */
        private void strike(double previous) {
            Predicate<LivingEntity> inShell = entity -> {
                if (!entity.isAlive() || entity.isSpectator() || struck.contains(entity.getUUID())) {
                    return false;
                }
                double distance = body(entity).distanceTo(centre);
                return distance <= radius && (distance > previous || previous == 0);
            };
            AABB box = new AABB(centre, centre).inflate(radius);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box, inShell);
            // Coming from the centre, so the hit's own knockback points away from it too.
            DamageSource source = cause.damage(level, CivilDefenseRegistry.BLAST_WAVE_DAMAGE, centre);
            for (LivingEntity entity : targets) {
                struck.add(entity.getUUID());
                Vec3 body = body(entity);
                if (ExplosiveConfig.claimsProtectEntities && claimed(BlockPos.containing(body))) {
                    continue;
                }
                double kPa = overpressureAt(level, centre, yieldKg, body, shelterReach);
                if (entity instanceof ServerPlayer player) {
                    deafen(player, kPa);
                }
                float damage = (float) BlastWaveModel.damage(kPa, CivilDefenseConfig.blastWaveDamageThreshold,
                        CivilDefenseConfig.blastWaveDamagePerKPa);
                if (damage > 0) {
                    entity.hurt(source, damage);
                }
                if (damage <= 0 && kPa > 0 && entity instanceof Player player && ShelterRegistry.isSheltered(level, body)) org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements.award(player, org.papiricoh.create_nuclearindustry.advancement.NuclearEvent.SHELTERED_FROM_BLAST);
                push(entity, body, kPa);
            }
        }

        /**
         * Throws the ships the shell reaches: the overpressure where the wave first meets each, on the
         * face it shows the blast, applied at that point so a ship struck off centre spins. Each ship
         * once per wave, and its fragile blocks shatter.
         */
        private void throwShips(double previous) {
            if (!org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace.enabled()) {
                return;
            }
            for (org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace.Ship ship : org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace.shipsIn(level, new AABB(centre, centre).inflate(radius))) {
                if (struck.contains(ship.id())) {
                    continue;
                }
                AABB bounds = ship.worldBounds();
                Vec3 nearest = new Vec3(Math.clamp(centre.x, bounds.minX, bounds.maxX), Math.clamp(centre.y, bounds.minY, bounds.maxY),
                        Math.clamp(centre.z, bounds.minZ, bounds.maxZ));
                double distance = nearest.distanceTo(centre);
                if (distance > radius || distance <= previous && previous != 0) {
                    continue;
                }
                struck.add(ship.id());
                double kPa = BlastWaveModel.overpressure(distance, yieldKg);
                if (kPa <= 0) {
                    continue;
                }
                org.papiricoh.create_nuclearindustry.explosive.ShipBlastManager.shatter(level, ship, centre, yieldKg);
                if (!org.papiricoh.create_nuclearindustry.integration.sable.AeronauticsConfig.blastPushOn()) {
                    continue;
                }
                Vec3 away = nearest.subtract(centre);
                Vec3 direction = away.lengthSqr() < 1.0e-6 ? new Vec3(0, 1, 0) : away.normalize();
                Vec3 local = ship.toLocalDirection(direction);
                net.minecraft.world.level.levelgen.structure.BoundingBox box = ship.localBounds();
                double area = org.papiricoh.create_nuclearindustry.explosive.blast.ShipImpulseModel.exposedArea(
                        box.getXSpan(), box.getYSpan(), box.getZSpan(), local.x, local.y, local.z);
                double impulse = org.papiricoh.create_nuclearindustry.explosive.blast.ShipImpulseModel.impulse(kPa, area, ship.mass(),
                        org.papiricoh.create_nuclearindustry.integration.sable.AeronauticsConfig.blastImpulseScale);
                if (impulse > 0) {
                    ship.impulse(nearest, direction.scale(impulse));
                }
            }
        }

        /** Deafens {@code player} for as long and as badly as {@code kPa} reaching them does. */
        private static void deafen(ServerPlayer player, double kPa) {
            if (!ExplosiveConfig.deafening || player.isCreative() || player.isSpectator()) {
                return;
            }
            int grade = DeafeningModel.grade(kPa, ExplosiveConfig.deafeningThresholdKPa);
            if (grade < 0) {
                return;
            }
            int ticks = (int) Math.round(20 * DeafeningModel.seconds(kPa, ExplosiveConfig.deafeningThresholdKPa));
            MobEffectInstance current = player.getEffect(ExplosionFxRegistry.DEAFENED);
            if (current == null || current.getAmplifier() < grade || current.getDuration() < ticks) {
                player.addEffect(new MobEffectInstance(ExplosionFxRegistry.DEAFENED, ticks, grade, false, false, true));
            }
        }

        /** Whether a claim keeps blasts away from {@code pos}'s chunk; asked once per chunk and wave. */
        private boolean claimed(BlockPos pos) {
            long key = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
            if (claimed.containsKey(key)) {
                return claimed.get(key);
            }
            boolean protectedChunk = ClaimGuards.protectsFromExplosions(level, pos.getX() >> 4, pos.getZ() >> 4);
            claimed.put(key, protectedChunk);
            return protectedChunk;
        }

        /** Throws the body away from the centre, a little upwards, less for whatever resists knockback. */
        private void push(LivingEntity entity, Vec3 body, double kPa) {
            if (entity instanceof Player player && player.isCreative() && player.getAbilities().flying) {
                return;
            }
            double speed = BlastWaveModel.knockback(kPa, CivilDefenseConfig.blastWaveKnockbackPerKPa)
                    * Math.max(0, 1.0 - entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            if (speed < MIN_PUSH) {
                return;
            }
            Vec3 away = body.subtract(centre);
            Vec3 direction = away.lengthSqr() < 1.0e-6 ? new Vec3(0, 1, 0) : away.normalize();
            entity.push(direction.x * speed, direction.y * speed + 0.3 * speed, direction.z * speed);
            entity.hurtMarked = true;
        }

        /**
         * Breaks fragile blocks in the top {@value #FRAGILE_DEPTH} blocks of the columns of the ring
         * just swept where the overpressure is high enough: the free-air value for exposed ones, less
         * what the blocks on the way let through for those under cover (a window in a wall facing the
         * blast breaks, one in the far wall may not). At most the configured number per tick.
         * Unloaded columns are skipped, never loaded.
         */
        private void shatter(double previous) {
            int cx = (int) Math.floor(centre.x);
            int cz = (int) Math.floor(centre.z);
            double inner2 = previous * previous;
            double outer2 = radius * radius;
            int area = (int) Math.ceil(Math.PI * (outer2 - inner2)) + 1;
            int stride = Math.max(1, (area + FRAGILE_COLUMNS_PER_TICK - 1) / FRAGILE_COLUMNS_PER_TICK);
            int budget = CivilDefenseConfig.blastWaveFragileBlocksPerTick;
            tracesLeft = COVERED_TRACES_PER_TICK;
            int[] broken = {0};
            int[] index = {0};
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            // Only the ring's columns, in the order the square loop over the whole disc met them.
            BlastShell.ringColumns(previous, radius, (dx, dz) -> {
                if (index[0]++ % stride != 0) {
                    return true;
                }
                broken[0] += shatterColumn(pos, cx + dx, cz + dz, budget - broken[0], broken[0]);
                return broken[0] < budget;
            });
        }

        private int shatterColumn(BlockPos.MutableBlockPos pos, int x, int z, int budget, int alreadyBroken) {
            pos.set(x, level.getMinBuildHeight(), z);
            if (!level.isLoaded(pos) || claimed(pos)) {
                return 0;
            }
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
            int broken = 0;
            boolean covered = false;
            for (int y = top; y > top - FRAGILE_DEPTH && y >= level.getMinBuildHeight() && broken < budget; y--) {
                pos.set(x, y, z);
                BlockState state = level.getBlockState(pos);
                if (state.isAir()) {
                    continue;
                }
                if (!state.is(CivilDefenseRegistry.BLAST_FRAGILE)) {
                    covered |= state.blocksMotion();
                    continue;
                }
                Vec3 at = Vec3.atCenterOf(pos);
                double kPa = BlastWaveModel.overpressure(at.distanceTo(centre), yieldKg);
                if (kPa < CivilDefenseConfig.blastWaveFragileOverpressure) {
                    continue;
                }
                if (covered) {
                    if (tracesLeft <= 0) {
                        continue;
                    }
                    tracesLeft--;
                    kPa *= RadiationRay.walk(level, centre, at, null, BlastWaveManager::blockTransmission);
                    if (kPa < CivilDefenseConfig.blastWaveFragileOverpressure) {
                        continue;
                    }
                }
                if (alreadyBroken + broken < EFFECTS_PER_TICK) {
                    level.levelEvent(2001, pos, Block.getId(state));
                }
                // Quietly: neighbours are not updated, so leaves round it do not start to decay at once.
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                broken++;
            }
            return broken;
        }

        private static Vec3 body(LivingEntity entity) {
            return entity.position().add(0, entity.getBbHeight() / 2, 0);
        }
    }
}
