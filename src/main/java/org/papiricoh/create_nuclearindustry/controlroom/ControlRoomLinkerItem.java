package org.papiricoh.create_nuclearindustry.controlroom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearDataComponents;
import org.papiricoh.create_nuclearindustry.Config;

import java.util.List;

/**
 * Links control room devices to what they watch. Click a reactor, turbine or cooling tower (any of
 * their blocks) or any other block to remember it, then click a monitor, panel or alarm to link it.
 * Sneak-click a device to unlink it; sneak-use in the air to forget the stored target.
 */
public class ControlRoomLinkerItem extends Item {
    private static final String KEY = "item.create_nuclearindustry.control_room_linker.";

    public ControlRoomLinkerItem(Properties properties) {
        super(properties);
    }

    @Nullable
    public static GlobalPos getLink(ItemStack stack) {
        return stack.get(AllNuclearDataComponents.LINKED_TARGET.get());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (level.isClientSide || player == null) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        if (level.getBlockEntity(clicked) instanceof ControlRoomDevice device) {
            if (player.isShiftKeyDown()) {
                device.setTarget(null);
                message(player, Component.translatable(KEY + "unlinked"), ChatFormatting.YELLOW);
                return InteractionResult.SUCCESS;
            }
            GlobalPos link = getLink(stack);
            if (link == null) {
                message(player, Component.translatable(KEY + "empty"), ChatFormatting.RED);
                return InteractionResult.SUCCESS;
            }
            String problem = LinkedTarget.reachProblem(level, clicked, link);
            if (LinkedTarget.OUT_OF_RANGE.equals(problem) || LinkedTarget.OTHER_DIMENSION.equals(problem)) {
                message(player, Component.translatable(KEY + "out_of_range", Config.controlRoomLinkRange), ChatFormatting.RED);
                return InteractionResult.SUCCESS;
            }
            device.setTarget(link);
            message(player, Component.translatable(KEY + "applied", describe(level, link)), ChatFormatting.GREEN);
            return InteractionResult.SUCCESS;
        }
        GlobalPos link = GlobalPos.of(level.dimension(), LinkedTarget.linkPosFor(level, clicked));
        stack.set(AllNuclearDataComponents.LINKED_TARGET.get(), link);
        message(player, Component.translatable(KEY + "stored", describe(level, link)), ChatFormatting.AQUA);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && getLink(stack) != null) {
            if (!level.isClientSide) {
                stack.remove(AllNuclearDataComponents.LINKED_TARGET.get());
                message(player, Component.translatable(KEY + "cleared"), ChatFormatting.YELLOW);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getLink(stack) != null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        GlobalPos link = getLink(stack);
        if (link == null) {
            tooltip.add(Component.translatable(KEY + "none").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        BlockPos pos = link.pos();
        tooltip.add(Component.translatable(KEY + "target", pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.AQUA));
    }

    /** "Reactor at x y z", naming what the link points at when it is loaded. */
    private static Component describe(Level level, GlobalPos link) {
        BlockPos pos = link.pos();
        TargetKind kind = level.dimension().equals(link.dimension()) && level.isLoaded(pos)
                ? LinkedTarget.kindAt(level, pos)
                : TargetKind.AREA;
        return Component.translatable(KEY + "describe", Component.translatable(kind.translationKey()), pos.getX(), pos.getY(), pos.getZ());
    }

    private static void message(Player player, Component text, ChatFormatting color) {
        player.displayClientMessage(text.copy().withStyle(color), true);
    }
}
