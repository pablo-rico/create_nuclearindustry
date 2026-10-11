package org.papiricoh.create_nuclearindustry.radiation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.PerformanceConfig;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.radiation.cache.BlockRadioactivity;
import org.papiricoh.create_nuclearindustry.radiation.cache.RadiationCaches;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;
import org.papiricoh.create_nuclearindustry.radiation.contamination.GeologicalStorage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Once a second, adds up the radiation a player is exposed to (carried items, nearby
 * radioactive blocks, fallout from accidents, running reactors and other emitters), applies
 * the shielding of the blocks in between and of worn armour, accumulates dose and applies its
 * symptoms. Dose decays slowly when exposure falls to background.
 * <p>
 * The block scan skips chunk sections whose palette holds nothing radioactive, and a player's
 * rays are reused (see {@link RadiationCaches}) while neither end moves and no block changes
 * along them; what each update cost is counted in {@link RadiationStats}.
 */
public final class RadiationHandler {
    private static final int INTERVAL = 20;
    private static final int BLOCK_SCAN_RADIUS = 3;
    private static final double FALLOUT_RANGE = 48.0;
    public static final double EMITTER_RANGE = 32.0;
    public static final int EMITTER_LIMIT = 16;

    /** Where a ray's result comes from: walked now, or a player's cache. */
    @FunctionalInterface
    private interface Rays {
        /** Transmission from {@code from} to {@code to}; {@code key} says where both ends are. */
        double transmission(Object key, Vec3 from, Vec3 to, @Nullable BoundingBox ignore);
    }

    /** A ray from the block at {@code source} (a radioactive block, a fallout source) to the body's block. */
    private record BlockRay(long source, long body) {}

    /** A ray from an emitter's exact origin, past its own core, to the body's block. */
    private record EmitterRay(Vec3 origin, @Nullable BoundingBox ignore, long body) {}

    /** The ray down from the ground surface to a body under cover. */
    private record GroundRay(long body, double surfaceY) {}

    private RadiationHandler() {}

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.tickCount % INTERVAL != 0) {
            return;
        }
        update(player);
    }

    /** One second of exposure. Public so tests can drive it. */
    public static void update(Player player) {
        if (!Config.radiationEnabled || player.isCreative() || player.isSpectator()) {
            player.setData(AllNuclearRadiation.RATE, 0.0f);
            publish(player, 0, player.getData(AllNuclearRadiation.DOSE), player.getData(AllNuclearRadiation.TOTAL_DOSE));
            return;
        }
        float rate = (float) (exposureRate(player) * (1.0 - shielding(player)) * absorption(player) * Config.radiationDoseMultiplier);
        if (rate < Config.radiationBackgroundRate) {
            rate = 0;
        }
        float dose = player.getData(AllNuclearRadiation.DOSE);
        dose = rate > 0 ? dose + rate : Math.max(0.0f, dose - (float) Config.radiationDecayPerSecond);
        dose = Math.max(0.0f, dose - purge(player));
        player.setData(AllNuclearRadiation.DOSE, dose);
        player.setData(AllNuclearRadiation.RATE, rate);
        float total = player.getData(AllNuclearRadiation.TOTAL_DOSE) + rate;
        if (rate > 0) {
            player.setData(AllNuclearRadiation.TOTAL_DOSE, total);
        }
        publish(player, rate, dose, total);
        applySymptoms(player, dose);
        applyDecayHeat(player);
    }

    /**
     * Updates the HUD reading only when its rounded values change; a synced attachment then
     * sends those few bytes to this player alone. Nothing is sent while there is nothing to show.
     */
    public static void publish(Player player, float rate, float dose, float total) {
        DoseReading reading = DoseReading.of(rate, dose, total);
        DoseReading previous = player.getExistingDataOrNull(AllNuclearRadiation.DOSE_HUD);
        if (!reading.visible()) {
            if (previous != null) {
                player.removeData(AllNuclearRadiation.DOSE_HUD);
            }
        } else if (!reading.equals(previous)) {
            player.setData(AllNuclearRadiation.DOSE_HUD, reading);
        }
    }

    /**
     * Plutonium is warm: carried without at least half shielding (gloves and suit), its decay
     * heat burns, up to a heart every two seconds.
     */
    private static void applyDecayHeat(Player player) {
        if ((player.tickCount / INTERVAL) % 2 != 0) {
            return;
        }
        double heat = inventoryHeat(player);
        if (heat <= 0 || shielding(player) >= 0.5f) {
            return;
        }
        float damage = (float) Math.min(2.0, 0.5 * Math.ceil(Math.sqrt(heat)));
        player.hurt(player.damageSources().source(AllNuclearRadiation.PLUTONIUM_BURN), damage);
    }

    public static double inventoryHeat(Player player) {
        double heat = 0;
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            Radioactivity radioactivity = stack.getItemHolder().getData(AllNuclearRadiation.RADIOACTIVITY);
            double perItem = (radioactivity != null ? radioactivity.heatPerSecond() : 0) + RadioactivityModifiers.extraHeat(stack);
            heat += perItem * stack.getCount();
        }
        return heat;
    }

    /** Everything the player takes in per second, before armour and medicine. Starts a new {@link RadiationStats}. */
    public static double exposureRate(Player player) {
        RadiationStats.begin();
        Level level = player.level();
        Vec3 body = bodyOf(player);
        Rays rays = cachedRays(player);
        return inventoryRate(player) + blockRate(level, player.blockPosition(), body, rays)
                + (falloutRate(level, body, rays) + groundRate(player, body, rays)) * ExposureModifiers.shelter(level, body)
                + emitterRate(level, body, rays);
    }

    /**
     * Dose rate a bare body at {@code body} would take from its surroundings: radioactive blocks,
     * fallout, emitters and contaminated ground, without anything carried. Used by control room monitors.
     */
    public static double ambientRate(ServerLevel level, Vec3 body) {
        Rays rays = directRays(level);
        double ground = Config.contaminationEnabled
                ? Contamination.exposure(level, body, body.y - 0.9, (from, to) -> rays.transmission(null, from, to, null))
                : 0;
        return blockRate(level, BlockPos.containing(body), body, rays) + emitterRate(level, body, rays)
                + (falloutRate(level, body, rays) + ground) * ExposureModifiers.shelter(level, body);
    }

    /** Contaminated ground under and around the player. */
    public static double groundRate(Player player, Vec3 body) {
        return groundRate(player, body, directRays(player.level()));
    }

    private static double groundRate(Player player, Vec3 body, Rays rays) {
        if (!Config.contaminationEnabled || !(player.level() instanceof ServerLevel level)) {
            return 0;
        }
        long bodyBlock = BlockPos.containing(body).asLong();
        return Contamination.exposure(level, body, player.getY(),
                (from, to) -> rays.transmission(new GroundRay(bodyBlock, from.y), from, to, null));
    }

    private static Vec3 bodyOf(Player player) {
        return player.position().add(0, player.getBbHeight() / 2, 0);
    }

    /** Rays walked every time, as monitors and anything without a player use them. */
    private static Rays directRays(Level level) {
        return (key, from, to, ignore) -> {
            RadiationStats.current().raysCast++;
            return RadiationRay.transmission(level, from, to, ignore);
        };
    }

    /**
     * Rays through the player's cache: reused for {@code performance.radiationRayCacheTicks} while
     * both ends stay in their blocks and nothing changes in the chunks between them.
     */
    private static Rays cachedRays(Player player) {
        Level level = player.level();
        if (level.isClientSide || PerformanceConfig.radiationRayCacheTicks <= 0) {
            return directRays(level);
        }
        RadiationCaches.PlayerCache cache = RadiationCaches.of(player);
        var changes = RadiationCaches.changes(level);
        long now = level.getGameTime();
        int ttl = PerformanceConfig.radiationRayCacheTicks;
        return (key, from, to, ignore) -> {
            RadiationStats stats = RadiationStats.current();
            int cast = stats.raysCast;
            double transmission = cache.rays().get(key, changes, now, ttl, from.x, from.z, to.x, to.z, () -> {
                stats.raysCast++;
                return RadiationRay.transmission(level, from, to, ignore);
            });
            if (stats.raysCast == cast) {
                stats.raysReused++;
            }
            return transmission;
        };
    }

    public static double inventoryRate(Player player) {
        double rate = 0;
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            rate += stackRate(inventory.getItem(slot));
        }
        return rate;
    }

    public static double stackRate(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        Radioactivity radioactivity = stack.getItemHolder().getData(AllNuclearRadiation.RADIOACTIVITY);
        if (radioactivity == null) {
            return 0;
        }
        double perItem = radioactivity.perSecond();
        if (radioactivity.scalesWithEnrichment()) {
            perItem *= UraniumItem.getEnrichment(stack) / 90.0;
        }
        return perItem * RadioactivityModifiers.doseScale(stack) * stack.getCount();
    }

    /**
     * Radioactive blocks (waste barrels...) within a few blocks, falling off with distance and
     * shielded by what is between. Only chunk sections whose palette may hold a radioactive state
     * are read; unloaded chunks hold nothing.
     */
    private static double blockRate(Level level, BlockPos center, Vec3 body, Rays rays) {
        RadiationStats stats = RadiationStats.current();
        int minX = center.getX() - BLOCK_SCAN_RADIUS, maxX = center.getX() + BLOCK_SCAN_RADIUS;
        int minY = center.getY() - BLOCK_SCAN_RADIUS, maxY = center.getY() + BLOCK_SCAN_RADIUS;
        int minZ = center.getZ() - BLOCK_SCAN_RADIUS, maxZ = center.getZ() + BLOCK_SCAN_RADIUS;
        long bodyBlock = BlockPos.asLong(center.getX(), center.getY(), center.getZ());
        double rate = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int chunkX = SectionPos.blockToSectionCoord(minX); chunkX <= SectionPos.blockToSectionCoord(maxX); chunkX++) {
            for (int chunkZ = SectionPos.blockToSectionCoord(minZ); chunkZ <= SectionPos.blockToSectionCoord(maxZ); chunkZ++) {
                ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (chunk == null) {
                    continue;
                }
                for (int sectionY = SectionPos.blockToSectionCoord(minY); sectionY <= SectionPos.blockToSectionCoord(maxY); sectionY++) {
                    int index = chunk.getSectionIndexFromSectionY(sectionY);
                    if (index < 0 || index >= chunk.getSectionsCount()) {
                        continue;
                    }
                    LevelChunkSection section = chunk.getSection(index);
                    if (section.hasOnlyAir() || !section.maybeHas(BlockRadioactivity::emits)) {
                        stats.sectionsSkipped++;
                        continue;
                    }
                    stats.sectionsScanned++;
                    int x0 = Math.max(minX, SectionPos.sectionToBlockCoord(chunkX)), x1 = Math.min(maxX, SectionPos.sectionToBlockCoord(chunkX) + 15);
                    int y0 = Math.max(minY, SectionPos.sectionToBlockCoord(sectionY)), y1 = Math.min(maxY, SectionPos.sectionToBlockCoord(sectionY) + 15);
                    int z0 = Math.max(minZ, SectionPos.sectionToBlockCoord(chunkZ)), z1 = Math.min(maxZ, SectionPos.sectionToBlockCoord(chunkZ) + 15);
                    // Neighbouring blocks are mostly the same state (air, stone): look each run up once.
                    BlockState lastState = null;
                    BlockRadioactivity.Source source = null;
                    for (int x = x0; x <= x1; x++) {
                        for (int y = y0; y <= y1; y++) {
                            for (int z = z0; z <= z1; z++) {
                                stats.blockLookups++;
                                BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                                if (state != lastState) {
                                    lastState = state;
                                    source = BlockRadioactivity.of(state);
                                }
                                if (source == null) {
                                    continue;
                                }
                                pos.set(x, y, z);
                                // Waste packages give off less the better they are packed and buried.
                                double strength = source.solid()
                                        ? GeologicalStorage.emission(level, state, pos, source.perSecond()) : source.perSecond();
                                if (strength > 0) {
                                    int dx = x - center.getX(), dy = y - center.getY(), dz = z - center.getZ();
                                    double open = strength / (1.0 + dx * dx + dy * dy + dz * dz);
                                    rate += open * rays.transmission(new BlockRay(pos.asLong(), bodyBlock), Vec3.atCenterOf(pos), body, null);
                                }
                            }
                        }
                    }
                }
            }
        }
        return rate;
    }

    private static double falloutRate(Level level, Vec3 body, Rays rays) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return 0;
        }
        double rate = 0;
        long bodyBlock = BlockPos.containing(body).asLong();
        for (FalloutData.Source source : FalloutData.get(serverLevel).sources()) {
            Vec3 origin = Vec3.atCenterOf(source.pos());
            double distance = origin.distanceTo(body);
            if (distance <= FALLOUT_RANGE) {
                double strength = source.currentStrength();
                if (strength > 0) {
                    rate += pointExposure(origin, strength, body,
                            rays.transmission(new BlockRay(source.pos().asLong(), bodyBlock), origin, body, null));
                }
            }
        }
        return rate;
    }

    /** Running reactors, RTGs and the like: the nearest {@link #EMITTER_LIMIT} within range. */
    private static double emitterRate(Level level, Vec3 body, Rays rays) {
        double rate = 0;
        long bodyBlock = BlockPos.containing(body).asLong();
        for (RadiationEmitter emitter : RadiationEmitters.near(level, body, EMITTER_RANGE, EMITTER_LIMIT)) {
            Vec3 origin = emitter.radiationOrigin();
            BoundingBox ignore = emitter.radiationIgnoreBox();
            rate += pointExposure(origin, emitter.radiationStrength(), body,
                    rays.transmission(new EmitterRay(origin, ignore, bodyBlock), origin, body, ignore));
        }
        return rate;
    }

    /**
     * Dose per second at {@code body} from a point source of {@code strength}: falls off over a
     * few blocks and is cut by every block in between (except inside {@code ignore}).
     */
    public static double pointExposure(Level level, Vec3 origin, double strength, Vec3 body, @Nullable BoundingBox ignore) {
        return pointExposure(origin, strength, body, RadiationRay.transmission(level, origin, body, ignore));
    }

    private static double pointExposure(Vec3 origin, double strength, Vec3 body, double transmission) {
        double distance = origin.distanceTo(body);
        return strength / (1.0 + (distance / 4.0) * (distance / 4.0)) * transmission;
    }

    /** Fraction of the dose rate that is taken up: potassium iodide halves it, each level again. */
    public static double absorption(Player player) {
        var effect = player.getEffect(AllNuclearRadiation.RADIOPROTECTION);
        return effect == null ? 1.0 : Math.pow(Config.iodideAbsorption, effect.getAmplifier() + 1);
    }

    /** Dose Prussian blue flushes this second; it never touches the lifetime total. */
    public static float purge(Player player) {
        var effect = player.getEffect(AllNuclearRadiation.RADIATION_PURGE);
        return effect == null ? 0.0f : (float) (Config.purgePerSecond * (effect.getAmplifier() + 1));
    }

    /** Fraction of radiation stopped by worn armour, from the radiation_shielding data map. */
    public static float shielding(Player player) {
        float total = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack worn = player.getItemBySlot(slot);
            if (worn.isEmpty()) {
                continue;
            }
            RadiationShielding shielding = worn.getItemHolder().getData(AllNuclearRadiation.SHIELDING);
            if (shielding != null) {
                total += shielding.fraction();
            }
        }
        return (float) Math.min(Config.radiationMaxShielding, total);
    }

    private static void applySymptoms(Player player, float dose) {
        if (dose >= Config.radiationSicknessThreshold) {
            org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements.award(player, org.papiricoh.create_nuclearindustry.advancement.NuclearEvent.RADIATION_SICKNESS);
            int amplifier = dose >= 2 * Config.radiationSicknessThreshold ? 1 : 0;
            player.addEffect(new MobEffectInstance(AllNuclearRadiation.RADIATION_SICKNESS, 60, amplifier, true, true));
        }
        if (dose >= Config.radiationWeaknessThreshold) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, true));
        }
        if (dose >= Config.radiationNauseaThreshold) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, true, false));
        }
    }
}
