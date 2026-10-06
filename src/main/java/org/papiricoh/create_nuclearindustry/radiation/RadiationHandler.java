package org.papiricoh.create_nuclearindustry.radiation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import net.minecraft.server.level.ServerLevel;

/**
 * Once a second, adds up the radiation a player is exposed to (carried items, nearby
 * radioactive blocks, fallout from accidents), applies hazmat shielding, accumulates dose
 * and applies its symptoms. Dose decays slowly when exposure stops.
 */
public final class RadiationHandler {
    private static final int INTERVAL = 20;
    private static final int BLOCK_SCAN_RADIUS = 3;
    private static final double EMITTER_RANGE = 48.0;
    private static final float HAZMAT_PER_PIECE = 0.24f;
    private static final float HAZMAT_MAX = 0.95f;

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
            return;
        }
        float rate = (float) (exposureRate(player) * (1.0 - shielding(player)) * Config.radiationDoseMultiplier);
        float dose = player.getData(AllNuclearRadiation.DOSE);
        dose = rate > 0 ? dose + rate : Math.max(0.0f, dose - (float) Config.radiationDecayPerSecond);
        player.setData(AllNuclearRadiation.DOSE, dose);
        player.setData(AllNuclearRadiation.RATE, rate);
        applySymptoms(player, dose);
    }

    public static double exposureRate(Player player) {
        return inventoryRate(player) + blockRate(player.level(), player.blockPosition()) + emitterRate(player.level(), player.blockPosition());
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

    /** Radioactive blocks (waste barrels...) within a few blocks, falling off with distance. */
    private static double blockRate(Level level, BlockPos center) {
        double rate = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -BLOCK_SCAN_RADIUS; dx <= BLOCK_SCAN_RADIUS; dx++) {
            for (int dy = -BLOCK_SCAN_RADIUS; dy <= BLOCK_SCAN_RADIUS; dy++) {
                for (int dz = -BLOCK_SCAN_RADIUS; dz <= BLOCK_SCAN_RADIUS; dz++) {
                    pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    var item = level.getBlockState(pos).getBlock().asItem();
                    Radioactivity radioactivity = item.builtInRegistryHolder().getData(AllNuclearRadiation.RADIOACTIVITY);
                    if (radioactivity != null) {
                        rate += radioactivity.perSecond() / (1.0 + dx * dx + dy * dy + dz * dz);
                    }
                }
            }
        }
        return rate;
    }

    private static double emitterRate(Level level, BlockPos center) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return 0;
        }
        double rate = 0;
        for (FalloutData.Source source : FalloutData.get(serverLevel).sources()) {
            double distance = Math.sqrt(source.pos().distSqr(center));
            if (distance <= EMITTER_RANGE) {
                rate += source.currentStrength() / (1.0 + (distance / 4.0) * (distance / 4.0));
            }
        }
        return rate;
    }

    /** Fraction of radiation stopped by worn hazmat pieces, 0..0.95. */
    public static float shielding(Player player) {
        int pieces = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (player.getItemBySlot(slot).getItem() instanceof ArmorItem armor
                    && armor.getMaterial().is(AllNuclearRadiation.HAZMAT_MATERIAL.getKey())) {
                pieces++;
            }
        }
        return Math.min(HAZMAT_MAX, pieces * HAZMAT_PER_PIECE);
    }

    private static void applySymptoms(Player player, float dose) {
        if (dose >= Config.radiationSicknessThreshold) {
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
