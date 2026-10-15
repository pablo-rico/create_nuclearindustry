package org.papiricoh.create_nuclearindustry.propulsion.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The nozzle at the end of a nuclear engine. {@code FACING} is where the exhaust leaves; the engine
 * lights it while it fires and draws the jet out of its open face (see {@code RocketEngineBlockEntity}).
 */
public class RocketNozzleBlock extends DirectionalBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    /** What comes out of it. */
    public enum Jet {
        /** Hot gas: a white plume with a flame at its root. */
        THERMAL,
        /** Pions steered by the field: a violet and white streak. */
        MAGNETIC
    }

    private final Jet jet;

    public RocketNozzleBlock(Properties properties, Jet jet) {
        super(properties);
        this.jet = jet;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN).setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return simpleCodec(properties -> new RocketNozzleBlock(properties, jet));
    }

    public Jet jet() {
        return jet;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    /** Placed against the engine's last segment, it points away from it. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }
}
