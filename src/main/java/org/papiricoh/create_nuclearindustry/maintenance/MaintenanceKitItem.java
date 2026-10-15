package org.papiricoh.create_nuclearindustry.maintenance;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements;
import org.papiricoh.create_nuclearindustry.advancement.NuclearEvent;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockController;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.steamgen.SteamGeneratorControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.turbine.AbstractTurbineControllerBlockEntity;

import java.util.List;

/**
 * Spare parts and tools for a plant's wearing parts. Used on any block of a fission reactor in a
 * refuelling outage (shut down cold, every rod in) it mends its control rod drives and relief valve
 * and frees a stuck rod; on a steam generator or a turbine that is not running, its tubes or
 * blades. Each use gives back {@code maintenance.kitRepair} of health and wears the kit; a Deployer
 * can use it too.
 */
public class MaintenanceKitItem extends Item {
    private static final String KEY = "item.create_nuclearindustry.maintenance_kit.";

    public MaintenanceKitItem(Properties properties) {
        super(properties);
    }

    /** What happened when a kit was used on a machine. */
    public enum Outcome {
        MENDED, NOTHING_TO_DO, NOT_STOPPED, NO_MACHINE;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Outcome outcome = service(level, context.getClickedPos(), MaintenanceConfig.kitRepair);
        Player player = context.getPlayer();
        if (player != null) {
            player.displayClientMessage(Component.translatable(KEY + outcome.id())
                    .withStyle(outcome == Outcome.MENDED ? ChatFormatting.GREEN : ChatFormatting.YELLOW), true);
        }
        if (outcome != Outcome.MENDED) {
            return outcome == Outcome.NO_MACHINE ? InteractionResult.PASS : InteractionResult.FAIL;
        }
        level.playSound(null, context.getClickedPos(), SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8f, 1.1f);
        NuclearAdvancements.award(player, NuclearEvent.MAINTENANCE_DONE);
        if (player != null) {
            context.getItemInHand().hurtAndBreak(1, player, context.getHand() == InteractionHand.MAIN_HAND
                    ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        return InteractionResult.CONSUME;
    }

    /** Services the machine at {@code pos}: the reactor, steam generator or turbine it is part of. */
    public static Outcome service(Level level, BlockPos pos, double amount) {
        MultiblockController machine = machineAt(level, pos);
        if (machine == null) {
            return Outcome.NO_MACHINE;
        }
        boolean stopped;
        boolean mended;
        if (machine instanceof ReactorBlockEntity reactor) {
            stopped = reactor.inOutage();
            mended = stopped && reactor.repair(amount);
        } else if (machine instanceof SteamGeneratorControllerBlockEntity generator) {
            stopped = generator.idleForRepair();
            mended = stopped && generator.repair(amount);
        } else if (machine instanceof AbstractTurbineControllerBlockEntity turbine) {
            stopped = turbine.idleForRepair();
            mended = stopped && turbine.repair(amount);
        } else {
            return Outcome.NO_MACHINE;
        }
        return !stopped ? Outcome.NOT_STOPPED : mended ? Outcome.MENDED : Outcome.NOTHING_TO_DO;
    }

    /** The reactor, steam generator or turbine whose structure holds {@code pos}. */
    @Nullable
    public static MultiblockController machineAt(Level level, BlockPos pos) {
        for (MultiblockController controller : MultiblockRegistry.affecting(level, pos)) {
            boolean kind = controller instanceof ReactorBlockEntity || controller instanceof SteamGeneratorControllerBlockEntity
                    || controller instanceof AbstractTurbineControllerBlockEntity;
            if (kind && (controller.getBlockPos().equals(pos) || controller.interestBounds().isInside(pos))) {
                return controller;
            }
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(KEY + "hint").withStyle(ChatFormatting.GRAY));
    }
}
