package org.papiricoh.create_nuclearindustry.isotope;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;
import org.papiricoh.create_nuclearindustry.isotope.physics.MedicalKitModel;

import java.util.List;
import java.util.Locale;

/**
 * Nuclear medicine: a scan with a dose of technetium-99m. Used on yourself, or on another player,
 * it takes one Tc-99m dose from the user's inventory (the strongest) and gives a full diagnosis:
 * the dose held, what is adding to it and from where, and the lifetime total. The tracer left in
 * the body also makes potassium iodide and Prussian blue work better for a while
 * ({@link MedicalKitModel}). Technetium-99m lasts hours, so doses are made shortly before use.
 */
public class MedicalIsotopeKitItem extends Item {
    private static final String KEY = "item.create_nuclearindustry.medical_isotope_kit.";

    public MedicalIsotopeKitItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        return scan(level, player, player) ? InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
                : InteractionResultHolder.fail(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Player patient)) {
            return InteractionResult.PASS;
        }
        return scan(player.level(), player, patient) ? InteractionResult.sidedSuccess(player.level().isClientSide) : InteractionResult.FAIL;
    }

    /** Scans {@code patient} with a dose from {@code doctor}'s inventory. Returns whether a dose was used. */
    public static boolean scan(Level level, Player doctor, Player patient) {
        if (level.isClientSide) {
            return true;
        }
        long now = level.getGameTime();
        ItemStack dose = strongestDose(doctor, now);
        if (dose == null) {
            doctor.displayClientMessage(Component.translatable(KEY + "no_dose").withStyle(ChatFormatting.RED), true);
            return false;
        }
        double activity = Isotopes.activity(dose, now);
        MedicalKitModel.Params p = IsotopeConfig.kit();
        if (!MedicalKitModel.usable(p, activity)) {
            doctor.displayClientMessage(Component.translatable(KEY + "decayed").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!doctor.getAbilities().instabuild) {
            dose.shrink(1);
        }
        for (Component line : diagnosis(patient)) {
            doctor.sendSystemMessage(line);
        }
        double boost = MedicalKitModel.boost(p, activity);
        int amplifier = MedicalKitModel.effectAmplifier(boost);
        if (amplifier >= 0) {
            patient.addEffect(new MobEffectInstance(IsotopeRegistry.TRACER, MedicalKitModel.boostTicks(p, activity), amplifier));
        }
        level.playSound(null, patient.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.4f, 1.8f);
        doctor.getCooldowns().addCooldown(IsotopeRegistry.MEDICAL_ISOTOPE_KIT.get(), 40);
        return true;
    }

    /** The patient's dose, what is adding to it now, and their lifetime total. */
    public static List<Component> diagnosis(Player patient) {
        float dose = patient.getData(AllNuclearRadiation.DOSE);
        float total = patient.getData(AllNuclearRadiation.TOTAL_DOSE);
        float rate = patient.getData(AllNuclearRadiation.RATE);
        double carried = RadiationHandler.inventoryRate(patient);
        double ground = RadiationHandler.groundRate(patient, patient.position().add(0, 0.9, 0));
        return List.of(
                Component.translatable(KEY + "header", patient.getDisplayName()).withStyle(ChatFormatting.GOLD),
                line("dose", ChatFormatting.WHITE, fmt(dose)),
                line("rate", rate > 0 ? ChatFormatting.YELLOW : ChatFormatting.GREEN, fmt(rate)),
                line("carried", carried > 0 ? ChatFormatting.YELLOW : ChatFormatting.GRAY, fmt(carried)),
                line("ground", ground > 0 ? ChatFormatting.YELLOW : ChatFormatting.GRAY, fmt(ground)),
                line("total", ChatFormatting.GRAY, fmt(total)),
                line(patient.hasEffect(AllNuclearRadiation.RADIOPROTECTION) ? "iodide_on" : "iodide_off", ChatFormatting.AQUA),
                line(patient.hasEffect(AllNuclearRadiation.RADIATION_PURGE) ? "purge_on" : "purge_off", ChatFormatting.AQUA));
    }

    private static Component line(String key, ChatFormatting style, Object... args) {
        return Component.literal("  ").append(Component.translatable(KEY + key, args)).withStyle(style);
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /** The Tc-99m dose with the most activity left in {@code player}'s inventory, or null. */
    @Nullable
    public static ItemStack strongestDose(Player player, long gameTime) {
        ItemStack best = null;
        double bestActivity = -1;
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(IsotopeRegistry.TECHNETIUM_99M.get())) {
                double activity = Isotopes.activity(stack, gameTime);
                if (activity > bestActivity) {
                    best = stack;
                    bestActivity = activity;
                }
            }
        }
        return best;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(KEY + "hint").withStyle(ChatFormatting.GRAY));
    }
}
