package org.papiricoh.create_nuclearindustry.missile.defense.battery;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.papiricoh.create_nuclearindustry.missile.defense.DefenseRegistry;

/** Shared teardown keeps an accidental break from leaving any invisible multiblock pieces behind. */
final class BatteryBlocks {
    private static boolean removing;
    private BatteryBlocks() {}
    static void remove(Level level, BlockPos master, boolean dropMaster) {
        if (removing || level.isClientSide) return;
        removing = true;
        try {
            if (dropMaster && level.getBlockEntity(master) instanceof InterceptorBatteryEntity battery)
                for (int i = 0; i < battery.tubeInventory().getSlots(); i++)
                    net.minecraft.world.Containers.dropItemStack(level, master.getX(), master.getY(), master.getZ(), battery.tubeInventory().getStackInSlot(i));
            for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
                BlockPos p = master.offset(x, y, z);
                if (level.getBlockState(p).is(DefenseRegistry.BATTERY.get()) || level.getBlockState(p).is(DefenseRegistry.BATTERY_PART.get()))
                    level.destroyBlock(p, dropMaster && p.equals(master));
            }
        } finally { removing = false; }
    }
}
