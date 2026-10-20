package org.papiricoh.create_nuclearindustry.missile.defense.battery;

import com.simibubi.create.api.equipment.goggles.IProxyHoveringInformation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.network.chat.Component;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipAssembly;
import org.papiricoh.create_nuclearindustry.missile.defense.DefenseRegistry;

/**
 * One of the seven blocks of an interceptor battery besides its master. {@link #OFFSET} says where it lies from the master, in
 * world axes: bit 0 is +x, bit 1 is +y, bit 2 is +z.
 */
public class InterceptorBatteryPartBlock extends Block implements IProxyHoveringInformation {
    public static final IntegerProperty OFFSET = IntegerProperty.create("offset", 1, 7);

    public InterceptorBatteryPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(OFFSET, 1));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OFFSET);
    }

    /** Goggles on any part read the battery: its master holds everything. */
    @Override
    public BlockPos getInformationSource(Level level, BlockPos pos, BlockState state) {
        return master(pos, state);
    }

    /** Where the master of the part {@code state} at {@code pos} is. */
    public static BlockPos master(BlockPos pos, BlockState state) {
        int offset = state.getValue(OFFSET);
        return pos.offset(-(offset & 1), -((offset >> 1) & 1), -((offset >> 2) & 1));
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!moving && !ShipAssembly.isMoving() && state.getBlock() != next.getBlock()) BatteryBlocks.remove(level, master(pos, state), true);
        super.onRemove(state, level, pos, next, moving);
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos master = master(pos, state);
        if (player.isShiftKeyDown() && level.getBlockEntity(master) instanceof InterceptorBatteryEntity battery) {
            if (!level.isClientSide) {
                ItemStack round = battery.takeReady();
                if (!round.isEmpty() && !player.getInventory().add(round)) player.drop(round, false);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && level.getBlockEntity(master) instanceof InterceptorBatteryEntity battery && ServerPayloadGuard.check(player, master))
            player.openMenu(new SimpleMenuProvider((id, inv, who) -> new InterceptorBatteryMenu(id, inv, battery, master), Component.translatable(DefenseRegistry.BATTERY.get().getDescriptionId())), b -> b.writeBlockPos(master));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(master(pos, state)) instanceof InterceptorBatteryEntity battery && stack.is(DefenseRegistry.INTERCEPTOR.get())) {
            if (level.isClientSide || battery.loadOne(stack)) return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
