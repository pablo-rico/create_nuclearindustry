package org.papiricoh.create_nuclearindustry.isotope;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.isotope.physics.RadiographyModel;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockController;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Industrial gamma radiography: a cobalt-60 source shines through a machine onto film. Used on any
 * block of a formed multiblock, with a cobalt-60 source in the inventory, it shows what the machine
 * hides ({@link RadiographyTarget}); a weaker source sees fewer details. The operator takes a small
 * dose per shot.
 */
public class RadiographyCameraItem extends Item {
    private static final String KEY = "item.create_nuclearindustry.radiography_camera.";

    public RadiographyCameraItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        long now = level.getGameTime();
        ItemStack source = strongestSource(player, now);
        RadiographyModel.Params p = IsotopeConfig.radiography();
        double activity = source == null ? 0 : Isotopes.activity(source, now);
        if (!RadiographyModel.usable(p, activity)) {
            player.displayClientMessage(Component.translatable(KEY + (source == null ? "no_source" : "weak_source"))
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        List<Component> lines = radiograph(level, context.getClickedPos());
        if (lines == null) {
            player.displayClientMessage(Component.translatable(KEY + "nothing").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.FAIL;
        }
        int depth = RadiographyModel.penetration(p, activity);
        player.sendSystemMessage(Component.translatable(KEY + "header", depth).withStyle(ChatFormatting.GOLD));
        for (int i = 0; i < lines.size(); i++) {
            player.sendSystemMessage(i < depth ? Component.literal("  ").append(lines.get(i))
                    : Component.translatable(KEY + "too_deep").withStyle(ChatFormatting.DARK_GRAY));
            if (i >= depth) {
                break;
            }
        }
        RadiationHandler.addPromptDose(player, (float) RadiographyModel.operatorDose(p, activity));
        level.playSound(null, context.getClickedPos(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 0.8f, 0.7f);
        player.getCooldowns().addCooldown(this, RadiographyModel.exposureTicks(p, activity));
        return InteractionResult.CONSUME;
    }

    /** What a radiograph of the machine at {@code pos} shows, or null if there is no machine there. */
    @Nullable
    public static List<Component> radiograph(Level level, BlockPos pos) {
        BlockEntity here = level.getBlockEntity(pos);
        if (here instanceof RadiographyTarget target) {
            return target.radiograph();
        }
        for (MultiblockController controller : MultiblockRegistry.affecting(level, pos)) {
            if (controller instanceof RadiographyTarget target && controller.interestBounds().isInside(pos)) {
                return target.radiograph();
            }
        }
        List<MultiblockController> any = new ArrayList<>(MultiblockRegistry.affecting(level, pos));
        if (!any.isEmpty() && any.get(0).interestBounds().isInside(pos)) {
            return List.of(Component.translatable(KEY + "sound").withStyle(ChatFormatting.GREEN));
        }
        return null;
    }

    /** The cobalt-60 source with the most activity left in {@code player}'s inventory, or null. */
    @Nullable
    public static ItemStack strongestSource(Player player, long gameTime) {
        ItemStack best = null;
        double bestActivity = -1;
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(IsotopeRegistry.COBALT_60.get())) {
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
