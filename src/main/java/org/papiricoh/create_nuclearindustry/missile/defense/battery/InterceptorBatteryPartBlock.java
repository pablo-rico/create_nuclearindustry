package org.papiricoh.create_nuclearindustry.missile.defense.battery;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * One of the seven blocks of an interceptor battery besides its master. {@link #OFFSET} says where it lies from the master, in
 * world axes: bit 0 is +x, bit 1 is +y, bit 2 is +z.
 */
public class InterceptorBatteryPartBlock extends Block {
    public static final IntegerProperty OFFSET = IntegerProperty.create("offset", 1, 7);

    public InterceptorBatteryPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(OFFSET, 1));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OFFSET);
    }

    /** Where the master of the part {@code state} at {@code pos} is. */
    public static BlockPos master(BlockPos pos, BlockState state) {
        int offset = state.getValue(OFFSET);
        return pos.offset(-(offset & 1), -((offset >> 1) & 1), -((offset >> 2) & 1));
    }
}
