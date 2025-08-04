package org.papiricoh.create_nuclearindustry.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class NuclearReactorControllerBlock extends Block {
    public static final BooleanProperty ON = BooleanProperty.create("on");

    public NuclearReactorControllerBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(ON, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(ON);
    }
}
