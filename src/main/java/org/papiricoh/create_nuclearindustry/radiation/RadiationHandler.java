package org.papiricoh.create_nuclearindustry.radiation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.radiation.contamination.GeologicalStorage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Once a second, adds up the radiation a player is exposed to (carried items, nearby
 * radioactive blocks, fallout from accidents, running reactors and other emitters), applies
 * the shielding of the blocks in between and of worn armour, accumulates dose and applies its
 * symptoms. Dose decays slowly when exposure falls to background.
 */
public final class RadiationHandler {
    private static final int INTERVAL = 20;
    private static final int BLOCK_SCAN_RADIUS = 3;
    private static final double FALLOUT_RANGE = 48.0;
    public static final double EMITTER_RANGE = 32.0;
    public static final int EMITTER_LIMIT = 16;

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
            if (radioactivity != null) {
                heat += radioactivity.heatPerSecond() * stack.getCount();
            }
        }
        return heat;
    }

    public static double exposureRate(Player player) {
        Vec3 body = bodyOf(player);
        return inventoryRate(player) + blockRate(player.level(), player.blockPosition(), body)
                + falloutRate(player.level(), body) + emitterRate(player.level(), body)
                + groundRate(player, body);
    }

    /**
     * Dose rate a bare body at {@code body} would take from its surroundings: radioactive blocks,
     * fallout, emitters and contaminated ground, without anything carried. Used by control room monitors.
     */
    public static double ambientRate(ServerLevel level, Vec3 body) {
        double ground = Config.contaminationEnabled
                ? org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination.exposure(level, body, body.y - 0.9)
                : 0;
        return blockRate(level, BlockPos.containing(body), body) + falloutRate(level, body) + emitterRate(level, body) + ground;
    }

    /** Contaminated ground under and around the player. */
    public static double groundRate(Player player, Vec3 body) {
        if (!Config.contaminationEnabled || !(player.level() instanceof ServerLevel level)) {
            return 0;
        }
        return org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination.exposure(level, body, player.getY());
    }

    private static Vec3 bodyOf(Player player) {
        return player.position().add(0, player.getBbHeight() / 2, 0);
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
        return perItem * stack.getCount();
    }

    /** Radioactive blocks (waste barrels...) within a few blocks, falling off with distance and shielded by what is between. */
    private static double blockRate(Level level, BlockPos center, Vec3 body) {
        double rate = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -BLOCK_SCAN_RADIUS; dx <= BLOCK_SCAN_RADIUS; dx++) {
            for (int dy = -BLOCK_SCAN_RADIUS; dy <= BLOCK_SCAN_RADIUS; dy++) {
                for (int dz = -BLOCK_SCAN_RADIUS; dz <= BLOCK_SCAN_RADIUS; dz++) {
                    pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    var state = level.getBlockState(pos);
                    Radioactivity radioactivity = state.getBlock().asItem().builtInRegistryHolder().getData(AllNuclearRadiation.RADIOACTIVITY);
                    double strength = radioactivity != null && !GeologicalStorage.isStored(state, pos)
                            ? radioactivity.perSecond() : fluidStrength(state);
                    if (strength > 0) {
                        double open = strength / (1.0 + dx * dx + dy * dy + dz * dz);
                        rate += open * RadiationRay.transmission(level, Vec3.atCenterOf(pos), body);
                    }
                }
            }
        }
        return rate;
    }

    /** Radioactive liquid in the world irradiates like its bucket, less as it thins out when flowing. */
    private static double fluidStrength(net.minecraft.world.level.block.state.BlockState state) {
        var fluid = state.getFluidState();
        if (fluid.isEmpty()) {
            return 0;
        }
        Radioactivity radioactivity = fluid.getType().getBucket().builtInRegistryHolder().getData(AllNuclearRadiation.RADIOACTIVITY);
        return radioactivity == null ? 0 : radioactivity.perSecond() * fluid.getAmount() / 8.0;
    }

    private static double falloutRate(Level level, Vec3 body) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return 0;
        }
        double rate = 0;
        for (FalloutData.Source source : FalloutData.get(serverLevel).sources()) {
            Vec3 origin = Vec3.atCenterOf(source.pos());
            double distance = origin.distanceTo(body);
            if (distance <= FALLOUT_RANGE) {
                rate += pointExposure(level, origin, source.currentStrength(), body, null);
            }
        }
        return rate;
    }

    /** Running reactors, RTGs and the like: the nearest {@link #EMITTER_LIMIT} within range. */
    private static double emitterRate(Level level, Vec3 body) {
        double rate = 0;
        for (RadiationEmitter emitter : RadiationEmitters.near(level, body, EMITTER_RANGE, EMITTER_LIMIT)) {
            rate += pointExposure(level, emitter.radiationOrigin(), emitter.radiationStrength(), body, emitter.radiationIgnoreBox());
        }
        return rate;
    }

    /**
     * Dose per second at {@code body} from a point source of {@code strength}: falls off over a
     * few blocks and is cut by every block in between (except inside {@code ignore}).
     */
    public static double pointExposure(Level level, Vec3 origin, double strength, Vec3 body,
                                       @org.jetbrains.annotations.Nullable net.minecraft.world.level.levelgen.structure.BoundingBox ignore) {
        double distance = origin.distanceTo(body);
        return strength / (1.0 + (distance / 4.0) * (distance / 4.0)) * RadiationRay.transmission(level, origin, body, ignore);
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
