package org.papiricoh.create_nuclearindustry.missile.holo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Forming, reading and breaking up a command table in the world. The geometry is {@link HoloGrid}'s. */
public final class HoloTable {
    private HoloTable() {
    }

    public static HoloPart part(BlockState state) {
        return state.is(HoloRegistry.TABLE.get()) ? state.getValue(HoloTableBlock.PART) : HoloPart.SINGLE;
    }

    /** The centre of the table a formed piece belongs to, or null for a loose piece or anything else. */
    @Nullable
    public static BlockPos centre(BlockPos pos, BlockState state) {
        HoloPart p = part(state);
        if (!p.formed()) return null;
        return pos.offset(-HoloGrid.dx(p.part()), 0, -HoloGrid.dz(p.part()));
    }

    /** Whether all nine pieces around {@code centre} stand, each in its place. */
    public static boolean intact(BlockGetter level, BlockPos centre) {
        for (int i = 0; i < HoloGrid.PARTS; i++) {
            if (part(level.getBlockState(centre.offset(HoloGrid.dx(i), 0, HoloGrid.dz(i)))).part() != i) return false;
        }
        return true;
    }

    /** Fuses the loose pieces around one just placed at {@code pos} into a table if they make a full 3×3; returns its centre or null. */
    @Nullable
    public static BlockPos tryForm(Level level, BlockPos pos) {
        int[] w = HoloGrid.window((x, z) -> {
            BlockState state = level.getBlockState(pos.offset(x, 0, z));
            return state.is(HoloRegistry.TABLE.get()) && part(state) == HoloPart.SINGLE;
        });
        if (w == null) return null;
        BlockPos centre = pos.offset(w[0], 0, w[1]);
        form(level, centre);
        return centre;
    }

    /** Writes the nine formed pieces around {@code centre}, the centre last so its block entity is made once. */
    public static void form(Level level, BlockPos centre) {
        BlockState base = HoloRegistry.TABLE.get().defaultBlockState();
        for (int i = 0; i < HoloGrid.PARTS; i++) {
            if (i == HoloGrid.CENTER) continue;
            level.setBlock(centre.offset(HoloGrid.dx(i), 0, HoloGrid.dz(i)), base.setValue(HoloTableBlock.PART, HoloPart.of(i)), Block.UPDATE_ALL);
        }
        level.setBlock(centre, base.setValue(HoloTableBlock.PART, HoloPart.CENTER), Block.UPDATE_ALL);
    }

    /** Turns every piece still in its place around {@code centre} back into a loose piece. */
    public static void unform(Level level, BlockPos centre) {
        BlockState loose = HoloRegistry.TABLE.get().defaultBlockState();
        for (int i = 0; i < HoloGrid.PARTS; i++) {
            BlockPos at = centre.offset(HoloGrid.dx(i), 0, HoloGrid.dz(i));
            if (part(level.getBlockState(at)).part() == i) level.setBlock(at, loose, Block.UPDATE_ALL);
        }
    }
}
