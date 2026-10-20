package org.papiricoh.create_nuclearindustry.missile.link;

import org.papiricoh.create_nuclearindustry.missile.defense.battery.InterceptorBatteryEntity;
import org.papiricoh.create_nuclearindustry.missile.defense.battery.InterceptorBatteryPartBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarBlockEntity;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloControllerEntity;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;
import java.util.List;
import java.util.Locale;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearDataComponents;

/**
 * Links missile devices: click a missile radar or a launch controller (or any block of its launch site) to remember it,
 * then click a command table or a radar scope to link or unlink it there. Sneak-click a device to clear its links;
 * sneak-use in the air to forget the stored one.
 */
public class TacticalLinkerItem extends Item {
    public TacticalLinkerItem(Properties properties) {
        super(properties);
    }

    @Nullable
    public static TacticalLink getLink(ItemStack stack) {
        return stack.get(AllNuclearDataComponents.TACTICAL_LINK.get());
    }

    private static final String KEY = "item.create_nuclearindustry.tactical_linker.";

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (level.isClientSide || player == null) return InteractionResult.SUCCESS;
        BlockPos clicked = context.getClickedPos();
        BlockPos centre = org.papiricoh.create_nuclearindustry.missile.holo.HoloTable.centre(clicked, level.getBlockState(clicked));
        if (centre != null) clicked = centre;
        if (level.getBlockState(clicked).getBlock() instanceof InterceptorBatteryPartBlock)
            clicked = InterceptorBatteryPartBlock.master(clicked, level.getBlockState(clicked));
        ItemStack stack = context.getItemInHand();
        BlockEntity entity = level.getBlockEntity(clicked);
        if (entity instanceof TacticalDevice device) {
            if (player.isShiftKeyDown()) { device.clearLinks(); message(player, "unlinked"); return InteractionResult.SUCCESS; }
            TacticalLink stored = getLink(stack);
            if (stored == null) { message(player, "empty"); return InteractionResult.SUCCESS; }
            GlobalPos target = stored.target();
            if (!level.dimension().equals(target.dimension())
                    || !(device instanceof org.papiricoh.create_nuclearindustry.missile.holo.HoloTableBlockEntity table
                        ? table.inLinkRange(target.pos()) : ShipSpace.closerThan(level, clicked, target.pos(), MissileConfig.tacticalLinkRange))) {
                message(player, "out_of_range"); return InteractionResult.SUCCESS;
            }
            message(player, device.toggleLink(target).name().toLowerCase(Locale.ROOT));
            return InteractionResult.SUCCESS;
        }
        if (entity instanceof org.papiricoh.create_nuclearindustry.missile.orbit.GroundStationBlockEntity) {
            stack.set(AllNuclearDataComponents.TACTICAL_LINK.get(), new TacticalLink(GlobalPos.of(level.dimension(), clicked), TacticalLink.Kind.GROUND_STATION));
            message(player, "stored");
            return InteractionResult.SUCCESS;
        }
        if (entity instanceof InterceptorBatteryEntity) {
            stack.set(AllNuclearDataComponents.TACTICAL_LINK.get(), new TacticalLink(GlobalPos.of(level.dimension(), clicked), TacticalLink.Kind.INTERCEPTOR_BATTERY));
            message(player, "stored");
            return InteractionResult.SUCCESS;
        }
        SiloControllerEntity controller = entity instanceof SiloControllerEntity silo ? silo : null;
        if (controller == null && !(entity instanceof RadarBlockEntity)) {
            for (var affected : MultiblockRegistry.affecting(level, clicked)) {
                if (affected instanceof SiloControllerEntity silo) { controller = silo; break; }
            }
        }
        if (controller == null && !(entity instanceof RadarBlockEntity)) { message(player, "unsupported"); return InteractionResult.SUCCESS; }
        TacticalLink link = new TacticalLink(GlobalPos.of(level.dimension(), controller == null ? clicked : controller.getBlockPos()),
                controller == null ? TacticalLink.Kind.RADAR : TacticalLink.Kind.LAUNCH_CONTROLLER);
        stack.set(AllNuclearDataComponents.TACTICAL_LINK.get(), link);
        message(player, "stored");
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) { stack.remove(AllNuclearDataComponents.TACTICAL_LINK.get()); message(player, "cleared"); }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        TacticalLink link = getLink(stack);
        if (link == null) tooltip.add(Component.translatable(KEY + "empty").withStyle(ChatFormatting.GRAY));
        else {
            BlockPos pos = link.target().pos();
            tooltip.add(Component.translatable(KEY + "target", Component.translatable(KEY + "kind." + link.kind().getSerializedName()),
                    pos.getX(), pos.getY(), pos.getZ(), link.target().dimension().location().toString()).withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable(Screen.hasShiftDown() ? KEY + "tooltip" : "silo.create_nuclearindustry.screen.tooltip.expand").withStyle(ChatFormatting.GRAY));
        if (Screen.hasShiftDown()) tooltip.add(Component.translatable("silo.create_nuclearindustry.screen.tooltip.linker_use").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void message(Player player, String suffix) {
        player.displayClientMessage(Component.translatable(KEY + suffix), true);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getLink(stack) != null;
    }
}
