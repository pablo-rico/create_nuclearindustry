package org.papiricoh.create_nuclearindustry.missile.defense.battery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.papiricoh.create_nuclearindustry.missile.defense.DefenseRegistry;

/**
 * Places a whole interceptor battery, if all eight blocks are free: its 2×2×2 grows from the block placed at, away from the player
 * and to their right, and its turret rests facing the way they look.
 */
public class InterceptorBatteryItem extends BlockItem {
    public InterceptorBatteryItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** The master (lowest corner) of the battery placed at {@code at} by someone looking {@code facing}. */
    public static BlockPos master(BlockPos at, Direction facing) {
        Direction right = facing.getClockWise();
        BlockPos far = at.relative(facing).relative(right);
        return new BlockPos(Math.min(at.getX(), far.getX()), at.getY(), Math.min(at.getZ(), far.getZ()));
    }

    /** Whether a battery fits with its master at {@code master}. */
    public static boolean fits(Level level, BlockPos master, BlockPlaceContext context) {
        for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
            BlockPos pos = master.offset(x, y, z);
            if (level.isOutsideBuildHeight(pos) || !level.getBlockState(pos).canBeReplaced(context)) return false;
        }
        return true;
    }

    /** Puts the master facing {@code facing} and its seven parts at {@code master}. */
    public static void build(Level level, BlockPos master, Direction facing) {
        level.setBlock(master, DefenseRegistry.BATTERY.get().defaultBlockState().setValue(InterceptorBatteryBlock.FACING, facing), Block.UPDATE_ALL);
        for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) if (x + y + z != 0)
            level.setBlock(master.offset(x, y, z), DefenseRegistry.BATTERY_PART.get().defaultBlockState()
                    .setValue(InterceptorBatteryPartBlock.OFFSET, x | y << 1 | z << 2), Block.UPDATE_ALL);
        if (level.getBlockEntity(master) instanceof InterceptorBatteryEntity battery) battery.restFacing(facing);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        Level level = context.getLevel();
        Direction facing = context.getHorizontalDirection();
        BlockPos master = master(context.getClickedPos(), facing);
        if (!fits(level, master, context)) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            build(level, master, facing);
            if (context.getPlayer() != null && level.getBlockEntity(master) instanceof InterceptorBatteryEntity battery)
                battery.setOwner(context.getPlayer().getUUID());
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
