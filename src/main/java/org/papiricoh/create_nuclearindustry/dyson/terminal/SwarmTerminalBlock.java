package org.papiricoh.create_nuclearindustry.dyson.terminal;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.papiricoh.create_nuclearindustry.dyson.DysonRegistry;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;

/** A Dyson swarm terminal: a console, facing whoever placed it, where the swarm's orbits are set and its yield read. */
public class SwarmTerminalBlock extends HorizontalDirectionalBlock implements IBE<SwarmTerminalBlockEntity> {
    public static final MapCodec<SwarmTerminalBlock> CODEC = simpleCodec(SwarmTerminalBlock::new);

    public SwarmTerminalBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SwarmTerminalBlockEntity terminal && ServerPayloadGuard.check(player, pos))
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new SwarmTerminalMenu(id, inventory, terminal, pos),
                    Component.translatable("block.create_nuclearindustry.swarm_terminal")), buf -> buf.writeBlockPos(pos));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public Class<SwarmTerminalBlockEntity> getBlockEntityClass() {
        return SwarmTerminalBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SwarmTerminalBlockEntity> getBlockEntityType() {
        return DysonRegistry.TERMINAL_ENTITY.get();
    }
}
